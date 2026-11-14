package dev.lacruzjs.sobreio

import androidx.compose.runtime.Composable
import dev.lacruzjs.sobreio.navigation.NavigationWrapper
import dev.lacruzjs.sobreio.ui.components.SwipeRowsHostProvider
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme

/** Root of the UI, shared by every platform. Each platform hosts it (an Activity, a UIViewController). */
@Composable
fun App() {
    SobreioTheme {
        // An open swipe row closes when anything outside it is touched, anywhere in the app
        SwipeRowsHostProvider { NavigationWrapper() }
    }
}
