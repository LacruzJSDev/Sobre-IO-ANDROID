package dev.lacruzjs.sobreio.data.cache

import io.ktor.client.plugins.api.createClientPlugin

/** What a request to Supabase does to [QueryCache]. */
enum class CacheEffect { None, Invalidate, Clear }

/**
 * RPC functions that only read. Every other RPC writes and invalidates the cache. Forgetting a read here only costs an
 * extra request; a write can never be missed, because anything not listed invalidates.
 *
 * `materialize_through_today` writes, but only what the calendar already implies (targets and due dates up to today),
 * runs every time the Envelopes tab shows and is cached for 30 s itself: invalidating on it would empty the cache on
 * every tab change.
 */
internal val READ_ONLY_RPCS = setOf(
    "account_balance_at", "account_balance_series", "account_deletion_preview", "envelope_balance_at",
    "envelope_outlook", "export_period", "group_outlook", "match_pending", "account_forecast", "account_period_flows",
    "account_projection", "bank_import_preview", "commitment_funding_status", "compare_with_bank", "current_period",
    "envelope_progress", "envelopes_summary", "group_balance_series", "group_free_projection", "group_period_flows",
    "group_today", "materialize_through_today", "pending_occurrences", "search_movements", "search_movements_by_envelope", "search_movements_by_period", "search_movements_count",
    "search_movements_summary",
    "tag_usage", "transfer_plan", "upcoming_occurrences",
)

/**
 * Pure rule behind [cacheInvalidationPlugin]. Reads (GET, HEAD and the RPCs in [READ_ONLY_RPCS]) change nothing.
 * Signing in, up or out clears it, so one user's figures are never shown to another; a token refresh keeps it.
 * Anything else (inserts, updates, write RPCs, Edge Functions, the profile name) invalidates it.
 */
fun cacheEffectOf(method: String, path: String, query: String = ""): CacheEffect {
    val verb = method.uppercase()
    if (verb == "GET" || verb == "HEAD" || verb == "OPTIONS") return CacheEffect.None
    val authAt = path.indexOf("/auth/v1/")
    if (authAt >= 0) {
        val endpoint = path.substring(authAt + "/auth/v1/".length).trimEnd('/')
        return when {
            endpoint == "user" -> CacheEffect.Invalidate
            endpoint == "token" && "grant_type=refresh_token" in query -> CacheEffect.None
            else -> CacheEffect.Clear
        }
    }
    val rpcAt = path.indexOf("/rest/v1/rpc/")
    if (rpcAt >= 0) {
        val name = path.substring(rpcAt + "/rest/v1/rpc/".length).trimEnd('/')
        if (name in READ_ONLY_RPCS) return CacheEffect.None
    }
    return CacheEffect.Invalidate
}

/** Installed on the Supabase HTTP client: every answer to a write invalidates [cache] (even a refusal: harmless). */
fun cacheInvalidationPlugin(cache: QueryCache) = createClientPlugin("SobreioCacheInvalidation") {
    onResponse { response ->
        val request = response.call.request
        when (cacheEffectOf(request.method.value, request.url.encodedPath, request.url.encodedQuery)) {
            CacheEffect.None -> Unit
            CacheEffect.Invalidate -> cache.invalidateAll()
            CacheEffect.Clear -> cache.clear()
        }
    }
}
