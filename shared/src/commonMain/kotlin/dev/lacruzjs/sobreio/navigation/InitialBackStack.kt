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

/**
 * Whether this key is shown in a detail or extra pane of the list-detail scene (see `AppNavigation`): an account,
 * envelope, group settings, review or profile detail, and the lower levels that open above them. A list pane (Group,
 * Groups) and the full-screen forms are not.
 */
fun NavKey.isPaneDetail(): Boolean = when (this) {
    is AccountDetail, is EnvelopeDetail, is EnvelopeTransfers, is GroupSettings, is GroupReview, Profile,
    is CommitmentDetail, is AccountMovements, is EnvelopeMovements, is Tags,
    -> true
    else -> false
}

/**
 * Opens [detail] in the detail pane: whatever detail (and extra pane) the list already has open is replaced, so choosing
 * another account or envelope beside an open one never stacks details. On a phone the list is only on top of the stack
 * when no detail is open, so this is a plain push there.
 */
fun MutableList<NavKey>.openDetail(detail: NavKey) {
    closeDetails()
    add(detail)
}

/**
 * Closes whatever detail (and extra pane) is open beside the list, leaving the list alone: changing the group's tab, since
 * an account's detail makes no sense beside the envelopes. On a phone the list is only on top when nothing is open, so
 * this does nothing there.
 */
fun MutableList<NavKey>.closeDetails() {
    while (size > 1 && last().isPaneDetail()) removeAt(lastIndex)
}
