package dev.lacruzjs.sobreio.ui.timezone

import kotlinx.datetime.TimeZone

actual fun deviceTimeZoneId(): String = TimeZone.currentSystemDefault().id
