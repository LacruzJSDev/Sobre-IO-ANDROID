package dev.lacruzjs.sobreio.ui.timezone

/** The browser knows the IANA name through Intl; kotlinx-datetime only sees "SYSTEM" there. UTC if it says nothing. */
actual fun deviceTimeZoneId(): String = browserTimeZone() ?: "UTC"

private fun browserTimeZone(): String? = js("Intl.DateTimeFormat().resolvedOptions().timeZone || null")
