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

enum class SessionType { WORK, SHORT_REST, LONG_REST }

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = TimerDataStore(application)
    private var timerService: TimerService? = null
    private var isBound by mutableStateOf(false)

    var workLengthMinutes by mutableIntStateOf(25)
        private set
    var shortRestLengthMinutes by mutableIntStateOf(5)
        private set
    var longRestLengthMinutes by mutableIntStateOf(15)
        private set

    var timeLeft by mutableLongStateOf(workLengthMinutes * 60L)
    var isRunning by mutableStateOf(false)
    var currentSession by mutableStateOf(SessionType.WORK)
    var cycleCount by mutableIntStateOf(1)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            isBound = true
            observeService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            timerService = null
        }
    }

    init {
        val intent = Intent(application, TimerService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            dataStore.workLengthMinutes.collect { workLengthMinutes = it }
        }
        viewModelScope.launch {
            dataStore.shortRestLengthMinutes.collect { shortRestLengthMinutes = it }
        }
        viewModelScope.launch {
            dataStore.longRestLengthMinutes.collect { longRestLengthMinutes = it }
        }
    }

    private fun observeService() {
        timerService?.let { service ->
            viewModelScope.launch {
                service.timeLeft.collect { timeLeft = it }
            }
            viewModelScope.launch {
                service.isRunning.collect { isRunning = it }
            }
            viewModelScope.launch {
                service.currentSession.collect { currentSession = it }
            }
            viewModelScope.launch {
                service.cycleCount.collect { cycleCount = it }
            }
        }
    }

    fun toggleTimer() {
        if (!isBound) {
            val intent = Intent(getApplication(), TimerService::class.java)
            getApplication<Application>().startForegroundService(intent)
            getApplication<Application>().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        timerService?.toggleTimer()
    }

    fun resetAll() {
        timerService?.resetTimer(workLengthMinutes)
    }

    fun skipNext() {
        timerService?.skipNext()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }

    fun updateWorkLength(minutes: Int) {
        val newMinutes = maxOf(1, minutes)
        viewModelScope.launch {
            dataStore.saveWorkLength(newMinutes)
        }
    }

    fun updateShortRestLength(minutes: Int) {
        val newMinutes = maxOf(1, minutes)
        viewModelScope.launch {
            dataStore.saveShortRestLength(newMinutes)
        }
    }

    fun updateLongRestLength(minutes: Int) {
        val newMinutes = maxOf(1, minutes)
        viewModelScope.launch {
            dataStore.saveLongRestLength(newMinutes)
        }
    }
}
