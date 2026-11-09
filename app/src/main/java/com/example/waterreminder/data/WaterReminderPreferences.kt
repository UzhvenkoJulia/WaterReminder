package com.example.waterreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Singleton екземпляр DataStore на рівні Context
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "water_reminder_prefs")

object PreferencesKeys {
    val REMINDER_COUNT = intPreferencesKey("key_reminder_count")
    val IS_ENABLED = booleanPreferencesKey("key_is_enabled")
    val INTERVAL_MINUTES = longPreferencesKey("key_interval_minutes")
}

class WaterReminderPreferences(private val context: Context) {

    val reminderCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMINDER_COUNT] ?: 0
        }

    val isEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] ?: false
        }

    val intervalMinutesFlow: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.INTERVAL_MINUTES] ?: 15L
        }

    // збільш ліч на +1 (викликається з CoroutineWorker)
    suspend fun incrementReminderCount() {
        context.dataStore.edit { preferences ->
            val currentCount = preferences[PreferencesKeys.REMINDER_COUNT] ?: 0
            preferences[PreferencesKeys.REMINDER_COUNT] = currentCount + 1
        }
    }

    suspend fun saveReminderSettings(isEnabled: Boolean, intervalMinutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = isEnabled
            preferences[PreferencesKeys.INTERVAL_MINUTES] = intervalMinutes
        }
    }

    // оновлен стану перемикача
    suspend fun setReminderEnabled(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = isEnabled
        }
    }

    // скид лише лічильника
    suspend fun resetCount() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_COUNT] = 0
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}