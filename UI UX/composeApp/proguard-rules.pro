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

# Ktor Client & OkHttp
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**

# SQLDelight
-keep class app.cash.sqldelight.** { *; }
-keep class io.panel.reader.db.** { *; }
-dontwarn app.cash.sqldelight.**

# Coil 3
-keep class coil3.** { *; }
-dontwarn coil3.**

# Voyager Navigation (Screen implementations & Java Serializable/Parcelable if any)
-keep class cafe.adriel.voyager.** { *; }
-keep class * implements cafe.adriel.voyager.core.screen.Screen { *; }
-dontwarn cafe.adriel.voyager.**

# QuickJS JNI / Android
-keep class com.dokar.quickjs.** { *; }
-dontwarn com.dokar.quickjs.**

# Keep AndroidManifest components & Koin
-keep class io.panel.reader.** { *; }
-keep class core.updates.LibraryUpdateJobService { *; }
-keep class org.jetbrains.compose.resources.AndroidContextProvider { *; }
-keep class org.koin.** { *; }
-dontwarn org.koin.**
