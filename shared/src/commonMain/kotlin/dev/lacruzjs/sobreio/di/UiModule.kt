package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.ui.accounts.AccountsViewModel
import dev.lacruzjs.sobreio.ui.auth.confirm.ConfirmEmailViewModel
import dev.lacruzjs.sobreio.ui.auth.recovery.PasswordRecoveryViewModel
import dev.lacruzjs.sobreio.ui.auth.signin.SignInViewModel
import dev.lacruzjs.sobreio.ui.auth.signup.SignUpViewModel
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesViewModel
import dev.lacruzjs.sobreio.ui.group.GroupViewModel
import dev.lacruzjs.sobreio.ui.groups.GroupsViewModel
import dev.lacruzjs.sobreio.ui.groupsettings.GroupSettingsViewModel
import dev.lacruzjs.sobreio.ui.profile.ProfileViewModel
import dev.lacruzjs.sobreio.ui.session.SessionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val uiModule = module {
    viewModelOf(::SessionViewModel)
    viewModelOf(::SignUpViewModel)
    viewModelOf(::PasswordRecoveryViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::GroupsViewModel)
    viewModelOf(::SignInViewModel)
    // Plain values come as Koin parameters from the Route (koinViewModel { parametersOf(...) }). Navigation 3 keys
    // are not in a SavedStateHandle, so the screen's argument has to be handed over explicitly.
    viewModel { params -> ConfirmEmailViewModel(email = params.get(), auth = get()) }
    viewModel { params ->
        GroupViewModel(groupId = params.get(), groups = get(), lastGroup = get(), auth = get())
    }
    viewModel { params ->
        GroupSettingsViewModel(
            groupId = params.get(),
            groups = get(),
            members = get(),
            auth = get(),
            lastGroup = get(),
        )
    }
    viewModel { params -> AccountsViewModel(groupId = params.get(), accounts = get()) }
    viewModel { params -> EnvelopesViewModel(groupId = params.get(), envelopes = get()) }
}
