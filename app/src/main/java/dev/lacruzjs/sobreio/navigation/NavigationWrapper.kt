package dev.lacruzjs.sobreio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.lacruzjs.sobreio.ui.welcome.WelcomeScreen

/** Top-level navigation. Screens only emit events; this layer owns the back stack. */
@Composable
fun NavigationWrapper(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Welcome)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        // Each entry keeps its own saved state and ViewModels, cleared when it leaves the back stack
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Welcome> { WelcomeScreen() }
        },
    )
}
