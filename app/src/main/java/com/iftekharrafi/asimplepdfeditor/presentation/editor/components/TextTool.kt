package com.iftekharrafi.asimplepdfeditor.presentation.editor.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.PdfFont
import com.iftekharrafi.asimplepdfeditor.domain.model.TextOverlay
import com.iftekharrafi.asimplepdfeditor.presentation.editor.mapper.toComposeFontFamily
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPurple

enum class TextToolSelector {
    CHANGE_COLOR,
    CHANGE_FONT
}

@Composable
fun TextTool(
    modifier: Modifier = Modifier,
    currentPageContent: PageContent,
    selectedTextIndex: Int,
    onColorChanged: (Int) -> Unit,
    onTextChanged: (String) -> Unit,
    onFontChanged: (PdfFont) -> Unit,
    onAlignmentChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onStyleBoldToggled: () -> Unit,
    onStyleItalicToggled: () -> Unit,
    onStyleUnderlineToggled: () -> Unit
) {
    var selector by rememberSaveable { mutableStateOf(TextToolSelector.CHANGE_COLOR) }

    // Intercept hardware system back button when TextTool is active to dismiss it
    BackHandler(enabled = true, onBack = onDismiss)

    val selectedOverlay = if (selectedTextIndex in currentPageContent.textOverlays.indices) {
        currentPageContent.textOverlays[selectedTextIndex]
    } else {
        TextOverlay()
    }

    val currentTextColor = Color(selectedOverlay.colorArgb)
    val currentTextAlign = when (selectedOverlay.alignment) {
        "LEFT" -> TextAlign.Left
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Center
    }

    // Auto Focus and Text Selection setup
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = selectedOverlay.text,
                selection = TextRange(0, selectedOverlay.text.length)
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.9f))
                .imePadding() // Essential feature: pushes layout elements up with the soft keyboard resizes
                .padding(paddingValues)
                .clickable(enabled = false, onClick = {}),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Selection Tabs & Dismiss Check Button (Top Bar) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Quick Toggle Text Alignment (LEFT -> CENTER -> RIGHT)
                IconButton(
                    onClick = {
                        val nextAlign = when (selectedOverlay.alignment) {
                            "CENTER" -> "RIGHT"
                            "RIGHT" -> "LEFT"
                            else -> "CENTER"
                        }
                        onAlignmentChanged(nextAlign)
                    }
                ) {
                    val alignIcon = when (selectedOverlay.alignment) {
                        "LEFT" -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        "RIGHT" -> Icons.AutoMirrored.Filled.FormatAlignRight
                        else -> Icons.Filled.FormatAlignCenter
                    }
                    Icon(
                        imageVector = alignIcon,
                        contentDescription = "Change text alignment",
                        tint = Color.White
                    )
                }

                // Center: Tabs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selector = TextToolSelector.CHANGE_COLOR }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ColorLens,
                            contentDescription = "Change text color",
                            tint = if (selector == TextToolSelector.CHANGE_COLOR) AccentBlue else Color.LightGray
                        )
                    }
                    IconButton(
                        onClick = { selector = TextToolSelector.CHANGE_FONT }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FontDownload,
                            contentDescription = "Change text font",
                            tint = if (selector == TextToolSelector.CHANGE_FONT) AccentBlue else Color.LightGray
                        )
                    }
                }

                // Right: Done / Check Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Done",
                        tint = Color.White
                    )
                }
            }

            // --- 2. Text Input Area (Takes remaining vertical space, multiline & vertically scrollable) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onTextChanged(it.text)
                    },
                    placeholder = {
                        Text(
                            text = "এখানে লিখুন...",
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = currentTextAlign
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = currentTextColor,
                        unfocusedTextColor = currentTextColor,
                        cursorColor = AccentBlue,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = TextStyle(
                        textAlign = currentTextAlign,
                        fontFamily = selectedOverlay.font.toComposeFontFamily(),
                        fontSize = 22.sp,
                        fontWeight = if (selectedOverlay.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (selectedOverlay.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                        textDecoration = if (selectedOverlay.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None
                    )
                )
            }

            // --- 3. Text Formatting Toggle Row (B / I / U Style Options) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Bold Toggle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedOverlay.isBold) AccentBlue else Color.White.copy(
                                alpha = 0.1f
                            )
                        )
                        .clickable { onStyleBoldToggled() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Italic Toggle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedOverlay.isItalic) AccentBlue else Color.White.copy(
                                alpha = 0.1f
                            )
                        )
                        .clickable { onStyleItalicToggled() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I",
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 18.sp
                    )
                }

                // Underline Toggle
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectedOverlay.isUnderline) AccentBlue else Color.White.copy(
                                alpha = 0.1f
                            )
                        )
                        .clickable { onStyleUnderlineToggled() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        fontSize = 18.sp
                    )
                }
            }

            // --- 4. Customizer Control Panel (Colors or Fonts - Kept above keyboard) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (selector) {
                    TextToolSelector.CHANGE_COLOR -> {
                        val textColors = listOf(
                            Color.Black, Color.White, AccentBlue, AccentPink, AccentPurple,
                            Color.Red, Color.Green, Color.Yellow
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                16.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            textColors.forEach { color ->
                                val isSelected = selectedOverlay.colorArgb == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(
                                                alpha = 0.2f
                                            ),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChanged(color.toArgb()) }
                                )
                            }
                        }
                    }

                    TextToolSelector.CHANGE_FONT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PdfFont.entries.forEach { fontOption ->
                                val isSelected = selectedOverlay.font == fontOption
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) AccentBlue else Color.White.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                        .clickable { onFontChanged(fontOption) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = fontOption.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        fontFamily = fontOption.toComposeFontFamily()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}