package dev.lacruzjs.sobreio.ui.components

import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
    inputTransformation: InputTransformation? = null,
    /** Names and notes start with a capital ([KeyboardCapitalization.Sentences]); codes, emails and searches do not. */
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    OutlinedTextField(
        state = state,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        lineLimits = TextFieldLineLimits.SingleLine,
        shape = MaterialTheme.shapes.medium,
        isError = errorRes != null,
        leadingIcon = leadingIcon,
        supportingText = errorRes?.let { { Text(stringResource(it)) } },
        inputTransformation = inputTransformation,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType, imeAction = imeAction),
        onKeyboardAction = rememberKeyboardActionHandler(imeAction, onImeAction),
    )
}

/** "Next" moves to the next field (or closes the keyboard on the last one); any other action closes it. */
@Composable
internal fun rememberKeyboardActionHandler(imeAction: ImeAction, onImeAction: () -> Unit): KeyboardActionHandler {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val latest by rememberUpdatedState(onImeAction)
    return KeyboardActionHandler {
        if (imeAction == ImeAction.Next) {
            if (!focusManager.moveFocus(FocusDirection.Down)) dismissKeyboard(focusManager, keyboard)
        } else {
            dismissKeyboard(focusManager, keyboard)
        }
        latest()
    }
}

/**
 * A money amount in cents typed as text ("1250,50"). It opens the numeric keypad with a decimal separator, the one
 * people expect for money. Amounts are always typed positive (a minus sign never gets in): a debt is chosen with the
 * "In credit / Debt" chips next to it.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorRes: StringResource? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    FormTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorRes = errorRes,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
        imeAction = imeAction,
        onImeAction = onImeAction,
        inputTransformation = noMinusSign,
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
 * keypad, no minus sign); with [focusRequester] the form can open the keypad by itself. [currencySymbol] sits right
 * next to the number, muted, where money is written in the user's language (after it in Spanish, `12,50 €`; before it
 * otherwise, `€12.50`), and [placeholder] shows when empty.
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
    val state = rememberSyncedTextFieldState(value, onValueChange)
    val style = MaterialTheme.typography.displaySmall.copy(
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        color = amountColor,
    )
    val symbolAfter = Locale.current.language == "es"
    val symbol: @Composable () -> Unit = {
        Text(text = currencySymbol, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
    }
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        // The field is as wide as what it shows, so the symbol stays next to the figure as it grows
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth(),
        ) {
        if (!symbolAfter) symbol()
        BasicTextField(
            state = state,
            modifier = Modifier
                .width(androidx.compose.foundation.layout.IntrinsicSize.Min)
                .widthIn(min = 32.dp)
                .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                .semantics { contentDescription = label },
            enabled = enabled,
            inputTransformation = noMinusSign,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = style,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            onKeyboardAction = rememberKeyboardActionHandler(ImeAction.Done) {},
            decorator = TextFieldDecorator { inner ->
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    if (state.text.isEmpty()) {
                        Text(text = placeholder, style = style.copy(color = MaterialTheme.colorScheme.outline))
                    }
                    inner()
                }
            },
        )
        if (symbolAfter) symbol()
        }
        if (errorRes != null) {
            Text(text = stringResource(errorRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
