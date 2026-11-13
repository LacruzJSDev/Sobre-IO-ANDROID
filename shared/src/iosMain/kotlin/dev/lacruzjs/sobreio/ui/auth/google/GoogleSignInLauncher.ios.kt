package dev.lacruzjs.sobreio.ui.auth.google

import androidx.compose.runtime.Composable

// Needs the GoogleSignIn iOS SDK; until then the button is hidden on iOS
@Composable
actual fun rememberGoogleSignInLauncher(webClientId: String): GoogleSignInLauncher? = null
