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
    val timeLeft = viewModel.timeLeft
    val isRunning = viewModel.isRunning
    val session = viewModel.currentSession
    val cycle = viewModel.cycleCount

    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Top Curved Text (Current Session)
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.padding(10.dp) 
        ) {
            curvedRow {
                curvedText(
                    text = "${formatSessionName(session).uppercase()} • $cycle/4",
                    style = CurvedTextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                )
            }
        }

        // Bottom Curved Text (Up Next)
        CurvedLayout(
            anchor = 90f,
            angularDirection = CurvedDirection.Angular.CounterClockwise,
            modifier = Modifier.padding(12.dp)
        ) {
            curvedRow {
                curvedText(
                    text = "UP NEXT: ${formatSessionName(getUpNextSession(session, cycle))}",
                    style = CurvedTextStyle(
                        fontSize = 12.sp,
                        color = onBackgroundColor.copy(alpha = 0.7f)
                    )
                )
            }
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
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // M3 Expressive Button Group
            PomodoroButtonGroup(
                isRunning = isRunning,
                onReset = { viewModel.resetAll() },
                onToggle = { viewModel.toggleTimer() },
                onSkip = { viewModel.skipNext() }
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
    
    // Scale group height based on screen width (roughly 30% of width)
    val groupHeight = screenWidth * 0.3f
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
            isResetPressed -> 1.8f
            isTogglePressed || isSkipPressed -> 0.6f
            else -> 1.0f
        },
        animationSpec = tween(400, easing = emphasizedEasing), label = ""
    )
    val toggleWeight by animateFloatAsState(
        targetValue = when {
            isTogglePressed -> 1.8f
            isResetPressed || isSkipPressed -> 0.6f
            else -> 1.0f
        },
        animationSpec = tween(400, easing = emphasizedEasing), label = ""
    )
    val skipWeight by animateFloatAsState(
        targetValue = when {
            isSkipPressed -> 1.8f
            isResetPressed || isTogglePressed -> 0.6f
            else -> 1.0f
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
            .fillMaxWidth(0.94f)
            .height(groupHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset Button
        GroupButton(
            weight = resetWeight,
            onClick = onReset,
            interactionSource = resetInteraction,
            icon = Icons.Default.Refresh,
            iconSize = smallIconSize,
            shape = RoundedCornerShape(
                topStart = outerCorner, 
                bottomStart = outerCorner, 
                topEnd = resetInnerCorner, 
                bottomEnd = resetInnerCorner
            ),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Play/Pause Button
        GroupButton(
            weight = toggleWeight,
            onClick = onToggle,
            interactionSource = toggleInteraction,
            icon = if (isRunning) PomodoroIcons.Pause else Icons.Default.PlayArrow,
            iconSize = largeIconSize,
            shape = RoundedCornerShape(
                topStart = toggleStartCorner, 
                bottomStart = toggleStartCorner, 
                topEnd = toggleEndCorner, 
                bottomEnd = toggleEndCorner
            ),
            containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Skip Button
        GroupButton(
            weight = skipWeight,
            onClick = onSkip,
            interactionSource = skipInteraction,
            icon = PomodoroIcons.SkipNext,
            iconSize = smallIconSize,
            shape = RoundedCornerShape(
                topStart = skipInnerCorner, 
                bottomStart = skipInnerCorner, 
                topEnd = outerCorner, 
                bottomEnd = outerCorner
            ),
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
