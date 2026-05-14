package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPurple
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PdfEditorScreen(
    viewModel: PdfViewModel = viewModel(),
    pdfUri: Uri?,
    onBackClick: () -> Unit
) {
    // নতুন: শুধুমাত্র একটি স্টেট কালেক্ট করা হলো!
    val state by viewModel.state.collectAsState()
    val currentPageContent = state.pageContents[state.currentPageIndex] ?: PageContent()

    val captureController = rememberCaptureController()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentPath by remember { mutableStateOf<Path?>(null) }
    LaunchedEffect(pdfUri) {
        if (pdfUri != null) {
            viewModel.loadPdf(uri = pdfUri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        // --- কাস্টম টপ বার ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(EditorSurface.copy(alpha = 0.6f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Editing Document",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.pageCount > 0) {
                        Text(
                            text = "Page ${state.currentPageIndex + 1} of ${state.pageCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentBlue
                        )
                    }
                }
            }

            // সেভ বাটন
            if (state.pdfBitmap != null) {
                IconButton(
                    onClick = {
                        if (!state.isSaving) {
                            // এখন আর কোনো প্যারামিটার লাগছে না!
                            viewModel.saveEntirePdf()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save PDF",
                        tint = if (state.isSaving) Color.Gray else AccentBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // --- পিডিএফ পেজ ও ভাসমান টেক্সট দেখানোর জায়গা ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .clipToBounds() // জুম করার পর পিডিএফ যেন এডিটর এরিয়ার বাইরে না চলে যায়
                .pointerInput(state.selectedTool) {
                    // শুধুমাত্র যখন কোনো টুল সিলেক্ট করা থাকবে না (NONE), তখন জুম কাজ করবে
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(elevation = 16.dp, shape = RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            // ম্যাজিক: ক্যানভাসের রিয়েল সাইজ মাপা হচ্ছে
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
                            contentScale = ContentScale.Fit
                        )
// ২. নতুন: ড্রয়িং ক্যানভাস
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(state.selectedTool) {
                                    // শুধুমাত্র DRAW টুল সিলেক্ট থাকলে ড্রয়িং কাজ করবে
                                    if (state.selectedTool == EditorTool.DRAW) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                // আঙুল ছোঁয়ালে নতুন Path শুরু হবে
                                                currentPath = Path().apply {
                                                    moveTo(offset.x, offset.y)
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentPath?.let {
                                                    it.lineTo(change.position.x, change.position.y)
                                                    // Path copy kore naya reference banano
                                                    currentPath = Path().apply { addPath(it) }
                                                }
                                            },
                                            onDragEnd = {
                                                // আঙুল তুলে নিলে Path টা ভিউমডেলে সেভ হয়ে যাবে
                                                currentPath?.let { path ->
                                                    viewModel.addStroke(
                                                        DrawingStroke(
                                                            path = path,
                                                            color = state.brushColor,
                                                            strokeWidth = state.brushSize
                                                        )
                                                    )
                                                }
                                                currentPath = null
                                            }
                                        )
                                    }
                                }
                        ) {
                            // সেভ করা সব স্ট্রোক ড্র করা হচ্ছে
                            currentPageContent.drawnStrokes.forEach { stroke ->
                                drawPath(
                                    path = stroke.path,
                                    color = stroke.color,
                                    style = Stroke(
                                        width = stroke.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // রিয়েল-টাইমে যে রেখাটা আঁকা হচ্ছে সেটা ড্র করা হচ্ছে
                            currentPath?.let { path ->
                                drawPath(
                                    path = path,
                                    color = state.brushColor,
                                    style = Stroke(
                                        width = state.brushSize,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                        if (currentPageContent.textOverlay.text.isNotEmpty()) {
                            Text(
                                text = currentPageContent.textOverlay.text,
                                color = currentPageContent.textOverlay.color,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
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
                                            viewModel.onTextTransformed(
                                                pan.x,
                                                pan.y,
                                                zoom,
                                                rotation
                                            )
                                        }
                                    }
                            )
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
            ) {
                // Text Tool Button
                IconButton(onClick = { viewModel.selectTool(EditorTool.TEXT) }) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Add Text",
                        tint = if (state.selectedTool == EditorTool.TEXT) AccentBlue else Color.White.copy(alpha = 0.6f)
                    )
                }

                // Draw Tool Button
                IconButton(onClick = { viewModel.selectTool(EditorTool.DRAW) }) {
                    Icon(
                        imageVector = Icons.Default.Create, // অথবা Brush আইকন
                        contentDescription = "Draw",
                        tint = if (state.selectedTool == EditorTool.DRAW) AccentPink else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
        // --- পেজ নেভিগেশন বার (Bottom Bar) ---
        if (state.pageCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(EditorSurface.copy(alpha = 0.8f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousPage() },
                        enabled = state.currentPageIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Page",
                            tint = if (state.currentPageIndex > 0) AccentBlue else Color.White.copy(alpha = 0.2f)
                        )
                    }

                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${state.currentPageIndex + 1} / ${state.pageCount}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextPage() },
                        enabled = state.currentPageIndex < state.pageCount - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Page",
                            tint = if (state.currentPageIndex < state.pageCount - 1) AccentBlue else Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
// --- নতুন: Pro Tool Bottom Sheet ---
    if (state.isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissBottomSheet() },
            sheetState = sheetState,
            containerColor = EditorSurface, // আমাদের ডার্ক থিমের সাথে ম্যাচ করার জন্য
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
                        // টাইটেল
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
                            modifier = Modifier.align(Alignment.Start)
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
                                            width = if (currentPageContent.textOverlay.color == color) 3.dp else 1.dp,
                                            color = if (currentPageContent.textOverlay.color == color) Color.White else Color.White.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .clickable { viewModel.onColorChanged(color) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // ২. টেক্সট ইনপুট ফিল্ড (OutlinedTextField)
                        Text(
                            text = "Enter Content",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = currentPageContent.textOverlay.text,
                            onValueChange = { viewModel.onTextChanged(it) },
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

                        // কিবোর্ড যেন নিচ থেকে ঢাকা না পড়ে সেজন্য একটু এক্সট্রা স্পেস
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                    // Bottom Sheet এর ভেতরে:
                    EditorTool.DRAW -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drawing Tools", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                            // Undo বাটন
                            IconButton(
                                onClick = { viewModel.undoLastStroke() },
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
                                        .clickable { viewModel.setBrushColor(color) }
                                )
                            }
                        }
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    // --- ফুল-স্ক্রিন লোডিং ওভারলে (যখন PDF সেভ হবে) ---
    if (state.isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // একটু চওড়া করলাম
                    .clip(RoundedCornerShape(20.dp))
                    .background(EditorSurface)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ১. স্টাইলিশ পার্সেন্টেজ টেক্সট
                val percentage = (state.savingProgress * 100).toInt()
                Text(
                    text = "$percentage%",
                    color = AccentBlue,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ২. লিনিয়ার প্রগ্রেস বার
                LinearProgressIndicator(
                    progress = { state.savingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentBlue,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Exporting Document...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High-quality processing takes time. Please don't close the app.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }}