package dev.lacruzjs.sobreio.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.ic_add
import org.jetbrains.compose.resources.painterResource

/** The "+" on the right of a list header: how every list creates its items. */
@Composable
fun AddIconButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painter = painterResource(Res.drawable.ic_add), contentDescription = contentDescription)
    }
}
