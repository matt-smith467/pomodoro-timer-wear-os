package com.example.pomodorotimer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_settings")

data class TimerState(
    val isRunning: Boolean,
    val targetEndTime: Long,
    val timeLeft: Long,
    val currentSession: String,
    val cycleCount: Int
)

class TimerDataStore(private val context: Context) {
    private val WORK_LENGTH_KEY = intPreferencesKey("work_length")
    private val SHORT_REST_LENGTH_KEY = intPreferencesKey("short_rest_length")
    private val LONG_REST_LENGTH_KEY = intPreferencesKey("long_rest_length")
    private val AUTO_START_NEXT_SESSION_KEY = booleanPreferencesKey("auto_start_next_session")
    private val IS_RUNNING_KEY = booleanPreferencesKey("is_running")
    private val TARGET_END_TIME_KEY = longPreferencesKey("target_end_time")
    private val TIME_LEFT_KEY = longPreferencesKey("time_left")
    private val CURRENT_SESSION_KEY = stringPreferencesKey("current_session")
    private val CYCLE_COUNT_KEY = intPreferencesKey("cycle_count")

    val workLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WORK_LENGTH_KEY] ?: 25
    }

    val shortRestLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SHORT_REST_LENGTH_KEY] ?: 5
    }

    val longRestLengthMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LONG_REST_LENGTH_KEY] ?: 15
    }

    val autoStartNextSession: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_START_NEXT_SESSION_KEY] ?: false
    }

    val timerState: Flow<TimerState> = context.dataStore.data.map { preferences ->
        TimerState(
            isRunning = preferences[IS_RUNNING_KEY] ?: false,
            targetEndTime = preferences[TARGET_END_TIME_KEY] ?: 0L,
            timeLeft = preferences[TIME_LEFT_KEY] ?: (25 * 60L),
            currentSession = preferences[CURRENT_SESSION_KEY] ?: "WORK",
            cycleCount = preferences[CYCLE_COUNT_KEY] ?: 1
        )
    }

    suspend fun saveTimerState(isRunning: Boolean, targetEndTime: Long, timeLeft: Long, session: String, cycle: Int) {
        context.dataStore.edit { preferences ->
            preferences[IS_RUNNING_KEY] = isRunning
            preferences[TARGET_END_TIME_KEY] = targetEndTime
            preferences[TIME_LEFT_KEY] = timeLeft
            preferences[CURRENT_SESSION_KEY] = session
            preferences[CYCLE_COUNT_KEY] = cycle
        }
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

    suspend fun saveAutoStartNextSession(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_START_NEXT_SESSION_KEY] = enabled
        }
    }
}
