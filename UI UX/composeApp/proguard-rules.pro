# Dokuha Release R8 / ProGuard Keep Rules

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class data.**$$serializer { *; }
-keep,includedescriptorclasses class core.**$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor Client & OkHttp (consumer rules handle reflection; keep only warnings suppressed)
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**

# SQLDelight (generated database/queries)
-keep class io.panel.reader.db.PanelDatabase { *; }
-dontwarn app.cash.sqldelight.**

# Coil 3 (consumer rules handle components)
-dontwarn coil3.**

# Voyager Navigation (Screen implementations)
-keep class * implements cafe.adriel.voyager.core.screen.Screen { *; }
-dontwarn cafe.adriel.voyager.**

# QuickJS JNI / Android
-keep class com.dokar.quickjs.** { *; }
-dontwarn com.dokar.quickjs.**

# Keep AndroidManifest entry points & Compose resources provider
-keep class io.panel.reader.MainActivity { *; }
-keep class io.panel.reader.DokuhaApp { *; }
-keep class core.updates.LibraryUpdateJobService { *; }
-keep class org.jetbrains.compose.resources.AndroidContextProvider { *; }
-dontwarn org.koin.**
