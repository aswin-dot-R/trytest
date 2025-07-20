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

# Keep LSL related classes
-keep class com.example.wearoslsl.lsl.** { *; }
-keep class com.example.wearoslsl.data.** { *; }

# Keep Health Services related classes
-keep class androidx.health.services.client.** { *; }

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.example.wearoslsl.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.wearoslsl.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Wear Compose
-keep class androidx.wear.compose.** { *; }
-keep class androidx.compose.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }