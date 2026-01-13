package com.example.kasir.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
        context.dataStore.data.collect { preferences ->
            jwtToken = preferences[TOKEN_KEY]
            val userJson = preferences[USER_KEY]
            if (userJson != null) {
                currentUser = gson.fromJson(userJson, User::class.java)
            }
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

    fun isLoggedIn(): Boolean {
        return jwtToken != null
    }
}
