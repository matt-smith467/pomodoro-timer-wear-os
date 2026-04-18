package com.example.pomodorotimer.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pomodorotimer.R
import com.example.pomodorotimer.data.TimerDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerService : Service() {
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private lateinit var dataStore: TimerDataStore

    private val _timeLeft = MutableStateFlow(0L)
    val timeLeft: StateFlow<Long> = _timeLeft

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _currentSession = MutableStateFlow(SessionType.WORK)
    val currentSession: StateFlow<SessionType> = _currentSession

    private val _cycleCount = MutableStateFlow(1)
    val cycleCount: StateFlow<Int> = _cycleCount

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private var targetEndTime = 0L

    override fun onCreate() {
        super.onCreate()
        dataStore = TimerDataStore(applicationContext)
        createNotificationChannel()
        
        serviceScope.launch {
            val state = dataStore.timerState.first()
            _isRunning.value = state.isRunning
            _currentSession.value = SessionType.valueOf(state.currentSession)
            _cycleCount.value = state.cycleCount
            
            if (state.isRunning) {
                targetEndTime = state.targetEndTime
                val remaining = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000)
                _timeLeft.value = remaining
                if (remaining > 0) {
                    startTimer()
                } else {
                    onTimerFinished()
                }
            } else {
                _timeLeft.value = state.timeLeft
            }

            // Observe settings changes (only fire if the value actually changes)
            launch {
                dataStore.workLengthMinutes.distinctUntilChanged().drop(1).collect { workLen ->
                    if (!_isRunning.value && _currentSession.value == SessionType.WORK) {
                        _timeLeft.value = workLen * 60L
                    }
                }
            }
            launch {
                dataStore.shortRestLengthMinutes.distinctUntilChanged().drop(1).collect { shortLen ->
                    if (!_isRunning.value && _currentSession.value == SessionType.SHORT_REST) {
                        _timeLeft.value = shortLen * 60L
                    }
                }
            }
            launch {
                dataStore.longRestLengthMinutes.distinctUntilChanged().drop(1).collect { longLen ->
                    if (!_isRunning.value && _currentSession.value == SessionType.LONG_REST) {
                        _timeLeft.value = longLen * 60L
                    }
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _isRunning.value = true
        // Only set targetEndTime if we're starting fresh, not recovering
        if (targetEndTime <= System.currentTimeMillis()) {
            targetEndTime = System.currentTimeMillis() + (_timeLeft.value * 1000)
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        saveState()

        timerJob = serviceScope.launch {
            while (_isRunning.value) {
                val remaining = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000)
                if (remaining != _timeLeft.value) {
                    _timeLeft.value = remaining
                    updateNotification()
                }
                
                if (remaining <= 0) {
                    onTimerFinished()
                    break
                }
                delay(500L) // Check more frequently for smoothness, but only update on change
            }
        }
    }

    fun toggleTimer() {
        if (_isRunning.value) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        
        // Capture remaining time before clearing target
        if (targetEndTime > 0) {
            _timeLeft.value = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000)
        }
        targetEndTime = 0L
        
        stopForeground(STOP_FOREGROUND_DETACH)
        saveState()
    }

    fun resetTimer(workLength: Int) {
        _isRunning.value = false
        timerJob?.cancel()
        targetEndTime = 0L
        
        _currentSession.value = SessionType.WORK
        _cycleCount.value = 1
        _timeLeft.value = workLength * 60L
        
        stopForeground(STOP_FOREGROUND_DETACH)
        saveState()
    }

    private fun onTimerFinished() {
        _isRunning.value = false
        timerJob?.cancel()
        targetEndTime = 0L
        
        serviceScope.launch {
            val workLen = dataStore.workLengthMinutes.first()
            val shortLen = dataStore.shortRestLengthMinutes.first()
            val longLen = dataStore.longRestLengthMinutes.first()

            if (_currentSession.value == SessionType.WORK) {
                if (_cycleCount.value >= 4) {
                    _currentSession.value = SessionType.LONG_REST
                    _timeLeft.value = longLen * 60L
                } else {
                    _currentSession.value = SessionType.SHORT_REST
                    _timeLeft.value = shortLen * 60L
                }
            } else {
                if (_currentSession.value == SessionType.LONG_REST) {
                    _currentSession.value = SessionType.WORK
                    _cycleCount.value = 1
                    _timeLeft.value = workLen * 60L
                } else {
                    _currentSession.value = SessionType.WORK
                    _cycleCount.value += 1
                    _timeLeft.value = workLen * 60L
                }
            }
            saveState()
            updateNotification("Session Finished!", "Tap to start next session.")
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    private fun saveState() {
        serviceScope.launch {
            dataStore.saveTimerState(
                isRunning = _isRunning.value,
                targetEndTime = targetEndTime,
                timeLeft = _timeLeft.value,
                session = _currentSession.value.name,
                cycle = _cycleCount.value
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Timer Updates",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(title: String = "Pomodoro Timer", content: String? = null): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val text = content ?: "${_currentSession.value.name}: ${formatTime(_timeLeft.value)}"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String = "Pomodoro Timer", content: String? = null) {
        val notification = createNotification(title, content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
    }
}
