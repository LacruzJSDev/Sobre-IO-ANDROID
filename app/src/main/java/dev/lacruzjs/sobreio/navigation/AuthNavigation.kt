package dev.lacruzjs.sobreio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.lacruzjs.sobreio.ui.auth.confirm.ConfirmEmailRoute
import dev.lacruzjs.sobreio.ui.auth.recovery.PasswordRecoveryRoute
import dev.lacruzjs.sobreio.ui.auth.signin.SignInRoute
import dev.lacruzjs.sobreio.ui.auth.signup.SignUpRoute

/** Screens shown while there is no session. Signing in is not handled here: the session change swaps this tree out. */
@Composable
fun AuthNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(SignIn)

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
            entry<SignIn> {
                SignInRoute(
                    onForgotPassword = { backStack.add(PasswordRecovery) },
                    onCreateAccount = { backStack.add(SignUp) },
                    onConfirmEmail = { email -> backStack.add(ConfirmEmail(email)) },
                )
            }
            entry<SignUp> {
                SignUpRoute(
                    onSignIn = { backStack.removeLastOrNull() },
                    // Replaces the sign-up form, so back from the code screen does not show a stale form
                    onConfirmEmail = { email ->
                        backStack.removeLastOrNull()
                        backStack.add(ConfirmEmail(email))
                    },
                )
            }
            entry<ConfirmEmail> { key ->
                ConfirmEmailRoute(email = key.email, onBackToSignIn = { backStack.backToSignIn() })
            }
            entry<PasswordRecovery> {
                PasswordRecoveryRoute(onBackToSignIn = { backStack.backToSignIn() })
            }
        },
    )
}

private fun MutableList<NavKey>.backToSignIn() {
    clear()
    add(SignIn)
}
