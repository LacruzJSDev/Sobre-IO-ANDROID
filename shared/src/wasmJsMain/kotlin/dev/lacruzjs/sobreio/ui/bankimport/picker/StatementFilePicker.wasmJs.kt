package dev.lacruzjs.sobreio.ui.bankimport.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** A hidden `<input type="file">`; the browser reads the file and hands it over as base64, decoded here. */
@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun rememberStatementFilePicker(onResult: (PickResult) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onResult)
    return remember {
        {
            openFilePicker(
                accept = ".csv,text/csv,text/plain",
                maxBytes = MAX_STATEMENT_BYTES,
                onPicked = { name, base64 ->
                    val bytes = runCatching { Base64.decode(base64) }.getOrNull()
                    latest.value(if (bytes == null) PickResult.Unreadable else PickResult.Picked(PickedFile(name, bytes)))
                },
                onTooLarge = { latest.value(PickResult.TooLarge) },
                onCancel = { latest.value(PickResult.Cancelled) },
                onError = { latest.value(PickResult.Unreadable) },
            )
        }
    }
}

private fun openFilePicker(
    accept: String,
    maxBytes: Int,
    onPicked: (String, String) -> Unit,
    onTooLarge: () -> Unit,
    onCancel: () -> Unit,
    onError: () -> Unit,
): Unit = js(
    """{
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.onchange = () => {
        const file = input.files && input.files[0];
        if (!file) { onCancel(); return; }
        if (file.size > maxBytes) { onTooLarge(); return; }
        const reader = new FileReader();
        reader.onload = () => {
            const url = reader.result;
            onPicked(file.name, url.substring(url.indexOf(',') + 1));
        };
        reader.onerror = () => onError();
        reader.readAsDataURL(file);
    };
    input.oncancel = () => onCancel();
    input.click();
}"""
)
