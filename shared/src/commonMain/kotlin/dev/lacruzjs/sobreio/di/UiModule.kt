package dev.lacruzjs.sobreio.di

import dev.lacruzjs.sobreio.ui.history.MovementHistoryViewModel
import dev.lacruzjs.sobreio.ui.export.ExportViewModel
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
import dev.lacruzjs.sobreio.ui.envelopetransfers.EnvelopeTransferFormViewModel
import dev.lacruzjs.sobreio.ui.envelopetransfers.EnvelopeTransfersViewModel
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
import dev.lacruzjs.sobreio.ui.bankimport.BankImportViewModel
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
        GroupViewModel(groupId = params.get(), groups = get(), lastGroup = get(), auth = get(), changes = get())
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
    viewModel { params ->
        MovementHistoryViewModel(params.get<String>(0), params.get<String>(1), params.get<String?>(2), audit = get(), members = get())
    }
    viewModel { params -> ExportViewModel(groupId = params.get(), exports = get(), groups = get(), calendar = get()) }
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
            calendar = get(),
        )
    }
    viewModel { params ->
        EnvelopeTransfersViewModel(
            groupId = params.get<String>(0),
            envelopeId = params.get<String?>(1),
            groups = get(),
            envelopes = get(),
            transfers = get(),
        )
    }
    viewModel { params ->
        EnvelopeTransferFormViewModel(
            groupId = params.get<String>(0),
            transferId = params.get<String?>(1),
            initialToId = params.get<String?>(2),
            groups = get(),
            envelopes = get(),
            transfers = get(),
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
            imports = get(),
            calendar = get(),
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
            calendar = get(),
        )
    }
    viewModel { params ->
        CommitmentDetailViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            commitmentId = params.get<String>(2),
            commitments = get(),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            calendar = get(),
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
            calendar = get(),
        )
    }
    viewModel { params ->
        GroupSummaryViewModel(
            groupId = params.get(),
            summary = get(),
            detail = get(),
            accounts = get(),
            envelopes = get(),
            calendar = get(),
        )
    }
    viewModel { params ->
        GroupReviewViewModel(
            groupId = params.get(),
            groups = get(),
            summary = get(),
            accounts = get(),
            envelopes = get(),
            calendar = get(),
            commitments = get(),
        )
    }
    viewModel { params ->
        BankImportViewModel(
            groupId = params.get<String>(0),
            accountId = params.get<String>(1),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            detail = get(),
            imports = get(),
            formats = get(),
            calendar = get(),
        )
    }
    viewModel { params -> AccountsViewModel(groupId = params.get(), accounts = get(), members = get(), detail = get(), summary = get(), calendar = get()) }
    viewModel { params -> EnvelopesViewModel(
            groupId = params.get(),
            envelopes = get(),
            members = get(),
            transfers = get(),
            summary = get(),
            calendar = get(),
        ) }
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
            calendar = get(),
        )
    }
    viewModel { params ->
        MovementsViewModel(
            groupId = params.get<String>(0),
            fixedAccountId = params.get<String?>(1),
            fixedEnvelopeId = params.get<String?>(2),
            // Optional fourth parameter: the tag the list starts filtered by
            initialTagIds = setOfNotNull(params.values.getOrNull(3) as String?),
            movements = get(),
            groups = get(),
            accounts = get(),
            envelopes = get(),
            members = get(),
            tags = get(),
            calendar = get(),
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
            auth = get(),
            lastAccount = get(),
            commitments = get(),
            summary = get(),
            calendar = get(),
        )
    }
}
