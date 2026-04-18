package com.example.pomodorotimer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import com.example.pomodorotimer.R

@Composable
fun SettingsScreen(viewModel: PomodoroViewModel) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Work Setting
        InlineSetting(
            label = "Work session",
            value = viewModel.workLengthMinutes,
            onValueChange = { viewModel.updateWorkLength(it) }
        )

        // Short Rest Setting
        InlineSetting(
            label = "Short break",
            value = viewModel.shortRestLengthMinutes,
            onValueChange = { viewModel.updateShortRestLength(it) }
        )

        // Long Rest Setting
        InlineSetting(
            label = "Long break",
            value = viewModel.longRestLengthMinutes,
            onValueChange = { viewModel.updateLongRestLength(it) }
        )

        CheckboxButton(
            checked = viewModel.autoStartNextSession,
            onCheckedChange = { viewModel.updateAutoStartNextSession(it) },
            label = { Text("Auto-start next") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun InlineSetting(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp) // Standard height to match CheckboxButton
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            IconButton(
                onClick = { if (value > 1) onValueChange(value - 1) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_remove),
                    contentDescription = "Decrease",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "$value",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            IconButton(
                onClick = { if (value < 99) onValueChange(value + 1) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Increase",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@WearPreviewDevices
@Composable
fun SettingsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<PomodoroViewModel>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PomodoroViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    com.example.pomodorotimer.presentation.theme.PomodoroTimerTheme {
        SettingsScreen(viewModel = viewModel)
    }
}
