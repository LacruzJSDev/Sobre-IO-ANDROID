package dev.lacruzjs.sobreio.ui.movementform

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_no
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_note
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_title
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_yes
import dev.lacruzjs.sobreio.ui.components.SoftTile
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.SecondaryButton
import dev.lacruzjs.sobreio.ui.format.formatDate
import dev.lacruzjs.sobreio.ui.format.formatMoney
import org.jetbrains.compose.resources.stringResource

/**
 * "Is it <commitment>?": the new movement looks like a pending due date. Yes registers that due date (in its own
 * envelope) instead of a separate movement, so the review never shows it as still to pay; No hides it for this form.
 */
@Composable
internal fun SuggestionCard(
    suggestion: PendingSuggestion,
    currencyCode: String,
    enabled: Boolean,
    onEvent: (MovementFormEvent) -> Unit,
) {
    val languageTag = Locale.current.toLanguageTag()
    SoftTile {
        Text(
            text = stringResource(Res.string.movement_suggestion_title, suggestion.name),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(
                Res.string.movement_suggestion_note,
                formatDate(suggestion.dueDate, languageTag),
                formatMoney(suggestion.amountCents, currencyCode, languageTag),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            SecondaryButton(
                text = stringResource(Res.string.movement_suggestion_no),
                onClick = { onEvent(MovementFormEvent.SuggestionDismissed) },
                enabled = enabled,
                compact = true,
            )
            PrimaryButton(
                text = stringResource(Res.string.movement_suggestion_yes),
                onClick = { onEvent(MovementFormEvent.SuggestionAccepted) },
                enabled = enabled,
                compact = true,
            )
        }
    }
}
