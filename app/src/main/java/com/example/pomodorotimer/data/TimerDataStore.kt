package com.example.pomodorotimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_settings")

class TimerDataStore(private val context: Context) {
    private val WORK_LENGTH_KEY = intPreferencesKey("work_length")
    private val SHORT_REST_LENGTH_KEY = intPreferencesKey("short_rest_length")
    private val LONG_REST_LENGTH_KEY = intPreferencesKey("long_rest_length")

    val workLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_LENGTH_KEY] ?: 25
    }

    val shortRestLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SHORT_REST_LENGTH_KEY] ?: 5
    }

    val longRestLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LONG_REST_LENGTH_KEY] ?: 15
    }

    suspend fun saveWorkLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[WORK_LENGTH_KEY] = minutes
        }
    }

    suspend fun saveShortRestLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SHORT_REST_LENGTH_KEY] = minutes
        }
    }

    suspend fun saveLongRestLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[LONG_REST_LENGTH_KEY] = minutes
        }
    }
}
