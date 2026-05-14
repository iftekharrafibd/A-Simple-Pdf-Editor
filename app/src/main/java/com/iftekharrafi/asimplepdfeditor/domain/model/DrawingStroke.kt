package com.iftekharrafi.asimplepdfeditor.domain.model

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color

data class DrawingStroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)