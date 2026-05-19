package com.iftekharrafi.asimplepdfeditor.presentation.editor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfEditorState
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface

@Composable
fun PageNavigation(
    modifier: Modifier = Modifier,
    state: PdfEditorState,
    previousPage: () -> Unit,
    nextPage: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(EditorSurface.copy(alpha = 0.85f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = previousPage,
                enabled = state.currentPageIndex > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Page",
                    tint = if (state.currentPageIndex > 0) AccentBlue else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${state.currentPageIndex + 1} / ${state.pageCount}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = nextPage,
                enabled = state.currentPageIndex < state.pageCount - 1,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Page",
                    tint = if (state.currentPageIndex < state.pageCount - 1) AccentBlue else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}