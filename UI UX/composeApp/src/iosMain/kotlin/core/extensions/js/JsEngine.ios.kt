package core.extensions.js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.JavaScriptCore.JSContext

actual class JsEngine actual constructor() {
    private var context: JSContext? = JSContext()
    private val mutex = Mutex()
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    actual suspend fun evaluate(script: String): String {
        val ctx = context ?: error("JsEngine is closed")
        val res = mutex.withLock {
            val evaluated = ctx.evaluateScript(script)
            val ex = ctx.exception
            if (ex != null) {
                val msg = ex.toString()
                ctx.exception = null
                error("JavaScript error: $msg")
            }
            evaluated
        }
        return res?.toString() ?: ""
    }

    actual fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        syncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript(
                    """
                    globalThis['$escapedName'] = function(...args) {
                        return null;
                    };
                    """.trimIndent()
                )
            }
        }
    }

    actual fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?) {
        val ctx = context ?: error("JsEngine is closed")
        asyncFunctions[name] = block
        val escapedName = name.replace("'", "\\'")
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript(
                    """
                    globalThis['$escapedName'] = async function(...args) {
                        return null;
                    };
                    """.trimIndent()
                )
            }
        }
    }

    actual fun setGlobalVariable(name: String, jsonValue: String) {
        val ctx = context ?: error("JsEngine is closed")
        val escapedName = name.replace("'", "\\'")
        runBlocking {
            mutex.withLock {
                ctx.evaluateScript("globalThis['$escapedName'] = $jsonValue;")
            }
        }
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
        val escapedFunctionName = functionName.replace("'", "\\'")
        val script = """
            (() => {
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
                const res = fn.call(parent, $argsJs);
                return typeof res === 'object' && res !== null ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
            })()
        """.trimIndent()

        val evaluated = mutex.withLock {
            val res = ctx.evaluateScript(script)
            if (ctx.exception != null) {
                val err = ctx.exception.toString()
                ctx.exception = null
                error("JavaScript error in $functionName: $err")
            }
            res
        }

        return evaluated?.toString() ?: ""
    }

    actual fun close() {
        coroutineScope.cancel()
        context = null
        syncFunctions.clear()
        asyncFunctions.clear()
    }
}
