package dev.lacruzjs.sobreio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Account
import dev.lacruzjs.sobreio.domain.model.Envelope
import dev.lacruzjs.sobreio.domain.model.Group
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.entity_select_placeholder
import dev.lacruzjs.sobreio.generated.resources.entity_select_search
import dev.lacruzjs.sobreio.ui.theme.EntityColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** From this many options on, the list gets a search box. */
const val ENTITY_SELECT_SEARCH_THRESHOLD = 8

private val ListMaxHeight = 280.dp

/**
 * One choice of an [EntitySelect]: an account, an envelope, a group, a member, or a special option such as "Unassigned"
 * (null [id], [icon] an inbox) or "Joint". With no [icon] the avatar shows the initial of [name]. [trailing] is a short
 * secondary text on the right: a balance, a role.
 */
data class EntityOption(
    val id: String?,
    val name: String,
    val icon: ImageVector? = null,
    val color: EntityColor? = null,
    val trailing: String? = null,
)

/** The "Unassigned" special option: a null id with the inbox icon, like the envelope list. */
fun unassignedOption(name: String, trailing: String? = null) =
    EntityOption(id = null, name = name, icon = Icons.Filled.Inbox, trailing = trailing)

fun Account.toEntityOption(trailing: String? = null) =
    EntityOption(id, name, iconFor(IconCategory.Account, icon), EntityColor.fromHex(color), trailing)

fun Envelope.toEntityOption(trailing: String? = null) =
    EntityOption(id, name, iconFor(IconCategory.Envelope, icon), EntityColor.fromHex(color), trailing)

fun Group.toEntityOption(trailing: String? = null) =
    EntityOption(id, name, iconFor(IconCategory.Group, icon), EntityColor.fromHex(color), trailing)

/** A member shows the initial of their name; [trailing] is usually their role. */
fun Member.toEntityOption(trailing: String? = null) = EntityOption(userId, displayName, trailing = trailing)

/** The options whose name contains [query] ignoring case; every option when the query is blank. */
fun filterEntityOptions(options: List<EntityOption>, query: String): List<EntityOption> {
    val needle = query.trim()
    if (needle.isEmpty()) return options
    return options.filter { it.name.contains(needle, ignoreCase = true) }
}

/**
 * A form row to choose one thing of the group (account, envelope, group, member). Closed it looks like a [FormTile]: the
 * [label] on the left and, on the right, the avatar and name of the selected option and a chevron (a muted placeholder
 * when nothing is selected). Open, the list of [options] appears under it, inline, with its own scroll and, from
 * [ENTITY_SELECT_SEARCH_THRESHOLD] options on, a search box. The caller keeps [expanded] so a form can keep one open at
 * a time. Tapping an option calls [onSelect] and closes the list through [onToggle]. A disabled select only shows its
 * value (no chevron).
 */
@Composable
fun EntitySelect(
    label: String,
    options: List<EntityOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: StringResource? = null,
) {
    val selected = options.firstOrNull { it.id == selectedId }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f)
    var query by remember { mutableStateOf("") }
    LaunchedEffect(expanded) { if (!expanded) query = "" }
    val open = expanded && enabled
    val expandedText = expandedStateText(expanded)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
                    .semantics { stateDescription = expandedText }
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (selected != null) {
                        EntityAvatar(name = selected.name, icon = selected.icon, color = selected.color, size = 28.dp)
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.entity_select_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (enabled) {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp).rotate(arrowRotation),
                    )
                } else {
                    Spacer(Modifier.size(4.dp))
                }
            }
            AnimatedVisibility(visible = open, enter = expandVertically(), exit = shrinkVertically()) {
                OptionList(
                    options = options,
                    selectedId = selectedId,
                    query = query,
                    onQueryChange = { query = it },
                    onPick = {
                        onSelect(it)
                        onToggle()
                    },
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun OptionList(
    options: List<EntityOption>,
    selectedId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    val visible = filterEntityOptions(options, query)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .padding(vertical = 4.dp),
    ) {
        if (options.size > ENTITY_SELECT_SEARCH_THRESHOLD) {
            FormTextField(
                value = query,
                onValueChange = onQueryChange,
                label = stringResource(Res.string.entity_select_search),
                imeAction = ImeAction.Done,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        // Bounded height: it scrolls on its own inside the scrolling form
        LazyColumn(modifier = Modifier.heightIn(max = ListMaxHeight)) {
            items(visible, key = { it.id ?: "" }) { option ->
                OptionRow(option, selected = option.id == selectedId, onClick = { onPick(option.id) })
            }
        }
    }
}

@Composable
internal fun OptionRow(option: EntityOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .heightIn(min = 52.dp)
            .clip(MaterialTheme.shapes.medium)
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        EntityAvatar(name = option.name, icon = option.icon, color = option.color, size = 32.dp)
        Text(
            text = option.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (option.trailing != null) {
            Text(
                text = option.trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
