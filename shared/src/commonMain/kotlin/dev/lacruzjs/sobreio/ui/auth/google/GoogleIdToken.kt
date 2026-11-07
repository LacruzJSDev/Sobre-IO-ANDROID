package dev.lacruzjs.sobreio.ui.auth.google

/** What Supabase needs from Google: the ID token and the raw nonce whose hash Google signed into it. */
data class GoogleIdToken(val idToken: String, val rawNonce: String)
