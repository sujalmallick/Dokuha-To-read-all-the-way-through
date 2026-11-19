package core.extensions.js

/**
 * Sandboxed JavaScript engine execution context for evaluating extension scripts.
 *
 * Implemented via QuickJS on Android/JVM and Apple JavaScriptCore on iOS.
 */
expect class JsEngine() {
    suspend fun evaluate(script: String): String
    fun setGlobalFunction(name: String, block: (List<Any?>) -> Any?)
    fun setGlobalAsyncFunction(name: String, block: suspend (List<Any?>) -> Any?)
    fun setGlobalVariable(name: String, jsonValue: String)
    suspend fun callFunction(functionName: String, vararg args: Any?): String
    fun close()
}
