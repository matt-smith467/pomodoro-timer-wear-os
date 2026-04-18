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
import androidx.compose.ui.res.vectorResource
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
import androidx.wear.compose.foundation.curvedRow
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

                // Request Notification Permission for Android 13+
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                AppScaffold(
                    timeText = { /* No clock */ }
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
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
    PomodoroContent(
        timeLeft = viewModel.timeLeft,
        isRunning = viewModel.isRunning,
        session = viewModel.currentSession,
        cycle = viewModel.cycleCount,
        onReset = { viewModel.resetAll() },
        onToggle = { viewModel.toggleTimer() },
        onSkip = { viewModel.skipNext() }
    )
}

@Composable
fun PomodoroContent(
    timeLeft: Long,
    isRunning: Boolean,
    session: SessionType,
    cycle: Int,
    onReset: () -> Unit,
    onToggle: () -> Unit,
    onSkip: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Top Curved Text (Current Session)
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.padding(10.dp) 
        ) {
            val sessionText = when(session) {
                SessionType.WORK -> "WORK • $cycle/4"
                SessionType.SHORT_REST -> "BREAK"
                SessionType.LONG_REST -> "LONG BREAK"
            }
            curvedText(
                text = sessionText,
                style = CurvedTextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Bottom Curved Text (Up Next)
        CurvedLayout(
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.padding(8.dp) // Less padding = larger radius = more space
        ) {
            val nextSession = getUpNextSession(session, cycle)
            val nextText = when(nextSession) {
                SessionType.WORK -> "NEXT: WORK"
                SessionType.SHORT_REST -> "NEXT: SHORT BREAK"
                SessionType.LONG_REST -> "NEXT: LONG BREAK"
            }
            curvedText(
                text = nextText,
                style = CurvedTextStyle(
                    fontSize = 11.sp, // Slightly smaller to fit
                    color = Color.White.copy(alpha = 0.7f)
                )
            )
        }

        // Central Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer Display
            Text(
                text = formatTime(timeLeft),
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                modifier = Modifier.offset(y = (-8).dp) // Move only clock up
            )

            // M3 Expressive Button Group (Centered)
            PomodoroButtonGroup(
                isRunning = isRunning,
                onReset = onReset,
                onToggle = onToggle,
                onSkip = onSkip
            )
        }
    }
}

private fun getUpNextSession(current: SessionType, cycle: Int): SessionType {
    return if (current == SessionType.WORK) {
        if (cycle >= 4) SessionType.LONG_REST else SessionType.SHORT_REST
    } else {
        SessionType.WORK
    }
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
    
    // Scale sizes
    val groupHeight = screenWidth * 0.28f
    val largeIconSize = groupHeight * 0.45f
    val smallIconSize = groupHeight * 0.35f

    val resetInteraction = remember { MutableInteractionSource() }
    val toggleInteraction = remember { MutableInteractionSource() }
    val skipInteraction = remember { MutableInteractionSource() }

    val isResetPressed by resetInteraction.collectIsPressedAsState()
    val isTogglePressed by toggleInteraction.collectIsPressedAsState()
    val isSkipPressed by skipInteraction.collectIsPressedAsState()

    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    
    val resetWeight by animateFloatAsState(
        targetValue = when {
            isResetPressed -> 1.5f
            isTogglePressed || isSkipPressed -> 0.7f
            else -> 0.8f
        },
        animationSpec = tween(400, easing = emphasizedEasing), label = ""
    )
    val toggleWeight by animateFloatAsState(
        targetValue = when {
            isTogglePressed -> 1.6f
            isResetPressed || isSkipPressed -> 0.8f
            else -> 1.2f
        },
        animationSpec = tween(400, easing = emphasizedEasing), label = ""
    )
    val skipWeight by animateFloatAsState(
        targetValue = when {
            isSkipPressed -> 1.5f
            isResetPressed || isTogglePressed -> 0.7f
            else -> 0.8f
        },
        animationSpec = tween(400, easing = emphasizedEasing), label = ""
    )

    val outerCorner = 40.dp
    val innerCornerPressed = 20.dp
    val innerCornerDefault = 8.dp

    val resetInnerCorner by animateDpAsState(if (isResetPressed || isTogglePressed) innerCornerPressed else innerCornerDefault, label = "")
    val toggleStartCorner by animateDpAsState(if (isTogglePressed || isResetPressed) innerCornerPressed else innerCornerDefault, label = "")
    val toggleEndCorner by animateDpAsState(if (isTogglePressed || isSkipPressed) innerCornerPressed else innerCornerDefault, label = "")
    val skipInnerCorner by animateDpAsState(if (isSkipPressed || isTogglePressed) innerCornerPressed else innerCornerDefault, label = "")

    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f) // Narrower row to be closer together
            .height(groupHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Reset Button (Pill)
        GroupButton(
            weight = resetWeight,
            onClick = onReset,
            interactionSource = resetInteraction,
            icon = Icons.Default.Refresh,
            iconSize = smallIconSize,
            shape = CircleShape, // Pill/Circle shape
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Play/Pause Button (Dynamic Shape)
        GroupButton(
            weight = toggleWeight,
            onClick = onToggle,
            interactionSource = toggleInteraction,
            icon = if (isRunning) PomodoroIcons.Pause else Icons.Default.PlayArrow,
            iconSize = largeIconSize,
            shape = RoundedCornerShape(24.dp), // More expressive rounded rect
            containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Skip Button (Pill)
        GroupButton(
            weight = skipWeight,
            onClick = onSkip,
            interactionSource = skipInteraction,
            icon = PomodoroIcons.SkipNext,
            iconSize = smallIconSize,
            shape = CircleShape, // Pill/Circle shape
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun RowScope.GroupButton(
    weight: Float,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    icon: ImageVector,
    iconSize: androidx.compose.ui.unit.Dp,
    shape: Shape,
    containerColor: Color
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val iconScale by animateFloatAsState(if (isPressed) 1.2f else 1.0f, label = "")

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight(),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(iconSize)
                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
            )
        }
    }
}

private fun formatSessionName(session: SessionType): String {
    return when(session) {
        SessionType.WORK -> "Work"
        SessionType.SHORT_REST -> "Short Break"
        SessionType.LONG_REST -> "Long Break"
    }
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
            timeLeft = 1500L,
            isRunning = false,
            session = SessionType.WORK,
            cycle = 1,
            onReset = {},
            onToggle = {},
            onSkip = {}
        )
    }
}

@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun PomodoroScreenRunningPreview() {
    PomodoroTimerTheme {
        PomodoroContent(
            timeLeft = 450L,
            isRunning = true,
            session = SessionType.SHORT_REST,
            cycle = 2,
            onReset = {},
            onToggle = {},
            onSkip = {}
        )
    }
}
