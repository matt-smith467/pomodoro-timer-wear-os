package com.example.pomodorotimer.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object PomodoroIcons {
    val Pause: ImageVector
        get() = ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 19f)
            horizontalLineTo(10f)
            verticalLineTo(5f)
            horizontalLineTo(6f)
            verticalLineTo(19f)
            close()
            moveTo(14f, 5f)
            verticalLineTo(19f)
            horizontalLineTo(18f)
            verticalLineTo(5f)
            horizontalLineTo(14f)
            close()
        }.build()

    val SkipNext: ImageVector
        get() = ImageVector.Builder(
            name = "SkipNext",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 18f)
            lineToRelative(8.5f, -6f)
            lineTo(6f, 6f)
            verticalLineTo(18f)
            close()
            moveTo(16f, 6f)
            verticalLineTo(18f)
            horizontalLineTo(18f)
            verticalLineTo(6f)
            horizontalLineTo(16f)
            close()
        }.build()
}
