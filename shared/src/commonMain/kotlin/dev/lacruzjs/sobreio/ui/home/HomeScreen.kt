package dev.lacruzjs.sobreio.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.User
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_sign_out
import dev.lacruzjs.sobreio.generated.resources.home_signed_in_as
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.LoadingButton
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoute(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

/** Temporary signed-in screen, replaced by the Groups screen. It only proves the session works. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val who = state.user?.let { it.displayName ?: it.email }.orEmpty()
            Text(
                text = stringResource(Res.string.home_signed_in_as, who),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            ErrorText(state.error)
            LoadingButton(
                text = stringResource(Res.string.action_sign_out),
                onClick = { onEvent(HomeEvent.SignOutClicked) },
                isLoading = state.isSigningOut,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    SobreioTheme {
        HomeScreen(
            state = HomeUiState(user = User("1", "ana@example.com", "Ana")),
            onEvent = {},
        )
    }
}
