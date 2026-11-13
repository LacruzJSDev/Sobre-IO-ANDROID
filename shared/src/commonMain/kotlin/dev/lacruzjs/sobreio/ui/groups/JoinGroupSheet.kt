package dev.lacruzjs.sobreio.ui.groups

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_join
import dev.lacruzjs.sobreio.generated.resources.field_invitation_code
import dev.lacruzjs.sobreio.generated.resources.join_group_body
import dev.lacruzjs.sobreio.generated.resources.join_group_title
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun JoinGroupSheet(state: GroupsUiState, onEvent: (GroupsEvent) -> Unit) {
    FlatBottomSheet(
        title = stringResource(Res.string.join_group_title),
        onDismiss = { onEvent(GroupsEvent.SheetDismissed) },
    ) {
        Text(
            text = stringResource(Res.string.join_group_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            value = state.code,
            onValueChange = { onEvent(GroupsEvent.CodeChanged(it)) },
            label = stringResource(Res.string.field_invitation_code),
            errorRes = state.codeError,
            enabled = !state.isSubmitting,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(GroupsEvent.JoinConfirmed) },
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.action_join),
            onClick = { onEvent(GroupsEvent.JoinConfirmed) },
            isLoading = state.isSubmitting,
        )
    }
}
