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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.PdfFont
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPurple
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground

@Composable
fun TextTool(
    currentPageContent: PageContent,
    onColorChanged: (Int) -> Unit,
    onTextChanged: (String) -> Unit,
    onFontChanged: (PdfFont) -> Unit

) {

    Text(
        text = "Text Editor Tools",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    Spacer(modifier = Modifier.height(24.dp))

    // ১. টেক্সট কালার পিকার (Text Color Picker)
    Text(
        text = "Select Color",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(12.dp))

    val textColors = listOf(Color.Black, Color.White, AccentBlue, AccentPink, AccentPurple, Color.Red)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        textColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (currentPageContent.textOverlay.colorArgb == color.toArgb()) 3.dp else 1.dp,
                        color = if (currentPageContent.textOverlay.colorArgb == color.toArgb()) Color.White else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .clickable { onColorChanged(color.toArgb()) }
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // ২. টেক্সট ইনপুট ফিল্ড (OutlinedTextField)
    Text(
        text = "Enter Content",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = currentPageContent.textOverlay.text,
        onValueChange = { onTextChanged(it) },
        placeholder = {
            Text("Type your notes here...", color = Color.White.copy(alpha = 0.3f))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentBlue,
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AccentBlue,
            focusedContainerColor = EditorBackground.copy(alpha = 0.5f),
            unfocusedContainerColor = EditorBackground.copy(alpha = 0.5f)
        )
    )

    Spacer(modifier = Modifier.height(32.dp))
    Text(
        text = "Select Font Style",
        style = MaterialTheme.typography.labelMedium,
        color = Color.White.copy(alpha = 0.5f),
    )
    Spacer(modifier = Modifier.height(12.dp))

// সব ফন্টের অপশন লুপ চালিয়ে দেখানো হচ্ছে
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PdfFont.entries.forEach { fontOption ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (currentPageContent.textOverlay.font == fontOption) AccentBlue else EditorBackground)
                    .clickable { onFontChanged(fontOption) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = fontOption.displayName,
                    color = if (currentPageContent.textOverlay.font == fontOption) Color.White else Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
    // কিবোর্ড যেন নিচ থেকে ঢাকা না পড়ে সেজন্য একটু এক্সট্রা স্পেস
    Spacer(modifier = Modifier.height(40.dp))
}