package dev.lacruzjs.sobreio.ui.auth.google

import androidx.compose.runtime.Composable

/**
 * Asks the platform for a Google ID token. It is a platform picker, like a file picker, so the Route calls it and
 * passes the token on as an event; ViewModels never see it.
 */
fun interface GoogleSignInLauncher {

    /** Fails with [GoogleSignInCancelledException] when the user dismisses the picker. */
    suspend fun request(): Result<GoogleIdToken>
}

/**
 * The launcher of this platform, or null when Google sign-in is not available: no [webClientId] configured, or a
 * platform without an implementation yet (iOS). [webClientId] is the Google *web* client id: Supabase checks the
 * token against it, not the Android client.
 */
@Composable
expect fun rememberGoogleSignInLauncher(webClientId: String): GoogleSignInLauncher?
