package dev.lacruzjs.sobreio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.ui.accountdetail.AccountDetailRoute
import dev.lacruzjs.sobreio.ui.accountform.AccountFormRoute
import dev.lacruzjs.sobreio.ui.commitmentdetail.CommitmentDetailRoute
import dev.lacruzjs.sobreio.ui.commitmentform.CommitmentFormRoute
import dev.lacruzjs.sobreio.ui.envelopedetail.EnvelopeDetailRoute
import dev.lacruzjs.sobreio.ui.envelopeform.EnvelopeFormRoute
import dev.lacruzjs.sobreio.ui.group.GroupRoute
import dev.lacruzjs.sobreio.ui.groupform.GroupFormRoute
import dev.lacruzjs.sobreio.ui.groupreview.GroupReviewRoute
import dev.lacruzjs.sobreio.ui.groups.GroupsRoute
import dev.lacruzjs.sobreio.ui.groupsettings.GroupSettingsRoute
import dev.lacruzjs.sobreio.ui.movements.AccountMovementsRoute
import dev.lacruzjs.sobreio.ui.movements.EnvelopeMovementsRoute
import dev.lacruzjs.sobreio.domain.model.EntryKind
import dev.lacruzjs.sobreio.ui.movementform.MovementFormRoute
import dev.lacruzjs.sobreio.ui.movementform.MovementPrefill
import dev.lacruzjs.sobreio.ui.profile.ProfileRoute
import dev.lacruzjs.sobreio.ui.timezone.TimeZonePickerRoute
import org.koin.compose.koinInject

/**
 * Screens shown while signed in. The stack starts with the user's last group alone (or Groups when there is none),
 * decided before the first frame from a synchronous read, so nothing flashes or animates at start-up. A group is a root and
 * so is Groups: the group's menu button replaces the stack with Groups, and picking a group replaces it with that group,
 * so back never moves between them.
 */
