package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.action_clear_search
import org.jetbrains.compose.resources.stringResource

/**
 * Compact search box: a 48 dp pill with a leading magnifier, the hint as a placeholder (not a floating label) and a clear
 * button while there is text. Use it for every search; [FormTextField] is for data entry. On a grey surface pass
 * `containerColor = MaterialTheme.colorScheme.background` so the pill still stands out.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    enabled: Boolean = true,
    onSearch: () -> Unit = {},
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    BasicTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = rememberKeyboardActionHandler(ImeAction.Search, onSearch),
        decorator = TextFieldDecorator { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(containerColor, RoundedCornerShape(50))
                    .padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = muted, modifier = Modifier.size(20.dp))
                Box(modifier = Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (state.text.isEmpty()) {
                        Text(text = placeholder, style = MaterialTheme.typography.bodyLarge, color = muted, maxLines = 1)
                    }
                    inner()
                }
                if (enabled && state.text.isNotEmpty()) {
                    IconButton(onClick = { state.clearText() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.action_clear_search),
                            tint = muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        },
    )
}
