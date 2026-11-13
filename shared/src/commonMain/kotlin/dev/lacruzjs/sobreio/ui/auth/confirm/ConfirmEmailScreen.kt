package dev.lacruzjs.sobreio.ui.auth.confirm

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
import dev.lacruzjs.sobreio.generated.resources.action_confirm
import dev.lacruzjs.sobreio.generated.resources.action_resend_code
import dev.lacruzjs.sobreio.generated.resources.action_resend_code_in
import dev.lacruzjs.sobreio.generated.resources.confirm_body
import dev.lacruzjs.sobreio.generated.resources.confirm_code_resent
import dev.lacruzjs.sobreio.generated.resources.confirm_title
import dev.lacruzjs.sobreio.generated.resources.field_code
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.PrimaryButton
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConfirmEmailRoute(
    email: String,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmEmailViewModel = koinViewModel { parametersOf(email) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfirmEmailScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                ConfirmEmailEvent.BackToSignInClicked -> onBackToSignIn()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ConfirmEmailScreen(
    state: ConfirmEmailUiState,
    onEvent: (ConfirmEmailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = stringResource(Res.string.confirm_title), modifier = modifier) {
        Text(
            text = stringResource(Res.string.confirm_body, state.email),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FormTextField(
            value = state.code,
            onValueChange = { onEvent(ConfirmEmailEvent.CodeChanged(it)) },
            label = stringResource(Res.string.field_code),
            errorRes = state.codeError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(ConfirmEmailEvent.ConfirmClicked) },
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
        PrimaryButton(
            text = stringResource(Res.string.action_confirm),
            onClick = { onEvent(ConfirmEmailEvent.ConfirmClicked) },
            isLoading = state.isLoading,
        )
        TextButton(
            onClick = { onEvent(ConfirmEmailEvent.ResendClicked) },
            enabled = state.resendSecondsLeft == 0 && !state.isResending && !state.isLoading,
        ) {
            Text(
                if (state.resendSecondsLeft > 0) {
                    stringResource(Res.string.action_resend_code_in, state.resendSecondsLeft)
                } else {
                    stringResource(Res.string.action_resend_code)
                },
            )
        }
        TextButton(onClick = { onEvent(ConfirmEmailEvent.BackToSignInClicked) }) {
            Text(stringResource(Res.string.action_back_to_sign_in))
        }
    }
}

@PreviewLightDark
@Composable
private fun ConfirmEmailScreenPreview() {
    SobreioTheme {
        ConfirmEmailScreen(
            state = ConfirmEmailUiState(email = "ana@example.com", code = "123", resendSecondsLeft = 42),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ConfirmEmailScreenErrorPreview() {
    SobreioTheme {
        ConfirmEmailScreen(
            state = ConfirmEmailUiState(email = "ana@example.com", code = "123456", error = AppError.InvalidCode),
            onEvent = {},
        )
    }
}
