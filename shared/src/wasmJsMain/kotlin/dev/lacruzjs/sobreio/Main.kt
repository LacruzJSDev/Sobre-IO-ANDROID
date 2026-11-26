package dev.lacruzjs.sobreio

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.lacruzjs.sobreio.di.initKoin
import kotlinx.browser.document

/** Loads the IANA time zone database into kotlinx-datetime, so `TimeZone.of(group.timezone)` works in the browser. */
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val timeZones = JsJodaTimeZoneModule

/** The web app: the same [App] as on the phone, drawn into the page body (`index.html`). */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    timeZones
    initKoin()
    ComposeViewport(document.body!!) { App() }
}
