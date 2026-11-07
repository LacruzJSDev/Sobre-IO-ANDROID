package dev.lacruzjs.sobreio.domain.model

/** Whether there is a session. Root navigation follows it. */
sealed interface AuthState {

    /** The stored session is still being read, so neither screen set can be shown yet. */
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val user: User) : AuthState
}
