package dev.lacruzjs.sobreio.ui.components

import androidx.compose.ui.platform.ClipEntry

/** A clipboard entry holding [text] as plain text, for `LocalClipboard.current.setClipEntry`. Each platform builds it its own way. */
expect fun plainTextClipEntry(text: String): ClipEntry
