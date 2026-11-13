package dev.lacruzjs.sobreio.ui.auth.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberGoogleSignInLauncher(webClientId: String): GoogleSignInLauncher? {
    if (webClientId.isBlank()) return null
    // Credential Manager shows its sheet on top of an Activity, which is what the composition context is
    val context = LocalContext.current
    return remember(context, webClientId) {
        val requester = GoogleSignInRequester(webClientId)
        GoogleSignInLauncher { requester.request(context) }
    }
}
