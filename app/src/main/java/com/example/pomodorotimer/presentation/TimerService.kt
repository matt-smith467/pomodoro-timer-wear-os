package com.example.pomodorotimer.presentation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.pomodorotimer.R
import com.example.pomodorotimer.data.TimerDataStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TimerService : Service() {
    // ── State ─────────────────────────────────────────────────────────────────

    private val _timeLeft = MutableStateFlow(0L)
    private val _isRunning = MutableStateFlow(false)
    private val _currentSession = MutableStateFlow(SessionType.WORK)
    private val _cycleCount = MutableStateFlow(1)

    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    val currentSession: StateFlow<SessionType> = _currentSession.asStateFlow()
    val cycleCount: StateFlow<Int> = _cycleCount.asStateFlow()

    // ── Infrastructure ────────────────────────────────────────────────────────

    private val binder = TimerBinder()

    // CoroutineExceptionHandler is critical: without it, any uncaught exception in a
    // SupervisorJob child coroutine crashes the app via Thread.defaultUncaughtExceptionHandler.
    private val serviceScope =
        CoroutineScope(
            Dispatchers.Main + SupervisorJob() +
                CoroutineExceptionHandler { _, t -> Log.e(TAG, "Coroutine error", t) },
        )

    private var timerJob: Job? = null
    private lateinit var dataStore: TimerDataStore
    private lateinit var nm: NotificationManager
    private val sessionManager = SessionManager()

    // Tracks how many clients are bound. When 0, app is not visible → post alert notification.
    // When > 0, app is visible → just vibrate, UI will update itself.
    private var boundClients = 0

    private var targetEndTime = 0L

    // Settings cache — updated by collectors in onCreate so hot paths need no DataStore reads.
    private var workLen = 25
    private var shortLen = 5
    private var longLen = 15
    private var autoStart = false

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent): IBinder {
        boundClients++
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        boundClients = maxOf(0, boundClients - 1)
        return false
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onCreate() {
        super.onCreate()
        dataStore = TimerDataStore(applicationContext)
        nm = getSystemService(NotificationManager::class.java)
        createChannels()

        serviceScope.launch {
            // Collectors run for the lifetime of this coroutine (and the service).
            // They keep the settings cache fresh with zero DataStore reads in hot paths.
            launch {
                dataStore.workLengthMinutes.collect {
                    if (it != workLen) {
                        workLen = it
                        sessionManager.workLenMin = it
                        refreshIdleTime()
                    }
                }
            }
            launch {
                dataStore.shortRestLengthMinutes.collect {
                    if (it != shortLen) {
                        shortLen = it
                        sessionManager.shortRestLenMin = it
                        refreshIdleTime()
                    }
                }
            }
            launch {
                dataStore.longRestLengthMinutes.collect {
                    if (it != longLen) {
                        longLen = it
                        sessionManager.longRestLenMin = it
                        refreshIdleTime()
                    }
                }
            }
            launch { dataStore.autoStartNextSession.collect { autoStart = it } }

            // Restore persisted timer state. By the time this suspends and resumes,
            // the settings collectors above will have emitted their first values.
            val state = dataStore.timerState.first()
            _currentSession.value =
                try {
                    SessionType.valueOf(state.currentSession)
                } catch (
                    _: IllegalArgumentException,
                ) {
                    SessionType.WORK
                }
            _cycleCount.value = state.cycleCount

            if (state.isRunning) {
                targetEndTime = state.targetEndTime
                val remaining = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000L)
                _timeLeft.value = remaining
                if (remaining > 0L) startTimer() else onTimerFinished()
            } else {
                _timeLeft.value = state.timeLeft
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun toggleTimer() {
        if (_isRunning.value) pauseTimer() else startTimer()
    }

    fun skipNext() {
        onTimerFinished()
    }

    fun resetTimer(workLength: Int) {
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
        targetEndTime = 0L
        _currentSession.value = SessionType.WORK
        _cycleCount.value = 1
        _timeLeft.value = workLength * 60L
        stopForegroundSafely()
        saveState()
    }

    // ── Timer logic ───────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        _isRunning.value = true

        if (targetEndTime <= System.currentTimeMillis()) {
            targetEndTime = System.currentTimeMillis() + (_timeLeft.value * 1000L)
        }

        startForegroundCompat(buildTimerNotification())
        saveState()

        timerJob =
            serviceScope.launch {
                while (true) {
                    val remaining = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000L)
                    if (remaining != _timeLeft.value) _timeLeft.value = remaining
                    if (remaining == 0L) {
                        onTimerFinished()
                        break
                    }
                    delay(500L)
                }
            }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
        if (targetEndTime > 0L) {
            _timeLeft.value = maxOf(0L, (targetEndTime - System.currentTimeMillis()) / 1000L)
        }
        targetEndTime = 0L
        stopForegroundSafely()
        saveState()
    }

    // Fully synchronous — no coroutine, no DataStore reads. Uses cached settings.
    // Can be safely called from the timer coroutine or directly from skipNext().
    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun onTimerFinished() {
        timerJob?.cancel()
        timerJob = null
        val wasRunning = _isRunning.value
        _isRunning.value = false
        targetEndTime = 0L

        vibrate()

        val finishedName =
            when (_currentSession.value) {
                SessionType.WORK -> "Work session"
                SessionType.SHORT_REST -> "Short break"
                SessionType.LONG_REST -> "Long break"
            }

        // Advance session state using SessionManager
        val nextState =
            sessionManager.nextSession(
                SessionState(_currentSession.value, _cycleCount.value, _timeLeft.value),
            )
        _currentSession.value = nextState.type
        _cycleCount.value = nextState.cycle
        _timeLeft.value = nextState.timeLeftSeconds

        saveState()

        if (wasRunning && autoStart) {
            // Keep the foreground service running; replace the notification with the next session.
            serviceScope.launch {
                delay(300L)
                startTimer()
            }
        } else {
            // Remove foreground + ongoing activity.
            stopForegroundSafely()

            val nextName =
                when (_currentSession.value) {
                    SessionType.WORK -> "Work"
                    SessionType.SHORT_REST -> "Short break"
                    SessionType.LONG_REST -> "Long break"
                }
            try {
                nm.notify(ALERT_ID, buildAlertNotification("$finishedName complete!", "Next: $nextName"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post alert notification", e)
            }
        }
    }

    // Updates the displayed time when settings change and the timer is idle.
    private fun refreshIdleTime() {
        if (_isRunning.value) return
        _timeLeft.value =
            when (_currentSession.value) {
                SessionType.WORK -> workLen * 60L
                SessionType.SHORT_REST -> shortLen * 60L
                SessionType.LONG_REST -> longLen * 60L
            }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun startForegroundCompat(notification: Notification) {
        startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    }

    private fun stopForegroundSafely() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "stopForeground failed", e)
        }
    }

    private fun buildTimerNotification(): Notification {
        val pi = pendingMainIntent()
        val label =
            when (_currentSession.value) {
                SessionType.WORK -> "Work"
                SessionType.SHORT_REST -> "Break"
                SessionType.LONG_REST -> "Long break"
            }
        val builder =
            NotificationCompat.Builder(this, TIMER_CHANNEL)
                .setContentTitle("Pomodoro Timer")
                .setContentText("$label: ${formatTime(_timeLeft.value)}")
                .setSmallIcon(R.drawable.ic_ongoing_activity)
                .setContentIntent(pi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setAnimatedIcon(R.drawable.ic_ongoing_activity)
            .setStaticIcon(R.drawable.ic_ongoing_activity)
            .setTouchIntent(pi)
            .setStatus(
                Status.Builder()
                    .addTemplate("$label: #time#")
                    .addPart("time", Status.TimerPart(targetEndTime))
                    .build(),
            )
            .build()
            .apply(applicationContext)

        return builder.build()
    }

    private fun buildAlertNotification(
        title: String,
        text: String,
    ): Notification =
        NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_ongoing_activity)
            .setContentIntent(pendingMainIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    private fun pendingMainIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createChannels() {
        nm.createNotificationChannel(
            NotificationChannel(TIMER_CHANNEL, "Timer", NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, "Session Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            },
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate() {
        try {
            val v = getSystemService(Vibrator::class.java) ?: return
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed", e)
        }
    }

    private fun saveState() {
        serviceScope.launch {
            dataStore.saveTimerState(
                isRunning = _isRunning.value,
                targetEndTime = targetEndTime,
                timeLeft = _timeLeft.value,
                session = _currentSession.value.name,
                cycle = _cycleCount.value,
            )
        }
    }

    private fun formatTime(s: Long) = "%02d:%02d".format(s / 60, s % 60)

    companion object {
        private const val TAG = "TimerService"
        const val TIMER_CHANNEL = "timer_channel_v3"
        const val ALERT_CHANNEL = "pomodoro_alerts_v2"
        const val NOTIFICATION_ID = 1
        const val ALERT_ID = 2
    }
}
