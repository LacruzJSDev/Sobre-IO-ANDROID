package dev.lacruzjs.sobreio.fake

import dev.lacruzjs.sobreio.data.repository.AuthRepository
import dev.lacruzjs.sobreio.domain.model.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Every call is recorded as a readable string in [calls]; set [result] to make all calls fail or succeed. */
class FakeAuthRepository(initial: AuthState = AuthState.SignedOut) : AuthRepository {

    val state = MutableStateFlow(initial)
    override val authState: Flow<AuthState> = state

    val calls = mutableListOf<String>()

    /** Result returned by every operation; change it between calls to script a failure and then a success. */
    var result: Result<Unit> = Result.success(Unit)

    override suspend fun signIn(email: String, password: String): Result<Unit> = record("signIn($email, $password)")

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> =
        record("signUp($name, $email, $password)")

    override suspend fun confirmSignUp(email: String, code: String): Result<Unit> = record("confirmSignUp($email, $code)")

    override suspend fun resendSignUpCode(email: String): Result<Unit> = record("resendSignUpCode($email)")

    override suspend fun requestPasswordReset(email: String): Result<Unit> = record("requestPasswordReset($email)")

    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        record("resetPassword($email, $code, $newPassword)")

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): Result<Unit> =
        record("signInWithGoogle($idToken, $rawNonce)")

    override suspend fun signOut(): Result<Unit> = record("signOut()")

    override suspend fun updateDisplayName(name: String): Result<Unit> = record("updateDisplayName($name)")

    private fun record(call: String): Result<Unit> {
        calls += call
        return result
    }
}
