package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_back
import dev.lacruzjs.sobreio.generated.resources.action_open_groups
import dev.lacruzjs.sobreio.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Back arrow of every header; its label is read by screen readers and shown on long press. */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LabelTooltip(stringResource(Res.string.action_back)) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(painter = painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.action_back))
        }
    }
}

/** Menu button of a group's header: opens the Groups screen (a group is a root, so it has no back arrow). */
@Composable
fun MenuButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LabelTooltip(stringResource(Res.string.action_open_groups)) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(imageVector = Icons.Filled.Menu, contentDescription = stringResource(Res.string.action_open_groups))
        }
    }
}

/**
 * Header with three zones: navigation on the left ([onBack], hidden when null), context in the centre ([title], cut
 * with an ellipsis so it never reaches the icons) and [actions] on the right. [leading] goes before the title, for
 * example a group avatar. [subtitle] is a small muted line under the title (the group's name on an account detail).
 * [isRefreshing] draws the [RefreshBar] over the header's bottom edge, so it never moves the content.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /** Replaces the back arrow, e.g. the [MenuButton] of a group. */
    navigation: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    isRefreshing: Boolean = false,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HeaderRow(title, onBack, navigation, subtitle, leading, actions)
        RefreshBar(visible = isRefreshing, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun HeaderRow(
    title: String,
    onBack: (() -> Unit)?,
    navigation: (@Composable () -> Unit)?,
    subtitle: String?,
    leading: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasNavigation = onBack != null || navigation != null
        when {
            navigation != null -> navigation()
            onBack != null -> BackButton(onClick = onBack)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (hasNavigation) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        actions()
    }
}
