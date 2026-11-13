package core.extensions.js

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsEnginePoolTest {

    @Test
    fun testPoolReusesEngineForSameSourceId() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 3)
        var initCount = 0

        val res1 = pool.withEngine(
            sourceId = 1L,
            initializer = {
                initCount++
                it.evaluate("var sourceSecret = 'initialized_1';")
            }
        ) { engine ->
            engine.evaluate("sourceSecret")
        }

        assertEquals("initialized_1", res1)
        assertEquals(1, initCount)

        // Second invocation should reuse the existing engine without re-initializing
        val res2 = pool.withEngine(
            sourceId = 1L,
            initializer = {
                initCount++
            }
        ) { engine ->
            engine.evaluate("sourceSecret")
        }

        assertEquals("initialized_1", res2)
        assertEquals(1, initCount, "Engine should be reused from pool without re-initialization")
        assertEquals(1, pool.activeCount())

        pool.close()
    }

    @Test
    fun testPoolEvictsLruWhenMaxCapacityReached() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 2)

        pool.withEngine(1L, { it.evaluate("var id = 1;") }) {
            assertEquals("1", it.evaluate("id"))
        }

        pool.withEngine(2L, { it.evaluate("var id = 2;") }) {
            assertEquals("2", it.evaluate("id"))
        }

        assertEquals(2, pool.activeCount())

        // Access 1L to make 2L the LRU
        pool.withEngine(1L) {
            assertEquals("1", it.evaluate("id"))
        }

        // Add 3L: Should evict 2L because 1L was accessed more recently
        pool.withEngine(3L, { it.evaluate("var id = 3;") }) {
            assertEquals("3", it.evaluate("id"))
        }

        assertEquals(2, pool.activeCount())

        // 1L should still be in pool
        pool.withEngine(1L) {
            assertEquals("1", it.evaluate("id"))
        }

        // 2L was evicted; if we access 2L, initializer will run again
        var reInitRan = false
        pool.withEngine(2L, {
            reInitRan = true
            it.evaluate("var id = 2;")
        }) {
            assertEquals("2", it.evaluate("id"))
        }
        assertTrue(reInitRan, "Evicted engine should be re-initialized upon fresh access")

        pool.close()
    }

    @Test
    fun testIdleEngineSweep() = runBlocking {
        val pool = JsEnginePool(maxActiveEngines = 5, idleTimeoutMs = 10L)

        pool.withEngine(10L, { it.evaluate("var x = 10;") }) {
            assertEquals("10", it.evaluate("x"))
        }
        assertEquals(1, pool.activeCount())

        // Wait past idle timeout
        kotlinx.coroutines.delay(20L)
        pool.sweepIdleEngines()

        assertEquals(0, pool.activeCount(), "Idle engines should be swept after timeout")

        pool.close()
    }
}
