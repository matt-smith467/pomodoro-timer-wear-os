package com.example.pomodorotimer.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodorotimer.data.TimerDataStore
import kotlinx.coroutines.launch

enum class SessionType { WORK, SHORT_REST, LONG_REST }

class PomodoroViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = TimerDataStore(application)

    var workLengthMinutes by mutableIntStateOf(25)
        private set
    var shortRestLengthMinutes by mutableIntStateOf(5)
        private set
    var longRestLengthMinutes by mutableIntStateOf(15)
        private set

    var timeLeft by mutableLongStateOf(workLengthMinutes * 60L)
    var isRunning by mutableStateOf(false)
    var currentSession by mutableStateOf(SessionType.WORK)
    var cycleCount by mutableIntStateOf(1) // 1 to 4

    init {
        viewModelScope.launch {
            dataStore.workLengthMinutes.collect {
                workLengthMinutes = it
                if (!isRunning && currentSession == SessionType.WORK) {
                    timeLeft = it * 60L
                }
            }
        }
        viewModelScope.launch {
            dataStore.shortRestLengthMinutes.collect {
                shortRestLengthMinutes = it
                if (!isRunning && currentSession == SessionType.SHORT_REST) {
                    timeLeft = it * 60L
                }
            }
        }
        viewModelScope.launch {
            dataStore.longRestLengthMinutes.collect {
                longRestLengthMinutes = it
                if (!isRunning && currentSession == SessionType.LONG_REST) {
                    timeLeft = it * 60L
                }
            }
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

    fun onTimerFinished() {
        isRunning = false
        if (currentSession == SessionType.WORK) {
            // After Work, we go to a break. 4th cycle gets the long break.
            if (cycleCount >= 4) {
                currentSession = SessionType.LONG_REST
                timeLeft = longRestLengthMinutes * 60L
            } else {
                currentSession = SessionType.SHORT_REST
                timeLeft = shortRestLengthMinutes * 60L
            }
        } else {
            // After any Rest, we go back to Work (unless it was the final session)
            if (currentSession == SessionType.LONG_REST) {
                resetAll() // Full set complete!
            } else {
                currentSession = SessionType.WORK
                cycleCount++
                timeLeft = workLengthMinutes * 60L
            }
        }
    }

    fun resetAll() {
        isRunning = false
        currentSession = SessionType.WORK
        cycleCount = 1
        timeLeft = workLengthMinutes * 60L
    }
}
