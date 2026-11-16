package dev.lacruzjs.sobreio.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
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
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedContentTransitionScope
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
import dev.lacruzjs.sobreio.ui.tags.TagsRoute
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
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(userId: String, modifier: Modifier = Modifier) {
    val lastGroupStore: LastGroupStore = koinInject()
    // Only the first value matters: the saved back stack wins on later compositions and after recreation
    val initial = remember(userId) { initialBackStack(lastGroupStore.get(userId)) }
    val backStack = rememberNavBackStack(NavSavedStateConfiguration, *initial.toTypedArray())

    // The time zone chosen in the picker, waiting for the group form below it to take it. Held here, above both
    // entries, because the form's ViewModel stays alive underneath the picker: the form applies it once and clears it.
    var pickedTimeZone by rememberSaveable { mutableStateOf<String?>(null) }

    // List-detail: on a wide window the group (or Groups) is the list pane and its detail screens open beside it. On compact
    // and medium windows the strategy shows one pane, so it yields to the single pane default and nothing changes
    val listDetail = rememberListDetailSceneStrategy<NavKey>()
    // Panes of the list-detail scene. The list takes the full width while nothing is open (no detail placeholder)
    val listPane = ListDetailSceneStrategy.listPane() + ListDetailSceneStrategy.preferredPaneSize(width = ListPaneWidth) + listPaneMarker
    val detailPane = ListDetailSceneStrategy.detailPane()
    val extraPane = ListDetailSceneStrategy.extraPane()

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.popToPrevious() },
        sceneStrategies = listOf(SplitWhenDetailOpen(listDetail), SinglePaneSceneStrategy()),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            // Groups replaces the stack, so NavDisplay sees a forward move; it sits "behind" a group in the user's mind
            // (the menu button), so it arrives with the platform's back animation instead
            entry<Groups>(metadata = listPane + NavDisplay.transitionSpec { defaultPopTransitionSpec<NavKey>().invoke(this as AnimatedContentTransitionScope<Scene<NavKey>>) }) {
                GroupsRoute(
                    onOpenGroup = { groupId -> backStack.openGroup(groupId) },
                    onOpenProfile = { backStack.openDetail(Profile) },
                    onNewGroup = { backStack.add(GroupForm(null)) },
                )
            }
            entry<Group>(metadata = listPane) { key ->
                GroupRoute(
                    groupId = key.groupId,
                    onOpenGroups = { backStack.showOnlyGroups() },
                    onGroupGone = { backStack.showOnlyGroups() },
                    onOpenSettings = { backStack.openDetail(GroupSettings(key.groupId)) },
                    onOpenReview = { backStack.openDetail(GroupReview(key.groupId)) },
                    onOpenAccountForm = { accountId -> backStack.add(AccountForm(key.groupId, accountId)) },
                    onOpenAccount = { accountId -> backStack.openDetail(AccountDetail(key.groupId, accountId)) },
                    onOpenEnvelopeForm = { envelopeId -> backStack.add(EnvelopeForm(key.groupId, envelopeId)) },
                    onOpenEnvelope = { envelopeId -> backStack.openDetail(EnvelopeDetail(key.groupId, envelopeId)) },
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
            entry<GroupReview>(metadata = detailPane) { key ->
                GroupReviewRoute(groupId = key.groupId, onBack = { backStack.popToPrevious() })
            }
            entry<AccountDetail>(metadata = detailPane) { key ->
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
            entry<AccountMovements>(metadata = extraPane) { key ->
                AccountMovementsRoute(
                    groupId = key.groupId,
                    accountId = key.accountId,
                    onBack = { backStack.popToPrevious() },
                    onOpenMovementForm = { movementId, source, accountId ->
                        backStack.add(MovementForm(key.groupId, movementId, source?.name, accountId))
                    },
                )
            }
            entry<EnvelopeDetail>(metadata = detailPane) { key ->
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
            entry<EnvelopeMovements>(metadata = extraPane) { key ->
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
            entry<GroupSettings>(metadata = detailPane) { key ->
                GroupSettingsRoute(
                    groupId = key.groupId,
                    onBack = { backStack.popToPrevious() },
                    onBackToGroups = { backStack.showOnlyGroups() },
                    onEditGroup = { backStack.add(GroupForm(key.groupId)) },
                    onOpenTags = { create -> backStack.add(Tags(key.groupId, startWithCreate = create)) },
                )
            }
            entry<Tags>(metadata = extraPane) { key ->
                TagsRoute(groupId = key.groupId, startWithCreate = key.startWithCreate, onBack = { backStack.popToPrevious() })
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
            entry<CommitmentDetail>(metadata = extraPane) { key ->
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
            entry<Profile>(metadata = detailPane) {
                ProfileRoute(onBack = { backStack.popToPrevious() })
            }
        },
    )
}

/** Width asked for the list pane (Group or Groups) when a detail is open beside it. */
private val ListPaneWidth = 420.dp
