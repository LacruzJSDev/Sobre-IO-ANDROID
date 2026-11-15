package dev.lacruzjs.sobreio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.data.local.AppPreferencesStore
import dev.lacruzjs.sobreio.navigation.NavigationWrapper
import dev.lacruzjs.sobreio.ui.components.SwipeRowsHostProvider
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.koin.compose.koinInject

/** Root of the UI, shared by every platform. Each platform hosts it (an Activity, a UIViewController). */
@Composable
fun App() {
    val preferences = koinInject<AppPreferencesStore>()
    val colorBlindSafe by preferences.colorBlindSafe.collectAsStateWithLifecycle()
    SobreioTheme(colorBlindSafe = colorBlindSafe) {
        // An open swipe row closes when anything outside it is touched, anywhere in the app
        SwipeRowsHostProvider { NavigationWrapper() }
    }
}
