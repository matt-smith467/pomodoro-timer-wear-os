package com.example.pomodorotimer.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodorotimer.data.TimerDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

enum class SessionType { WORK, SHORT_REST, LONG_REST }

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = TimerDataStore(application)
    private var serviceRef = WeakReference<TimerService?>(null)

    // ── UI state ──────────────────────────────────────────────────────────────

    var isBound by mutableStateOf(false)
        private set
    var timeLeft by mutableLongStateOf(0L)
    var isRunning by mutableStateOf(false)
    var currentSession by mutableStateOf(SessionType.WORK)
    var cycleCount by mutableIntStateOf(1)

    var workLengthMinutes by mutableIntStateOf(25)
        private set
    var shortRestLengthMinutes by mutableIntStateOf(5)
        private set
    var longRestLengthMinutes by mutableIntStateOf(15)
        private set
    var autoStartNextSession by mutableStateOf(false)
        private set

    // ── Service connection ────────────────────────────────────────────────────

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                binder: IBinder?,
            ) {
                val service = (binder as TimerService.TimerBinder).getService()
                serviceRef = WeakReference(service)
                isBound = true
                observeService(service)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isBound = false
                serviceRef.clear()
            }
        }

    init {
        // Bind to service. BIND_AUTO_CREATE starts it if not already running.
        // The service becomes persistent (outlives bindings) once it calls startForeground()
        // when the timer starts.
        val intent = Intent(application, TimerService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Pre-load saved state from DataStore so the UI shows the correct values
        // immediately — before the async service binding completes (~100-200 ms).
        viewModelScope.launch {
            val state = dataStore.timerState.first()
            if (!isBound) {
                timeLeft = state.timeLeft
                isRunning = state.isRunning
                currentSession =
                    try {
                        SessionType.valueOf(state.currentSession)
                    } catch (
                        _: Exception,
                    ) {
                        SessionType.WORK
                    }
                cycleCount = state.cycleCount
            }
            // Settings: read once for the initial value, then keep a live observer below.
            workLengthMinutes = dataStore.workLengthMinutes.first()
            shortRestLengthMinutes = dataStore.shortRestLengthMinutes.first()
            longRestLengthMinutes = dataStore.longRestLengthMinutes.first()
            autoStartNextSession = dataStore.autoStartNextSession.first()
        }

        // Live observers for settings changes (e.g., user changes duration in settings screen).
        viewModelScope.launch { dataStore.workLengthMinutes.collect { workLengthMinutes = it } }
        viewModelScope.launch { dataStore.shortRestLengthMinutes.collect { shortRestLengthMinutes = it } }
        viewModelScope.launch { dataStore.longRestLengthMinutes.collect { longRestLengthMinutes = it } }
        viewModelScope.launch { dataStore.autoStartNextSession.collect { autoStartNextSession = it } }
    }

    private fun observeService(svc: TimerService) {
        // Each collect overwrites the DataStore preload with the live service value.
        viewModelScope.launch { svc.timeLeft.collect { timeLeft = it } }
        viewModelScope.launch { svc.isRunning.collect { isRunning = it } }
        viewModelScope.launch { svc.currentSession.collect { currentSession = it } }
        viewModelScope.launch { svc.cycleCount.collect { cycleCount = it } }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun toggleTimer() {
        if (!isBound) {
            // Service died unexpectedly (e.g., system killed it while paused).
            // Re-bind; the user will need to tap again once connected.
            val intent = Intent(getApplication(), TimerService::class.java)
            getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
            return
        }
        serviceRef.get()?.toggleTimer()
    }

    fun resetAll() {
        serviceRef.get()?.resetTimer(workLengthMinutes)
    }

    fun skipNext() {
        serviceRef.get()?.skipNext()
    }

    fun updateWorkLength(minutes: Int) {
        viewModelScope.launch { dataStore.saveWorkLength(maxOf(1, minutes)) }
    }

    fun updateShortRestLength(minutes: Int) {
        viewModelScope.launch { dataStore.saveShortRestLength(maxOf(1, minutes)) }
    }

    fun updateLongRestLength(minutes: Int) {
        viewModelScope.launch { dataStore.saveLongRestLength(maxOf(1, minutes)) }
    }

    fun updateAutoStartNextSession(enabled: Boolean) {
        viewModelScope.launch { dataStore.saveAutoStartNextSession(enabled) }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}
