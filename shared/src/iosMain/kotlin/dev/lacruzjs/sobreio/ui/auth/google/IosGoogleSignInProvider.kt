package dev.lacruzjs.sobreio.ui.auth.google

/**
 * Implemented in Swift (`iosApp/GoogleSignInProvider.swift`) on top of the GoogleSignIn iOS SDK, which Kotlin does not
 * link. The app passes it to `setUpKoin` at start-up.
 */
interface IosGoogleSignInProvider {

    /**
     * Shows the Google sign-in screen, using [hashedNonce] (SHA-256 hex) as the token nonce, and calls [onResult] once:
     * with the ID token on success; with a null token and [cancelled] true when the user dismissed the screen; with a
     * null token and [cancelled] false on any other error.
     */
    fun signIn(hashedNonce: String, onResult: (idToken: String?, cancelled: Boolean) -> Unit)
}
