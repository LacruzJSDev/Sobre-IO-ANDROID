package dev.lacruzjs.sobreio.domain.model

/** A current member of a group, with the name from their profile. */
data class Member(val userId: String, val displayName: String, val role: GroupRole)
