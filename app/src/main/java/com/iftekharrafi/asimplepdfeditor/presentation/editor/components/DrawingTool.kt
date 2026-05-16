package com.iftekharrafi.asimplepdfeditor.presentation.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfEditorState
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPurple

@Composable
fun DrawingTool(
    state: PdfEditorState,
    currentPageContent: PageContent,
    onUndo: () -> Unit,
    onColorSelected: (Color) -> Unit,
    onSizeChanged: (Float) -> Unit

    ) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Drawing Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        // Undo বাটন
        IconButton(
            onClick = {onUndo()},
            enabled = currentPageContent.drawnStrokes.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                tint = if (currentPageContent.drawnStrokes.isNotEmpty()) AccentBlue else Color.Gray
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ব্রাশ কালার পিকার
    val brushColors = listOf(Color.Red, Color.Black, AccentBlue, AccentPink, AccentPurple)
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        brushColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (state.brushColor == color) 3.dp else 0.dp,
                        color = Color.White,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) }
            )
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    // --- নতুন: ব্রাশ সাইজ কন্ট্রোলার (Slider) ---
    Text(
        text = "Brush Size: ${state.brushSize.toInt()}",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(8.dp))

    Slider(
        value = state.brushSize,
        onValueChange = { onSizeChanged(it) },
        valueRange = 2f..40f, // ২ থেকে ৪০ পর্যন্ত সাইজ
        colors = SliderDefaults.colors(
            thumbColor = AccentBlue,
            activeTrackColor = AccentBlue,
            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}