package com.iftekharrafi.asimplepdfeditor.domain.model

/**
 * Represents a single drawing stroke on a PDF page.
 * Uses framework-agnostic types: List<StrokePoint> instead of Compose Path,
 * and Int (ARGB) instead of Compose Color.
 */
data class DrawingStroke(
    val points: List<StrokePoint>,
    val colorArgb: Int,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)