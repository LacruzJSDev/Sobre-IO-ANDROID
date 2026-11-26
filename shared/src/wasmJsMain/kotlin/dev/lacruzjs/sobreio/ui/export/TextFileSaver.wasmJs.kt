package dev.lacruzjs.sobreio.ui.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** The browser has no save dialog to wait for: the file goes to the downloads folder at once. */
@Composable
actual fun rememberTextFileSaver(mimeType: String, onResult: (SaveResult) -> Unit): (fileName: String, content: String) -> Unit {
    val latest = rememberUpdatedState(onResult)
    return remember(mimeType) {
        { fileName, content ->
            val saved = runCatching { downloadText(fileName, content, mimeType) }.isSuccess
            latest.value(if (saved) SaveResult.Saved else SaveResult.Failed)
        }
    }
}

private fun downloadText(fileName: String, content: String, mimeType: String): Unit = js(
    """{
    const url = URL.createObjectURL(new Blob([content], { type: mimeType + ';charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
}"""
)
