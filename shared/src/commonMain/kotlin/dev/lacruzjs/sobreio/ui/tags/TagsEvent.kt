package dev.lacruzjs.sobreio.ui.tags

import dev.lacruzjs.sobreio.ui.theme.TagColor

sealed interface TagsEvent {
    /** Handled by the Route, which owns the back stack. */
    data object BackClicked : TagsEvent

    /** A tap on a tag: the Route opens its movements with their totals. */
    data class OpenClicked(val tagId: String) : TagsEvent
    data object Retry : TagsEvent

    /** The "+" of the header and the Edit swipe action: both open the same sheet. */
    data object AddClicked : TagsEvent
    data class EditClicked(val tagId: String) : TagsEvent
    data class NameChanged(val value: String) : TagsEvent
    data class ColorSelected(val color: TagColor) : TagsEvent
    data object SaveClicked : TagsEvent

    data class MergeClicked(val tagId: String) : TagsEvent
    data class MergeTargetSelected(val tagId: String?) : TagsEvent
    data object MergeConfirmed : TagsEvent

    data class DeleteClicked(val tagId: String) : TagsEvent
    data class ReopenClicked(val tagId: String) : TagsEvent

    data object SheetDismissed : TagsEvent
    data object UndoClicked : TagsEvent
    data object UndoExpired : TagsEvent
}
