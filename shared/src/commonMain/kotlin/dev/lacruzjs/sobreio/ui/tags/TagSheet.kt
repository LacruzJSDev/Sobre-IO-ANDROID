package dev.lacruzjs.sobreio.ui.tags

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_save
import dev.lacruzjs.sobreio.generated.resources.field_tag_color
import dev.lacruzjs.sobreio.generated.resources.field_tag_name
import dev.lacruzjs.sobreio.generated.resources.tag_edit_title
import dev.lacruzjs.sobreio.generated.resources.tag_new_title
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormNote
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.components.TagColorPicker
import org.jetbrains.compose.resources.stringResource

/** Short sheet to create or rename a tag and pick its colour. A duplicate name shows under the field. */
@Composable
internal fun TagSheet(state: TagsUiState, onEvent: (TagsEvent) -> Unit) {
    FlatBottomSheet(
        title = stringResource(if (state.editingId == null) Res.string.tag_new_title else Res.string.tag_edit_title),
        onDismiss = { onEvent(TagsEvent.SheetDismissed) },
    ) {
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(TagsEvent.NameChanged(it)) },
            label = stringResource(Res.string.field_tag_name),
            capitalization = KeyboardCapitalization.Sentences,
            errorRes = state.nameError,
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(TagsEvent.SaveClicked) },
        )
        FormNote(stringResource(Res.string.field_tag_color))
        TagColorPicker(
            selected = state.color,
            onSelect = { onEvent(TagsEvent.ColorSelected(it)) },
            enabled = !state.isSubmitting,
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_save),
            onClick = { onEvent(TagsEvent.SaveClicked) },
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
