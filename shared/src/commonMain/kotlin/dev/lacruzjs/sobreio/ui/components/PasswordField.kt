package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_hide_password
import dev.lacruzjs.sobreio.generated.resources.action_show_password
import dev.lacruzjs.sobreio.generated.resources.ic_visibility
import dev.lacruzjs.sobreio.generated.resources.ic_visibility_off
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Password input with a show/hide toggle. Visibility is purely visual, so it lives here and not in the ViewModel. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorRes: StringResource? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val state = rememberSyncedTextFieldState(value, onValueChange)
    SecureTextField(
        state = state,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = tileShape(),
        colors = softTextFieldColors(),
        isError = errorRes != null,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        textObfuscationMode = if (visible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        onKeyboardAction = { onImeAction() },
        trailingIcon = {
            val label = stringResource(if (visible) Res.string.action_hide_password else Res.string.action_show_password)
            LabelTooltip(label) {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        painter = painterResource(if (visible) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility),
                        contentDescription = label,
                    )
                }
            }
        },
    )
}
