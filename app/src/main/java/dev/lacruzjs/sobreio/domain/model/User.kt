package dev.lacruzjs.sobreio.domain.model

/** The signed-in person. [displayName] is what they typed at sign-up (or the name Google gave us). */
data class User(
    val id: String,
    val email: String?,
    val displayName: String?,
)
