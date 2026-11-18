package dev.lacruzjs.sobreio.ui.bankimport.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberStatementFilePicker(onResult: (PickResult) -> Unit): () -> Unit {
    val latest = rememberUpdatedState(onResult)
    // The delegate is kept by the composition: UIKit holds it weakly
    val delegate = remember { StatementPickerDelegate { latest.value(it) } }
    return {
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeCommaSeparatedText, UTTypePlainText, UTTypeText),
            asCopy = true,
        )
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class StatementPickerDelegate(private val onResult: (PickResult) -> Unit) :
    NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        onResult(if (url == null) PickResult.Cancelled else read(url))
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(PickResult.Cancelled)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun read(url: NSURL): PickResult {
    val data = NSData.dataWithContentsOfURL(url) ?: return PickResult.Unreadable
    val size = data.length.toInt()
    if (size > MAX_STATEMENT_BYTES) return PickResult.TooLarge
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return PickResult.Picked(PickedFile(url.lastPathComponent ?: "statement.csv", bytes))
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
