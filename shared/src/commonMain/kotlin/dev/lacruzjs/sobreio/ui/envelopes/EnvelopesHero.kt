package dev.lacruzjs.sobreio.ui.envelopes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.EnvelopeSummary
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_all_assigned
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_covered
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_due
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_free
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_in_envelopes
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_later
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_later_covered
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_later_uncovered
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_open
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_other
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_overspent
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_short
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_to_assign
import dev.lacruzjs.sobreio.ui.components.HeroCard
import dev.lacruzjs.sobreio.ui.components.HeroFigure
import dev.lacruzjs.sobreio.ui.components.HeroLabel
import dev.lacruzjs.sobreio.ui.components.HeroSegmentAlphas
import dev.lacruzjs.sobreio.ui.components.HeroSegmentBar
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * The sentence under the figure of the Envelopes hero card: it says why "Unassigned" is what it is. Only a choice among
 * database figures; nothing is computed except the sign flip of [Short].
 */
internal sealed interface EnvelopesHeroLine {
    /** Negative, with money set aside for later payments and an expected income that brings it back. */
    data class LaterCovered(val laterCents: Long, val incomeName: String, val on: LocalDate) : EnvelopesHeroLine

    /** Negative, with money set aside for later payments and no expected income that covers it. */
    data class LaterUncovered(val laterCents: Long) : EnvelopesHeroLine

    /** Negative, and an expected income brings it back. */
    data class Covered(val incomeName: String, val on: LocalDate) : EnvelopesHeroLine

    /** Negative, with nothing to explain it: [deficitCents] is more set aside than there is (positive). */
    data class Short(val deficitCents: Long) : EnvelopesHeroLine

    /** Positive: money that is not in any envelope yet. */
    data object ToAssign : EnvelopesHeroLine

    /** Zero: everything is in envelopes. */
    data object AllAssigned : EnvelopesHeroLine
}

internal fun envelopesHeroLine(summary: EnvelopeSummary): EnvelopesHeroLine {
    val unassigned = summary.unassignedCents
    if (unassigned > 0) return EnvelopesHeroLine.ToAssign
    if (unassigned == 0L) return EnvelopesHeroLine.AllAssigned
    val on = summary.coveredOn
    val by = summary.coveredBy
    return when {
        summary.reservedLaterCents > 0 && on != null && by != null ->
            EnvelopesHeroLine.LaterCovered(summary.reservedLaterCents, by, on)
        summary.reservedLaterCents > 0 -> EnvelopesHeroLine.LaterUncovered(summary.reservedLaterCents)
        on != null && by != null -> EnvelopesHeroLine.Covered(by, on)
        else -> EnvelopesHeroLine.Short(-unassigned)
    }
}

/**
 * The share of each of the three positive parts of the envelope money (this period, later, the rest) in their sum, for
 * the segment bar: a display ratio of database figures. Null when their sum is 0.
 */
internal fun heroSegmentFractions(dueCents: Long, laterCents: Long, otherCents: Long): List<Float>? {
    val total = dueCents.toDouble() + laterCents.toDouble() + otherCents.toDouble()
    if (total <= 0.0) return null
    return listOf(dueCents, laterCents, otherCents).map { (it.coerceAtLeast(0L).toDouble() / total).toFloat() }
}

/**
 * The hero card of the Envelopes tab: the money still free to distribute ("Unassigned"), a sentence that explains it, a
 * bar of where the envelope money is and its legend. The whole card opens the "Unassigned" detail.
 */
@Composable
internal fun EnvelopesHero(state: EnvelopesUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val summary = state.summary ?: return
    val hero = LocalHeroColors.current
    val ink = hero.onContainer
    val languageTag = Locale.current.toLanguageTag()
    val currentYear = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
    val currency = state.currencyCode
    val openLabel = stringResource(Res.string.envelopes_hero_open)
    val line = envelopesHeroLine(summary)
    val money = { cents: Long -> formatMoney(cents, currency, languageTag) }
    val day = { date: LocalDate -> formatShortDate(date, languageTag, currentYear) }
    val sentence = when (line) {
        is EnvelopesHeroLine.LaterCovered -> stringResource(
            Res.string.envelopes_hero_later_covered, money(line.laterCents), line.incomeName, day(line.on),
        )
        is EnvelopesHeroLine.LaterUncovered -> stringResource(Res.string.envelopes_hero_later_uncovered, money(line.laterCents))
        is EnvelopesHeroLine.Covered -> stringResource(Res.string.envelopes_hero_covered, line.incomeName, day(line.on))
        is EnvelopesHeroLine.Short -> stringResource(Res.string.envelopes_hero_short, money(line.deficitCents))
        EnvelopesHeroLine.ToAssign -> stringResource(Res.string.envelopes_hero_to_assign)
        EnvelopesHeroLine.AllAssigned -> stringResource(Res.string.envelopes_hero_all_assigned)
    }
    val fractions = heroSegmentFractions(summary.dueThisPeriodCents, summary.reservedLaterCents, summary.otherAssignedCents)
    HeroCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroLabel(stringResource(Res.string.envelopes_hero_free), modifier = Modifier.weight(1f).alignByBaseline())
                HeroLabel(stringResource(Res.string.envelopes_hero_in_envelopes), modifier = Modifier.alignByBaseline())
                MoneyText(
                    amountCents = summary.inEnvelopesTotalCents,
                    currencyCode = currency,
                    role = MoneyRole.Plain,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    ink = ink,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            HeroFigure(amountCents = summary.unassignedCents, currencyCode = currency)
            HeroLabel(
                text = sentence,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (fractions != null) HeroSegmentBar(fractions)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val parts = listOf(
                    stringResource(Res.string.envelopes_hero_due) to summary.dueThisPeriodCents,
                    stringResource(Res.string.envelopes_hero_later) to summary.reservedLaterCents,
                    stringResource(Res.string.envelopes_hero_other) to summary.otherAssignedCents,
                )
                parts.forEachIndexed { index, (label, cents) ->
                    if (cents != 0L) HeroLegendItem(label, cents, currency, ink, ink.copy(alpha = HeroSegmentAlphas[index]))
                }
                if (summary.overspentCents > 0) {
                    HeroLegendItem(
                        label = stringResource(Res.string.envelopes_hero_overspent),
                        amountCents = summary.overspentCents,
                        currencyCode = currency,
                        ink = hero.negativeOnContainer,
                        dot = hero.negativeOnContainer,
                    )
                }
            }
        }
    }
}

/** A dot in the colour of its segment, its name and its figure. */
@Composable
private fun HeroLegendItem(label: String, amountCents: Long, currencyCode: String, ink: Color, dot: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dot))
            HeroLabel(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        MoneyText(
            amountCents = amountCents,
            currencyCode = currencyCode,
            role = MoneyRole.Plain,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            ink = ink,
            modifier = Modifier.padding(start = 13.dp),
        )
    }
}
