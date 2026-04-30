package id.quacxel.mejapesan.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import id.quacxel.mejapesan.data.model.User
import id.quacxel.mejapesan.data.model.LoginStore
import com.google.gson.Gson
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private val Context.dataStore by preferencesDataStore(name = "user_session")

object SessionManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_KEY = stringPreferencesKey("user_data")
    private val gson = Gson()
    
    // Memory Cache for fast access (Interceptor)
    var jwtToken: String? = null
    var currentUser: User? = null

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("SessionManager", "Error init EncryptedSharedPreferences", e)
            null
        }
    }

    // Load session from DataStore (Called in MainActivity/Splash)
    suspend fun loadSession(context: Context) {
        // use first() to get data once and resume, instead of collect() which waits forever
        val preferences = context.dataStore.data.first()
        
        val encryptedPrefs = getEncryptedPrefs(context)
        jwtToken = encryptedPrefs?.getString("jwt_token", null) ?: preferences[TOKEN_KEY]

        val userJson = preferences[USER_KEY]
        if (userJson != null) {
            currentUser = gson.fromJson(userJson, User::class.java)
        }
    }
    
    // Observe session state
    fun getSessionToken(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
             jwtToken ?: getEncryptedPrefs(context)?.getString("jwt_token", null) ?: preferences[TOKEN_KEY]
        }
    }

    suspend fun saveSession(context: Context, token: String, user: User) {
        jwtToken = token
        currentUser = user
        
        getEncryptedPrefs(context)?.edit()?.putString("jwt_token", token)?.apply()

        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    private suspend fun clear(context: Context) {
        jwtToken = null
        currentUser = null
        getEncryptedPrefs(context)?.edit()?.clear()?.commit()
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Professional Logout: Clear Session + Revoke Google
    fun logout(context: Context, scope: kotlinx.coroutines.CoroutineScope, onComplete: () -> Unit = {}) {
        // 1. Clear Local Data
        scope.launch {
            clear(context)
            
            // 2. Revoke Google Session (Harus pakai konfigurasi ClientID yang sama dengan saat Login)
            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
            .requestIdToken(id.quacxel.mejapesan.BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
            
            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
            
            googleSignInClient.signOut()
                .addOnCompleteListener {
                    // Coba revoke access sekalian untuk menjamin lepas komplit dari riwayat email
                    googleSignInClient.revokeAccess().addOnCompleteListener {
                        onComplete()
                    }
                }
                .addOnFailureListener {
                    // Kalaupun gagal di Google SDK, setidaknya kita panggil onComplete
                    // supaya app tetep lanjut restart.
                    onComplete()
                }
        }
    }

    private val ALWAYS_ON_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("always_on_enabled")
    private val CUSTOM_SOUND_PATH_KEY = stringPreferencesKey("custom_sound_path")

    // Observe Custom Sound Path
    fun getCustomSoundPath(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[CUSTOM_SOUND_PATH_KEY]
        }
    }

    suspend fun setCustomSoundPath(context: Context, path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(CUSTOM_SOUND_PATH_KEY)
            else preferences[CUSTOM_SOUND_PATH_KEY] = path
        }
        // ALSO save to SharedPreferences for instant synchronous read by FCM service
        val prefs = context.getSharedPreferences("mejapesan_sound", Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (path == null) remove("custom_sound_uri") else putString("custom_sound_uri", path)
        }.apply()
    }

    // Synchronous read for FCM service (when app is killed, DataStore is too slow)
    fun getCustomSoundPathSync(context: Context): String? {
        val prefs = context.getSharedPreferences("mejapesan_sound", Context.MODE_PRIVATE)
        return prefs.getString("custom_sound_uri", null)
    }

    // Observe Always On state
    fun getAlwaysOn(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[ALWAYS_ON_KEY] ?: false // Default to false (Normal behavior)
        }
    }

    suspend fun setAlwaysOn(context: Context, isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALWAYS_ON_KEY] = isEnabled
        }
    }

    fun isLoggedIn(): Boolean {
        return jwtToken != null
    }
}
