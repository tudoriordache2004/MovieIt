package com.app.movieit.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val usernameKey = stringPreferencesKey("username")

    suspend fun saveToken(token: String) {
        context.authDataStore.edit { prefs ->
            prefs[accessTokenKey] = token
        }
    }

    suspend fun saveTokenAndUsername(token: String, username: String) {
        context.authDataStore.edit { prefs ->
            prefs[accessTokenKey] = token
            prefs[usernameKey] = username
        }
    }

    fun tokenFlow(): Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[accessTokenKey]
    }

    suspend fun getToken(): String? = tokenFlow().first()

    suspend fun getUsername(): String? = context.authDataStore.data.map { prefs ->
        prefs[usernameKey]
    }.first()

    suspend fun clearToken() {
        context.authDataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(usernameKey)
        }
    }
}