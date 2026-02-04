package com.example.kasir.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.example.kasir.data.model.User
import com.example.kasir.data.model.LoginStore
import com.google.gson.Gson

private val Context.dataStore by preferencesDataStore(name = "user_session")

object SessionManager {
    private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    private val USER_KEY = stringPreferencesKey("user_data")
    private val gson = Gson()
    
    // Memory Cache for fast access (Interceptor)
    var jwtToken: String? = null
    var currentUser: User? = null

    // Load session from DataStore (Called in MainActivity/Splash)
    suspend fun loadSession(context: Context) {
        // use first() to get data once and resume, instead of collect() which waits forever
        val preferences = context.dataStore.data.first()
        
        jwtToken = preferences[TOKEN_KEY]
        val userJson = preferences[USER_KEY]
        if (userJson != null) {
            currentUser = gson.fromJson(userJson, User::class.java)
        }
    }
    
    // Observe session state
    fun getSessionToken(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
             preferences[TOKEN_KEY]
        }
    }

    suspend fun saveSession(context: Context, token: String, user: User) {
        jwtToken = token
        currentUser = user
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_KEY] = gson.toJson(user)
        }
    }

    suspend fun clear(context: Context) {
        jwtToken = null
        currentUser = null
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Professional Logout: Clear Session + Revoke Google
    fun logout(context: Context, onComplete: () -> Unit = {}) {
        // 1. Clear Local Data (Run in coroutine scope if needed, or assume caller handles suspension)
        // Since clear() is suspend, we might need a scope. 
        // But to keep it simple, we let the caller call clear() first or we do it here if possible.
        // Better: Make logout() suspend or callback based.
        
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).build()
        
        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
        
        googleSignInClient.signOut().addOnCompleteListener {
            // Google Session Cleared
            onComplete()
        }
    }

    private val ALWAYS_ON_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("always_on_enabled")

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
