package dev.lacruzjs.sobreio.ui.movementform

import dev.lacruzjs.sobreio.domain.model.EntryKind

/**
 * What a screen that opens the movement form already knows (the Group tab's transfer suggestions): the kind, the accounts
 * and the amount. Only applied when creating; the user reviews everything and saves, the date is the group's today.
 */
data class MovementPrefill(
    val kind: EntryKind,
    val fromAccountId: String?,
    val toAccountId: String?,
    val amountCents: Long?,
) {
    companion object {
        /** The prefill described by the route's optional fields, or null when there is none (or its kind is unknown). */
        fun of(kind: String?, fromAccountId: String?, toAccountId: String?, amountCents: Long?): MovementPrefill? {
            val entryKind = EntryKind.entries.firstOrNull { it.name == kind } ?: return null
            return MovementPrefill(entryKind, fromAccountId, toAccountId, amountCents)
        }
    }
}
