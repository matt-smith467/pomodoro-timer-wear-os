package com.example.pomodorotimer.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.*
import com.example.pomodorotimer.R

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Work Setting
        item {
            InlineSetting(
                label = "Work",
                value = viewModel.workLengthMinutes,
                onValueChange = { viewModel.updateWorkLength(it) }
            )
        }

        // Short Rest Setting
        item {
            InlineSetting(
                label = "Short Break",
                value = viewModel.shortRestLengthMinutes,
                onValueChange = { viewModel.updateShortRestLength(it) }
            )
        }

        // Long Rest Setting
        item {
            InlineSetting(
                label = "Long Break",
                value = viewModel.longRestLengthMinutes,
                onValueChange = { viewModel.updateLongRestLength(it) }
            )
        }
    }
}

@Composable
fun InlineSetting(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(value - 1) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_remove),
                    contentDescription = "Decrease"
                )
            }
            Text(
                "$value",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            IconButton(onClick = { onValueChange(value + 1) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Increase"
                )
            }
        }
    }
}
