package com.iftekharrafi.asimplepdfeditor.presentation.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.StrokePoint
import com.iftekharrafi.asimplepdfeditor.presentation.editor.EditorTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfEditorState

/**
 * Converts a list of domain StrokePoints into a Compose Path for rendering.
 */
private fun List<StrokePoint>.toComposePath(): Path {
    return Path().apply {
        if (this@toComposePath.isNotEmpty()) {
            moveTo(first().x, first().y)
            drop(1).forEach { lineTo(it.x, it.y) }
        }
    }
}

@Composable
fun DrawingCanvas(
    state: PdfEditorState,
    currentPageContent: PageContent,
    drawingStroke: (DrawingStroke) -> Unit

    ) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // --- ম্যাজিক: ইরেজারকে কাজ করানোর জন্য এই লেয়ার স্ট্র্যাটেজি দিতেই হবে ---
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .pointerInput(state.selectedTool) {
                // Draw অথবা Eraser টুল সিলেক্ট থাকলে কাজ করবে
                if (state.selectedTool == EditorTool.DRAW || state.selectedTool == EditorTool.ERASER) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath =
                                Path().apply { moveTo(offset.x, offset.y) }
                            currentPoints = listOf(StrokePoint(offset.x, offset.y))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.let { path ->
                                path.lineTo(
                                    change.position.x,
                                    change.position.y
                                )
                                currentPath = null
                                currentPath = path
                            }
                            currentPoints = currentPoints + StrokePoint(
                                change.position.x,
                                change.position.y
                            )
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                drawingStroke(
                                    DrawingStroke(
                                        points = currentPoints,
                                        colorArgb = state.brushColor.toArgb(),
                                        strokeWidth = state.brushSize,
                                        // টুল অনুযায়ী ইরেজার কি না সেট করা
                                        isEraser = state.selectedTool == EditorTool.ERASER
                                    ))
                            }
                            currentPath = null
                            currentPoints = emptyList()
                        }
                    )
                }
            }
    )
    {
        currentPageContent.drawnStrokes.forEach { stroke ->
            val path = stroke.points.toComposePath()
            drawPath(
                path = path,
                color = if (stroke.isEraser) Color.Transparent else Color(stroke.colorArgb),
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                // ইরেজার হলে Clear ব্লেন্ড মোড ব্যবহার হবে
                blendMode = if (stroke.isEraser) BlendMode.Clear else BlendMode.SrcOver
            )
        }

        // রিয়েল টাইমে আঁকা বা মোছা
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = if (state.selectedTool == EditorTool.ERASER) Color.Transparent else state.brushColor,
                style = Stroke(
                    width = state.brushSize,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = if (state.selectedTool == EditorTool.ERASER) BlendMode.Clear else BlendMode.SrcOver
            )
        }
    }
}