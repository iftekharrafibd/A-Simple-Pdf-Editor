package com.iftekharrafi.asimplepdfeditor.presentation.editor.screen

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.presentation.editor.EditorTool
import com.iftekharrafi.asimplepdfeditor.presentation.editor.PdfViewModel
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.CustomTopBar
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.DrawingCanvas
import com.iftekharrafi.asimplepdfeditor.presentation.editor.components.DrawingTool
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
    viewModel: PdfViewModel = hiltViewModel(), // Issue 1 Fix: hiltViewModel() instead of viewModel()
    pdfUri: Uri?,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // নতুন: শুধুমাত্র একটি স্টেট কালেক্ট করা হলো!
    val state by viewModel.state.collectAsState()
    val currentPageContent = state.pageContents[state.currentPageIndex] ?: PageContent()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        // --- কাস্টম টপ বার ---
        CustomTopBar(
            state = state,
            onBackClick = onBackClick,
            savePdf = {
                // pdfUri যেহেতু nullable (Uri?), তাই null চেক করে পাস করে দিচ্ছি
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
                        if (currentPageContent.textOverlay.text.isNotEmpty()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .graphicsLayer(
                                        translationX = currentPageContent.textOverlay.offsetX,
                                        translationY = currentPageContent.textOverlay.offsetY,
                                        scaleX = currentPageContent.textOverlay.scale,
                                        scaleY = currentPageContent.textOverlay.scale,
                                        rotationZ = currentPageContent.textOverlay.rotation
                                    )
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, rotation ->
                                            viewModel.onTextTransformed(pan.x, pan.y, zoom, rotation)
                                        }
                                    }
                                    .border(
                                        width = (1.5.dp / maxOf(currentPageContent.textOverlay.scale, 0.1f)),
                                        color = if (state.selectedTool == EditorTool.TEXT) AccentBlue.copy(alpha = 0.8f) else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (state.selectedTool == EditorTool.TEXT) Color.Black.copy(alpha = 0.05f) else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    // টেক্সট বক্সের রিয়েল সাইজ মেপে পাঠানো হচ্ছে
                                    .onGloballyPositioned { coordinates ->
                                        viewModel.onTextSizeChanged(
                                            coordinates.size.width.toFloat(),
                                            coordinates.size.height.toFloat()
                                        )
                                    }
                            ) {
                                Text(
                                    text = currentPageContent.textOverlay.text,
                                    color = Color(currentPageContent.textOverlay.colorArgb),
                                    fontSize = 24.sp, // UI বেস সাইজ ২৪
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = currentPageContent.textOverlay.font.toComposeFontFamily(),
                                    textAlign = TextAlign.Center
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
        }

        // ---  টুলস (টেক্সট ইনপুট এবং কালার) ---
        if (state.pdfBitmap != null) {
            EditorToolLayout(
                editorTool = { viewModel.selectTool(it) },
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
// --- নতুন: Pro Tool Bottom Sheet ---
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
                // কোন টুল সিলেক্ট করা হয়েছে তার ওপর ভিত্তি করে UI দেখাবে
                when (state.selectedTool) {
                    EditorTool.TEXT -> {
                        TextTool(
                            currentPageContent = currentPageContent,
                            onColorChanged = { viewModel.onColorChanged(it) },
                            onTextChanged = { viewModel.onTextChanged(it) },
                            onFontChanged = { viewModel.onFontChanged(it) }
                        )
                    }
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
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    // --- ফুল-স্ক্রিন লোডিং ওভারলে (যখন PDF সেভ হবে) ---
    if (state.isSaving) {
        SavingProgress(state.savingProgress)
    }
}
