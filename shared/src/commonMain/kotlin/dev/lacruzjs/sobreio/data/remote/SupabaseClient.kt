package dev.lacruzjs.sobreio.data.remote

import dev.lacruzjs.sobreio.AppConfig
import dev.lacruzjs.sobreio.data.cache.QueryCache
import dev.lacruzjs.sobreio.data.cache.cacheInvalidationPlugin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * The single Supabase client of the app. Only the publishable key ships in the app; every rule is enforced by
 * RLS and RPC functions in the database (D-19, D-25). Every write it sends invalidates [cache].
 */
// httpConfig is marked internal in supabase-kt 3.8 but is the documented way to add a Ktor plugin
@OptIn(SupabaseInternal::class)
fun buildSupabaseClient(cache: QueryCache): SupabaseClient = createSupabaseClient(
    supabaseUrl = supabaseUrl,
    supabaseKey = AppConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    // New columns or view fields must not break older APKs
    defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    install(Auth)
    install(Postgrest)
    install(Functions)
    httpConfig { install(cacheInvalidationPlugin(cache)) }
}
