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

# ---- Ktor Server ----
-keep class io.ktor.** { *; }
-keepnames class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**
-dontwarn java.nio.**
-dontwarn sun.misc.**
-dontwarn sun.nio.**

# ---- kotlinx.serialization (web DTOs) ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep @kotlinx.serialization.Serializable class com.osscabinet.ossWallet.data.web.dto.** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- QRCode Kotlin ----
-keep class qrcode.** { *; }
-dontwarn qrcode.**

# ---- Room (ARC-64) ----
# Room generates DAO impls and reflectively maps columns to entity fields by name.
# Without these, R8 obfuscation in release builds renames @ColumnInfo-bound fields,
# causing runtime "no such column" SQLiteException on the first DB op.
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
# Embedded / TypeConverter classes referenced from entities
-keepclasseswithmembers class * { @androidx.room.TypeConverter <methods>; }
-keep @androidx.room.DatabaseView class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract <methods>;
}

# ---- AppFunctions ----
-keep @androidx.appfunctions.AppFunctionSerializable class * { *; }
-keepclassmembers @androidx.appfunctions.AppFunctionSerializable class * { *; }
-keepclassmembers class * {
    @androidx.appfunctions.AppFunctionDeclaration <methods>;
    @androidx.appfunctions.AppFunction <methods>;
}
-keep class * extends androidx.appfunctions.AppFunctionService { *; }
-keep class com.osscabinet.ossWallet.appfunctions.** { *; }
-keep class androidx.appfunctions.** { *; }
-dontwarn androidx.appfunctions.**

# ---- Stack trace line numbers ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile