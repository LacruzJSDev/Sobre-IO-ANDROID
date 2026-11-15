package dev.lacruzjs.sobreio.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.amount_sign_toggle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Outlined text field with an optional validation message ([errorRes]) as supporting text.
 *
 * The keyboard always goes away when it should: "Next" moves to the next field (or closes the keyboard on the last
 * one), "Done" and the other actions close it and then call [onImeAction]. Screens add [clearFocusOnTap] so a tap
 * outside any field closes it too.
 */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorRes: StringResource? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        isError = errorRes != null,
        leadingIcon = leadingIcon,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = {
                if (!focusManager.moveFocus(FocusDirection.Down)) dismissKeyboard(focusManager, keyboard)
                onImeAction()
            },
            onDone = {
                dismissKeyboard(focusManager, keyboard)
                onImeAction()
            },
            onGo = {
                dismissKeyboard(focusManager, keyboard)
                onImeAction()
            },
            onSearch = {
                dismissKeyboard(focusManager, keyboard)
                onImeAction()
            },
            onSend = {
                dismissKeyboard(focusManager, keyboard)
                onImeAction()
            },
        ),
    )
}

/**
 * A money amount in cents typed as text ("1250,50"). It opens the numeric keypad with a decimal separator, the one
 * people expect for money. The numeric pads of some platforms have no minus key, so when [allowNegative] (an opening
 * balance that is a debt) a "±" button in the field flips the sign.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorRes: StringResource? = null,
    enabled: Boolean = true,
    allowNegative: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    FormTextField(
        value = value,
        onValueChange = { text -> onValueChange(if (allowNegative) text else text.replace("-", "")) },
        label = label,
        modifier = modifier,
        errorRes = errorRes,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
        onImeAction = onImeAction,
        leadingIcon = if (allowNegative) {
            {
                val signLabel = stringResource(Res.string.amount_sign_toggle)
                TextButton(
                    onClick = { onValueChange(if (value.startsWith("-")) value.removePrefix("-") else "-$value") },
                    enabled = enabled,
                    // Read by screen readers instead of the bare symbol
                    modifier = Modifier.semantics { contentDescription = signLabel },
                ) {
                    Text(text = "±", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            null
        },
    )
}

/** A tap that no child handles (outside any field or button) closes the keyboard. */
fun Modifier.clearFocusOnTap(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    pointerInput(Unit) { detectTapGestures { dismissKeyboard(focusManager, keyboard) } }
}

/**
 * Closes the keyboard and clears the focus. Opening a choice (an [EntitySelect], an [ExpandableFormTile]) calls it
 * first, so the keypad of an amount never covers the options that open below.
 */
@Composable
internal fun rememberDismissKeyboard(): () -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return { dismissKeyboard(focusManager, keyboard) }
}

private fun dismissKeyboard(focusManager: FocusManager, keyboard: SoftwareKeyboardController?) {
    // On iOS clearing the focus alone can leave the keyboard up
    keyboard?.hide()
    focusManager.clearFocus(force = true)
}

/**
 * The amount of a movement, big and centred: the one thing the form is about. Same rules as [AmountField] (decimal
 * keypad, no minus sign); with [focusRequester] the form can open the keypad by itself. [currencyCode] is shown muted
 * under the number and [placeholder] when empty.
 */
@Composable
fun LargeAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    errorRes: StringResource? = null,
    enabled: Boolean = true,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    /** The colour of the typed amount (the placeholder stays muted): the money colour of the kind of movement. */
    amountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val style = MaterialTheme.typography.displaySmall.copy(
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = amountColor,
    )
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.replace("-", "")) },
            modifier = Modifier
                .fillMaxWidth()
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .semantics { contentDescription = label },
            enabled = enabled,
            singleLine = true,
            textStyle = style,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { dismissKeyboard(focusManager, keyboard) }),
            decorationBox = { inner ->
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
                    }
                    inner()
                }
            },
        )
        Text(text = currencySymbol, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (errorRes != null) {
            Text(text = stringResource(errorRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
