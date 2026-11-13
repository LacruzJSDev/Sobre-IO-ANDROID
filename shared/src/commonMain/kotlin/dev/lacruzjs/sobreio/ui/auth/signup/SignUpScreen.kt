package dev.lacruzjs.sobreio.ui.auth.signup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_create_account
import dev.lacruzjs.sobreio.generated.resources.action_have_account
import dev.lacruzjs.sobreio.generated.resources.field_email
import dev.lacruzjs.sobreio.generated.resources.field_name
import dev.lacruzjs.sobreio.generated.resources.field_password
import dev.lacruzjs.sobreio.generated.resources.sign_up_title
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.components.PasswordField
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignUpRoute(
    onSignIn: () -> Unit,
    onConfirmEmail: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    state.navigateToConfirm?.let { email ->
        LaunchedEffect(email) {
            onConfirmEmail(email)
            viewModel.onEvent(SignUpEvent.NavigationHandled)
        }
    }

    SignUpScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                SignUpEvent.SignInClicked -> onSignIn()
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onEvent: (SignUpEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = stringResource(Res.string.sign_up_title), modifier = modifier) {
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(SignUpEvent.NameChanged(it)) },
            label = stringResource(Res.string.field_name),
            errorRes = state.nameError,
            enabled = !state.isLoading,
        )
        FormTextField(
            value = state.email,
            onValueChange = { onEvent(SignUpEvent.EmailChanged(it)) },
            label = stringResource(Res.string.field_email),
            errorRes = state.emailError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = { onEvent(SignUpEvent.PasswordChanged(it)) },
            label = stringResource(Res.string.field_password),
            errorRes = state.passwordError,
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(SignUpEvent.SignUpClicked) },
        )
        ErrorText(state.error, modifier = Modifier.fillMaxWidth())
        LoadingButton(
            text = stringResource(Res.string.action_create_account),
            onClick = { onEvent(SignUpEvent.SignUpClicked) },
            isLoading = state.isLoading,
        )
        TextButton(onClick = { onEvent(SignUpEvent.SignInClicked) }, enabled = !state.isLoading) {
            Text(stringResource(Res.string.action_have_account))
        }
    }
}

@PreviewLightDark
@Composable
private fun SignUpScreenPreview() {
    SobreioTheme {
        SignUpScreen(state = SignUpUiState(name = "Ana", email = "ana@example.com"), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun SignUpScreenErrorPreview() {
    SobreioTheme {
        SignUpScreen(
            state = SignUpUiState(name = "Ana", email = "ana@example.com", error = AppError.EmailAlreadyRegistered),
            onEvent = {},
        )
    }
}
