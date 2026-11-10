package core.extensions.js

import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSString
import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSValue

actual class JsEngine actual constructor() {
    private var context: JSContext? = JSContext()
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default)

    init {
        context?.let { ctx ->
            val dispatcher: (String, String) -> String? = { fnName, argsJson ->
                val fn = syncFunctions[fnName]
                if (fn != null) {
                    val result = fn(listOf(argsJson))
                    result?.toString()
                } else {
                    null
                }
            }
            ctx.setObject(dispatcher, forKeyedSubscript = "__dokuha_native_dispatcher" as NSString)

            val asyncDispatcher: (String, String, String) -> Unit = { fnName, reqId, argsJson ->
                val fn = asyncFunctions[fnName]
                if (fn != null) {
                    kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val result = fn(listOf(argsJson))
                            val serializedResult = if (result is String) "\"${result.replace("\"", "\\\"")}\"" else (result?.toString() ?: "null")
                            context?.evaluateScript("__dokuha_resolve_async('$reqId', $serializedResult);")
                        } catch (e: Exception) {
                            val err = e.message ?: "Error"
                            context?.evaluateScript("__dokuha_reject_async('$reqId', '$err');")
                        }
                    }
                }
            }
            ctx.setObject(asyncDispatcher, forKeyedSubscript = "__dokuha_native_async_dispatcher" as NSString)
        }
    }

    actual suspend fun evaluate(script: String): String {
        val ctx = context ?: error("JsEngine is closed")
        val res = ctx.evaluateScript(script)
        val ex = ctx.exception
        if (ex != null) {
            val msg = ex.toString()
            ctx.exception = null
            error("JavaScript error: $msg")
        }
        return res?.toString() ?: ""
    }

    actual fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        syncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        ctx.evaluateScript(
            """
            globalThis['$escapedName'] = function(...args) {
                const serialized = JSON.stringify(args);
                const res = __dokuha_native_dispatcher('$escapedName', serialized);
                try {
                    return JSON.parse(res);
                } catch (_) {
                    return res;
                }
            };
            """.trimIndent()
        )
    }

    actual fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        asyncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        ctx.evaluateScript(
            """
            globalThis.__dokuha_pending = globalThis.__dokuha_pending || {};
            globalThis.__dokuha_resolve_async = function(id, res) {
                if (globalThis.__dokuha_pending[id]) {
                    globalThis.__dokuha_pending[id].resolve(res);
                    delete globalThis.__dokuha_pending[id];
                }
            };
            globalThis.__dokuha_reject_async = function(id, err) {
                if (globalThis.__dokuha_pending[id]) {
                    globalThis.__dokuha_pending[id].reject(new Error(err));
                    delete globalThis.__dokuha_pending[id];
                }
            };
            globalThis['$escapedName'] = function(...args) {
                return new Promise(function(resolve, reject) {
                    const id = Math.random().toString(36).substring(2);
                    globalThis.__dokuha_pending[id] = { resolve: resolve, reject: reject };
                    __dokuha_native_async_dispatcher('$escapedName', id, JSON.stringify(args));
                });
            };
            """.trimIndent()
        )
    }

    actual fun setGlobalVariable(name: String, jsonValue: String) {
        val ctx = context ?: error("JsEngine is closed")
        val escapedName = name.replace("'", "\\'")
        ctx.evaluateScript("globalThis['$escapedName'] = $jsonValue;")
    }

    actual suspend fun callFunction(functionName: String, vararg args: Any?): String {
        val ctx = context ?: error("JsEngine is closed")
        val argsJs = args.joinToString(", ") { arg ->
            when (arg) {
                null -> "null"
                is Number, is Boolean -> arg.toString()
                is String -> "\"${arg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""
                else -> arg.toString()
            }
        }
        val script = """
            (async () => {
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
                    const res = await fn.call(parent, $argsJs);
                    return typeof res === 'object' && res !== null ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
            })()
        """.trimIndent()

        val promiseVal = ctx.evaluateScript(script)
        if (ctx.exception != null) {
            val err = ctx.exception.toString()
            ctx.exception = null
            error("JavaScript error: $err")
        }

        if (promiseVal != null && promiseVal.hasProperty("then")) {
            val deferred = CompletableDeferred<String>()
            val resolveCallback: (Any?) -> Unit = { value ->
                deferred.complete(value?.toString() ?: "")
            }
            val rejectCallback: (Any?) -> Unit = { error ->
                deferred.completeExceptionally(RuntimeException("Promise rejected: $error"))
            }
            promiseVal.invokeMethod("then", listOf(resolveCallback, rejectCallback))
            return deferred.await()
        }

        return promiseVal?.toString() ?: ""
    }

    actual fun close() {
        context = null
        syncFunctions.clear()
    }
}
