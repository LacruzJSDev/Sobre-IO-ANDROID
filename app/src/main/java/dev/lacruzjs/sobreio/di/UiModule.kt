package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.ui.auth.confirm.ConfirmEmailViewModel
import dev.lacruzjs.sobreio.ui.auth.recovery.PasswordRecoveryViewModel
import dev.lacruzjs.sobreio.ui.auth.signin.SignInViewModel
import dev.lacruzjs.sobreio.ui.auth.signup.SignUpViewModel
import dev.lacruzjs.sobreio.ui.home.HomeViewModel
import dev.lacruzjs.sobreio.ui.session.SessionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::SessionViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::PasswordRecoveryViewModel)
    viewModelOf(::HomeViewModel)
    // Plain values come as Koin parameters from the Route (koinViewModel { parametersOf(...) }). Navigation 3 keys
    // are not in a SavedStateHandle, so the screen's argument has to be handed over explicitly.
    viewModel { params -> SignInViewModel(auth = get(), isGoogleAvailable = params.get()) }
    viewModel { params -> ConfirmEmailViewModel(email = params.get(), auth = get()) }
}
