package dev.lacruzjs.sobreio.ui.movementform

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_no
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_note
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_title
import dev.lacruzjs.sobreio.generated.resources.movement_suggestion_yes
import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.NoticeTone
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
    InlineNotice(
        title = stringResource(Res.string.movement_suggestion_title, suggestion.name),
        text = stringResource(
            Res.string.movement_suggestion_note,
            formatDate(suggestion.dueDate, languageTag),
            formatMoney(suggestion.amountCents, currencyCode, languageTag),
        ),
        tone = NoticeTone.Suggestion,
        actions = {
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
        },
    )
}
