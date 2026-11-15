package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.hero_below_zero
import dev.lacruzjs.sobreio.ui.theme.HeroLabelAlpha
import dev.lacruzjs.sobreio.ui.theme.LocalHeroColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole
import dev.lacruzjs.sobreio.ui.theme.heroContainer
import dev.lacruzjs.sobreio.ui.theme.onHeroContainer
import dev.lacruzjs.sobreio.ui.theme.showsBelowZeroChip
import org.jetbrains.compose.resources.stringResource

/** Height of every hero card, so the Group and Accounts tabs match. It fits the Group tab, the densest content. */
val HeroCardHeight = 232.dp

/**
 * The card that holds the main figure of a tab (Group: the really free money; Accounts: the total in accounts): the
 * dark `heroContainer` with `onHeroContainer` text, [HeroCardHeight] high, `SobreioShapes.large` corners. The content
 * is spread from top to bottom. Use [HeroLabel] for labels, [HeroFigure] for the big figure and [HeroSplitBar] for ratios.
 */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroCardHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.heroContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

/** A label on a hero card: the card's text colour at 85 % alpha. */
@Composable
fun HeroLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text = text,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        color = MaterialTheme.colorScheme.onHeroContainer.copy(alpha = HeroLabelAlpha),
        modifier = modifier,
    )
}

/**
 * The big figure of a hero card. It is always `onHeroContainer` (a [MoneyRole.Plain] figure, so the colour logic stays
 * in `moneyColorFor`); when it is negative a small "Below zero" chip, light with the negative colour, sits next to it
 * (or under it when there is no room). Any other figure on the card is a plain `MoneyText` with `ink = onHeroContainer`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeroFigure(
    amountCents: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
) {
    val colors = LocalHeroColors.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MoneyText(
            amountCents = amountCents,
            currencyCode = currencyCode,
            role = MoneyRole.Plain,
            style = style,
            maxLines = 1,
            ink = colors.onContainer,
        )
        if (showsBelowZeroChip(amountCents)) {
            Text(
                text = stringResource(Res.string.hero_below_zero),
                style = MaterialTheme.typography.labelMedium,
                color = colors.chipContent,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.chipContainer)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * A thin two-part bar on a hero card: [fraction] (0..1) is filled in the card's text colour over a faint track. A display
 * ratio of two database figures, never a figure itself.
 */
@Composable
fun HeroSplitBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 4.dp) {
    val ink = MaterialTheme.colorScheme.onHeroContainer
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(ink.copy(alpha = 0.2f))) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(ink))
    }
}

/** The alphas of the segments of a [HeroSegmentBar] and of the legend dots that name them, darkest first. */
val HeroSegmentAlphas: List<Float> = listOf(1f, 0.55f, 0.28f)

/**
 * A thin bar on a hero card split in segments side by side, each in the card's text colour at the alpha of its position
 * in [HeroSegmentAlphas]. [fractions] are display ratios (0..1) of the same total; empty ones are skipped and the others
 * are 2 dp apart. The whole bar has rounded ends over a faint track.
 */
@Composable
fun HeroSegmentBar(fractions: List<Float>, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val ink = MaterialTheme.colorScheme.onHeroContainer
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(ink.copy(alpha = 0.12f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        fractions.forEachIndexed { index, fraction ->
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(ink.copy(alpha = HeroSegmentAlphas.getOrElse(index) { HeroSegmentAlphas.last() })),
                )
            }
        }
    }
}
