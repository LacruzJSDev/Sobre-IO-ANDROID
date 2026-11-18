package dev.lacruzjs.sobreio.data.cache

import kotlin.test.Test
import kotlin.test.assertEquals

class CacheInvalidationTest {

    @Test
    fun readsChangeNothing() {
        assertEquals(CacheEffect.None, cacheEffectOf("GET", "/rest/v1/accounts"))
        assertEquals(CacheEffect.None, cacheEffectOf("HEAD", "/rest/v1/movements"))
        assertEquals(CacheEffect.None, cacheEffectOf("POST", "/rest/v1/rpc/envelopes_summary"))
        assertEquals(CacheEffect.None, cacheEffectOf("POST", "/rest/v1/rpc/search_movements"))
    }

    @Test
    fun everyWriteInvalidates() {
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("POST", "/rest/v1/rpc/create_transaction"))
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("POST", "/rest/v1/envelope_transfers"))
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("PATCH", "/rest/v1/transactions"))
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("DELETE", "/rest/v1/transaction_tags"))
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("POST", "/functions/v1/delete-account"))
    }

    @Test
    fun anRpcNotKnownToReadIsTakenAsAWrite() {
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("POST", "/rest/v1/rpc/some_new_function"))
    }

    @Test
    fun changingUserClearsButARefreshKeeps() {
        assertEquals(CacheEffect.Clear, cacheEffectOf("POST", "/auth/v1/token", "grant_type=password"))
        assertEquals(CacheEffect.Clear, cacheEffectOf("POST", "/auth/v1/logout"))
        assertEquals(CacheEffect.Clear, cacheEffectOf("POST", "/auth/v1/verify"))
        assertEquals(CacheEffect.None, cacheEffectOf("POST", "/auth/v1/token", "grant_type=refresh_token"))
        assertEquals(CacheEffect.Invalidate, cacheEffectOf("PUT", "/auth/v1/user"))
    }
}
