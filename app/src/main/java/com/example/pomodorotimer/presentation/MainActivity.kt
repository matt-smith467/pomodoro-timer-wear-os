package com.example.pomodorotimer.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // State is now directly observed from ViewModel which observes TimerService
    val timeLeft = viewModel.timeLeft
    val isRunning = viewModel.isRunning
    val session = viewModel.currentSession
    val cycle = viewModel.cycleCount

    // 3. UI: How it looks (Material 3)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(timeLeft),
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { viewModel.toggleTimer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(if (isRunning) "Pause" else "Start")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.resetAll()
                }) {
                    Text("Reset")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cycle $cycle/4 - ${session.name}")
        }
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
