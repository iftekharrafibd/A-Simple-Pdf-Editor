package com.iftekharrafi.asimplepdfeditor.presentation.editor.screen

import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.PdfFont
import com.iftekharrafi.asimplepdfeditor.presentation.editor.EditorTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfEditorState
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfViewModel
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.CustomTopBar
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.DrawingCanvas
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.EditorToolLayout
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.PageNavigation
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.SavingProgress
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.TextTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.mapper.toComposeFontFamily
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun PdfEditorScreen(
    viewModel: PdfViewModel = hiltViewModel(),
    pdfUri: Uri?,
    initialPageIndex: Int = 0,
    onSaveComplete: (Uri) -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // নতুন: শুধুমাত্র একটি স্টেট কালেক্ট করা হলো!
    val state by viewModel.state.collectAsState()

    LaunchedEffect(pdfUri, initialPageIndex) {
        if (pdfUri != null) {
            viewModel.loadPdf(uri = pdfUri, initialPageIndex = initialPageIndex)
        }
    }

    // ফাইল সেভ হওয়া মাত্রই Home Screen-এ ফেরত যাবে
    LaunchedEffect(state.savedFileUri) {
        state.savedFileUri?.let { uri ->
            onSaveComplete(uri)
        }
    }

    // Clean up saved URI state on exit to prevent stale navigation triggers
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSavedFileUri()
        }
    }

    PdfEditorContent(
        state = state,
        onBackClick = onBackClick,
        onSavePdf = {
            pdfUri?.let { uri ->
                viewModel.saveEntirePdf(uri)
            }
        },
        onUndoClick = { viewModel.undoLastStroke() },
        onRedoClick = { viewModel.redoLastStroke() },
        onPdfTransformed = { x, y, zoom -> viewModel.onPdfTransformed(x, y, zoom) },
        onPageSelected = { viewModel.setCurrentPageIndex(it) },
        onSelectTool = { viewModel.selectTool(it) },
        onCanvasSizeChanged = { w, h -> viewModel.onCanvasSizeChanged(w, h) },
        onAddStroke = { viewModel.addStroke(it) },
        onSelectTextOverlay = { pageIndex, textIndex -> viewModel.selectTextOverlay(pageIndex, textIndex) },
        onTextTransformed = { x, y, zoom, rot -> viewModel.onTextTransformed(x, y, zoom, rot) },
        onTextSizeChanged = { w, h -> viewModel.onTextSizeChanged(w, h) },
        onAddNewTextOverlay = { viewModel.addNewTextOverlay() },
        onPreviousPage = { viewModel.previousPage() },
        onNextPage = { viewModel.nextPage() },
        onColorChanged = { viewModel.onColorChanged(it) },
        onTextChanged = { viewModel.onTextChanged(it) },
        onFontChanged = { viewModel.onFontChanged(it) },
        onAlignmentChanged = { viewModel.onAlignmentChanged(it) },
        onStyleBoldToggled = { viewModel.onStyleBoldToggled() },
        onStyleItalicToggled = { viewModel.onStyleItalicToggled() },
        onStyleUnderlineToggled = { viewModel.onStyleUnderlineToggled() },
        onRequestPageBitmap = { viewModel.requestPageBitmap(it) }
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorContent(
    state: PdfEditorState,
    onBackClick: () -> Unit,
    onSavePdf: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onPdfTransformed: (Float, Float, Float) -> Unit,
    onPageSelected: (Int) -> Unit,
    onSelectTool: (EditorTool) -> Unit,
    onCanvasSizeChanged: (Float, Float) -> Unit,
    onAddStroke: (DrawingStroke) -> Unit,
    onSelectTextOverlay: (Int, Int?) -> Unit,
    onTextTransformed: (Float, Float, Float, Float) -> Unit,
    onTextSizeChanged: (Float, Float) -> Unit,
    onAddNewTextOverlay: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onColorChanged: (Int) -> Unit,
    onTextChanged: (String) -> Unit,
    onFontChanged: (PdfFont) -> Unit,
    onAlignmentChanged: (String) -> Unit,
    onStyleBoldToggled: () -> Unit,
    onStyleItalicToggled: () -> Unit,
    onStyleUnderlineToggled: () -> Unit,
    onRequestPageBitmap: (Int) -> Unit
) {
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
                savePdf = onSavePdf,
                onUndoClick = onUndoClick,
                onRedoClick = onRedoClick
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
                                onPdfTransformed(pan.x, pan.y, zoom)
                            }
                        }
                    }
                    .pointerInput(state.selectedTool) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (state.selectedTool == EditorTool.NONE) {
                                    if (state.pdfScale > 1f) {
                                        // Reset zoom to 1x
                                        onPdfTransformed(0f, 0f, 0.01f)
                                    } else {
                                        // Zoom in to 2.5x
                                        onPdfTransformed(0f, 0f, 2.5f)
                                    }
                                }
                            },
                            onTap = {
                                onSelectTool(EditorTool.NONE)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // ১. ট্রান্সফর্মেশন বক্স (এখানেই ভিজ্যুয়াল জুম এবং প্যান হবে)
                Box(
                    modifier = Modifier.graphicsLayer(
                        scaleX = state.pdfScale,
                        scaleY = state.pdfScale,
                        translationX = state.pdfOffsetX,
                        translationY = state.pdfOffsetY
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    val pdfBitmaps = state.pdfBitmaps
                    if (state.pageCount > 0) {
                        val pageIndex = state.currentPageIndex
                        val pageBitmap = pdfBitmaps[pageIndex]
                        val pageContent = state.pageContents[pageIndex] ?: PageContent()

                        LaunchedEffect(pageIndex) {
                            onRequestPageBitmap(pageIndex)
                        }

                        if (pageBitmap != null) {
                            val pdfRatio = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .aspectRatio(pdfRatio)
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .onGloballyPositioned { coordinates ->
                                        onCanvasSizeChanged(
                                            coordinates.size.width.toFloat(),
                                            coordinates.size.height.toFloat()
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = pageBitmap.asImageBitmap(),
                                    contentDescription = "PDF Page ${pageIndex + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )

                                // Drawing Canvas for this specific page
                                DrawingCanvas(
                                    state = state,
                                    currentPageContent = pageContent,
                                    drawingStroke = { stroke ->
                                        onPageSelected(pageIndex)
                                        onAddStroke(stroke)
                                    }
                                )

                                // Canva-style smart text boxes for this specific page
                                pageContent.textOverlays.forEachIndexed { index, textOverlay ->
                                    if (textOverlay.text.isNotEmpty()) {
                                        val isSelectedText = state.selectedTextIndex == Pair(pageIndex, index) && state.selectedTool == EditorTool.TEXT

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
                                                        onPageSelected(pageIndex)
                                                        onSelectTextOverlay(pageIndex, index)
                                                        onTextTransformed(pan.x, pan.y, zoom, rotation)
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
                                                .onGloballyPositioned { coordinates ->
                                                    if (isSelectedText) {
                                                        onTextSizeChanged(
                                                            coordinates.size.width.toFloat(),
                                                            coordinates.size.height.toFloat()
                                                        )
                                                    }
                                                }
                                                .clickable(
                                                    onClick = {
                                                        onPageSelected(pageIndex)
                                                        onSelectTextOverlay(pageIndex, index)
                                                        onSelectTool(EditorTool.TEXT)
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
                            // Premium white placeholder card during lazy loading
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.95f)
                                    .aspectRatio(0.707f) // Standard A4 Aspect Ratio
                                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AccentBlue,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Page ${pageIndex + 1} Loading...",
                                        color = Color.DarkGray.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
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

                // ২. পেজ নেভিগেশন বার (Floating on the bottom-left of the PDF editor canvas)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (state.pageCount > 0) {
                        PageNavigation(
                            state = state,
                            previousPage = onPreviousPage,
                            nextPage = onNextPage
                        )
                    }
                }
            }
            // ---  টুলস (টেক্সট ইনপুট এবং কালার) ---
            if (state.pageCount > 0) {
                EditorToolLayout(
                    editorTool = { tool ->
                        if (tool == EditorTool.TEXT) {
                            onAddNewTextOverlay()
                        } else {
                            onSelectTool(tool)
                        }
                    },
                    state = state
                )
            }


        }
        if (state.selectedTool == EditorTool.TEXT) {
            val selectedIndex = if (state.selectedTextIndex?.first == state.currentPageIndex) {
                state.selectedTextIndex.second
            } else {
                0
            }
            TextTool(
                currentPageContent = currentPageContent,
                selectedTextIndex = selectedIndex,
                onColorChanged = onColorChanged,
                onTextChanged = onTextChanged,
                onFontChanged = onFontChanged,
                onAlignmentChanged = onAlignmentChanged,
                onDismiss = { onSelectTool(EditorTool.NONE) },
                onStyleBoldToggled = onStyleBoldToggled,
                onStyleItalicToggled = onStyleItalicToggled,
                onStyleUnderlineToggled = onStyleUnderlineToggled
            )
        }
    }

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

/*@RequiresApi(Build.VERSION_CODES.Q)
@Preview(showBackground = true)
@Composable
fun PdfEditorPreview() {
    val sampleBitmap = remember {
        Bitmap.createBitmap(1000, 1500, Bitmap.Config.ARGB_8888).apply {
            val canvas = android.graphics.Canvas(this)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.LTGRAY
                strokeWidth = 5f
                style = android.graphics.Paint.Style.STROKE
            }
            // Draw some lines to simulate PDF content
            for (i in 1..10) {
                canvas.drawLine(100f, i * 100f, 900f, i * 100f, paint)
            }
        }
    }
    ASimplePdfEditorTheme {
        PdfEditorContent(
            state = PdfEditorState(
                pdfBitmap = sampleBitmap,
                pageCount = 3,
                currentPageIndex = 0,
                pageContents = mapOf(
                    0 to PageContent(
                        drawnStrokes = listOf(
                            DrawingStroke(
                                points = listOf(
                                    StrokePoint(100f, 100f),
                                    StrokePoint(200f, 300f),
                                    StrokePoint(400f, 200f)
                                ),
                                colorArgb = android.graphics.Color.BLUE,
                                strokeWidth = 10f
                            )
                        ),
                        textOverlays = listOf(
                            TextOverlay(
                                text = "Sample PDF Editor",
                                offsetX = 300f,
                                offsetY = 500f,
                                colorArgb = android.graphics.Color.RED,
                                isBold = true
                            )
                        )
                    )
                )
            ),
            onBackClick = {},
            onSavePdf = {},
            onUndoClick = {},
            onRedoClick = {},
            onPdfTransformed = { _, _, _ -> },
            onPageSelected = {},
            onSelectTool = {},
            onCanvasSizeChanged = { _, _ -> },
            onAddStroke = {},
            onSelectTextOverlay = {},
            onTextTransformed = { _, _, _, _ -> },
            onTextSizeChanged = { _, _ -> },
            onAddNewTextOverlay = {},
            onPreviousPage = {},
            onNextPage = {},
            onColorChanged = {},
            onTextChanged = {},
            onFontChanged = {},
            onAlignmentChanged = {},
            onStyleBoldToggled = {},
            onStyleItalicToggled = {},
            onStyleUnderlineToggled = {}
        )
    }
}*/
