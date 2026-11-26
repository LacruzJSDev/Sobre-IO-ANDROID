package dev.lacruzjs.sobreio.ui.export

import androidx.compose.runtime.Composable

/** What came back from the system's save dialog. */
enum class SaveResult { Saved, Cancelled, Failed }

/**
 * Hands a text file to the system so the user chooses where it goes: Android's "Save as" document dialog, iOS's
 * document picker for exporting (Files, iCloud Drive, another app). It is a platform API, so only a Route holds it.
 * [mimeType] tells Android what kind of file it is (`text/csv`, `application/json`); iOS goes by the file name's
 * extension. Returns the function that opens it with a file name and its contents.
 */
@Composable
expect fun rememberTextFileSaver(mimeType: String, onResult: (SaveResult) -> Unit): (fileName: String, content: String) -> Unit
