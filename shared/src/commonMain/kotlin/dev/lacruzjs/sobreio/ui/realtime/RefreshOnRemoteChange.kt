package dev.lacruzjs.sobreio.ui.realtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import dev.lacruzjs.sobreio.data.realtime.GroupChangeFeed
import org.koin.compose.koinInject

/**
 * Placed in a Route: while the screen is on display, a change made elsewhere in the group (D-55) runs [onChange], which
 * sends the screen's own Refresh (the cache is already stale, so it reads again with the thin bar). While [paused] (an
 * Undo snackbar is waiting) the refresh waits until it is false, so a reload never takes the snackbar away or brings back a
 * row the user has just hidden.
 */
@Composable
fun RefreshOnRemoteChange(paused: Boolean = false, onChange: () -> Unit) {
    val feed = koinInject<GroupChangeFeed>()
    val latest by rememberUpdatedState(onChange)
    var pending by remember { mutableStateOf(false) }
    LaunchedEffect(feed) { feed.remoteChanges.collect { pending = true } }
    LaunchedEffect(pending, paused) {
        if (pending && !paused) {
            pending = false
            latest()
        }
    }
}
