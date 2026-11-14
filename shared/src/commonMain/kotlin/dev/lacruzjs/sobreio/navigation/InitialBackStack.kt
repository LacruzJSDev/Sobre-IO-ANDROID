package dev.lacruzjs.sobreio.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Back stack the signed-in app starts with: Groups, and on top of it the last group the user opened, so back from that
 * group lands on Groups. Without a stored group only Groups is shown.
 */
fun initialBackStack(lastGroupId: String?): List<NavKey> =
    if (lastGroupId == null) listOf(Groups) else listOf(Groups, Group(lastGroupId))
