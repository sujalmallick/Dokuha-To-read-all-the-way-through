package core.extensions.js

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class JsEngine actual constructor() {
    private var quickJs: QuickJs? = QuickJs.create(Dispatchers.Default)
    private val mutex = Mutex()
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()

    private var lastEvalResult: String? = null
    private var lastEvalError: String? = null

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
                val value = args.getOrNull(1)?.toString()
                if (isSuccess) {
                    lastEvalResult = value
                    lastEvalError = null
                } else {
                    lastEvalResult = null
                    lastEvalError = value ?: "Unknown error"
                }
            }
        }
    }

    actual suspend fun evaluate(script: String): String = mutex.withLock {
        val qjs = quickJs ?: error("JsEngine is closed")
        val res = qjs.evaluate<Any?>(script)
        val str = when (res) {
            null -> "null"
            is String -> res
            else -> res.toString()
        }
        if (str.startsWith("Promise { <state>:")) {
            lastEvalResult = null
            lastEvalError = null
            val asyncRunner = """
                (async () => {
                    try {
                        let res = ($script);
                        if (res && typeof res.then === 'function') {
                            let done = false;
                            let val = null;
                            let err = null;
                            res.then(v => { done = true; val = v; }, e => { done = true; err = e; });
                            while (!done) {
                                await __dokuha_yield();
                            }
                            if (err) throw err;
                            res = val;
                        }
                        const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                        await __dokuha_finish(true, serialized);
                    } catch (e) {
                        await __dokuha_finish(false, e && e.message ? e.message : String(e));
                    }
                })();
            """.trimIndent()
            qjs.evaluate<Any?>(asyncRunner)
            lastEvalError?.let { error("JavaScript error: $it") }
            return lastEvalResult ?: "null"
        }
        return str
    }

    actual fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?) {
        val qjs = quickJs ?: error("JsEngine is closed")
        syncFunctions[name] = block
        // Register JS wrapper
        val escapedName = name.replace("'", "\\'")
        val bridgeScript = "globalThis['$escapedName'] = function(...args) { return __dokuha_dispatch('$escapedName', ...args); };"
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                qjs.evaluate<Any?>(bridgeScript)
            }
        }
    }

    actual fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?) {
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

    actual fun setGlobalVariable(name: String, jsonValue: String) {
        val qjs = quickJs ?: error("JsEngine is closed")
        val escapedName = name.replace("'", "\\'")
        val script = "globalThis['$escapedName'] = $jsonValue;"
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                qjs.evaluate<Any?>(script)
            }
        }
    }

    actual suspend fun callFunction(functionName: String, vararg args: Any?): String = mutex.withLock {
        val qjs = quickJs ?: error("JsEngine is closed")
        val argsJs = args.joinToString(", ") { arg ->
            when (arg) {
                null -> "null"
                is Number, is Boolean -> arg.toString()
                is String -> "\"${arg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""
                else -> arg.toString()
            }
        }
        lastEvalResult = null
        lastEvalError = null

        val script = """
            (async () => {
                try {
                    let parent = globalThis;
                    let fn;
                    try {
                        fn = eval('$functionName');
                    } catch (_) {}
                    const lastDot = '$functionName'.lastIndexOf('.');
                    if (lastDot !== -1) {
                        try {
                            parent = eval('$functionName'.substring(0, lastDot));
                        } catch (_) {}
                    }
                    if (typeof fn !== 'function') {
                        throw new Error('$functionName is not a function');
                    }
                    let res = fn.call(parent, $argsJs);
                    if (res && typeof res.then === 'function') {
                        let done = false;
                        let val = null;
                        let err = null;
                        res.then(v => { done = true; val = v; }, e => { done = true; err = e; });
                        while (!done) {
                            await __dokuha_yield();
                        }
                        if (err) throw err;
                        res = val;
                    }
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    await __dokuha_finish(true, serialized);
                } catch (e) {
                    await __dokuha_finish(false, e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        qjs.evaluate<Any?>(script)

        lastEvalError?.let { error("JavaScript error in $functionName: $it") }
        return lastEvalResult ?: "null"
    }

    actual fun close() {
        quickJs?.close()
        quickJs = null
        syncFunctions.clear()
        asyncFunctions.clear()
    }
}
