package dev.lacruzjs.sobreio.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.lacruzjs.sobreio.data.local.LastGroupStore
import dev.lacruzjs.sobreio.ui.group.GroupRoute
import dev.lacruzjs.sobreio.ui.groups.GroupsRoute
import dev.lacruzjs.sobreio.ui.profile.ProfileRoute
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
                )
            }
            entry<Group> { key ->
                GroupRoute(groupId = key.groupId, onBack = { backStack.popToPrevious() })
            }
            entry<Profile> {
                ProfileRoute(onBack = { backStack.popToPrevious() })
            }
        },
    )
}

/** Groups is the root of the stack and stays; back from it leaves the app. */
private fun MutableList<NavKey>.popToPrevious() {
    if (size > 1) removeAt(lastIndex)
}
