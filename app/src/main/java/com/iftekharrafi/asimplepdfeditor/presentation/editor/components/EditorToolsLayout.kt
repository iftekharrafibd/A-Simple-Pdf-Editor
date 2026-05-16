package com.iftekharrafi.asimplepdfeditor.presentation.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iftekharrafi.asimplepdfeditor.presentation.editor.EditorTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfEditorState
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface

@Composable
fun EditorToolLayout(
    editorTool:(EditorTool) -> Unit,
    state: PdfEditorState

) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(EditorSurface.copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    )
    {
        // Text Tool Button
        IconButton(onClick = { editorTool(EditorTool.TEXT)}) {
            Icon(
                imageVector = Icons.Default.TextFields,
                contentDescription = "Add Text",
                tint = if (state.selectedTool == EditorTool.TEXT) AccentBlue else Color.White.copy(alpha = 0.6f)
            )
        }

        // Draw Tool Button
        IconButton(onClick = { editorTool(EditorTool.DRAW) }) {
            Icon(
                imageVector = Icons.Default.Create, // অথবা Brush আইকন
                contentDescription = "Draw",
                tint = if (state.selectedTool == EditorTool.DRAW) AccentPink else Color.White.copy(alpha = 0.6f)
            )
        }
        // --- নতুন: Eraser Tool Button ---
        IconButton(onClick = {editorTool(EditorTool.ERASER)}) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh, // ইরেজার আইকন
                contentDescription = "Eraser",
                tint = if (state.selectedTool == EditorTool.ERASER) Color.White else Color.White.copy(alpha = 0.6f)
            )
        }
    }

}