package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/** Everything about who is signed in. Failures are [dev.lacruzjs.sobreio.domain.error.AppError]s. */
interface AuthRepository {

    val authState: Flow<AuthState>

    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Creates the account and sends a 6-digit code; the user is not signed in until [confirmSignUp]. */
    suspend fun signUp(name: String, email: String, password: String): Result<Unit>

    /** Confirms the email with the code from [signUp]; on success the session starts. */
    suspend fun confirmSignUp(email: String, code: String): Result<Unit>

    suspend fun resendSignUpCode(email: String): Result<Unit>

    /** Sends a 6-digit recovery code to [email]. */
    suspend fun requestPasswordReset(email: String): Result<Unit>

    /** Checks the recovery code and sets the new password; on success the session starts. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit>

    /** [rawNonce] is the one whose SHA-256 hash was sent to Google. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String): Result<Unit>

    suspend fun signOut(): Result<Unit>
}
