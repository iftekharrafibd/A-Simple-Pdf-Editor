package com.iftekharrafi.asimplepdfeditor.domain.model

data class PageContent(
    val drawnStrokes: List<DrawingStroke> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList()
)