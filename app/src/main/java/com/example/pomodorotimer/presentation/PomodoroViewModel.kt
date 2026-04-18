package com.example.pomodorotimer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class SessionType { WORK, SHORT_REST, LONG_REST }

class PomodoroViewModel : ViewModel() {
    var workLengthMinutes by mutableIntStateOf(25)
    var shortRestLengthMinutes by mutableIntStateOf(5)
    var longRestLengthMinutes by mutableIntStateOf(15)

    var timeLeft by mutableLongStateOf(workLengthMinutes * 60L)
    var isRunning by mutableStateOf(false)
    var currentSession by mutableStateOf(SessionType.WORK)
    var cycleCount by mutableIntStateOf(1) // 1 to 4

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
