package core.extensions.js

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsEngineTest {

    @Test
    fun testBasicMathEvaluation() = runBlocking {
        val engine = JsEngine()
        val result = engine.evaluate("2 + 2")
        assertEquals("4", result)
        engine.close()
    }

    @Test
    fun testStringEvaluation() = runBlocking {
        val engine = JsEngine()
        val result = engine.evaluate("'hello ' + 'world'")
        assertEquals("hello world", result)
        engine.close()
    }

    @Test
    fun testAsyncPromiseEvaluation() = runBlocking {
        val engine = JsEngine()
        val result = engine.evaluate(
            """
            (async () => {
                const p = new Promise(resolve => resolve('async_resolved'));
                return await p;
            })()
            """.trimIndent()
        )
        assertEquals("async_resolved", result)
        engine.close()
    }

    @Test
    fun testPromiseEvaluationDoesNotExecuteTwice() = runBlocking {
        val engine = JsEngine()
        engine.evaluate("var evalCounter = 0;")
        val result = engine.evaluate(
            """
            (async () => {
                evalCounter++;
                return evalCounter;
            })()
            """.trimIndent()
        )
        val finalCount = engine.evaluate("evalCounter")
        assertEquals("1", result)
        assertEquals("1", finalCount)
        engine.close()
    }

    @Test
    fun testGlobalVariableAndFunctionCall() = runBlocking {
        val engine = JsEngine()
        engine.setGlobalVariable("config", """{"appName":"Dokuha","version":1}""")
        val result = engine.evaluate("config.appName + ' v' + config.version")
        assertEquals("Dokuha v1", result)

        engine.evaluate("function multiply(a, b) { return a * b; }")
        val calc = engine.callFunction("multiply", 6, 7)
        assertEquals("42", calc)
        engine.close()
    }

    @Test
    fun testHostCallback() = runBlocking {
        val engine = JsEngine()
        var called = false
        var passedValue = ""
        engine.setGlobalFunction("nativePing") { args ->
            called = true
            passedValue = args.getOrNull(0)?.toString() ?: ""
            "native_pong_$passedValue"
        }
        val result = engine.evaluate("nativePing('test123')")
        assertTrue(called)
        assertEquals("test123", passedValue)
        assertEquals("native_pong_test123", result)
        engine.close()
    }
}
