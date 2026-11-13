package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.ui.auth.google.IosGoogleSignInProvider
import dev.lacruzjs.sobreio.ui.auth.google.IosGoogleSignInRegistry

/**
 * Entry point for Swift (`IosKoinKt.setUpKoin(googleSignIn:)`), called once when the iOS app starts. Not named
 * `initKoin`: Kotlin/Native would export a function starting with `init` as `doInitKoin`.
 *
 * [googleSignIn] is the Swift implementation on top of the GoogleSignIn SDK; with null the Google button stays hidden.
 */
fun setUpKoin(googleSignIn: IosGoogleSignInProvider?) {
    IosGoogleSignInRegistry.provider = googleSignIn
    initKoin()
}
