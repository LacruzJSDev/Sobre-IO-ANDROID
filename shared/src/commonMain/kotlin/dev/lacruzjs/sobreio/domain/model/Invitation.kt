package dev.lacruzjs.sobreio.domain.model

import kotlin.time.Instant

/** A code that lets someone join a group until [expiresAt]. */
data class Invitation(val code: String, val expiresAt: Instant)
