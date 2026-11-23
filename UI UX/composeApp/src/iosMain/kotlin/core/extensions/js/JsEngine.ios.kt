@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package core.extensions.js

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.get
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import platform.JavaScriptCore.JSContext
import platform.JavaScriptCore.JSContextGetGlobalContext
import platform.JavaScriptCore.JSContextGetGlobalObject
import platform.JavaScriptCore.JSContextRef
import platform.JavaScriptCore.JSGlobalContextRef
import platform.JavaScriptCore.JSObjectCallAsFunctionCallback
import platform.JavaScriptCore.JSObjectMakeFunctionWithCallback
import platform.JavaScriptCore.JSObjectRef
import platform.JavaScriptCore.JSObjectSetProperty
import platform.JavaScriptCore.JSStringCreateWithUTF8CString
import platform.JavaScriptCore.JSStringGetMaximumUTF8CStringSize
import platform.JavaScriptCore.JSStringGetUTF8CString
import platform.JavaScriptCore.JSStringRelease
import platform.JavaScriptCore.JSValueIsBoolean
import platform.JavaScriptCore.JSValueIsNull
import platform.JavaScriptCore.JSValueIsNumber
import platform.JavaScriptCore.JSValueIsString
import platform.JavaScriptCore.JSValueIsUndefined
import platform.JavaScriptCore.JSValueMakeBoolean
import platform.JavaScriptCore.JSValueMakeNull
import platform.JavaScriptCore.JSValueMakeNumber
import platform.JavaScriptCore.JSValueMakeString
import platform.JavaScriptCore.JSValueMakeUndefined
import platform.JavaScriptCore.JSValueRef
import platform.JavaScriptCore.JSValueRefVar
import platform.JavaScriptCore.JSValueToBoolean
import platform.JavaScriptCore.JSValueToNumber
import platform.JavaScriptCore.JSValueToStringCopy
import platform.posix.size_t

private val activeEngines = mutableMapOf<JSGlobalContextRef, JsEngine>()

private fun syncDispatcherCallback(
    ctx: JSContextRef?,
    function: JSObjectRef?,
    thisObject: JSObjectRef?,
    argumentCount: size_t,
    arguments: CPointer<JSValueRefVar>?,
    exception: CPointer<JSValueRefVar>?
): JSValueRef? {
    if (ctx == null) return null
    val engine = activeEngines[ctx] ?: activeEngines[JSContextGetGlobalContext(ctx)] ?: return JSValueMakeUndefined(ctx)
    return engine.handleSyncDispatch(ctx, argumentCount, arguments)
}

private fun asyncDispatcherCallback(
    ctx: JSContextRef?,
    function: JSObjectRef?,
    thisObject: JSObjectRef?,
    argumentCount: size_t,
    arguments: CPointer<JSValueRefVar>?,
    exception: CPointer<JSValueRefVar>?
): JSValueRef? {
    if (ctx == null) return null
    val engine = activeEngines[ctx] ?: activeEngines[JSContextGetGlobalContext(ctx)] ?: return JSValueMakeUndefined(ctx)
    engine.handleAsyncDispatch(ctx, argumentCount, arguments)
    return JSValueMakeUndefined(ctx)
}

private fun finishCallback(
    ctx: JSContextRef?,
    function: JSObjectRef?,
    thisObject: JSObjectRef?,
    argumentCount: size_t,
    arguments: CPointer<JSValueRefVar>?,
    exception: CPointer<JSValueRefVar>?
): JSValueRef? {
    if (ctx == null) return null
    val engine = activeEngines[ctx] ?: activeEngines[JSContextGetGlobalContext(ctx)] ?: return JSValueMakeUndefined(ctx)
    engine.handleFinish(ctx, argumentCount, arguments)
    return JSValueMakeUndefined(ctx)
}

