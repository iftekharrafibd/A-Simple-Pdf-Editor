package com.iftekharrafi.asimplepdfeditor.presentation.editor.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.presentation.editor.EditorTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfViewModel
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.CustomTopBar
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.DrawingCanvas
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.EditorToolLayout
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.PageNavigation
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.SavingProgress
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.TextTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.helper.sharePdf
import com.iftekharrafi.asimplepdfeditor.presentation.editor.mapper.toComposeFontFamily
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    viewModel: PdfViewModel = hiltViewModel(),
    pdfUri: Uri?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // নতুন: শুধুমাত্র একটি স্টেট কালেক্ট করা হলো!
    val state by viewModel.state.collectAsState()
    val currentPageContent = state.pageContents[state.currentPageIndex] ?: PageContent()

    var showDiscardChangesDialog by remember { mutableStateOf(false) }

    val hasUnsavedEdits = state.pageContents.values.any { pageContent ->
        pageContent.drawnStrokes.isNotEmpty() || pageContent.textOverlays.any { it.text.isNotEmpty() }
    }

    val handleBackNavigation = {
        if (hasUnsavedEdits) {
            showDiscardChangesDialog = true
        } else {
            onBackClick()
        }
    }

    // Intercept hardware system back button if not actively editing a text box
    if (state.selectedTool != EditorTool.TEXT) {
        BackHandler(enabled = true, onBack = handleBackNavigation)
    }

    LaunchedEffect(pdfUri) {
        if (pdfUri != null) {
            viewModel.loadPdf(uri = pdfUri)
        }
    }

    // ফাইল সেভ হওয়া মাত্রই শেয়ার করার অপশন দেখাবে
    LaunchedEffect(state.savedFileUri) {
        state.savedFileUri?.let { uri ->
            sharePdf(context, uri)
        }
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorBackground)
        ) {
            // --- কাস্টম টপ বার ---
            CustomTopBar(
                state = state,
                onBackClick = handleBackNavigation,
                savePdf = {
                    pdfUri?.let { uri ->
                        viewModel.saveEntirePdf(uri)
                    }
                }
            )

            // --- পিডিএফ পেজ ও ভাসমান টেক্সট দেখানোর জায়গা ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clipToBounds()
                    .pointerInput(state.selectedTool) {
                        if (state.selectedTool == EditorTool.NONE) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                viewModel.onPdfTransformed(pan.x, pan.y, zoom)
                            }
                        }
                    }
                    .clickable(onClick = {
                        viewModel.selectTool(EditorTool.NONE)
                    }),
                contentAlignment = Alignment.Center
            ) {
                // ১. ট্রান্সফর্মেশন বক্স (এখানেই ভিজ্যুয়াল জুম এবং প্যান হবে)
                Box(
                    modifier = Modifier.graphicsLayer(
                        scaleX = state.pdfScale,
                        scaleY = state.pdfScale,
                        translationX = state.pdfOffsetX,
                        translationY = state.pdfOffsetY
                    )
                ) {
                    if (state.pdfBitmap != null) {
                        // ২. অরিজিনাল ক্যানভাস (যেটার ভেতর Image, Canvas, Text আছে)
                        // --- পিডিএফের আসল রেশিও বের করা ---
                        val pdfRatio = state.pdfBitmap!!.width.toFloat() / state.pdfBitmap!!.height.toFloat()

                        Box(
                            modifier = Modifier
                                .aspectRatio(pdfRatio)
                                .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                // ক্যানভাসের রিয়েল সাইজ মাপা হচ্ছে
                                .onGloballyPositioned { coordinates ->
                                    viewModel.onCanvasSizeChanged(
                                        coordinates.size.width.toFloat(),
                                        coordinates.size.height.toFloat()
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = state.pdfBitmap!!.asImageBitmap(),
                                contentDescription = "PDF Page",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                            // ২. নতুন: ড্রয়িং ক্যানভাস
                            DrawingCanvas(
                                state = state,
                                currentPageContent = currentPageContent,
                                drawingStroke = { viewModel.addStroke(it) }
                            )

                            // --- Canva-স্টাইল স্মার্ট টেক্সট বক্স ---
                            currentPageContent.textOverlays.forEachIndexed { index, textOverlay ->
                                if (textOverlay.text.isNotEmpty()) {
                                    val isSelectedText = state.selectedTextIndex == index && state.selectedTool == EditorTool.TEXT
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .graphicsLayer(
                                                translationX = textOverlay.offsetX,
                                                translationY = textOverlay.offsetY,
                                                scaleX = textOverlay.scale,
                                                scaleY = textOverlay.scale,
                                                rotationZ = textOverlay.rotation
                                            )
                                            .pointerInput(index) {
                                                detectTransformGestures { _, pan, zoom, rotation ->
                                                    viewModel.selectTextOverlay(index)
                                                    viewModel.onTextTransformed(pan.x, pan.y, zoom, rotation)
                                                }
                                            }
                                            .border(
                                                width = (1.5.dp / maxOf(textOverlay.scale, 0.1f)),
                                                color = if (isSelectedText) AccentBlue.copy(alpha = 0.8f) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .background(
                                                color = if (isSelectedText) Color.Black.copy(alpha = 0.05f) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            // টেক্সট ক্যানভাসের সাইজ মাপা
                                            .onGloballyPositioned { coordinates ->
                                                if (isSelectedText) {
                                                    viewModel.onTextSizeChanged(
                                                        coordinates.size.width.toFloat(),
                                                        coordinates.size.height.toFloat()
                                                    )
                                                }
                                            }
                                            .clickable(
                                                onClick = {
                                                    viewModel.selectTextOverlay(index)
                                                    viewModel.selectTool(EditorTool.TEXT)
                                                }
                                            )
                                    ) {
                                        Text(
                                            text = textOverlay.text,
                                            color = Color(textOverlay.colorArgb),
                                            fontSize = 24.sp,
                                            fontWeight = if (textOverlay.isBold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (textOverlay.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                            textDecoration = if (textOverlay.isUnderline) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
                                            fontFamily = textOverlay.font.toComposeFontFamily(),
                                            textAlign = when (textOverlay.alignment) {
                                                "LEFT" -> TextAlign.Left
                                                "RIGHT" -> TextAlign.Right
                                                else -> TextAlign.Center
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentBlue)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Loading PDF...", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // ---  টুলস (টেক্সট ইনপুট এবং কালার) ---
            if (state.pdfBitmap != null) {
                EditorToolLayout(
                    editorTool = { tool ->
                        if (tool == EditorTool.TEXT) {
                            viewModel.addNewTextOverlay()
                        } else {
                            viewModel.selectTool(tool)
                        }
                    },
                    state = state
                )
            }
            // --- পেজ নেভিগেশন বার (Bottom Bar) ---
            if (state.pageCount > 0) {
                PageNavigation(
                    state = state,
                    previousPage = { viewModel.previousPage() },
                    nextPage = { viewModel.nextPage() }

                )
            }

        }
        if (state.selectedTool == EditorTool.TEXT) {
            val selectedIndex = state.selectedTextIndex ?: 0
            TextTool(
                currentPageContent = currentPageContent,
                selectedTextIndex = selectedIndex,
                onColorChanged = { viewModel.onColorChanged(it) },
                onTextChanged = { viewModel.onTextChanged(it) },
                onFontChanged = { viewModel.onFontChanged(it) },
                onAlignmentChanged = { viewModel.onAlignmentChanged(it) },
                onDismiss = { viewModel.selectTool(EditorTool.NONE) },
                onStyleBoldToggled = { viewModel.onStyleBoldToggled() },
                onStyleItalicToggled = { viewModel.onStyleItalicToggled() },
                onStyleUnderlineToggled = { viewModel.onStyleUnderlineToggled() }
            )
        }
    }

// --- নতুন: Pro Tool Bottom Sheet ---
/*
    if (state.isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissBottomSheet() },
            sheetState = sheetState,
            containerColor = EditorSurface,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
              */
/*  // কোন টুল সিলেক্ট করা হয়েছে তার ওপর ভিত্তি করে UI দেখাবে
                when (state.selectedTool) {

                    // Bottom Sheet এর ভেতরে:
                    EditorTool.DRAW -> {
                        DrawingTool(
                            state = state,
                            currentPageContent = currentPageContent,
                            onUndo = { viewModel.undoLastStroke() },
                            onColorSelected = { viewModel.setBrushColor(it) },
                            onSizeChanged = { viewModel.setBrushSize(it) }
                        )
                    }
                    else -> {}
                }*//*

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
*/

    // --- ফুল-স্ক্রিন লোডিং ওভারলে (যখন PDF সেভ হবে) ---
    if (state.isSaving) {
        SavingProgress(state.savingProgress)
    }

    // --- Discard Changes Warning Alert Dialog ---
    if (showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardChangesDialog = false },
            title = {
                Text(
                    text = "Discard changes?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to discard your edits? This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardChangesDialog = false
                        onBackClick()
                    }
                ) {
                    Text(text = "Discard", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardChangesDialog = false }
                ) {
                    Text(text = "Keep Editing", color = AccentBlue, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = EditorSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
