package com.example.pomodorotimer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// A refined, professional "Electric Blue" palette
private val CrystalBluePrimary = Color(0xFF60C3FF)
private val CrystalBlueOnPrimary = Color(0xFF00344E)
private val CrystalBluePrimaryContainer = Color(0xFF004C6F)
private val CrystalBlueOnPrimaryContainer = Color(0xFFC1E8FF)

private val CrystalBlueSecondary = Color(0xFFB1CADE)
private val CrystalBlueOnSecondary = Color(0xFF1B3343)

private val CrystalBlueTertiary = Color(0xFFFFB4AB) // Material 3 Red for active state
private val CrystalBlueOnTertiary = Color(0xFF690005)
private val CrystalBlueTertiaryContainer = Color(0xFF93000A)
private val CrystalBlueOnTertiaryContainer = Color(0xFFFFDAD6)

private val CrystalBlueColorScheme =
    ColorScheme(
        primary = CrystalBluePrimary,
        onPrimary = CrystalBlueOnPrimary,
        primaryContainer = CrystalBluePrimaryContainer,
        onPrimaryContainer = CrystalBlueOnPrimaryContainer,
        secondary = CrystalBlueSecondary,
        onSecondary = CrystalBlueOnSecondary,
        tertiary = CrystalBlueTertiary,
        onTertiary = CrystalBlueOnTertiary,
        tertiaryContainer = CrystalBlueTertiaryContainer,
        onTertiaryContainer = CrystalBlueOnTertiaryContainer,
        background = Color.Black,
        onBackground = Color.White,
        // Using a clean charcoal for surfaces instead of muddy blues
        surfaceContainerLow = Color(0xFF121212),
        surfaceContainer = Color(0xFF1C1C1C),
        surfaceContainerHigh = Color(0xFF282828),
        onSurface = Color(0xFFE2E2E2),
        onSurfaceVariant = Color(0xFFC4C7CC),
        outline = Color(0xFF8E9199),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    )

@Composable
fun PomodoroTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CrystalBlueColorScheme,
        content = content,
    )
}
