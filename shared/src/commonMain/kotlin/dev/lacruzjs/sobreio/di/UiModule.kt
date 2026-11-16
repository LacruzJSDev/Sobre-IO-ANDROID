package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.ui.accountdetail.AccountDetailViewModel
import dev.lacruzjs.sobreio.ui.accounts.AccountsViewModel
import dev.lacruzjs.sobreio.ui.commitmentdetail.CommitmentDetailViewModel
import dev.lacruzjs.sobreio.ui.commitmentform.CommitmentFormViewModel
import dev.lacruzjs.sobreio.ui.commitments.AccountCommitmentsViewModel
import dev.lacruzjs.sobreio.ui.auth.confirm.ConfirmEmailViewModel
import dev.lacruzjs.sobreio.ui.auth.recovery.PasswordRecoveryViewModel
import dev.lacruzjs.sobreio.ui.auth.signin.SignInViewModel
import dev.lacruzjs.sobreio.ui.auth.signup.SignUpViewModel
import dev.lacruzjs.sobreio.ui.envelopedetail.EnvelopeDetailViewModel
import dev.lacruzjs.sobreio.ui.envelopes.EnvelopesViewModel
import dev.lacruzjs.sobreio.ui.group.GroupViewModel
import dev.lacruzjs.sobreio.ui.accountform.AccountFormViewModel
import dev.lacruzjs.sobreio.ui.envelopeform.EnvelopeFormViewModel
import dev.lacruzjs.sobreio.ui.groupform.GroupFormViewModel
import dev.lacruzjs.sobreio.ui.groups.GroupsViewModel
import dev.lacruzjs.sobreio.ui.groupsettings.GroupSettingsViewModel
import dev.lacruzjs.sobreio.ui.tags.TagsViewModel
import dev.lacruzjs.sobreio.ui.groupreview.GroupReviewViewModel
import dev.lacruzjs.sobreio.ui.groupsummary.GroupSummaryViewModel
import dev.lacruzjs.sobreio.ui.movementform.MovementFormViewModel
import dev.lacruzjs.sobreio.ui.movementform.MovementPrefill
import dev.lacruzjs.sobreio.ui.movements.MovementsViewModel
import dev.lacruzjs.sobreio.ui.deleteaccount.DeleteAccountViewModel
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
    viewModelOf(::DeleteAccountViewModel)
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
            tags = get(),
        )
    }
    viewModel { params -> TagsViewModel(groupId = params.get(), tags = get(), groups = get()) }
    viewModel { params -> GroupFormViewModel(groupId = params.get<String?>(0), groups = get()) }
    viewModel { params -> AccountFormViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String?>(1),
            accounts = get(),
            members = get(),
        ) }
    viewModel { params ->
        EnvelopeFormViewModel(
            groupId = params.get<String>(0),
            envelopeId = params.get<String?>(1),
            envelopes = get(),
            members = get(),
        )
    }
    viewModel { params ->
        AccountDetailViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            groups = get(),
            accounts = get(),
            members = get(),
            envelopes = get(),
            detail = get(),
        )
    }
    viewModel { params ->
        AccountCommitmentsViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            commitments = get(),
            groups = get(),
            detail = get(),
            accounts = get(),
        )
    }
    viewModel { params ->
        CommitmentDetailViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            commitmentId = params.get<String>(2),
            commitments = get(),
            groups = get(),
            detail = get(),
            accounts = get(),
            envelopes = get(),
        )
    }
    viewModel { params ->
        CommitmentFormViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            commitmentId = params.get<String?>(2),
            commitments = get(),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            detail = get(),
        )
    }
    viewModel { params ->
        GroupSummaryViewModel(
            groupId = params.get(),
            summary = get(),
            detail = get(),
            accounts = get(),
            envelopes = get(),
        )
    }
    viewModel { params ->
        GroupReviewViewModel(
            groupId = params.get(),
            groups = get(),
            summary = get(),
            detail = get(),
            accounts = get(),
            envelopes = get(),
        )
    }
    viewModel { params -> AccountsViewModel(groupId = params.get(), accounts = get(), members = get(), detail = get()) }
    viewModel { params -> EnvelopesViewModel(groupId = params.get(), envelopes = get(), members = get()) }
    viewModel { params ->
        EnvelopeDetailViewModel(
            groupId = params.get<String>(0),
            envelopeId = params.get<String>(1),
            groups = get(),
            envelopes = get(),
            members = get(),
            accounts = get(),
            movements = get(),
            commitments = get(),
            detail = get(),
        )
    }
    viewModel { params ->
        MovementsViewModel(
            groupId = params.get<String>(0),
            fixedAccountId = params.get<String?>(1),
            fixedEnvelopeId = params.get<String?>(2),
            movements = get(),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            members = get(),
            tags = get(),
        )
    }
    viewModel { params ->
        MovementFormViewModel(
            groupId = params.get<String>(0),
            movementId = params.get<String?>(1),
            source = params.get<String?>(2),
            fixedAccountId = params.get<String?>(3),
            prefill = params.get<MovementPrefill?>(4),
            form = get(),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            tags = get(),
            detail = get(),
            auth = get(),
            lastAccount = get(),
        )
    }
}
