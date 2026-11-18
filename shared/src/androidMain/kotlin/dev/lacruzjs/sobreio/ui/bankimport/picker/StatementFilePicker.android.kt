package dev.lacruzjs.sobreio.ui.bankimport.picker

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Banks label CSV files in many ways; text and spreadsheet types are offered too, the reader decides. */
private val statementMimeTypes = arrayOf(
    "text/csv", "text/comma-separated-values", "text/plain", "text/*", "application/csv", "application/vnd.ms-excel",
)

@Composable
actual fun rememberStatementFilePicker(onResult: (PickResult) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            latest.value(PickResult.Cancelled)
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) { read(context, uri) }
                latest.value(result)
            }
        }
    }
    return { launcher.launch(statementMimeTypes) }
}

private fun read(context: Context, uri: Uri): PickResult = runCatching {
    val resolver = context.contentResolver
    var name = "statement.csv"
    var size = -1L
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(0)?.let { name = it }
            if (!cursor.isNull(1)) size = cursor.getLong(1)
        }
    }
    if (size > MAX_STATEMENT_BYTES) return PickResult.TooLarge
    val bytes = resolver.openInputStream(uri)?.use { input ->
        // Read at most one byte past the limit (readNBytes needs API 33)
        val out = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        while (out.size() <= MAX_STATEMENT_BYTES) {
            val n = input.read(chunk)
            if (n < 0) break
            out.write(chunk, 0, n)
        }
        if (out.size() > MAX_STATEMENT_BYTES) return PickResult.TooLarge
        out.toByteArray()
    } ?: return PickResult.Unreadable
    PickResult.Picked(PickedFile(name, bytes))
}.getOrElse { PickResult.Unreadable }
