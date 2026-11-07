package dev.lacruzjs.sobreio.ui.auth

import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_code_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_email_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_password_short
import dev.lacruzjs.sobreio.generated.resources.validation_passwords_mismatch
import dev.lacruzjs.sobreio.generated.resources.validation_required
import org.jetbrains.compose.resources.StringResource

// UI-level checks only: they save a round trip for obvious typos. The server stays the authority (D-43).

internal const val MIN_PASSWORD_LENGTH = 6
internal const val CODE_LENGTH = 6

// Deliberately loose; the confirmation code is what proves the address is real
private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

/** Each function returns the string resource of the problem, or null when the value is fine. */
internal fun validateRequired(value: String): StringResource? =
    if (value.isBlank()) Res.string.validation_required else null

internal fun validateEmail(value: String): StringResource? = when {
    value.isBlank() -> Res.string.validation_required
    !EmailShape.matches(value.trim()) -> Res.string.validation_email_invalid
    else -> null
}

internal fun validatePassword(value: String): StringResource? = when {
    value.isEmpty() -> Res.string.validation_required
    value.length < MIN_PASSWORD_LENGTH -> Res.string.validation_password_short
    else -> null
}

internal fun validatePasswordsMatch(password: String, repeated: String): StringResource? =
    if (password != repeated) Res.string.validation_passwords_mismatch else null

internal fun validateCode(value: String): StringResource? =
    if (value.length != CODE_LENGTH || !value.all(Char::isDigit)) Res.string.validation_code_invalid else null
