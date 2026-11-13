package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_join_with_code
import dev.lacruzjs.sobreio.generated.resources.action_profile
import dev.lacruzjs.sobreio.generated.resources.action_sign_out
import dev.lacruzjs.sobreio.ui.components.DestructiveButton
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

/** Fixed section under the list, separated by spacing only. New buttons (for example Settings) go inside the Column. */
@Composable
internal fun GroupsBottomActions(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ErrorText(state.signOutError ?: state.actionError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_join_with_code),
            onClick = { onEvent(GroupsEvent.JoinClicked) },
        )
        PrimaryButton(
            text = stringResource(Res.string.action_profile),
            onClick = { onEvent(GroupsEvent.ProfileClicked) },
        )
        DestructiveButton(
            text = stringResource(Res.string.action_sign_out),
            onClick = { onEvent(GroupsEvent.SignOutClicked) },
            isLoading = state.isSigningOut,
        )
    }
}
