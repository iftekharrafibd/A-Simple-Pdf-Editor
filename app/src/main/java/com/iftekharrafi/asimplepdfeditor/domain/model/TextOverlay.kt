package com.iftekharrafi.asimplepdfeditor.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class TextOverlay(
    val text: String = "",
    val color: Color = Color.Red,
    val fontFamily: FontFamily = FontFamily.Default,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f
)
