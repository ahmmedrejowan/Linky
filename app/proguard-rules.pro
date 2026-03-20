# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ==================== Jsoup ====================
# Jsoup optionally uses re2j for regex - not included in this app
-dontwarn com.google.re2j.**

# ==================== Ktor ====================
# Ktor client rules
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# ==================== Kotlinx Serialization ====================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.rejowan.linky.**$$serializer { *; }
-keepclassmembers class com.rejowan.linky.** {
    *** Companion;
}
-keepclasseswithmembers class com.rejowan.linky.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== Coil ====================
-dontwarn coil.**

# ==================== Koin ====================
-keepnames class * extends org.koin.core.module.Module

# ==================== Coroutines ====================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# ==================== OkHttp/Okio (Ktor dependency) ====================
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== SLF4J (logging facade) ====================
-dontwarn org.slf4j.**