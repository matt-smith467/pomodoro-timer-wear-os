package com.example.pomodorotimer.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.curvedText
import com.example.pomodorotimer.presentation.theme.PomodoroTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTimerTheme {
                val viewModel: PomodoroViewModel = viewModel()
                val pagerState = rememberPagerState(pageCount = { 2 })

                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                AppScaffold(timeText = {}) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> PomodoroScreen(viewModel)
                            1 -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val isRunning = viewModel.isRunning
    val session = viewModel.currentSession
    val cycle = viewModel.cycleCount
    val isBound = viewModel.isBound

    PomodoroContent(
        timeLeftProvider = { viewModel.timeLeft },
        isRunning = isRunning,
        isBound = isBound,
        session = session,
        cycle = cycle,
        onReset = viewModel::resetAll,
        onToggle = viewModel::toggleTimer,
        onSkip = viewModel::skipNext
    )
}

@Composable
fun PomodoroContent(
    timeLeftProvider: () -> Long,
    isRunning: Boolean,
    isBound: Boolean,
    session: SessionType,
    cycle: Int,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    val sessionText = remember(session, cycle) {
        when(session) {
            SessionType.WORK -> "WORK • $cycle/4"
            SessionType.SHORT_REST -> "BREAK"
            SessionType.LONG_REST -> "LONG BREAK"
        }
    }

    val nextText = remember(session, cycle) {
        val nextSession = getUpNextSession(session, cycle)
        when(nextSession) {
            SessionType.WORK -> "NEXT: WORK"
            SessionType.SHORT_REST -> "NEXT: SHORT BREAK"
            SessionType.LONG_REST -> "NEXT: LONG BREAK"
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CurvedLayout(anchor = 270f, modifier = Modifier.padding(10.dp)) {
            curvedText(
                text = sessionText,
                style = CurvedTextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }

        CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.CounterClockwise, modifier = Modifier.padding(8.dp)) {
            curvedText(
                text = nextText,
                style = CurvedTextStyle(fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimerText(timeLeftProvider, isBound)

            PomodoroButtonGroup(
                isRunning = isRunning,
                onReset = onReset,
                onToggle = onToggle,
                onSkip = onSkip
            )
        }
    }
}

@Composable
fun TimerText(timeLeftProvider: () -> Long, isBound: Boolean) {
    val timeLeft = timeLeftProvider()
    // Show "--:--" only when we have no data yet (service not connected, DataStore not loaded)
    val text = if (isBound || timeLeft > 0L) formatTime(timeLeft) else "--:--"
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        color = Color.White,
        modifier = Modifier.offset(y = (-8).dp)
    )
}

@Composable
fun PomodoroButtonGroup(
    isRunning: Boolean,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val groupHeight = screenWidth * 0.28f

    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(groupHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        GroupButton(
            weight = 1.0f,
            onClick = onReset,
            icon = Icons.Default.Refresh,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentDescription = "Reset"
        )

        GroupButton(
            weight = 1.3f,
            onClick = onToggle,
            icon = if (isRunning) PomodoroIcons.Pause else Icons.Default.PlayArrow,
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            isLarge = true,
            contentDescription = if (isRunning) "Pause" else "Play"
        )

        GroupButton(
            weight = 1.0f,
            onClick = onSkip,
            icon = PomodoroIcons.SkipNext,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentDescription = "Skip"
        )
    }
}

@Composable
fun RowScope.GroupButton(
    weight: Float,
    onClick: () -> Unit,
    icon: ImageVector,
    shape: Shape,
    containerColor: Color,
    isLarge: Boolean = false,
    contentDescription: String? = null
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        // Centering content inside the weighted button
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(if (isLarge) 32.dp else 24.dp)
            )
        }
    }
}

private fun getUpNextSession(current: SessionType, cycle: Int): SessionType {
    return if (current == SessionType.WORK) {
        if (cycle >= 4) SessionType.LONG_REST else SessionType.SHORT_REST
    } else SessionType.WORK
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun PomodoroScreenPreview() {
    PomodoroTimerTheme {
        PomodoroContent(
            timeLeftProvider = { 1500L },
            isRunning = false,
            isBound = true,
            session = SessionType.WORK,
            cycle = 1,
            onReset = {},
            onToggle = {},
            onSkip = {}
        )
    }
}
