package dev.lacruzjs.sobreio.ui.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_copy_code
import dev.lacruzjs.sobreio.generated.resources.code_copied
import dev.lacruzjs.sobreio.generated.resources.invite_body
import dev.lacruzjs.sobreio.generated.resources.invite_title
import dev.lacruzjs.sobreio.generated.resources.invite_valid_until
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FlatSnackbarHost
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.format.formatDate
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Shows the invitation code of the group, big, with its expiry and a button that copies it. */
@Composable
internal fun InviteSheet(state: GroupUiState, onEvent: (GroupEvent) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    FlatBottomSheet(
        title = stringResource(Res.string.invite_title),
        onDismiss = { onEvent(GroupEvent.InviteDismissed) },
    ) {
        Text(
            text = stringResource(Res.string.invite_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ErrorText(state.inviteError, modifier = Modifier.fillMaxWidth())
        if (state.isInviting) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        }
        state.invitation?.let { invitation ->
            Text(
                text = invitation.code,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFeatureSettings = "ss01, zero, tnum",
                    letterSpacing = 2.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Text(
                text = stringResource(
                    Res.string.invite_valid_until,
                    formatDate(invitation.expiresAt, Locale.current.toLanguageTag()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = stringResource(Res.string.action_copy_code),
                onClick = {
                    clipboard.setText(AnnotatedString(invitation.code))
                    scope.launch { snackbar.showSnackbar(getString(Res.string.code_copied)) }
                },
            )
        }
        FlatSnackbarHost(hostState = snackbar)
    }
}
