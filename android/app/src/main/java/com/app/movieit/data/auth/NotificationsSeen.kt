package com.app.movieit.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.notificationsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "notifications_seen")

@Singleton
class NotificationsSeen @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lastSeenKey = stringPreferencesKey("last_seen_iso")

    val lastSeenFlow: Flow<String?> = context.notificationsDataStore.data
        .map { it[lastSeenKey] }

    suspend fun markSeen(iso: String) {
        context.notificationsDataStore.edit { it[lastSeenKey] = iso }
    }
}
