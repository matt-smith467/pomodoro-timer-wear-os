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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_settings")

data class TimerState(
    val isRunning: Boolean,
    val targetEndTime: Long,
    val timeLeft: Long,
    val currentSession: String,
    val cycleCount: Int,
)

class TimerDataStore(private val context: Context) {
    private val workLengthKey = intPreferencesKey("work_length")
    private val shortRestLengthKey = intPreferencesKey("short_rest_length")
    private val longRestLengthKey = intPreferencesKey("long_rest_length")
    private val autoStartNextSessionKey = booleanPreferencesKey("auto_start_next_session")
    private val isRunningKey = booleanPreferencesKey("is_running")
    private val targetEndTimeKey = longPreferencesKey("target_end_time")
    private val timeLeftKey = longPreferencesKey("time_left")
    private val currentSessionKey = stringPreferencesKey("current_session")
    private val cycleCountKey = intPreferencesKey("cycle_count")

    val workLengthMinutes: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[workLengthKey] ?: 25
        }.distinctUntilChanged()

    val shortRestLengthMinutes: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[shortRestLengthKey] ?: 5
        }.distinctUntilChanged()

    val longRestLengthMinutes: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[longRestLengthKey] ?: 15
        }.distinctUntilChanged()

    val autoStartNextSession: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[autoStartNextSessionKey] ?: false
        }.distinctUntilChanged()

    val timerState: Flow<TimerState> =
        context.dataStore.data.map { preferences ->
            TimerState(
                isRunning = preferences[isRunningKey] ?: false,
                targetEndTime = preferences[targetEndTimeKey] ?: 0L,
                timeLeft = preferences[timeLeftKey] ?: (25 * 60L),
                currentSession = preferences[currentSessionKey] ?: "WORK",
                cycleCount = preferences[cycleCountKey] ?: 1,
            )
        }

    suspend fun saveTimerState(
        isRunning: Boolean,
        targetEndTime: Long,
        timeLeft: Long,
        session: String,
        cycle: Int,
    ) {
        context.dataStore.edit { preferences ->
            preferences[isRunningKey] = isRunning
            preferences[targetEndTimeKey] = targetEndTime
            preferences[timeLeftKey] = timeLeft
            preferences[currentSessionKey] = session
            preferences[cycleCountKey] = cycle
        }
    }

    suspend fun saveWorkLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[workLengthKey] = minutes
        }
    }

    suspend fun saveShortRestLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[shortRestLengthKey] = minutes
        }
    }

    suspend fun saveLongRestLength(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[longRestLengthKey] = minutes
        }
    }

    suspend fun saveAutoStartNextSession(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[autoStartNextSessionKey] = enabled
        }
    }
}
