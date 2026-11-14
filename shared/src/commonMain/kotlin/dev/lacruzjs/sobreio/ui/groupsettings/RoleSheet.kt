package dev.lacruzjs.sobreio.ui.groupsettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.role_sheet_title
import dev.lacruzjs.sobreio.ui.components.ErrorText
import dev.lacruzjs.sobreio.ui.components.FlatBottomSheet
import org.jetbrains.compose.resources.stringResource

/** Small sheet to change a member's role: tapping a chip applies it. An error (for example the last owner) shows inside. */
@Composable
internal fun RoleSheet(state: GroupSettingsUiState, onEvent: (GroupSettingsEvent) -> Unit) {
    val member = state.roleMember ?: return
    FlatBottomSheet(
        title = stringResource(Res.string.role_sheet_title, member.displayName),
        onDismiss = { onEvent(GroupSettingsEvent.SheetDismissed) },
    ) {
        RoleChips(
            roles = listOf(GroupRole.Owner, GroupRole.Admin, GroupRole.Member),
            selected = member.role,
            onSelect = { onEvent(GroupSettingsEvent.RoleSelected(it)) },
            enabled = !state.isSubmitting,
        )
        ErrorText(state.submitError, modifier = Modifier.fillMaxWidth())
    }
}
