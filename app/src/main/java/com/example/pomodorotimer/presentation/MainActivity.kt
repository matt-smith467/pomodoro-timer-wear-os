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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
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

                AppScaffold {
                    HorizontalPagerScaffold(
                        pagerState = pagerState,
                        pageIndicator = {
                            HorizontalPageIndicator(pagerState = pagerState)
                        }
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
}

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel) {
    val timeLeft = viewModel.timeLeft
    val isRunning = viewModel.isRunning
    val session = viewModel.currentSession
    val cycle = viewModel.cycleCount

    // Use standard Material Icons from the core library
    val playIcon = Icons.Default.PlayArrow
    val pauseIcon = PomodoroIcons.Pause
    val skipNextIcon = PomodoroIcons.SkipNext
    val refreshIcon = Icons.Default.Refresh

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Timer Display (Very Large)
            Text(
                text = formatTime(timeLeft),
                style = MaterialTheme.typography.displayLarge,
                color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "${formatSessionName(session)} • Cycle $cycle/4",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Expressive Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().height(IconButtonDefaults.LargeButtonSize + 12.dp)
            ) {
                // Reset Button - Slides in/out
                AnimatedVisibility(
                    visible = !isRunning,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut()
                ) {
                    FilledTonalIconButton(
                        onClick = { viewModel.resetAll() },
                        modifier = Modifier.size(IconButtonDefaults.ExtraSmallButtonSize)
                    ) {
                        Icon(
                            imageVector = refreshIcon,
                            contentDescription = "Reset",
                            modifier = Modifier.size(IconButtonDefaults.iconSizeFor(IconButtonDefaults.ExtraSmallButtonSize))
                        )
                    }
                }

                if (!isRunning) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Play/Pause Button - Central Focus
                IconButton(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier.size(IconButtonDefaults.LargeButtonSize),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer 
                                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) pauseIcon else playIcon,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        modifier = Modifier.size(IconButtonDefaults.iconSizeFor(IconButtonDefaults.LargeButtonSize))
                    )
                }

                if (!isRunning) {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Skip Button - Slides in/out
                AnimatedVisibility(
                    visible = !isRunning,
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                ) {
                    FilledTonalIconButton(
                        onClick = { viewModel.skipNext() },
                        modifier = Modifier.size(IconButtonDefaults.ExtraSmallButtonSize)
                    ) {
                        Icon(
                            imageVector = skipNextIcon,
                            contentDescription = "Skip",
                            modifier = Modifier.size(IconButtonDefaults.iconSizeFor(IconButtonDefaults.ExtraSmallButtonSize))
                        )
                    }
                }
            }
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
