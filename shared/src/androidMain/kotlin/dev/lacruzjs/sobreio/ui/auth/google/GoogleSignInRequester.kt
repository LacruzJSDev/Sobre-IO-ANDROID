package dev.lacruzjs.sobreio.ui.auth.google

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.cancellation.CancellationException

/** Asks Credential Manager for a Google ID token, the Android side of [rememberGoogleSignInLauncher]. */
internal class GoogleSignInRequester(private val webClientId: String) {

    /**
     * [activityContext] must be an Activity, since Credential Manager shows its sheet on top of it.
     * Fails with [GoogleSignInCancelledException] when the user dismisses the sheet.
     */
    suspend fun request(activityContext: Context): Result<GoogleIdToken> {
        val rawNonce = randomNonce()
        // Google embeds the hash in the token; Supabase hashes the raw value again and compares
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(sha256Hex(rawNonce))
            .build()
        // The explicit "Sign in with Google" option fits a button the user pressed. GetGoogleIdOption is meant for
        // automatic one-tap prompts and can return "no credentials" for users who never signed in on this device.
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.success(GoogleIdToken(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce))
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(GoogleSignInCancelledException())
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            // No Google account on the device; the UI shows the generic error
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            // createFrom can fail on a malformed payload
            Result.failure(e)
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return bytes.toHex()
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
