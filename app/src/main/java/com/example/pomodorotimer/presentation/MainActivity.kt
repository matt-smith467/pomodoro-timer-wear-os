package com.example.pomodorotimer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material3.*
import com.example.pomodorotimer.presentation.theme.PomodoroTimerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroTimerTheme {
                AppScaffold {
                    PomodoroScreen()
                }
            }
        }
    }
}

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModel = viewModel()) {
    // 1. State: What changes in our app?
    val timeLeft = viewModel.timeLeft
    val session = viewModel.currentSession
    val cycle = viewModel.cycleCount


    LaunchedEffect(viewModel.isRunning) {
        if (viewModel.isRunning) {
            while (viewModel.timeLeft > 0) {
                delay(1000L)
                viewModel.timeLeft--
            }
            viewModel.onTimerFinished()
        }
    }

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
                    onClick = { viewModel.isRunning = !viewModel.isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.isRunning) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(if (viewModel.isRunning) "Pause" else "Start")
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