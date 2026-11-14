package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.domain.model.Member
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.holder_joint
import dev.lacruzjs.sobreio.ui.theme.SobreioTheme
import org.jetbrains.compose.resources.stringResource

/** A person who can hold an account. */
data class MemberOption(val userId: String, val displayName: String)

fun Member.toOption() = MemberOption(userId = userId, displayName = displayName)

/**
 * Chips to choose who holds an account: "Joint" (null) plus one chip per member. Callers hide it when the group has
 * only one member, since there is nothing to choose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HolderPicker(
    members: List<MemberOption>,
    selectedUserId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HolderChip(stringResource(Res.string.holder_joint), selectedUserId == null, enabled) { onSelect(null) }
        members.forEach { member ->
            HolderChip(member.displayName, selectedUserId == member.userId, enabled) { onSelect(member.userId) }
        }
    }
}

@Composable
private fun HolderChip(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        shape = MaterialTheme.shapes.medium,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = null,
    )
}

@PreviewLightDark
@Composable
private fun HolderPickerPreview() {
    SobreioTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            HolderPicker(
                members = listOf(MemberOption("1", "Juan"), MemberOption("2", "Marta")),
                selectedUserId = "2",
                onSelect = {},
            )
        }
    }
}
