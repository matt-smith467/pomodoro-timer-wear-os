package com.example.pomodorotimer.presentation

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.pomodorotimer.presentation.theme.GoogleSansFlexBody
import com.example.pomodorotimer.presentation.theme.GoogleSansFlexHeadline
import com.example.pomodorotimer.presentation.theme.PomodoroTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTimerTheme {
                val viewModel: PomodoroViewModel = viewModel()
                val pagerState = rememberPagerState(pageCount = { 2 })

                val permissionLauncher =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                AppScaffold(timeText = {}) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
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
        onSkip = viewModel::skipNext,
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
    onSkip: () -> Unit,
) {
    val sessionText =
        remember(session, cycle) {
            when (session) {
                SessionType.WORK -> "WORK • $cycle/4"
                SessionType.SHORT_REST -> "BREAK"
                SessionType.LONG_REST -> "LONG BREAK"
            }
        }

    val nextText =
        remember(session, cycle) {
            val nextSession = getUpNextSession(session, cycle)
            when (nextSession) {
                SessionType.WORK -> "NEXT: WORK"
                SessionType.SHORT_REST -> "NEXT: SHORT BREAK"
                SessionType.LONG_REST -> "NEXT: LONG BREAK"
            }
        }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CurvedLayout(anchor = 270f, modifier = Modifier.padding(10.dp)) {
            curvedText(
                text = sessionText,
                style = CurvedTextStyle(
                    fontSize = 14.sp,
                    fontFamily = GoogleSansFlexHeadline,
                    color = Color.White
                ),
            )
        }

        CurvedLayout(
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.padding(8.dp),
        ) {
            curvedText(
                text = nextText,
                style = CurvedTextStyle(
                    fontSize = 11.sp,
                    fontFamily = GoogleSansFlexBody,
                    color = Color.White.copy(alpha = 0.7f)
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            TimerText(timeLeftProvider, isBound)

            PomodoroButtonGroup(
                isRunning = isRunning,
                onReset = onReset,
                onToggle = onToggle,
                onSkip = onSkip,
            )
        }
    }
}

@Composable
fun TimerText(
    timeLeftProvider: () -> Long,
    isBound: Boolean,
) {
    val timeLeft = timeLeftProvider()
    // Show "--:--" only when we have no data yet (service not connected, DataStore not loaded)
    val text = if (isBound || timeLeft > 0L) formatTime(timeLeft) else "--:--"
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        color = Color.White,
        modifier = Modifier.offset(y = (-8).dp),
    )
}

@Composable
fun PomodoroButtonGroup(
    isRunning: Boolean,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val groupHeight = screenWidth * 0.28f

    val interactionSource1 = remember { MutableInteractionSource() }
    val interactionSource2 = remember { MutableInteractionSource() }
    val interactionSource3 = remember { MutableInteractionSource() }

    ButtonGroup(
        modifier =
            Modifier
                .fillMaxWidth(0.85f)
                .height(groupHeight),
    ) {
        Button(
            onClick = onReset,
            modifier = Modifier.animateWidth(interactionSource1),
            interactionSource = interactionSource1,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White,
                    modifier = Modifier.requiredSize(24.dp),
                )
            }
        }

        Button(
            onClick = onToggle,
            modifier = Modifier.weight(1.3f).animateWidth(interactionSource2),
            interactionSource = interactionSource2,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        if (isRunning) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                ),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isRunning) PomodoroIcons.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.requiredSize(32.dp),
                )
            }
        }

        Button(
            onClick = onSkip,
            modifier = Modifier.animateWidth(interactionSource3),
            interactionSource = interactionSource3,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = PomodoroIcons.SkipNext,
                    contentDescription = "Skip",
                    tint = Color.White,
                    modifier = Modifier.requiredSize(24.dp),
                )
            }
        }
    }
}

private fun getUpNextSession(
    current: SessionType,
    cycle: Int,
): SessionType {
    return if (current == SessionType.WORK) {
        if (cycle >= 4) SessionType.LONG_REST else SessionType.SHORT_REST
    } else {
        SessionType.WORK
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@WearPreviewDevices
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
            onSkip = {},
        )
    }
}
