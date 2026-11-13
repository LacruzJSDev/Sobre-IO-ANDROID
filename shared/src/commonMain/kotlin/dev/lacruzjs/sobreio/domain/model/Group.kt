package dev.lacruzjs.sobreio.domain.model

/** A group of people who share accounts and envelopes. [currencyCode] is the ISO 4217 code and never changes. */
data class Group(
    val id: String,
    val name: String,
    val currencyCode: String,
)
