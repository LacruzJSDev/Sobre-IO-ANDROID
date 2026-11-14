package dev.lacruzjs.sobreio.data.repository

import dev.lacruzjs.sobreio.data.remote.mapper.toDomain
import dev.lacruzjs.sobreio.data.remote.safeCall
import dev.lacruzjs.sobreio.domain.error.AppError
import dev.lacruzjs.sobreio.domain.model.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.runningFold
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(client: SupabaseClient) : AuthRepository {

    private val auth = client.auth
    private val postgrest = client.postgrest

    // True while a password reset is half-way: the recovery code already opened a session, but the app must not
    // be shown until the new password is set.
    private val holdSession = MutableStateFlow(false)

    private val sessionState: Flow<AuthState> = auth.sessionStatus
        .runningFold<SessionStatus, AuthState>(AuthState.Loading) { previous, status ->
            when (status) {
                SessionStatus.Initializing -> AuthState.Loading
                is SessionStatus.Authenticated ->
                    status.session.user?.let { AuthState.SignedIn(it.toDomain()) } ?: AuthState.SignedOut
                // The token could not be refreshed (usually no network). RefreshFailure carries no session, so the
                // last known user is kept: a network blip must not throw the user out. If nothing was known
                // (cold start offline) there is nobody to show, so it is SignedOut.
                is SessionStatus.RefreshFailure -> previous as? AuthState.SignedIn ?: AuthState.SignedOut
                is SessionStatus.NotAuthenticated -> AuthState.SignedOut
            }
        }
        .distinctUntilChanged()

    override val authState: Flow<AuthState> = combine(sessionState, holdSession) { state, hold ->
        if (hold && state is AuthState.SignedIn) AuthState.SignedOut else state
    }.distinctUntilChanged()

    override suspend fun signIn(email: String, password: String): Result<Unit> = safeCall {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<Unit> = safeCall {
        // Email confirmation is required (D-43), so this returns no session. The profile trigger reads "name".
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("name", name) }
        }
        Unit
    }

    override suspend fun confirmSignUp(email: String, code: String): Result<Unit> = safeCall {
        auth.verifyEmailOtp(type = OtpType.Email.SIGNUP, email = email, token = code)
        Unit
    }

    override suspend fun resendSignUpCode(email: String): Result<Unit> = safeCall {
        auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> = safeCall {
        auth.resetPasswordForEmail(email)
    }

    // The recovery code opens a session before the new password is set. Both steps run as one non-cancellable unit
    // with the session held back, so leaving the screen half-way can never end with the user signed in on the old
    // password: either the new password is set, or the session is dropped.
    override suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> =
        withContext(NonCancellable) {
            holdSession.value = true
            try {
                safeCall {
                    auth.verifyEmailOtp(OtpType.Email.RECOVERY, email, code)
                    safeCall { auth.updateUser { password = newPassword } }.onFailure {
                        safeCall { auth.signOut() }
                        throw it
                    }
                    Unit
                }
            } finally {
                holdSession.value = false
            }
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String): Result<Unit> = safeCall {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            provider = Google
            nonce = rawNonce
        }
    }

    override suspend fun signOut(): Result<Unit> = safeCall { auth.signOut() }

    override suspend fun updateDisplayName(name: String): Result<Unit> = safeCall {
        val userId = auth.currentUserOrNull()?.id ?: throw AppError.Unknown(IllegalStateException("No signed-in user"))
        // The profile is what the other members see; the session's metadata is what this app shows as the user's name
        postgrest.from("profiles").update({ set("display_name", name) }) { filter { eq("id", userId) } }
        auth.updateUser { data = buildJsonObject { put("name", name) } }
        Unit
    }
}
