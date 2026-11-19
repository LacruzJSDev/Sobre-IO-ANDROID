package dev.lacruzjs.sobreio.ui.validation

import dev.lacruzjs.sobreio.generated.resources.Res
import dev.lacruzjs.sobreio.generated.resources.validation_amount_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_amount_positive
import dev.lacruzjs.sobreio.generated.resources.validation_code_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_email_invalid
import dev.lacruzjs.sobreio.generated.resources.validation_name_too_long
import dev.lacruzjs.sobreio.generated.resources.validation_password_short
import dev.lacruzjs.sobreio.generated.resources.validation_passwords_mismatch
import dev.lacruzjs.sobreio.generated.resources.validation_required
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidationTest {

    @Test
    fun aRequiredValueMustNotBeBlank() {
        assertEquals(Res.string.validation_required, validateRequired("  "))
        assertNull(validateRequired("Casa"))
    }

    @Test
    fun aNameIsPresentAndWithinItsLimit() {
        assertEquals(Res.string.validation_required, validateName("", 10))
        assertEquals(Res.string.validation_name_too_long, validateName("x".repeat(11), 10))
        assertNull(validateName("x".repeat(10), 10))
    }

    @Test
    fun anEmailHasAnAtAndADomain() {
        assertEquals(Res.string.validation_required, validateEmail(""))
        assertEquals(Res.string.validation_email_invalid, validateEmail("ana@casa"))
        assertNull(validateEmail(" ana@casa.es "))
    }

    @Test
    fun aPasswordHasAMinimumLengthAndBothCopiesMatch() {
        assertEquals(Res.string.validation_required, validatePassword(""))
        assertEquals(Res.string.validation_password_short, validatePassword("x".repeat(MIN_PASSWORD_LENGTH - 1)))
        assertNull(validatePassword("x".repeat(MIN_PASSWORD_LENGTH)))
        assertEquals(Res.string.validation_passwords_mismatch, validatePasswordsMatch("secret1", "secret2"))
        assertNull(validatePasswordsMatch("secret1", "secret1"))
    }

    @Test
    fun aCodeHasExactlySixDigits() {
        assertNull(validateCode("123456"))
        listOf("12345", "1234567", "12a456").forEach { assertEquals(Res.string.validation_code_invalid, validateCode(it), it) }
    }

    @Test
    fun anAmountIsReadableAndAboveZero() {
        assertEquals(Res.string.validation_amount_invalid, validatePositiveAmount(null))
        assertEquals(Res.string.validation_amount_positive, validatePositiveAmount(0))
        assertEquals(Res.string.validation_amount_positive, validatePositiveAmount(-1))
        assertNull(validatePositiveAmount(1))
    }
}
