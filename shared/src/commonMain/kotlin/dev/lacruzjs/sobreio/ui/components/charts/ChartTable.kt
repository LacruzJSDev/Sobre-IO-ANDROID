package dev.lacruzjs.sobreio.ui.components.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.ui.components.MoneyText
import dev.lacruzjs.sobreio.ui.theme.MoneyRole

/** One line of a [ChartTable]: its label (a day, a period) and one figure per column, as the database gave them. */
data class ChartTableRow(val label: String, val values: List<Long>)

/**
 * The figures behind a zoomed chart: a header of [columns] and one row per point or period, each figure a [MoneyText]
 * of [role]. Nothing is computed here; it shows the same rows the chart draws.
 */
@Composable
fun ChartTable(columns: List<String>, rows: List<ChartTableRow>, currencyCode: String, role: MoneyRole, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("", modifier = Modifier.weight(1.2f))
            columns.forEach {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f))
                row.values.forEach { cents ->
                    MoneyText(
                        amountCents = cents,
                        currencyCode = currencyCode,
                        role = role,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
