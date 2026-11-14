package dev.lacruzjs.sobreio.domain.model

/** A current member of a group, with the name from their profile. */
data class Member(val userId: String, val displayName: String, val role: GroupRole)

/** True when the group has more than one active member: only then do accounts and envelopes ask who holds them. */
fun List<Member>.hasSeveralMembers(): Boolean = size > 1
