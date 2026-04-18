package com.example.pomodorotimer.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class SessionType { WORK, SHORT_REST, LONG_REST }

class PomodoroViewModel : ViewModel() {
    // Current state of the timer
    var timeLeft by mutableLongStateOf(25 * 60L)
    var isRunning by mutableStateOf(false)
    var currentSession by mutableStateOf(SessionType.WORK)
    var cycleCount by mutableIntStateOf(1) // 1 to 4

    var nextSession by mutableStateOf(SessionType.SHORT_REST)

    fun onTimerFinished() {
        isRunning = false
        if (currentSession == SessionType.WORK) {
            // Is it the 4th cycle? (Long break)
            timeLeft = if (cycleCount >= 4) 15 * 60L else 5 * 60L

            // After Work, we go to SHORT_REST or LONG_REST depending on the cycle count
            currentSession = if (cycleCount >= 4) SessionType.LONG_REST else SessionType.SHORT_REST

        } else {
            // After Rest, we go back to Work (if not finished)
            if (cycleCount >= 4) {
                resetAll() // All 4 cycles done!
            } else {
                currentSession = SessionType.WORK
                cycleCount++
                timeLeft = 25 * 60L
            }
        }
    }

    fun resetAll() {
        isRunning = false
        currentSession = SessionType.WORK
        cycleCount = 1
        timeLeft = 25 * 60L
    }
}