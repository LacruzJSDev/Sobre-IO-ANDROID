package dev.lacruzjs.sobreio.data.remote

/**
 * The Supabase URL of this platform. The local stack is reached at a different host from the Android emulator
 * (10.0.2.2) than from the iOS simulator (127.0.0.1); a hosted project has the same URL on both.
 */
internal expect val supabaseUrl: String
