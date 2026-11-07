package dev.lacruzjs.sobreio.ui.auth.signin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.BuildConfig
import dev.lacruzjs.sobreio.R
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.ui.auth.google.GoogleSignInCancelledException
import dev.lacruzjs.sobreio.ui.auth.google.GoogleSignInRequester
import dev.lacruzjs.sobreio.ui.components.AuthScaffold
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FormTextField
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.components.PasswordField
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SignInRoute(
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onConfirmEmail: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = koinViewModel { parametersOf(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleRequester = remember { GoogleSignInRequester(BuildConfig.GOOGLE_WEB_CLIENT_ID) }

    state.navigateToConfirm?.let { email ->
        LaunchedEffect(email) {
            onConfirmEmail(email)
            viewModel.onEvent(SignInEvent.NavigationHandled)
        }
    }

    SignInScreen(
        state = state,
        onEvent = { event ->
            when (event) {
                SignInEvent.ForgotPasswordClicked -> onForgotPassword()
                SignInEvent.CreateAccountClicked -> onCreateAccount()
                SignInEvent.GoogleClicked -> scope.launch {
                    googleRequester.request(context)
                        .onSuccess { viewModel.onEvent(SignInEvent.GoogleTokenReceived(it.idToken, it.rawNonce)) }
                        .onFailure {
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
    AuthScaffold(title = stringResource(R.string.sign_in_title), modifier = modifier) {
        FormTextField(
            value = state.email,
            onValueChange = { onEvent(SignInEvent.EmailChanged(it)) },
            label = stringResource(R.string.field_email),
            errorRes = state.emailError,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
        )
        PasswordField(
            value = state.password,
            onValueChange = { onEvent(SignInEvent.PasswordChanged(it)) },
            label = stringResource(R.string.field_password),
            errorRes = state.passwordError,
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = { onEvent(SignInEvent.SignInClicked) },
        )
        ErrorText(state.error, modifier = Modifier.fillMaxWidth())
        if (state.error == AppError.EmailNotConfirmed) {
            TextButton(onClick = { onEvent(SignInEvent.ConfirmEmailClicked) }, enabled = !state.isLoading) {
                Text(stringResource(R.string.sign_in_confirm_email))
            }
        }
        LoadingButton(
            text = stringResource(R.string.action_sign_in),
            onClick = { onEvent(SignInEvent.SignInClicked) },
            isLoading = state.isLoading,
        )
        if (state.isGoogleAvailable) {
            OutlinedButton(
                onClick = { onEvent(SignInEvent.GoogleClicked) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_continue_with_google))
            }
        }
        TextButton(onClick = { onEvent(SignInEvent.ForgotPasswordClicked) }, enabled = !state.isLoading) {
            Text(stringResource(R.string.action_forgot_password))
        }
        TextButton(onClick = { onEvent(SignInEvent.CreateAccountClicked) }, enabled = !state.isLoading) {
            Text(stringResource(R.string.action_create_account))
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
