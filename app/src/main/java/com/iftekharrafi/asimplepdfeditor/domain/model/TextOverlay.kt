package com.iftekharrafi.asimplepdfeditor.domain.model

/**
 * Represents a text annotation overlay on a PDF page.
 * Uses Int (ARGB) for color instead of Compose Color,
 * and references PdfFont from the domain layer.
 */
data class TextOverlay(
    val text: String = "",
    val colorArgb: Int = 0xFFFF0000.toInt(), // Red
    val font: PdfFont = PdfFont.DEFAULT,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val uiWidth: Float = 0f,
    val uiHeight: Float = 0f
)
