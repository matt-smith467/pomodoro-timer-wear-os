package com.example.pomodorotimer.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.curvedText
import com.example.pomodorotimer.presentation.theme.PomodoroTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTimerTheme {
                val viewModel: PomodoroViewModel = viewModel()
                val pagerState = rememberPagerState(pageCount = { 2 })

                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasNotificationPermission = it }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
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
    
    PomodoroContent(
        timeLeftProvider = { viewModel.timeLeft },
        isRunning = isRunning,
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
            TimerText(timeLeftProvider)

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
fun TimerText(timeLeftProvider: () -> Long) {
    Text(
        text = formatTime(timeLeftProvider()),
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
        // PERFORMANCE FIX: Fixed weights for smooth operation on Pixel Watch 4
        GroupButton(
            weight = 1f,
            onClick = onReset,
            icon = Icons.Default.Refresh,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )

        GroupButton(
            weight = 1.3f,
            onClick = onToggle,
            icon = if (isRunning) PomodoroIcons.Pause else Icons.Default.PlayArrow,
            shape = RoundedCornerShape(24.dp),
            containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            isLarge = true
        )

        GroupButton(
            weight = 1f,
            onClick = onSkip,
            icon = PomodoroIcons.SkipNext,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
    isLarge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Scale on press: GPU accelerated, zero layout impact
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = ""
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        // Centering content inside the weighted button
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
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
            session = SessionType.WORK,
            cycle = 1,
            onReset = {},
            onToggle = {},
            onSkip = {}
        )
    }
}
