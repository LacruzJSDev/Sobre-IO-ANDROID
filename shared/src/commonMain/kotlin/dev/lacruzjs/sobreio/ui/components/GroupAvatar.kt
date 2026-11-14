package dev.lacruzjs.sobreio.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** First letter of a group name, in upper case; empty for a blank name. */
internal fun groupInitial(name: String): String = name.trim().firstOrNull()?.uppercase().orEmpty()

/** Square avatar with the group's initial in the brand ink. Decorative: the name always appears next to it. */
@Composable
fun GroupAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    EntityAvatar(name = name, icon = null, color = null, modifier = modifier, size = size)
}
