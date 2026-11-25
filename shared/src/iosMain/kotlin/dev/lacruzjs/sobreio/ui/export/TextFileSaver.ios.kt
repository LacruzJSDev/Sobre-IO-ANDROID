package dev.lacruzjs.sobreio.ui.export

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun rememberTextFileSaver(mimeType: String, onResult: (SaveResult) -> Unit): (fileName: String, content: String) -> Unit {
    val latest = rememberUpdatedState(onResult)
    // The delegate is kept by the composition: UIKit holds it weakly
    val delegate = remember { TextFileSaveDelegate { latest.value(it) } }
    return { fileName, content ->
        val url = writeTemporary(fileName, content)
        if (url == null) {
            latest.value(SaveResult.Failed)
        } else {
            val picker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
                ?: latest.value(SaveResult.Failed)
        }
    }
}

private class TextFileSaveDelegate(private val onResult: (SaveResult) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onResult(if (didPickDocumentsAtURLs.isEmpty()) SaveResult.Cancelled else SaveResult.Saved)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(SaveResult.Cancelled)
    }
}

/** Writes the file into the app's temporary folder, from where the picker copies it; null when it cannot be written. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun writeTemporary(fileName: String, content: String): NSURL? {
    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    @Suppress("CAST_NEVER_SUCCEEDS")
    val text = NSString.create(string = content)
    return if (text.writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)) url else null
}

private fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) controller = controller.presentedViewController
    return controller
}
