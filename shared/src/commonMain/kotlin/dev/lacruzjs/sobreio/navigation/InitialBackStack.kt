package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Back stack the signed-in app starts with: the last group the user opened, alone, or Groups when there is none. A group
 * is a root, not a page above Groups: back from it never lands on Groups, which only opens from the group's menu button.
 */
fun initialBackStack(lastGroupId: String?): List<NavKey> =
    if (lastGroupId == null) listOf(Groups) else listOf(Group(lastGroupId))

/** Opens [groupId] as the new root: whatever was on the stack (Groups, another group) goes away. */
fun MutableList<NavKey>.openGroup(groupId: String) {
    clear()
    add(Group(groupId))
}

/**
 * Only Groups is left, as the root: the group's menu button, or a group that is gone (deleted, left, no longer a
 * member). Groups has no back: one moves on from it by picking a group. It stays a screen of its own, so a wide layout
 * can later show it as a pane beside the group.
 */
fun MutableList<NavKey>.showOnlyGroups() {
    clear()
    add(Groups)
}

/** Back one screen; the root (a group or Groups) stays, so back from it leaves the app instead. */
fun MutableList<NavKey>.popToPrevious() {
    if (size > 1) removeAt(lastIndex)
}
