package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * The single Supabase client of the app. Only the publishable key ships in the APK; every rule is enforced by
 * RLS and RPC functions in the database (D-19, D-25).
 */
fun buildSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    // New columns or view fields must not break older APKs
    defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    install(Auth)
    install(Postgrest)
}
