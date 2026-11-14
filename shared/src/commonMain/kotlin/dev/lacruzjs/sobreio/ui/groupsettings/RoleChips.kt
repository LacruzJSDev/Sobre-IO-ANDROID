package dev.lacruzjs.sobreio.ui.groupsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.GroupRole
import org.jetbrains.compose.resources.stringResource

/** One chip per role in [roles]; the selected one is filled with the brand ink, like the other pickers. */
@Composable
internal fun RoleChips(
    roles: List<GroupRole>,
    selected: GroupRole,
    onSelect: (GroupRole) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.forEach { role ->
            FilterChip(
                selected = selected == role,
                onClick = { onSelect(role) },
                enabled = enabled,
                label = { Text(stringResource(role.labelRes())) },
                shape = MaterialTheme.shapes.medium,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = null,
            )
        }
    }
}
