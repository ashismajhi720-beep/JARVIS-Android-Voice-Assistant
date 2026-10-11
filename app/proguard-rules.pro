# ProGuard Rules for JARVIS App

# Keep Android entry points safe
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep service classes
-keep class com.example.jarvis.service.** { *; }
-keep class com.example.jarvis.MainActivity { *; }
-keep class com.example.jarvis.SystemEventReceiver { *; }
-keep class com.example.jarvis.NotificationChannels { *; }

# Keep ONNX Runtime classes (Step 3)
-keep class com.microsoft.onnxruntime.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keepclasseswithmembernames class androidx.compose.** { *; }

# Gson
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
