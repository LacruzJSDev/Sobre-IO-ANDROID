package dev.lacruzjs.sobreio.data.cache

import dev.lacruzjs.sobreio.ui.loading.revalidate
import dev.lacruzjs.sobreio.ui.loading.stopIfCachePass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class QueryCacheTest {

    private val time = TestTimeSource()
    private val cache = QueryCache(maxAge = 30.seconds, timeSource = time)
    private var fetches = 0

    private suspend fun read(value: String = "v", key: String = "k"): Result<String> =
        cache.get("q", key) { fetches++; Result.success(value) }

    @Test
    fun aFreshValueIsServedWithoutAskingAgain() = runTest {
        read("a")
        time += 29.seconds
        assertEquals("a", read("b").getOrThrow())
        assertEquals(1, fetches)
    }

    @Test
    fun aValueOlderThanTheLimitIsReadAgain() = runTest {
        read("a")
        time += 31.seconds
        assertEquals("b", read("b").getOrThrow())
        assertEquals(2, fetches)
    }

    @Test
    fun differentArgumentsAreDifferentKeys() = runTest {
        read("a", key = "1")
        read("b", key = "2")
        assertEquals(2, fetches)
    }

    @Test
    fun aWriteInvalidatesEverything() = runTest {
        read("a")
        cache.invalidateAll()
        assertEquals("b", read("b").getOrThrow())
        assertEquals(2, fetches)
    }

    @Test
    fun failuresAreNeverStored() = runTest {
        cache.get("q", "k") { fetches++; Result.failure<String>(IllegalStateException()) }
        assertEquals("a", read("a").getOrThrow())
        assertEquals(2, fetches)
    }

    @Test
    fun anAnswerThatStartedBeforeAWriteIsKeptStale() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slow = async { cache.get("q", "k") { gate.await(); Result.success("old") } }
        yield()
        cache.invalidateAll()
        gate.complete(Unit)
        slow.await()
        // The value stays to be painted, but the next read asks the database
        assertEquals("new", read("new").getOrThrow())
        assertEquals(1, fetches)
    }

    @Test
    fun twoReadersOfOneKeyMakeOneRequest() = runTest {
        val gate = CompletableDeferred<Unit>()
        val first = async { cache.get("q", "k") { fetches++; gate.await(); Result.success("a") } }
        val second = async { read("b") }
        yield()
        gate.complete(Unit)
        assertEquals("a", first.await().getOrThrow())
        assertEquals("a", second.await().getOrThrow())
        assertEquals(1, fetches)
    }

    @Test
    fun clearForgetsTheValues() = runTest {
        read("a")
        cache.clear()
        var painted: String? = null
        revalidate(onRefreshing = {}) { painted = read("b").getOrThrow() }
        assertEquals("b", painted)
    }

    @Test
    fun revalidatePaintsTheCachedValueThenTheFreshOneWithTheBar() = runTest {
        read("old")
        cache.invalidateAll()
        val painted = mutableListOf<String>()
        val refreshing = mutableListOf<Boolean>()
        val asked = revalidate(onRefreshing = { refreshing += it }) { painted += read("new").getOrThrow() }
        assertTrue(asked)
        assertEquals(listOf("old", "new"), painted)
        assertEquals(listOf(true, false), refreshing)
    }

    @Test
    fun revalidateStopsAtTheCacheWhenEverythingIsFresh() = runTest {
        read("a")
        val painted = mutableListOf<String>()
        val asked = revalidate(onRefreshing = { error("no bar expected") }) { painted += read("b").getOrThrow() }
        assertFalse(asked)
        assertEquals(listOf("a"), painted)
    }

    @Test
    fun revalidateGoesToTheDatabaseWhenSomethingWasNeverRead() = runTest {
        val painted = mutableListOf<String>()
        revalidate(onRefreshing = {}) { painted += read("a").getOrThrow() }
        assertEquals(listOf("a"), painted)
        assertEquals(1, fetches)
    }

    @Test
    fun anInconsistentCachedAnswerLetsTheDatabaseDecide() = runTest {
        read("stale list")
        cache.invalidateAll()
        val errors = mutableListOf<String>()
        revalidate(onRefreshing = {}) {
            val value = read("fresh list").getOrThrow()
            if (value == "stale list") {
                stopIfCachePass()
                errors += "not found"
            }
        }
        assertTrue(errors.isEmpty())
    }
}
