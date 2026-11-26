package dev.lacruzjs.sobreio.ui.auth.google

import androidx.compose.runtime.Composable

/**
 * Not on the web yet: there is no native ID token in a browser, so Google needs Supabase's OAuth redirect flow
 * (plan 1.1, phase 6, point 3). Until then the button is hidden and email sign-in works as on the phone.
 */
@Composable
actual fun rememberGoogleSignInLauncher(webClientId: String): GoogleSignInLauncher? = null
