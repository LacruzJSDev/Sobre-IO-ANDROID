package dev.lacruzjs.sobreio.ui.timezone

/**
 * The IANA name of the device's time zone ("Europe/Madrid"), the default of a new group. Not
 * `TimeZone.currentSystemDefault().id` everywhere: in the browser that id is "SYSTEM", which the group must never store.
 */
expect fun deviceTimeZoneId(): String
