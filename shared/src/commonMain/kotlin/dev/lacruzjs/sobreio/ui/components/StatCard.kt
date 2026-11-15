package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.ui.theme.LocalMoneyColors
import dev.lacruzjs.sobreio.ui.theme.MoneyRole

/** One line of a [StatCard]: a [label] and its figure, coloured by [role]. [emphasised] makes it the bold result line. */
data class StatRow(
    val label: String,
    val amountCents: Long,
    val role: MoneyRole,
    val emphasised: Boolean = false,
)

/**
 * Several related figures in ONE soft tile: label on the left (muted), value on the right, a few dp between rows and no
 * dividers. A row marked [StatRow.emphasised] (the result) has an ink label, a bigger bold value and a little more space
 * above it. [title] is a small muted caption inside the top of the card (so the screen needs no separate section
 * title) and [warning] a last line in the negative money colour (a shortfall). Same background and shape as `FormTile`.
 */
@Composable
fun StatCard(
    rows: List<StatRow>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    warning: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (row.emphasised && index > 0) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = row.label,
                    style = if (row.emphasised) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    color = if (row.emphasised) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MoneyText(
                    amountCents = row.amountCents,
                    currencyCode = currencyCode,
                    role = row.role,
                    style = if (row.emphasised) {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
        if (warning != null) {
            Text(
                text = warning,
                style = MaterialTheme.typography.bodySmall,
                color = LocalMoneyColors.current.negative,
            )
        }
    }
}
