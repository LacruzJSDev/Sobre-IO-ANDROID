package dev.lacruzjs.sobreio.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lacruzjs.sobreio.domain.model.AuthState
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.ic_sobreio_mark
import dev.lacruzjs.sobreio.ui.session.SessionViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Top-level navigation: the session decides which tree is shown. Screens only emit events; each tree owns its back
 * stack. A branch has its own composition group, so when the session flips the other tree starts fresh (sign-out
 * lands on sign-in, sign-in lands on Home) and nothing of the old one is restored.
 */
@Composable
fun NavigationWrapper(
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = koinViewModel(),
) {
    val authState by sessionViewModel.state.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> SessionLoading(modifier)
        AuthState.SignedOut -> AuthNavigation(modifier)
        is AuthState.SignedIn -> AppNavigation(modifier)
    }
}

/** Shown while the stored session is read, so the sign-in form never flashes for someone who is signed in. */
@Composable
private fun SessionLoading(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.ic_sobreio_mark),
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
        }
    }
}
