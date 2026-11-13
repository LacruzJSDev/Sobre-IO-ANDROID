package dev.lacruzjs.sobreio.ui.auth.recovery

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_back_to_sign_in
import dev.lacruzjs.sobreio.generated.resources.action_change_password
import dev.lacruzjs.sobreio.generated.resources.action_resend_code
import dev.lacruzjs.sobreio.generated.resources.action_send_code
import dev.lacruzjs.sobreio.generated.resources.action_use_another_email
import dev.lacruzjs.sobreio.generated.resources.confirm_code_resent
import dev.lacruzjs.sobreio.generated.resources.field_code
import dev.lacruzjs.sobreio.generated.resources.field_email
import dev.lacruzjs.sobreio.generated.resources.field_new_password
import dev.lacruzjs.sobreio.generated.resources.field_repeat_password
import dev.lacruzjs.sobreio.generated.resources.recovery_email_body
import dev.lacruzjs.sobreio.generated.resources.recovery_reset_body
import dev.lacruzjs.sobreio.generated.resources.recovery_title
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.components.PasswordField
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PasswordRecoveryRoute(
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordRecoveryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PasswordRecoveryScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                PasswordRecoveryEvent.BackToSignInClicked -> onBackToSignIn()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun PasswordRecoveryScreen(
    state: PasswordRecoveryUiState,
    onEvent: (PasswordRecoveryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = stringResource(Res.string.recovery_title), modifier = modifier) {
        when (state.step) {
            RecoveryStep.Email -> EmailStep(state, onEvent)
            RecoveryStep.Reset -> ResetStep(state, onEvent)
        }
        TextButton(onClick = { onEvent(PasswordRecoveryEvent.BackToSignInClicked) }) {
            Text(stringResource(Res.string.action_back_to_sign_in))
        }
    }
}

@Composable
private fun EmailStep(state: PasswordRecoveryUiState, onEvent: (PasswordRecoveryEvent) -> Unit) {
    Text(
        text = stringResource(Res.string.recovery_email_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    FormTextField(
        value = state.email,
        onValueChange = { onEvent(PasswordRecoveryEvent.EmailChanged(it)) },
        label = stringResource(Res.string.field_email),
        errorRes = state.emailError,
        enabled = !state.isLoading,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        onImeAction = { onEvent(PasswordRecoveryEvent.SendCodeClicked) },
    )
    ErrorText(state.error, modifier = Modifier.fillMaxWidth())
    LoadingButton(
        text = stringResource(Res.string.action_send_code),
        onClick = { onEvent(PasswordRecoveryEvent.SendCodeClicked) },
        isLoading = state.isLoading,
    )
}

@Composable
private fun ResetStep(state: PasswordRecoveryUiState, onEvent: (PasswordRecoveryEvent) -> Unit) {
    Text(
        text = stringResource(Res.string.recovery_reset_body, state.email),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    FormTextField(
        value = state.code,
        onValueChange = { onEvent(PasswordRecoveryEvent.CodeChanged(it)) },
        label = stringResource(Res.string.field_code),
        errorRes = state.codeError,
        enabled = !state.isLoading,
        keyboardType = KeyboardType.NumberPassword,
    )
    PasswordField(
        value = state.newPassword,
        onValueChange = { onEvent(PasswordRecoveryEvent.NewPasswordChanged(it)) },
        label = stringResource(Res.string.field_new_password),
        errorRes = state.newPasswordError,
        enabled = !state.isLoading,
    )
    PasswordField(
        value = state.repeatedPassword,
        onValueChange = { onEvent(PasswordRecoveryEvent.RepeatedPasswordChanged(it)) },
        label = stringResource(Res.string.field_repeat_password),
        errorRes = state.repeatedPasswordError,
        enabled = !state.isLoading,
        imeAction = ImeAction.Done,
        onImeAction = { onEvent(PasswordRecoveryEvent.ResetClicked) },
    )
    if (state.codeResent) {
        Text(
            text = stringResource(Res.string.confirm_code_resent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    ErrorText(state.error, modifier = Modifier.fillMaxWidth())
    LoadingButton(
        text = stringResource(Res.string.action_change_password),
        onClick = { onEvent(PasswordRecoveryEvent.ResetClicked) },
        isLoading = state.isLoading,
    )
    TextButton(onClick = { onEvent(PasswordRecoveryEvent.ResendCodeClicked) }, enabled = !state.isLoading) {
        Text(stringResource(Res.string.action_resend_code))
    }
    TextButton(onClick = { onEvent(PasswordRecoveryEvent.ChangeEmailClicked) }, enabled = !state.isLoading) {
        Text(stringResource(Res.string.action_use_another_email))
    }
}

@PreviewLightDark
@Composable
private fun PasswordRecoveryEmailStepPreview() {
    SobreioTheme {
        PasswordRecoveryScreen(state = PasswordRecoveryUiState(email = "ana@example.com"), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun PasswordRecoveryResetStepPreview() {
    SobreioTheme {
        PasswordRecoveryScreen(
            state = PasswordRecoveryUiState(
                step = RecoveryStep.Reset,
                email = "ana@example.com",
                code = "123456",
                error = AppError.InvalidCode,
            ),
            onEvent = {},
        )
    }
}
