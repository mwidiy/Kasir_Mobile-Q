# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, AnnotationDefault
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# --- Gson (Keep Data Models) ---
# VERY IMPORTANT: Keep your models package so Gson can find fields
-keep class id.quacxel.mejapesan.data.model.** { *; }
-keep class id.quacxel.mejapesan.data.network.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# --- Socket.io ---
-keep class io.socket.** { *; }
-keep class okhttp3.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Coil (Image Loading) ---
-keep class coil.** { *; }

# --- AndroidX Security Crypto ---
-keep class androidx.security.crypto.** { *; }

# Preserve line numbers for better error reporting in Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile