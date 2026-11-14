package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_join_with_code
import dev.lacruzjs.sobreio.generated.resources.action_profile
import dev.lacruzjs.sobreio.generated.resources.action_sign_out
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import org.jetbrains.compose.resources.stringResource

/**
 * The very bottom of the Groups screen: one row of icons spread evenly (join with a code, profile, sign out in red), with
 * no dividers. Each has an accessible name that also shows on long press.
 */
@Composable
internal fun GroupsBottomActions(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ErrorText(state.signOutError ?: state.actionError, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            IconActionButton(
                icon = Icons.Filled.GroupAdd,
                label = stringResource(Res.string.action_join_with_code),
                onClick = { onEvent(GroupsEvent.JoinClicked) },
            )
            IconActionButton(
                icon = Icons.Filled.Person,
                label = stringResource(Res.string.action_profile),
                onClick = { onEvent(GroupsEvent.ProfileClicked) },
            )
            IconActionButton(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = stringResource(Res.string.action_sign_out),
                onClick = { onEvent(GroupsEvent.SignOutClicked) },
                tint = MaterialTheme.colorScheme.error,
                isLoading = state.isSigningOut,
            )
        }
    }
}
