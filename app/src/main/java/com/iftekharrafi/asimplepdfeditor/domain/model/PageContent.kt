package com.iftekharrafi.asimplepdfeditor.domain.model

data class PageContent(
    val drawnStrokes: List<DrawingStroke> = emptyList(),
    val textOverlay: TextOverlay = TextOverlay()
)