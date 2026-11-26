package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.AppConfig

/** A browser on the Mac sees the local stack at 127.0.0.1, like the iOS simulator; a hosted project is the same URL. */
internal actual val supabaseUrl: String = AppConfig.SUPABASE_URL_IOS
