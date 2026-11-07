package dev.lacruzjs.sobreio.ui.auth

import androidx.annotation.StringRes
import dev.lacruzjs.sobreio.R

// UI-level checks only: they save a round trip for obvious typos. The server stays the authority (D-43).

internal const val MIN_PASSWORD_LENGTH = 6
internal const val CODE_LENGTH = 6

// Deliberately loose; the confirmation code is what proves the address is real
private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Each function returns the string resource of the problem, or null when the value is fine. */
@StringRes
internal fun validateRequired(value: String): Int? =
    if (value.isBlank()) R.string.validation_required else null

@StringRes
internal fun validateEmail(value: String): Int? = when {
    value.isBlank() -> R.string.validation_required
    !EmailShape.matches(value.trim()) -> R.string.validation_email_invalid
    else -> null
}

@StringRes
internal fun validatePassword(value: String): Int? = when {
    value.isEmpty() -> R.string.validation_required
    value.length < MIN_PASSWORD_LENGTH -> R.string.validation_password_short
    else -> null
}

@StringRes
internal fun validatePasswordsMatch(password: String, repeated: String): Int? =
    if (password != repeated) R.string.validation_passwords_mismatch else null

@StringRes
internal fun validateCode(value: String): Int? =
    if (value.length != CODE_LENGTH || !value.all(Char::isDigit)) R.string.validation_code_invalid else null
