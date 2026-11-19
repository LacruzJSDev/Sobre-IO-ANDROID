package dev.lacruzjs.sobreio.ui.validation

import dev.lacruzjs.sobreio.domain.MAX_TAG_NAME_LENGTH
import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_code_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_email_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_password_short
import dev.lacruzjs.sobreio.generated.resources.validation_passwords_mismatch
import dev.lacruzjs.sobreio.generated.resources.validation_required
import dev.lacruzjs.sobreio.generated.resources.validation_tag_name_too_long
import org.jetbrains.compose.resources.StringResource

/*
 * Checks of what the user typed, shared by every form. Each returns the string resource of the problem, or null when the
 * value is fine. They only save a round trip for obvious mistakes: the database stays the authority (its constraints and
 * FZ codes).
 */

internal const val MIN_PASSWORD_LENGTH = 6

/** Length of the codes the auth emails send (D-43). */
internal const val CODE_LENGTH = 6

// Deliberately loose; the confirmation code is what proves the address is real
private val EmailShape = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

internal fun validateRequired(value: String): StringResource? =
    if (value.isBlank()) Res.string.validation_required else null

/** A name the user typed: present and at most [maxLength] characters (the database's limit for that column). */
internal fun validateName(name: String, maxLength: Int): StringResource? = when {
    name.isEmpty() -> validateRequired(name)
    name.length > maxLength -> Res.string.validation_name_too_long
    else -> null
}

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

/** An amount read with `parseMoneyInput`: null means the text was not an amount; it must be above zero. */
internal fun validatePositiveAmount(cents: Long?): StringResource? = when {
    cents == null -> Res.string.validation_amount_invalid
    cents <= 0 -> Res.string.validation_amount_positive
    else -> null
}

/** A tag name already normalised (`normalizeTagName`): present and at most [MAX_TAG_NAME_LENGTH] characters. */
internal fun validateTagName(name: String): StringResource? = when {
    name.isEmpty() -> Res.string.validation_required
    name.length > MAX_TAG_NAME_LENGTH -> Res.string.validation_tag_name_too_long
    else -> null
}
