package io.panel.reader

import android.app.Application
import android.content.Context
import android.util.Log
import core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class DokuhaApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Global crash guard to prevent silent crash loops and capture errors
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("DokuhaApp", "Fatal uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                val prefs = getSharedPreferences("dokuha_crash", Context.MODE_PRIVATE)
                prefs.edit().putString("last_crash", sw.toString()).commit()
            } catch (_: Throwable) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize compose resources context via reflection safely
        initComposeResourcesContext(this)

        // Initialize Koin safely once at application level
        if (GlobalContext.getOrNull() == null) {
            try {
                startKoin {
                    androidContext(this@DokuhaApp)
                    modules(appModule)
                }
            } catch (e: Throwable) {
                Log.e("DokuhaApp", "Failed to start Koin in DokuhaApp", e)
            }
        }
    }

    companion object {
        fun initComposeResourcesContext(context: Context) {
            try {
                val clazz = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
                val companionField = clazz.getField("Companion")
                val companion = companionField.get(null)
                val method = companion.javaClass.getMethod("setANDROID_CONTEXT", Context::class.java)
                method.invoke(companion, context)
            } catch (_: Throwable) {
                // Silently fallback if running on non-Android or different resource version
            }
        }
    }
}
