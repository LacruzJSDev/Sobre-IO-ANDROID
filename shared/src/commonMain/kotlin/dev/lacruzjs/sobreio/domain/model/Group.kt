package dev.lacruzjs.sobreio.domain.model

/**
 * A group of people who share accounts and envelopes. [currencyCode] is the ISO 4217 code and never changes.
 * [role] is the signed-in user's role in it.
 */
data class Group(
    val id: String,
    val name: String,
    val currencyCode: String,
    val role: GroupRole = GroupRole.Member,
) {
    val canEdit: Boolean get() = role == GroupRole.Owner || role == GroupRole.Admin
    val isOwner: Boolean get() = role == GroupRole.Owner
}