@Composable
fun AppNavigation(userId: String, modifier: Modifier = Modifier) {
    val lastGroupStore: LastGroupStore = koinInject()
    // Only the first value matters: the saved back stack wins on later compositions and after recreation
    val initial = remember(userId) { initialBackStack(lastGroupStore.get(userId)) }
    val backStack = rememberNavBackStack(NavSavedStateConfiguration, *initial.toTypedArray())

    // The time zone chosen in the picker, waiting for the group form below it to take it. Held here, above both
    // entries, because the form's ViewModel stays alive underneath the picker: the form applies it once and clears it.
    var pickedTimeZone by rememberSaveable { mutableStateOf<String?>(null) }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.popToPrevious() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Groups> {
                GroupsRoute(
                    onOpenGroup = { groupId -> backStack.openGroup(groupId) },
                    onOpenProfile = { backStack.add(Profile) },
                    onNewGroup = { backStack.add(GroupForm(null)) },
                )
            }
            entry<Group> { key ->
                GroupRoute(
                    groupId = key.groupId,
                    onOpenGroups = { backStack.showOnlyGroups() },
                    onGroupGone = { backStack.showOnlyGroups() },
                    onOpenSettings = { backStack.add(GroupSettings(key.groupId)) },
                    onOpenReview = { backStack.add(GroupReview(key.groupId)) },
                    onOpenAccountForm = { accountId -> backStack.add(AccountForm(key.groupId, accountId)) },
                    onOpenAccount = { accountId -> backStack.add(AccountDetail(key.groupId, accountId)) },
                    onOpenEnvelopeForm = { envelopeId -> backStack.add(EnvelopeForm(key.groupId, envelopeId)) },
                    onOpenEnvelope = { envelopeId -> backStack.add(EnvelopeDetail(key.groupId, envelopeId)) },
                    onOpenMovementForm = { movementId, source ->
                        backStack.add(MovementForm(key.groupId, movementId, source?.name, null))
                    },
                    onOpenTransferForm = { suggestion ->
                        backStack.add(
                            MovementForm(
                                groupId = key.groupId,
                                movementId = null,
                                source = null,
                                accountId = null,
                                prefillKind = EntryKind.Transfer.name,
                                prefillFromAccountId = suggestion.fromAccountId,
                                prefillToAccountId = suggestion.toAccountId,
                                prefillAmountCents = suggestion.amountCents,
                            ),
                        )
                    },
                )
            }
            entry<GroupReview> { key ->
                GroupReviewRoute(groupId = key.groupId, onBack = { backStack.popToPrevious() })
            }
            entry<AccountDetail> { key ->
                AccountDetailRoute(
                    groupId = key.groupId,
                    accountId = key.accountId,
                    onBack = { backStack.popToPrevious() },
                    onEdit = { backStack.add(AccountForm(key.groupId, key.accountId)) },
                    onOpenCommitmentForm = { commitmentId ->
                        backStack.add(CommitmentForm(key.groupId, key.accountId, commitmentId))
                    },
                    onOpenCommitment = { commitmentId ->
                        backStack.add(CommitmentDetail(key.groupId, key.accountId, commitmentId))
                    },
                    onSeeAllMovements = { backStack.add(AccountMovements(key.groupId, key.accountId)) },
                )
            }
            entry<AccountMovements> { key ->
                AccountMovementsRoute(
                    groupId = key.groupId,
                    accountId = key.accountId,
                    onBack = { backStack.popToPrevious() },
                    onOpenMovementForm = { movementId, source, accountId ->
                        backStack.add(MovementForm(key.groupId, movementId, source?.name, accountId))
                    },
                )
            }
            entry<EnvelopeDetail> { key ->
                EnvelopeDetailRoute(
                    groupId = key.groupId,
                    envelopeId = key.envelopeId,
                    onBack = { backStack.popToPrevious() },
                    onEdit = { backStack.add(EnvelopeForm(key.groupId, key.envelopeId)) },
                    onSeeAllMovements = { backStack.add(EnvelopeMovements(key.groupId, key.envelopeId)) },
                    onOpenCommitment = { accountId, commitmentId ->
                        backStack.add(CommitmentDetail(key.groupId, accountId, commitmentId))
                    },
                )
            }
            entry<EnvelopeMovements> { key ->
                EnvelopeMovementsRoute(
                    groupId = key.groupId,
                    envelopeId = key.envelopeId,
                    onBack = { backStack.popToPrevious() },
                    onOpenMovementForm = { movementId, source ->
                        backStack.add(MovementForm(key.groupId, movementId, source?.name, null))
                    },
                )
            }
            entry<MovementForm> { key ->
                MovementFormRoute(
                    groupId = key.groupId,
                    movementId = key.movementId,
                    source = key.source,
                    accountId = key.accountId,
                    prefill = MovementPrefill.of(key.prefillKind, key.prefillFromAccountId, key.prefillToAccountId, key.prefillAmountCents),
                    onClose = { backStack.popToPrevious() },
                )
            }
            entry<GroupSettings> { key ->
                GroupSettingsRoute(
                    groupId = key.groupId,
                    onBack = { backStack.popToPrevious() },
                    onBackToGroups = { backStack.showOnlyGroups() },
                    onEditGroup = { backStack.add(GroupForm(key.groupId)) },
                )
            }
            entry<GroupForm> { key ->
                GroupFormRoute(
                    groupId = key.groupId,
                    pickedTimeZone = pickedTimeZone,
                    onTimeZoneConsumed = { pickedTimeZone = null },
                    onClose = { backStack.popToPrevious() },
                    onPickTimeZone = { current -> backStack.add(TimeZonePicker(current)) },
                    onSaved = { savedId, created ->
                        // A new group opens at once as the root, as joining one does
                        if (created) backStack.openGroup(savedId) else backStack.popToPrevious()
                    },
                )
            }
            entry<AccountForm> { key ->
                AccountFormRoute(groupId = key.groupId, accountId = key.accountId, onClose = { backStack.popToPrevious() })
            }
            entry<CommitmentDetail> { key ->
                CommitmentDetailRoute(
                    groupId = key.groupId,
                    accountId = key.accountId,
                    commitmentId = key.commitmentId,
                    onBack = { backStack.popToPrevious() },
                    onEdit = { backStack.add(CommitmentForm(key.groupId, key.accountId, key.commitmentId)) },
                )
            }
            entry<CommitmentForm> { key ->
                CommitmentFormRoute(
                    groupId = key.groupId,
                    accountId = key.accountId,
                    commitmentId = key.commitmentId,
                    onClose = { backStack.popToPrevious() },
                )
            }
            entry<EnvelopeForm> { key ->
                EnvelopeFormRoute(groupId = key.groupId, envelopeId = key.envelopeId, onClose = { backStack.popToPrevious() })
            }
            entry<TimeZonePicker> { key ->
                TimeZonePickerRoute(
                    current = key.current,
                    onBack = { backStack.popToPrevious() },
                    onPicked = { id ->
                        pickedTimeZone = id
                        backStack.popToPrevious()
                    },
                )
            }
            entry<Profile> {
                ProfileRoute(onBack = { backStack.popToPrevious() })
            }
        },
    )
}
