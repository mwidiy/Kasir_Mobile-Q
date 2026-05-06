# --- Retrofit & OkHttp ---
-keepattributes Signature, InnerClasses, AnnotationDefault, EnclosingMethod, *Annotation*
-keepclassmembers interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# --- Gson (Keep Data Models) ---
-keep class id.quacxel.mejapesan.data.model.** { *; }
-keep class id.quacxel.mejapesan.data.network.** { *; }
-keep class com.google.gson.** { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keep class kotlin.coroutines.Continuation { *; }

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