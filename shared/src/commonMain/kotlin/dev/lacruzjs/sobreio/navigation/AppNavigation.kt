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
import dev.lacruzjs.sobreio.ui.accountform.AccountFormRoute
import dev.lacruzjs.sobreio.ui.envelopeform.EnvelopeFormRoute
import dev.lacruzjs.sobreio.ui.group.GroupRoute
import dev.lacruzjs.sobreio.ui.groupform.GroupFormRoute
import dev.lacruzjs.sobreio.ui.groups.GroupsRoute
import dev.lacruzjs.sobreio.ui.groupsettings.GroupSettingsRoute
import dev.lacruzjs.sobreio.ui.profile.ProfileRoute
import dev.lacruzjs.sobreio.ui.timezone.TimeZonePickerRoute
import org.koin.compose.koinInject

/**
 * Screens shown while signed in. The stack starts as Groups plus the user's last group, decided before the first
 * frame from a synchronous read, so the Groups screen never flashes and nothing animates at start-up.
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
                    onOpenGroup = { groupId -> backStack.add(Group(groupId)) },
                    onOpenProfile = { backStack.add(Profile) },
                    onNewGroup = { backStack.add(GroupForm(null)) },
                )
            }
            entry<Group> { key ->
                GroupRoute(
                    groupId = key.groupId,
                    onBack = { backStack.popToPrevious() },
                    onOpenSettings = { backStack.add(GroupSettings(key.groupId)) },
                    onOpenAccountForm = { accountId -> backStack.add(AccountForm(key.groupId, accountId)) },
                    onOpenEnvelopeForm = { envelopeId -> backStack.add(EnvelopeForm(key.groupId, envelopeId)) },
                )
            }
            entry<GroupSettings> { key ->
                GroupSettingsRoute(
                    groupId = key.groupId,
                    onBack = { backStack.popToPrevious() },
                    onBackToGroups = { backStack.popToRoot() },
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
                        backStack.popToPrevious()
                        // A new group opens at once, as joining one does
                        if (created) backStack.add(Group(savedId))
                    },
                )
            }
            entry<AccountForm> { key ->
                AccountFormRoute(groupId = key.groupId, accountId = key.accountId, onClose = { backStack.popToPrevious() })
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

/** Back to the Groups screen, the root, whatever is above it (a group deleted or left from its settings). */
private fun MutableList<NavKey>.popToRoot() {
    while (size > 1) removeAt(lastIndex)
}

/** Groups is the root of the stack and stays; back from it leaves the app. */
private fun MutableList<NavKey>.popToPrevious() {
    if (size > 1) removeAt(lastIndex)
}
