package dev.lacruzjs.sobreio.ui.auth.google

/** Holds the [IosGoogleSignInProvider] that Swift registers at start-up; null leaves Google sign-in hidden. */
internal object IosGoogleSignInRegistry {
    var provider: IosGoogleSignInProvider? = null
}
