package dev.lacruzjs.sobreio.ui.groupsettings

import dev.lacruzjs.sobreio.domain.model.GroupRole
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.role_admin
import dev.lacruzjs.sobreio.generated.resources.role_member
import dev.lacruzjs.sobreio.generated.resources.role_owner
import org.jetbrains.compose.resources.StringResource

/** Name of the role for the user. */
internal fun GroupRole.labelRes(): StringResource = when (this) {
    GroupRole.Owner -> Res.string.role_owner
    GroupRole.Admin -> Res.string.role_admin
    GroupRole.Member -> Res.string.role_member
}
