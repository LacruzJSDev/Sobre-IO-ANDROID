package dev.lacruzjs.sobreio.ui.auth.signin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.AppConfig
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_continue_with_google
import dev.lacruzjs.sobreio.generated.resources.action_create_account
import dev.lacruzjs.sobreio.generated.resources.action_forgot_password
import dev.lacruzjs.sobreio.generated.resources.action_sign_in
import dev.lacruzjs.sobreio.generated.resources.field_email
import dev.lacruzjs.sobreio.generated.resources.field_password
import dev.lacruzjs.sobreio.generated.resources.sign_in_confirm_email
import dev.lacruzjs.sobreio.generated.resources.sign_in_title
import dev.lacruzjs.sobreio.ui.auth.google.GoogleSignInCancelledException
import dev.lacruzjs.sobreio.ui.auth.google.rememberGoogleSignInLauncher
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.components.PasswordField
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInRoute(
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onConfirmEmail: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Null where Google sign-in is not available (iOS for now, or no web client id configured): the button is hidden
    val googleSignIn = rememberGoogleSignInLauncher(AppConfig.GOOGLE_WEB_CLIENT_ID)

    state.navigateToConfirm?.let { email ->
        LaunchedEffect(email) {
            onConfirmEmail(email)
            viewModel.onEvent(SignInEvent.NavigationHandled)
        }
    }

    SignInScreen(
        state = state.copy(isGoogleAvailable = googleSignIn != null),
        onEvent = { event ->
            when (event) {
                SignInEvent.ForgotPasswordClicked -> onForgotPassword()
                SignInEvent.CreateAccountClicked -> onCreateAccount()
                SignInEvent.GoogleClicked -> scope.launch {
                    googleSignIn?.request()
                        ?.onSuccess { viewModel.onEvent(SignInEvent.GoogleTokenReceived(it.idToken, it.rawNonce)) }
                        ?.onFailure {
                            // Closing the account picker is not an error
                            if (it !is GoogleSignInCancelledException) viewModel.onEvent(SignInEvent.GoogleFailed)
                        }
                }
                else -> viewModel.onEvent(event)
            }
        },
        modifier = modifier,
    )
}

@Composable
fun SignInScreen(
    state: SignInUiState,
    onEvent: (SignInEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AuthScaffold(title = stringResource(Res.string.sign_in_title), modifier = modifier) {
        FormTextField(
            value = state.email,
            onValueChange = { onEvent(SignInEvent.EmailChanged(it)) },
            label = stringResource(Res.string.field_email),
            errorRes = state.emailError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = { onEvent(SignInEvent.PasswordChanged(it)) },
            label = stringResource(Res.string.field_password),
            errorRes = state.passwordError,
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(SignInEvent.SignInClicked) },
        )
        ErrorText(state.error, modifier = Modifier.fillMaxWidth())
        if (state.error == AppError.EmailNotConfirmed) {
            TextButton(onClick = { onEvent(SignInEvent.ConfirmEmailClicked) }, enabled = !state.isLoading) {
                Text(stringResource(Res.string.sign_in_confirm_email))
            }
        }
        LoadingButton(
            text = stringResource(Res.string.action_sign_in),
            onClick = { onEvent(SignInEvent.SignInClicked) },
            isLoading = state.isLoading,
        )
        if (state.isGoogleAvailable) {
            OutlinedButton(
                onClick = { onEvent(SignInEvent.GoogleClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.action_continue_with_google))
            }
        }
        TextButton(onClick = { onEvent(SignInEvent.ForgotPasswordClicked) }, enabled = !state.isLoading) {
            Text(stringResource(Res.string.action_forgot_password))
        }
        TextButton(onClick = { onEvent(SignInEvent.CreateAccountClicked) }, enabled = !state.isLoading) {
            Text(stringResource(Res.string.action_create_account))
        }
    }
}

@PreviewLightDark
@Composable
private fun SignInScreenPreview() {
    SobreioTheme {
        SignInScreen(
            state = SignInUiState(email = "ana@example.com", isGoogleAvailable = true),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun SignInScreenNotConfirmedPreview() {
    SobreioTheme {
        SignInScreen(
            state = SignInUiState(email = "ana@example.com", error = AppError.EmailNotConfirmed),
            onEvent = {},
        )
    }
}
