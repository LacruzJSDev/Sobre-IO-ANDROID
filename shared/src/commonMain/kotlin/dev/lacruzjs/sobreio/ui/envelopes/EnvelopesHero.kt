package dev.lacruzjs.sobreio.ui.envelopes

import dev.lacruzjs.sobreio.generated.resources.info_free_to_distribute_3
import dev.lacruzjs.sobreio.generated.resources.info_free_to_distribute_2
import dev.lacruzjs.sobreio.generated.resources.info_free_to_distribute_1
import dev.lacruzjs.sobreio.ui.components.HeroLabelWithInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_free
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_open
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_row_accounts
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_row_due
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_row_later
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_row_other
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_row_overspent
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_to_assign
import dev.lacruzjs.sobreio.generated.resources.envelopes_hero_uncovered
import dev.lacruzjs.sobreio.ui.components.HeroCard
import dev.lacruzjs.sobreio.ui.components.HeroFigure
import dev.lacruzjs.sobreio.ui.components.HeroLabel
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.format.formatMoney
import dev.lacruzjs.sobreio.ui.format.formatShortDate
import dev.lacruzjs.sobreio.ui.theme.HeroLabelAlpha
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * The footer of the Envelopes hero card: what happens next with "Unassigned". Only a choice among database figures.
 */
internal sealed interface EnvelopesHeroLine {
    /** Negative, and expected income ([incomeCents], from the database) brings it back on [on]. */
    data class Covered(val incomeCents: Long, val on: LocalDate) : EnvelopesHeroLine

    /** Negative, and no expected income brings it back. */
    data object Uncovered : EnvelopesHeroLine

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
    val income = summary.coveringIncomeCents
    return if (on != null && income != null) EnvelopesHeroLine.Covered(income, on) else EnvelopesHeroLine.Uncovered
}

/**
 * The hero card of the Envelopes tab: the money still free to distribute ("Unassigned"), the database figures that make
 * it (accounts, minus what is spoken for, plus envelopes below zero) as rows, and a footer with what happens next. The
 * minus and plus signs are display only. The whole card opens the "Unassigned" detail.
 */
@Composable
internal fun EnvelopesHero(state: EnvelopesUiState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val summary = state.summary ?: return
    val ink = LocalHeroColors.current.onContainer
    val languageTag = Locale.current.toLanguageTag()
    val currentYear = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
    val currency = state.currencyCode
    val openLabel = stringResource(Res.string.envelopes_hero_open)
    val line = envelopesHeroLine(summary)
    HeroCard(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onClick),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HeroLabelWithInfo(
                text = stringResource(Res.string.envelopes_hero_free),
                infoTitle = stringResource(Res.string.envelopes_hero_free),
                paragraphs = listOf(Res.string.info_free_to_distribute_1, Res.string.info_free_to_distribute_2, Res.string.info_free_to_distribute_3),
            )
            HeroFigure(amountCents = summary.unassignedCents, currencyCode = currency)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            SumRow(stringResource(Res.string.envelopes_hero_row_accounts), summary.inEnvelopesTotalCents, currency, ink)
            // The minus is a display sign: the database figures are positive
            if (summary.dueThisPeriodCents > 0) {
                SumRow(stringResource(Res.string.envelopes_hero_row_due), -summary.dueThisPeriodCents, currency, ink)
            }
            if (summary.reservedLaterCents > 0) {
                SumRow(stringResource(Res.string.envelopes_hero_row_later), -summary.reservedLaterCents, currency, ink)
            }
            if (summary.otherAssignedCents > 0) {
                SumRow(stringResource(Res.string.envelopes_hero_row_other), -summary.otherAssignedCents, currency, ink)
            }
            if (summary.overspentCents > 0) {
                SumRow(stringResource(Res.string.envelopes_hero_row_overspent), summary.overspentCents, currency, ink, showPlus = true)
            }
        }
        val footer = when (line) {
            is EnvelopesHeroLine.Covered -> stringResource(
                Res.string.envelopes_hero_covered,
                formatMoney(line.incomeCents, currency, languageTag),
                formatShortDate(line.on, languageTag, currentYear),
            )
            EnvelopesHeroLine.Uncovered -> stringResource(Res.string.envelopes_hero_uncovered)
            EnvelopesHeroLine.ToAssign -> stringResource(Res.string.envelopes_hero_to_assign)
            EnvelopesHeroLine.AllAssigned -> stringResource(Res.string.envelopes_hero_all_assigned)
        }
        val icon = when (line) {
            is EnvelopesHeroLine.Covered -> Icons.Outlined.Event
            EnvelopesHeroLine.Uncovered -> Icons.Outlined.Info
            else -> null
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink.copy(alpha = HeroLabelAlpha),
                    modifier = Modifier.size(14.dp),
                )
            }
            HeroLabel(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A row of the sum: the label on the left, the database figure on the right (with an explicit "+" when [showPlus]). */
@Composable
private fun SumRow(label: String, amountCents: Long, currencyCode: String, ink: Color, showPlus: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HeroLabel(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (showPlus) {
            Text(text = "+", style = MaterialTheme.typography.titleSmall, color = ink, maxLines = 1)
        }
        MoneyText(
            amountCents = amountCents,
            currencyCode = currencyCode,
            role = MoneyRole.Plain,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            ink = ink,
        )
    }
}
