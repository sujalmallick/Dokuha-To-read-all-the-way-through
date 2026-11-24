package core.extensions.js

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * Sandboxed JavaScript engine execution context for evaluating extension scripts.
 *
 * Implemented via QuickJS across all platforms (Android, iOS, Desktop) via quickjs-kt.
 */
class JsEngine {
    private var quickJs: QuickJs? = QuickJs.create(Dispatchers.Default)
    private val mutex = Mutex()
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()

    private var currentCompletion: CompletableDeferred<String>? = null

    init {
        quickJs?.let { qjs ->
            qjs.function("__dokuha_dispatch") { args ->
                val fnName = args.getOrNull(0)?.toString() ?: error("Missing function name")
                val fn = syncFunctions[fnName] ?: error("Unregistered host function: $fnName")
                val subArgs = if (args.size > 1) args.drop(1) else emptyList()
                fn(subArgs)
            }
            qjs.asyncFunction("__dokuha_dispatchAsync") { args ->
                val fnName = args.getOrNull(0)?.toString() ?: error("Missing async function name")
                val fn = asyncFunctions[fnName] ?: error("Unregistered async host function: $fnName")
                val subArgs = if (args.size > 1) args.drop(1) else emptyList()
                fn(subArgs)
            }
            qjs.asyncFunction("__dokuha_yield") {
                // Yield to allow microtasks and coroutine event loops to execute
            }
            qjs.asyncFunction("__dokuha_finish") { args ->
                val isSuccess = args.getOrNull(0) as? Boolean ?: true
                val value = args.getOrNull(1)?.toString() ?: ""
                val deferred = currentCompletion
                if (deferred != null && !deferred.isCompleted) {
                    if (isSuccess) {
                        deferred.complete(value)
                    } else {
                        deferred.completeExceptionally(RuntimeException(value.ifBlank { "Unknown JavaScript error" }))
                    }
                }
            }
        }
    }

    suspend fun evaluate(script: String): String = mutex.withLock {
        val qjs = quickJs ?: error("JsEngine is closed")
        val trimmed = script.trim()
        val looksAsync = trimmed.startsWith("(async") ||
            trimmed.startsWith("async") ||
            trimmed.startsWith("new Promise") ||
            trimmed.startsWith("Promise.") ||
            trimmed.startsWith("await ") ||
            trimmed.startsWith("JSON.stringify(await ") ||
            trimmed.startsWith("JSON.stringify((await ")

        if (looksAsync) {
            return runAsyncScript(qjs, script)
        }

        val res = qjs.evaluate<Any?>(script)
        val str = when (res) {
            null -> "null"
            is String -> res
            else -> res.toString()
        }
        if (str.startsWith("Promise { <state>:") || str == "[object Promise]") {
            return runAsyncScript(qjs, script)
        }
        return str
    }

    private suspend fun runAsyncScript(qjs: QuickJs, script: String): String {
        val deferred = CompletableDeferred<String>()
        currentCompletion = deferred
        val cleanScript = script.trim().trimEnd(';')
        val asyncRunner = """
            (async () => {
                try {
                    let res = await ($cleanScript);
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    await __dokuha_finish(true, serialized);
                } catch (e) {
                    await __dokuha_finish(false, e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        return try {
            qjs.evaluate<Any?>(asyncRunner)
            withTimeout(30_000L) {
                deferred.await()
            }
        } finally {
            currentCompletion = null
        }
    }

    fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?) {
        val qjs = quickJs ?: error("JsEngine is closed")
        syncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        val bridgeScript = "globalThis['$escapedName'] = function(...args) { return __dokuha_dispatch('$escapedName', ...args); };"
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                qjs.evaluate<Any?>(bridgeScript)
            }
        }
    }

    fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?) {
        val qjs = quickJs ?: error("JsEngine is closed")
        asyncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        val bridgeScript = "globalThis['$escapedName'] = async function(...args) { return await __dokuha_dispatchAsync('$escapedName', ...args); };"
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                qjs.evaluate<Any?>(bridgeScript)
            }
        }
    }

    fun setGlobalVariable(name: String, jsonValue: String) {
        val qjs = quickJs ?: error("JsEngine is closed")
        val escapedName = name.replace("'", "\\'")
        val script = "globalThis['$escapedName'] = $jsonValue;"
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                qjs.evaluate<Any?>(script)
            }
        }
    }

    suspend fun callFunction(functionName: String, vararg args: Any?): String = mutex.withLock {
        val qjs = quickJs ?: error("JsEngine is closed")
        val argsJs = args.joinToString(", ") { arg ->
            when (arg) {
                null -> "null"
                is Number, is Boolean -> arg.toString()
                is String -> "\"${arg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""
                else -> arg.toString()
            }
        }
        val escapedFunctionName = functionName.replace("'", "\\'")

        val deferred = CompletableDeferred<String>()
        currentCompletion = deferred

        val script = """
            (async () => {
                try {
                    let parent = globalThis;
                    let fn;
                    try {
                        fn = eval('$escapedFunctionName');
                    } catch (_) {}
                    const lastDot = '$escapedFunctionName'.lastIndexOf('.');
                    if (lastDot !== -1) {
                        try {
                            parent = eval('$escapedFunctionName'.substring(0, lastDot));
                        } catch (_) {}
                    }
                    if (typeof fn !== 'function') {
                        throw new Error('$escapedFunctionName is not a function');
                    }
                    let res = await fn.call(parent, $argsJs);
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    await __dokuha_finish(true, serialized);
                } catch (e) {
                    await __dokuha_finish(false, e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        return try {
            qjs.evaluate<Any?>(script)
            withTimeout(30_000L) {
                deferred.await()
            }
        } finally {
            currentCompletion = null
        }
    }

    fun close() {
        currentCompletion?.cancel()
        currentCompletion = null
        // In quickjs-kt 1.0.0-alpha13, quickjs.close() calls native clear_java_vm_cache()
        // which zeroes out the process-global JavaVM pointer on Android, breaking JNI callbacks across
        // all pooled engines. We clear listeners and let the instance be reclaimed without zeroing JavaVM.
        quickJs = null
        syncFunctions.clear()
        asyncFunctions.clear()
    }
}
