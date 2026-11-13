@file:OptIn(ExperimentalForeignApi::class)

package dev.lacruzjs.sobreio.ui.auth.google

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import kotlin.coroutines.resume

/** Asks the Swift [IosGoogleSignInProvider] for a Google ID token, the iOS side of [rememberGoogleSignInLauncher]. */
internal class IosGoogleSignInRequester(private val provider: IosGoogleSignInProvider) {

    /** Fails with [GoogleSignInCancelledException] when the user dismisses the Google screen. */
    suspend fun request(): Result<GoogleIdToken> {
        val rawNonce = randomNonce()
        // Google embeds the hash in the token; Supabase hashes the raw value again and compares
        val hashedNonce = sha256Hex(rawNonce)
        return suspendCancellableCoroutine { continuation ->
            provider.signIn(hashedNonce) { idToken, cancelled ->
                // The completion could in theory fire twice; only the first one counts
                if (!continuation.isActive) return@signIn
                val result = when {
                    idToken != null -> Result.success(GoogleIdToken(idToken, rawNonce))
                    cancelled -> Result.failure(GoogleSignInCancelledException())
                    else -> Result.failure(IllegalStateException("Google sign-in returned no ID token"))
                }
                continuation.resume(result)
            }
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES)
        val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, bytes.size.convert(), it.addressOf(0)) }
        check(status == 0) { "SecRandomCopyBytes failed with status $status" }
        return bytes.toHex()
    }

    private fun sha256Hex(value: String): String {
        val input = value.encodeToByteArray()
        val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
        input.usePinned { inputPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(inputPinned.addressOf(0), input.size.convert(), digestPinned.addressOf(0))
            }
        }
        return digest.toByteArray().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private companion object {
        const val NONCE_BYTES = 32
    }
}
