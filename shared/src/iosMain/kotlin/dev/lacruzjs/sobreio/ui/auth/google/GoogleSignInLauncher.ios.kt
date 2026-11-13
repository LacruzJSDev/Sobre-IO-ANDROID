package dev.lacruzjs.sobreio.ui.auth.google

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Google sign-in runs in the GoogleSignIn iOS SDK through the Swift [IosGoogleSignInProvider]. The SDK reads its own
 * client id from Info.plist, so [webClientId] only serves as the "Google sign-in is configured" switch here.
 */
@Composable
actual fun rememberGoogleSignInLauncher(webClientId: String): GoogleSignInLauncher? {
    if (webClientId.isBlank()) return null
    val provider = IosGoogleSignInRegistry.provider ?: return null
    return remember(provider) {
        val requester = IosGoogleSignInRequester(provider)
        GoogleSignInLauncher { requester.request() }
    }
}
