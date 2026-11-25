package dev.lacruzjs.sobreio.ui.auth.google

/**
 * Whether an Android Credential Manager "user cancelled" error really comes from the user closing the account picker.
 *
 * Google Play services reports some failures with the same TYPE_USER_CANCELED: when the app's package and signing
 * certificate are not registered as an Android OAuth client, the picker shows, the user chooses an account and the
 * result is "[16] Account reauth failed." (seen on 2026-11-25 with an unregistered release key), while closing the
 * picker gives "[16] Cancelled by user.". Only a message that says the user cancelled, or no message at all, is a
 * cancellation; anything else is an error the sign-in screen must show.
 */
internal fun isUserCancellation(errorMessage: CharSequence?): Boolean {
    if (errorMessage.isNullOrBlank()) return true
    val text = errorMessage.toString().lowercase()
    return "cancelled by user" in text || "canceled by user" in text ||
        "cancelled by the user" in text || "canceled by the user" in text
}
