package core.extensions.js

import core.platform.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * LRU bounded manager for [JsEngine] instances.
 * Caps active engine instances to [maxActiveEngines], prevents memory bloat
 * from hundreds of installed extensions, and periodically sweeps idle engines.
 */
class JsEnginePool(
    private val maxActiveEngines: Int = 5,
    private val idleTimeoutMs: Long = 300_000L // 5 minutes
) {
    private val lock = Mutex()
    private val activeEngines = mutableMapOf<Long, PooledEngineEntry>()
    private var accessCounter: Long = 0L

    data class PooledEngineEntry(
        val sourceId: Long,
        val engine: JsEngine,
        var lastAccessedAt: Long,
        var accessOrder: Long = 0L
    )

    /**
     * Executes [block] using a pooled [JsEngine] keyed by [sourceId].
     * If the engine is not yet pooled, a new instance is created and initialized via [initializer].
     */
    suspend fun <T> withEngine(
        sourceId: Long,
        initializer: suspend (JsEngine) -> Unit = {},
        block: suspend (JsEngine) -> T
    ): T {
        val engine = acquireEngine(sourceId, initializer)
        return try {
            block(engine)
        } finally {
            releaseEngine(sourceId)
        }
    }

    private suspend fun acquireEngine(sourceId: Long, initializer: suspend (JsEngine) -> Unit): JsEngine {
        val (engineToUse, needsInit) = lock.withLock {
            val now = currentTimeMillis()
            val order = ++accessCounter
            val existing = activeEngines[sourceId]
            if (existing != null) {
                existing.lastAccessedAt = now
                existing.accessOrder = order
                existing.engine to false
            } else {
                // Evict LRU engine if max limit reached
                if (activeEngines.size >= maxActiveEngines) {
                    val oldestKey = activeEngines.minByOrNull { it.value.accessOrder }?.key
                    val evicted = oldestKey?.let { activeEngines.remove(it) }
                    try {
                        evicted?.engine?.close()
                    } catch (_: Exception) {}
                }

                val engine = JsEngine()
                activeEngines[sourceId] = PooledEngineEntry(sourceId, engine, now, order)
                engine to true
            }
        }

        if (needsInit) {
            try {
                try {
                    engineToUse.evaluate("if (typeof globalThis.__qjs_set_memory_limit === 'function') globalThis.__qjs_set_memory_limit(32 * 1024 * 1024);")
                } catch (_: Exception) {}
                initializer(engineToUse)
            } catch (e: Exception) {
                lock.withLock {
                    activeEngines.remove(sourceId)?.engine?.close()
                }
                throw e
            }
        }

        return engineToUse
    }

    private suspend fun releaseEngine(sourceId: Long) = lock.withLock {
        val entry = activeEngines[sourceId] ?: return@withLock
        entry.lastAccessedAt = currentTimeMillis()
        try {
            entry.engine.evaluate("if (typeof globalThis.gc === 'function') globalThis.gc();")
        } catch (_: Exception) {}
    }

    suspend fun sweepIdleEngines() = lock.withLock {
        val now = currentTimeMillis()
        val iterator = activeEngines.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next().value
            if (now - entry.lastAccessedAt > idleTimeoutMs) {
                try {
                    entry.engine.close()
                } catch (_: Exception) {}
                iterator.remove()
            }
        }
    }

    suspend fun activeCount(): Int = lock.withLock {
        activeEngines.size
    }

    suspend fun evict(sourceId: Long) = lock.withLock {
        val entry = activeEngines.remove(sourceId)
        try {
            entry?.engine?.close()
        } catch (_: Exception) {}
    }

    suspend fun close() = lock.withLock {
        activeEngines.values.forEach {
            try {
                it.engine.close()
            } catch (_: Exception) {}
        }
        activeEngines.clear()
    }
}
