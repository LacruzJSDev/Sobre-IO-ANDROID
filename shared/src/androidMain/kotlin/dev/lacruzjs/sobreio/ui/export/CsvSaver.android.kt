package dev.lacruzjs.sobreio.ui.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberCsvSaver(onResult: (SaveResult) -> Unit): (fileName: String, content: String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latest = rememberUpdatedState(onResult)
    // The contents wait here while the dialog is open; the dialog only returns where to write them
    val pending = remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = pending.value
        pending.value = null
        if (uri == null || content == null) {
            latest.value(SaveResult.Cancelled)
        } else {
            scope.launch {
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                            ?: error("No output stream")
                    }.isSuccess
                }
                latest.value(if (written) SaveResult.Saved else SaveResult.Failed)
            }
        }
    }
    return { fileName, content ->
        pending.value = content
        launcher.launch(fileName)
    }
}
