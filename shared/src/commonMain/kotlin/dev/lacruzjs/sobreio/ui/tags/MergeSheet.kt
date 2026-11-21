package dev.lacruzjs.sobreio.ui.tags

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_merge
import dev.lacruzjs.sobreio.generated.resources.field_tag_merge_into
import dev.lacruzjs.sobreio.generated.resources.tag_merge_note
import dev.lacruzjs.sobreio.generated.resources.tag_merge_title
import dev.lacruzjs.sobreio.generated.resources.tags_movements_count
import dev.lacruzjs.sobreio.ui.components.InlineNotice
import dev.lacruzjs.sobreio.ui.components.NoticeTone
import dev.lacruzjs.sobreio.ui.components.EntityOption
import dev.lacruzjs.sobreio.ui.components.EntitySelect
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Sheet that folds [TagsUiState.mergeSource] into another live tag. The movements of the source move to the chosen tag and
 * the source is archived; it cannot be undone, so the sheet says so before the button.
 */
@Composable
internal fun MergeSheet(state: TagsUiState, onEvent: (TagsEvent) -> Unit) {
    val source = state.mergeSource ?: return
    FlatBottomSheet(
        title = stringResource(Res.string.tag_merge_title, source.name),
        onDismiss = { onEvent(TagsEvent.SheetDismissed) },
    ) {
        var open by rememberSaveable { mutableStateOf(false) }
        EntitySelect(
            insideSheet = true,
            label = stringResource(Res.string.field_tag_merge_into),
            options = state.mergeCandidates.map { tag ->
                val count = tag.movements.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                EntityOption(
                    id = tag.id,
                    name = tag.name,
                    trailing = pluralStringResource(Res.plurals.tags_movements_count, count, count),
                )
            },
            selectedId = state.mergeTargetId,
            onSelect = { onEvent(TagsEvent.MergeTargetSelected(it)) },
            expanded = open,
            onToggle = { open = !open },
            enabled = !state.isSubmitting,
            error = state.mergeTargetError,
        )
        InlineNotice(stringResource(Res.string.tag_merge_note, source.name), tone = NoticeTone.Warning)
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_merge),
            onClick = { onEvent(TagsEvent.MergeConfirmed) },
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
