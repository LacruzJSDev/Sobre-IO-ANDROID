package dev.lacruzjs.sobreio.ui.movements

import dev.lacruzjs.sobreio.ui.components.LabelTooltip
import dev.lacruzjs.sobreio.generated.resources.export_open
import dev.lacruzjs.sobreio.ui.components.IconActionButton
import androidx.compose.material.icons.filled.FileDownload
import dev.lacruzjs.sobreio.ui.components.RefreshBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_new_movement
import dev.lacruzjs.sobreio.generated.resources.action_search
import dev.lacruzjs.sobreio.generated.resources.action_search_close
import dev.lacruzjs.sobreio.generated.resources.movements_search_label
import dev.lacruzjs.sobreio.ui.components.AddIconButton
import dev.lacruzjs.sobreio.ui.components.FormTextField
import org.jetbrains.compose.resources.stringResource

/** The two icons on the right of the Movements header: search and "+" (the header allows at most two). */
@Composable
fun MovementsHeaderActions(onSearch: () -> Unit, onNew: () -> Unit, onExport: (() -> Unit)? = null) {
    // Only the group's whole ledger exports a period (the Movements tab); an account's list does not
    if (onExport != null) {
        IconActionButton(icon = Icons.Filled.FileDownload, label = stringResource(Res.string.export_open), onClick = onExport)
    }
    LabelTooltip(stringResource(Res.string.action_search)) {
        IconButton(onClick = onSearch) {
            Icon(imageVector = Icons.Filled.Search, contentDescription = stringResource(Res.string.action_search))
        }
    }
    AddIconButton(contentDescription = stringResource(Res.string.action_new_movement), onClick = onNew)
}

/**
 * The header while searching: it becomes a text field that takes the focus at once, with an X that closes the search and
 * clears the text. Typing is debounced by the ViewModel.
 */
@Composable
fun MovementsSearchHeader(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        SearchRow(text, onTextChange, onClose)
        RefreshBar(visible = isRefreshing, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SearchRow(text: String, onTextChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormTextField(
            value = text,
            onValueChange = onTextChange,
            label = stringResource(Res.string.movements_search_label),
            imeAction = ImeAction.Search,
            modifier = Modifier.weight(1f).focusRequester(focus).padding(vertical = 4.dp),
        )
        LabelTooltip(stringResource(Res.string.action_search_close)) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(Res.string.action_search_close))
            }
        }
    }
}