actual class JsEngine actual constructor() {
    private var context: JSContext? = JSContext()
    private val mutex = Mutex()
    private val syncFunctions = mutableMapOf<String, (List<Any?>) -> Any?>()
    private val asyncFunctions = mutableMapOf<String, suspend (List<Any?>) -> Any?>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var currentCompletion: CompletableDeferred<String>? = null

    init {
        context?.let { ctx ->
            val globalContextRef = ctx.JSGlobalContextRef()
            if (globalContextRef != null) {
                activeEngines[globalContextRef] = this
                registerNativeFunction(globalContextRef, "__dokuha_native_sync_dispatch", staticCFunction(::syncDispatcherCallback))
                registerNativeFunction(globalContextRef, "__dokuha_native_async_dispatch", staticCFunction(::asyncDispatcherCallback))
                registerNativeFunction(globalContextRef, "__dokuha_finish", staticCFunction(::finishCallback))
            }
            ctx.evaluateScript(
                """
                globalThis.__dokuha_call_seq = 0;
                globalThis.__dokuha_pending_calls = {};
                """.trimIndent()
            )
        }
    }

    actual suspend fun evaluate(script: String): String = mutex.withLock {
        val ctx = context ?: error("JsEngine is closed")
        val trimmed = script.trim()
        val looksAsync = trimmed.startsWith("(async") ||
            trimmed.startsWith("async") ||
            trimmed.startsWith("new Promise") ||
            trimmed.startsWith("Promise.") ||
            trimmed.startsWith("await ") ||
            trimmed.startsWith("JSON.stringify(await ") ||
            trimmed.startsWith("JSON.stringify((await ")

        if (looksAsync) {
            return runAsyncScript(ctx, script)
        }

        val evaluated = ctx.evaluateScript(script)
        val ex = ctx.exception
        if (ex != null) {
            val msg = ex.toString()
            ctx.exception = null
            error("JavaScript error: $msg")
        }
        val str = evaluated?.toString() ?: ""
        if (str.startsWith("Promise { <state>:") || str == "[object Promise]") {
            return runAsyncScript(ctx, script)
        }
        return str
    }

    private suspend fun runAsyncScript(ctx: JSContext, script: String): String {
        val deferred = CompletableDeferred<String>()
        currentCompletion = deferred
        val cleanScript = script.trim().trimEnd(';')
        val asyncRunner = """
            (async () => {
                try {
                    let res = await ($cleanScript);
                    const serialized = (typeof res === 'object' && res !== null) ? JSON.stringify(res) : (res === undefined ? 'undefined' : String(res));
                    __dokuha_finish('true', serialized);
                } catch (e) {
                    __dokuha_finish('false', e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        return try {
            ctx.evaluateScript(asyncRunner)
            withTimeout(30_000L) {
                deferred.await()
            }
        } finally {
            currentCompletion = null
        }
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
                        return __dokuha_native_sync_dispatch('$escapedName', JSON.stringify(args));
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
                    globalThis['$escapedName'] = function(...args) {
                        const callId = String(++globalThis.__dokuha_call_seq);
                        return new Promise(function(resolve, reject) {
                            globalThis.__dokuha_pending_calls[callId] = { resolve: resolve, reject: reject };
                            __dokuha_native_async_dispatch('$escapedName', callId, JSON.stringify(args));
                        });
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

    actual suspend fun callFunction(functionName: String, vararg args: Any?): String = mutex.withLock {
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
                    __dokuha_finish('true', serialized);
                } catch (e) {
                    __dokuha_finish('false', e && e.message ? e.message : String(e));
                }
            })();
        """.trimIndent()

        return try {
            ctx.evaluateScript(script)
            withTimeout(30_000L) {
                deferred.await()
            }
        } finally {
            currentCompletion = null
        }
    }

    actual fun close() {
        val globalContextRef = context?.JSGlobalContextRef()
        if (globalContextRef != null) {
            activeEngines.remove(globalContextRef)
        }
        currentCompletion?.cancel()
        currentCompletion = null
        coroutineScope.cancel()
        context = null
        syncFunctions.clear()
        asyncFunctions.clear()
    }

    internal fun handleSyncDispatch(
        ctx: JSContextRef,
        argumentCount: size_t,
        arguments: CPointer<JSValueRefVar>?
    ): JSValueRef? {
        val count = argumentCount.toInt()
        if (count < 1 || arguments == null) {
            return JSValueMakeNull(ctx)
        }
        val fnName = jsValueToString(ctx, arguments[0])
        val argsJson = if (count > 1) jsValueToString(ctx, arguments[1]) else "[]"
        val fn = syncFunctions[fnName] ?: return JSValueMakeNull(ctx)

        val kotlinArgs = parseArgsJson(argsJson)
        val result = try {
            fn(kotlinArgs)
        } catch (_: Throwable) {
            return JSValueMakeNull(ctx)
        }

        return when (result) {
            null -> JSValueMakeNull(ctx)
            is Boolean -> JSValueMakeBoolean(ctx, result)
            is Number -> JSValueMakeNumber(ctx, result.toDouble())
            is String -> stringToJsValue(ctx, result)
            else -> stringToJsValue(ctx, result.toString())
        }
    }

    internal fun handleAsyncDispatch(
        ctx: JSContextRef,
        argumentCount: size_t,
        arguments: CPointer<JSValueRefVar>?
    ) {
        val count = argumentCount.toInt()
        if (count < 2 || arguments == null) return
        val fnName = jsValueToString(ctx, arguments[0])
        val callId = jsValueToString(ctx, arguments[1])
        val argsJson = if (count > 2) jsValueToString(ctx, arguments[2]) else "[]"

        val fn = asyncFunctions[fnName] ?: run {
            resolveAsyncCall(callId, null, error = "Unregistered async function: $fnName")
            return
        }

        val kotlinArgs = parseArgsJson(argsJson)
        coroutineScope.launch {
            try {
                val result = fn(kotlinArgs)
                resolveAsyncCall(callId, result, error = null)
            } catch (e: Throwable) {
                resolveAsyncCall(callId, null, error = e.message ?: "Unknown error")
            }
        }
    }

    internal fun handleFinish(
        ctx: JSContextRef,
        argumentCount: size_t,
        arguments: CPointer<JSValueRefVar>?
    ) {
        val count = argumentCount.toInt()
        if (count < 1 || arguments == null) return
        val isSuccessStr = jsValueToString(ctx, arguments[0])
        val isSuccess = isSuccessStr == "true" || isSuccessStr == "1"
        val value = if (count > 1) jsValueToString(ctx, arguments[1]) else ""
        val deferred = currentCompletion
        if (deferred != null && !deferred.isCompleted) {
            if (isSuccess) {
                deferred.complete(value)
            } else {
                deferred.completeExceptionally(RuntimeException(value.ifBlank { "Unknown JavaScript error" }))
            }
        }
    }

    private fun resolveAsyncCall(callId: String, result: Any?, error: String?) {
        val escapedCallId = callId.replace("'", "\\'")
        coroutineScope.launch {
            mutex.withLock {
                val ctx = context ?: return@withLock
                val script = if (error != null) {
                    val escapedErr = error.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ")
                    """
                    if (globalThis.__dokuha_pending_calls && globalThis.__dokuha_pending_calls['$escapedCallId']) {
                        const cb = globalThis.__dokuha_pending_calls['$escapedCallId'];
                        delete globalThis.__dokuha_pending_calls['$escapedCallId'];
                        cb.reject(new Error('$escapedErr'));
                    }
                    """.trimIndent()
                } else {
                    val serialized = when (result) {
                        null -> "null"
                        is Boolean, is Number -> result.toString()
                        is String -> "\"${result.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""
                        else -> "\"${result.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""
                    }
                    """
                    if (globalThis.__dokuha_pending_calls && globalThis.__dokuha_pending_calls['$escapedCallId']) {
                        const cb = globalThis.__dokuha_pending_calls['$escapedCallId'];
                        delete globalThis.__dokuha_pending_calls['$escapedCallId'];
                        cb.resolve($serialized);
                    }
                    """.trimIndent()
                }
                ctx.evaluateScript(script)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun registerNativeFunction(
    ctx: JSGlobalContextRef,
    name: String,
    callback: JSObjectCallAsFunctionCallback
) {
    val globalObj = JSContextGetGlobalObject(ctx) ?: return
    val nameJsStr = JSStringCreateWithUTF8CString(name) ?: return
    val funcObj = JSObjectMakeFunctionWithCallback(ctx, nameJsStr, callback) ?: run {
        JSStringRelease(nameJsStr)
        return
    }
    JSObjectSetProperty(
        ctx,
        globalObj,
        nameJsStr,
        funcObj,
        0u, // kJSPropertyAttributeNone
        null
    )
    JSStringRelease(nameJsStr)
}

@OptIn(ExperimentalForeignApi::class)
private fun jsValueToString(ctx: JSContextRef?, value: JSValueRef?): String {
    if (ctx == null || value == null) return ""
    if (JSValueIsUndefined(ctx, value)) return ""
    if (JSValueIsNull(ctx, value)) return "null"
    if (JSValueIsBoolean(ctx, value)) return JSValueToBoolean(ctx, value).toString()
    if (JSValueIsNumber(ctx, value)) {
        val num = JSValueToNumber(ctx, value, null)
        return if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
    }
    val jsStr = JSValueToStringCopy(ctx, value, null) ?: return ""
    return try {
        val maxBytes = JSStringGetMaximumUTF8CStringSize(jsStr)
        if (maxBytes == 0UL) return ""
        val buffer = ByteArray(maxBytes.toInt())
        val bytesWritten = buffer.usePinned { pinned ->
            JSStringGetUTF8CString(jsStr, pinned.addressOf(0), maxBytes)
        }
        if (bytesWritten <= 1UL) return ""
        buffer.decodeToString(0, (bytesWritten - 1UL).toInt())
    } finally {
        JSStringRelease(jsStr)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun stringToJsValue(ctx: JSContextRef?, str: String): JSValueRef? {
    if (ctx == null) return null
    val jsStr = JSStringCreateWithUTF8CString(str) ?: return null
    val jsVal = JSValueMakeString(ctx, jsStr)
    JSStringRelease(jsStr)
    return jsVal
}

private fun parseArgsJson(jsonStr: String): List<Any?> {
    if (jsonStr.isBlank() || jsonStr == "[]" || jsonStr == "null") return emptyList()
    return try {
        val element = Json.parseToJsonElement(jsonStr)
        if (element is JsonArray) {
            element.map { jsonElementToAny(it) }
        } else {
            listOf(jsonElementToAny(element))
        }
    } catch (_: Throwable) {
        listOf(jsonStr)
    }
}

private fun jsonElementToAny(element: JsonElement): Any? {
    return when (element) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (element.isString) {
                element.content
            } else {
                element.booleanOrNull
                    ?: element.longOrNull
                    ?: element.doubleOrNull
                    ?: element.content
            }
        }
        is JsonArray -> element.map { jsonElementToAny(it) }
        is JsonObject -> element.mapValues { jsonElementToAny(it.value) }
    }
}
