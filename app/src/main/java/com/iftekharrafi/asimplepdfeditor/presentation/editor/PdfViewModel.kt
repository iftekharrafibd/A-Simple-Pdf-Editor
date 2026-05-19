package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.app.Application
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.pdf.PdfRenderer
import android.graphics.text.LineBreaker
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.PdfFont
import com.iftekharrafi.asimplepdfeditor.domain.model.RecentPdf
import com.iftekharrafi.asimplepdfeditor.domain.model.TextOverlay
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import com.iftekharrafi.asimplepdfeditor.presentation.editor.mapper.toNativeTypeface
import com.iftekharrafi.asimplepdfeditor.utils.getFileName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PdfViewModel @Inject constructor(
    application: Application,
    private val repository: PdfRepository
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(PdfEditorState())
    val state = _state.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null

    // Issue 2 Fix: Single-threaded dispatcher ensures PdfRenderer operations
    // are serialized and never run concurrently (PdfRenderer is NOT thread-safe)
    private val renderDispatcher = Dispatchers.IO.limitedParallelism(1)

    // A map of page index to list of undone strokes for Redo support
    private val undoneStrokesMap = mutableMapOf<Int, List<DrawingStroke>>()

    // LRU cache to store page bitmaps and prevent OutOfMemory crashes (holds max 5 pages)
    private val lruCache = object : android.util.LruCache<Int, android.graphics.Bitmap>(5) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: android.graphics.Bitmap?, newValue: android.graphics.Bitmap?) {
            // Memory is reclaimed naturally by standard Android JVM Garbage Collection
        }
    }

    fun loadPdf(uri: Uri, initialPageIndex: Int = 0) {
        undoneStrokesMap.clear()
        lruCache.evictAll()
        // লোডিং শুরু করার আগে আগের পিডিএফ ক্লিয়ার করে দাও
        _state.update { PdfEditorState() }

        viewModelScope.launch(renderDispatcher) {
            try {
                val context = getApplication<Application>().applicationContext

                fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw Exception("ফাইলটি খুঁজে পাওয়া যাচ্ছে না বা পারমিশন নেই")

                pdfRenderer = PdfRenderer(fileDescriptor!!)
                val count = pdfRenderer!!.pageCount
                _state.update {
                    it.copy(
                        pageCount = count,
                        currentPageIndex = initialPageIndex
                    )
                }

                updateUndoRedoAvailability()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun requestPageBitmap(pageIndex: Int) {
        if (pdfRenderer == null) return

        // 1. Cache Hit: Reactively expose LruCache contents to State Flow
        val cached = lruCache.get(pageIndex)
        if (cached != null) {
            if (!_state.value.pdfBitmaps.containsKey(pageIndex)) {
                _state.update { it.copy(pdfBitmaps = lruCache.snapshot()) }
            }
            return
        }

        // 2. Cache Miss: Render on the single-threaded rendering background dispatcher
        viewModelScope.launch(renderDispatcher) {
            try {
                pdfRenderer?.let { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        val page = renderer.openPage(pageIndex)
                        val bitmap = createBitmap(page.width * 2, page.height * 2)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        lruCache.put(pageIndex, bitmap)
                        _state.update { currentState ->
                            currentState.copy(
                                pdfBitmaps = lruCache.snapshot(),
                                pdfBitmap = if (pageIndex == 0) bitmap else currentState.pdfBitmap
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Must be called from renderDispatcher to ensure thread safety
    private fun renderPage(pageIndex: Int) {
        pdfRenderer?.let { renderer ->
            if (pageIndex < renderer.pageCount) {
                currentPage?.close()
                val page = renderer.openPage(pageIndex)
                currentPage = page

                val bitmap = createBitmap(page.width * 2, page.height * 2)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // State আপডেট: পেজ পাল্টালে শুধু জুম এবং পজিশন রিসেট হবে, পুরোনো টেক্সট মুছবে না
                _state.update {
                    it.copy(
                        pdfBitmap = bitmap,
                        pdfScale = 1f,
                        pdfOffsetX = 0f,
                        pdfOffsetY = 0f
                    )
                }
            }
        }
    }

    fun nextPage() {
        val currentState = _state.value
        if (currentState.currentPageIndex < currentState.pageCount - 1) {
            val nextIndex = currentState.currentPageIndex + 1
            _state.update { it.copy(currentPageIndex = nextIndex) }
            updateUndoRedoAvailability()
        }
    }

    fun previousPage() {
        val currentState = _state.value
        if (currentState.currentPageIndex > 0) {
            val prevIndex = currentState.currentPageIndex - 1
            _state.update { it.copy(currentPageIndex = prevIndex) }
            updateUndoRedoAvailability()
        }
    }
    fun setCurrentPageIndex(index: Int) {
        if (index in 0 until _state.value.pageCount) {
            _state.update { it.copy(currentPageIndex = index) }
            updateUndoRedoAvailability()
        }
    }
    // --- পেজ কন্টেন্ট আপডেট করার প্রো-লেভেল হেল্পার ফাংশন ---
    private fun updateCurrentPageContent(update: (PageContent) -> PageContent) {
        _state.update { currentState ->
            val currentIndex = currentState.currentPageIndex
            // বর্তমান পেজের ডেটা নাও, না থাকলে নতুন ফাঁকা ডেটা নাও
            val currentContent = currentState.pageContents[currentIndex] ?: PageContent()
            val newContent = update(currentContent)
            // Map-এর ভেতরে বর্তমান পেজের ডেটা রিপ্লেস করে দাও
            currentState.copy(pageContents = currentState.pageContents + (currentIndex to newContent))
        }
        updateUndoRedoAvailability()
    }

    private fun updateUndoRedoAvailability() {
        val currentState = _state.value
        val pageIndex = currentState.currentPageIndex
        val pageContent = currentState.pageContents[pageIndex] ?: PageContent()
        val canUndo = pageContent.drawnStrokes.isNotEmpty()
        val canRedo = (undoneStrokesMap[pageIndex] ?: emptyList()).isNotEmpty()
        
        _state.update { it.copy(canUndo = canUndo, canRedo = canRedo) }
    }

    // --- টেক্সট কন্ট্রোল (আপডেটেড) ---
    private fun updateSelectedTextOverlay(update: (TextOverlay) -> TextOverlay) {
        val selection = _state.value.selectedTextIndex ?: return
        val pageIndex = selection.first
        val selectedIndex = selection.second
        _state.update { currentState ->
            val pageContent = currentState.pageContents[pageIndex] ?: PageContent()
            val updatedOverlays = pageContent.textOverlays.toMutableList().apply {
                if (selectedIndex in indices) {
                    this[selectedIndex] = update(this[selectedIndex])
                }
            }
            val newContents = currentState.pageContents + (pageIndex to pageContent.copy(textOverlays = updatedOverlays))
            currentState.copy(pageContents = newContents)
        }
    }

    fun onTextChanged(newText: String) {
        updateSelectedTextOverlay { it.copy(text = newText) }
    }
    fun onTextSizeChanged(width: Float, height: Float) {
        updateSelectedTextOverlay { it.copy(uiWidth = width, uiHeight = height) }
    }
    fun onColorChanged(newColorArgb: Int) {
        updateSelectedTextOverlay { it.copy(colorArgb = newColorArgb) }
    }

    fun onTextTransformed(panX: Float, panY: Float, zoomChange: Float, rotationChange: Float) {
        updateSelectedTextOverlay { currentOverlay ->
            currentOverlay.copy(
                offsetX = currentOverlay.offsetX + panX,
                offsetY = currentOverlay.offsetY + panY,
                scale = (currentOverlay.scale * zoomChange).coerceIn(0.5f, 5f),
                rotation = currentOverlay.rotation + rotationChange
            )
        }
    }
    fun onFontChanged(newFont: PdfFont) {
        updateSelectedTextOverlay { it.copy(font = newFont) }
    }
    fun onAlignmentChanged(newAlignment: String) {
        updateSelectedTextOverlay { it.copy(alignment = newAlignment) }
    }
    fun onStyleBoldToggled() {
        updateSelectedTextOverlay { it.copy(isBold = !it.isBold) }
    }
    fun onStyleItalicToggled() {
        updateSelectedTextOverlay { it.copy(isItalic = !it.isItalic) }
    }
    fun onStyleUnderlineToggled() {
        updateSelectedTextOverlay { it.copy(isUnderline = !it.isUnderline) }
    }
    fun selectTextOverlay(pageIndex: Int, index: Int?) {
        _state.update { 
            it.copy(
                selectedTextIndex = if (index != null) Pair(pageIndex, index) else null
            )
        }
    }
    // --- ড্রয়িং কন্ট্রোল (আপডেটেড) ---
    fun addStroke(stroke: DrawingStroke) {
        val pageIndex = _state.value.currentPageIndex
        // Clear redo stack for this page since a new stroke was added
        undoneStrokesMap[pageIndex] = emptyList()

        updateCurrentPageContent { content ->
            content.copy(drawnStrokes = content.drawnStrokes + stroke)
        }
    }

    fun setBrushColor(color: Color) {
        // ব্রাশ কালার গ্লোবাল থাকবে, তাই সরাসরি _state আপডেট
        _state.update { it.copy(brushColor = color) }
    }

    fun undoLastStroke() {
        val pageIndex = _state.value.currentPageIndex
        val currentState = _state.value
        val pageContent = currentState.pageContents[pageIndex] ?: PageContent()

        if (pageContent.drawnStrokes.isNotEmpty()) {
            val lastStroke = pageContent.drawnStrokes.last()
            val remainingStrokes = pageContent.drawnStrokes.dropLast(1)

            // Add to redo stack
            val currentUndone = undoneStrokesMap[pageIndex] ?: emptyList()
            undoneStrokesMap[pageIndex] = currentUndone + lastStroke

            updateCurrentPageContent { content ->
                content.copy(drawnStrokes = remainingStrokes)
            }
        }
    }

    fun redoLastStroke() {
        val pageIndex = _state.value.currentPageIndex
        val currentUndone = undoneStrokesMap[pageIndex] ?: emptyList()

        if (currentUndone.isNotEmpty()) {
            val strokeToRedo = currentUndone.last()
            undoneStrokesMap[pageIndex] = currentUndone.dropLast(1)

            updateCurrentPageContent { content ->
                content.copy(drawnStrokes = content.drawnStrokes + strokeToRedo)
            }
        }
    }
    fun setBrushSize(size: Float) {
        _state.update { it.copy(brushSize = size) }
    }

    // --- PDF Zoom & Pan Control ---
    fun onPdfTransformed(panX: Float, panY: Float, zoomChange: Float) {
        _state.update {
            val newScale = (it.pdfScale * zoomChange).coerceIn(1f, 5f)
            if (newScale == 1f) {
                it.copy(
                    pdfScale = newScale,
                    pdfOffsetX = 0f,
                    pdfOffsetY = 0f
                )
            } else {
                // Mathematically clamp translation offsets based on screen size (canvasWidth/canvasHeight) and pdfScale,
                // so the user cannot pan the zoomed PDF completely off the screen
                val maxOffsetX = (it.canvasWidth * (newScale - 1f)) / 2f
                val maxOffsetY = (it.canvasHeight * (newScale - 1f)) / 2f
                
                val boundedOffsetX = (it.pdfOffsetX + panX).coerceIn(-maxOffsetX, maxOffsetX)
                val boundedOffsetY = (it.pdfOffsetY + panY).coerceIn(-maxOffsetY, maxOffsetY)

                it.copy(
                    pdfScale = newScale,
                    pdfOffsetX = boundedOffsetX,
                    pdfOffsetY = boundedOffsetY
                )
            }
        }
    }
    fun onCanvasSizeChanged(width: Float, height: Float) {
        if (_state.value.canvasWidth != width || _state.value.canvasHeight != height) {
            _state.update { it.copy(canvasWidth = width, canvasHeight = height) }
        }
    }
    fun selectTool(tool: EditorTool) {
        _state.update {
            it.copy(
                selectedTool = tool,
                isBottomSheetVisible = true
            )
        }
        if (tool == EditorTool.TEXT) {
            val currentState = _state.value
            val currentIndex = currentState.currentPageIndex
            val pageContent = currentState.pageContents[currentIndex] ?: PageContent()
            val selectedIndex = currentState.selectedTextIndex
            
            if (selectedIndex == null || selectedIndex.first != currentIndex || selectedIndex.second !in pageContent.textOverlays.indices) {
                val newOverlay = TextOverlay(text = "enter your text here")
                val newIndex = pageContent.textOverlays.size
                _state.update { it.copy(selectedTextIndex = Pair(currentIndex, newIndex)) }
                updateCurrentPageContent { content ->
                    content.copy(textOverlays = content.textOverlays + newOverlay)
                }
            }
        } else {
            _state.update { it.copy(selectedTextIndex = null) }
        }
    }

    fun addNewTextOverlay() {
        _state.update {
            it.copy(
                selectedTool = EditorTool.TEXT,
                isBottomSheetVisible = true
            )
        }
        val currentState = _state.value
        val currentIndex = currentState.currentPageIndex
        val pageContent = currentState.pageContents[currentIndex] ?: PageContent()
        
        val newOverlay = TextOverlay(text = "enter your text here")
        val newIndex = pageContent.textOverlays.size
        _state.update { it.copy(selectedTextIndex = Pair(currentIndex, newIndex)) }
        updateCurrentPageContent { content ->
            content.copy(textOverlays = content.textOverlays + newOverlay)
        }
    }

    fun dismissBottomSheet() {
        _state.update {
            it.copy(isBottomSheetVisible = false)
        }
    }

    fun clearSavedFileUri() {
        _state.update {
            it.copy(savedFileUri = null)
        }
    }

    // --- মেগা ফিচার: Native Multiple Page Saving ---

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveEntirePdf(originalPdfUri: Uri) { // অরিজিনাল পিডিএফের Uri প্যারামিটার হিসেবে লাগবে
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isSaving = true, savedFileUri = null) } // সেভিং শুরু, আগের URI ক্লিয়ার

            try {
                val context = getApplication<Application>().applicationContext
                val currentState = _state.value

                var fileName = ""
                var finalUri: Uri? = null

                // ১. অরিজিনাল পিডিএফ ফাইলটা লোড করা হচ্ছে (PdfBox দিয়ে)
                context.contentResolver.openInputStream(originalPdfUri).use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    try {

                val totalPages = document.numberOfPages

                // ২. শুধু যেসব পেজে এডিট আছে, সেগুলো নিয়ে কাজ করব
                for (i in 0 until totalPages) {
                    val pageContent = currentState.pageContents[i]
                    val hasEdits = pageContent != null && (pageContent.drawnStrokes.isNotEmpty() || pageContent.textOverlays.any { it.text.isNotEmpty() })

                    if (hasEdits) {
                        val page = document.getPage(i)

                        // পিডিএফের আসল পেজ সাইজ (Points এ হিসাব হয়)
                        val pdfWidth = page.mediaBox.width
                        val pdfHeight = page.mediaBox.height

                        // কোয়ালিটি ভালো রাখার জন্য আমরা ২ গুণ বড় স্বচ্ছ ক্যানভাস নেব
                        val bmpWidth = (pdfWidth * 2).toInt()
                        val bmpHeight = (pdfHeight * 2).toInt()

                        // --- সম্পূর্ণ স্বচ্ছ একটি কাঁচ (Transparent Bitmap) তৈরি ---
                        val overlayBitmap = createBitmap(bmpWidth, bmpHeight)
                        val nativeCanvas = android.graphics.Canvas(overlayBitmap)

                        val scaleX = bmpWidth.toFloat() / currentState.canvasWidth
                        val scaleY = bmpHeight.toFloat() / currentState.canvasHeight

                        // ১. ড্রয়িং রেন্ডার করা (using StrokePoints)
                        if (pageContent.drawnStrokes.isNotEmpty()) {
                            val paint = Paint().apply {
                                style = Paint.Style.STROKE
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            val matrix = Matrix().apply { setScale(scaleX, scaleY) }

                            pageContent.drawnStrokes.forEach { stroke ->
                                paint.strokeWidth = stroke.strokeWidth * scaleX

                                if (stroke.isEraser) {
                                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                    paint.color = android.graphics.Color.TRANSPARENT
                                } else {
                                    paint.xfermode = null
                                    paint.color = stroke.colorArgb
                                }

                                // Convert StrokePoints to android.graphics.Path
                                val androidPath = android.graphics.Path()
                                if (stroke.points.isNotEmpty()) {
                                    androidPath.moveTo(stroke.points.first().x, stroke.points.first().y)
                                    stroke.points.drop(1).forEach { point ->
                                        androidPath.lineTo(point.x, point.y)
                                    }
                                }
                                androidPath.transform(matrix)
                                nativeCanvas.drawPath(androidPath, paint)
                            }
                        }

                        // ২. টেক্সট রেন্ডার করা (Absolute Layout Strategy - Pro Level)
                        pageContent.textOverlays.forEach { textOverlay ->
                            if (textOverlay.text.isNotEmpty() && textOverlay.uiWidth > 0f) {

                                // StaticLayout এর জন্য TextPaint ব্যবহার করা হলো
                                val textPaint = TextPaint().apply {
                                    color = textOverlay.colorArgb

                                    // --- দ্য আল্টিমেট ম্যাজিক ফিক্স: SP to PX Conversion ---
                                    val baseTextSizePx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        24f * context.resources.configuration.fontScale * context.resources.displayMetrics.density
                                    } else {
                                        @Suppress("DEPRECATION")
                                        24f * context.resources.displayMetrics.scaledDensity
                                    }

                                    textSize = baseTextSizePx * scaleX
                                    typeface = textOverlay.font.toNativeTypeface(context, textOverlay.isBold, textOverlay.isItalic)
                                    isAntiAlias = true
                                    
                                    // Custom text styles
                                    isFakeBoldText = textOverlay.isBold
                                    if (textOverlay.isItalic) {
                                        textSkewX = -0.25f
                                    }
                                    isUnderlineText = textOverlay.isUnderline
                                }
                                // --- WYSIWYG Alignment Fix ---
                                val nativeAlignment = when (textOverlay.alignment) {
                                    "LEFT" -> Layout.Alignment.ALIGN_NORMAL
                                    "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                                    else -> Layout.Alignment.ALIGN_CENTER
                                }

                                // By using `isFakeBoldText = textOverlay.isBold`, Native HarfBuzz perfectly matches Compose Skia's synthetic bold width.
                                // We still add a tiny 4px buffer to absorb any fractional pixel rounding differences.
                                val textWidth = (textOverlay.uiWidth * scaleX).toInt() + 4

                                // 1st Pass: Build default StaticLayout to measure Native line count and height
                                var staticLayout =
                                    StaticLayout.Builder.obtain(textOverlay.text, 0, textOverlay.text.length, textPaint, textWidth)
                                        .setAlignment(nativeAlignment)
                                        .setLineSpacing(0f, 1.0f)
                                        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                                        .setIncludePad(false)
                                        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                                        .build()

                                // --- WYSIWYG Fix 5: Force Line Spacing (Height) Matching ---
                                // Native Bengali fonts (like Kalpana) often have massive default line gaps that Compose ignores.
                                // We mathematically force Native line spacing to match the Compose UI height.
                                val nativeUiHeight = textOverlay.uiHeight * scaleY
                                
                                if (staticLayout.lineCount > 1) {
                                    // How much total height needs to be added/removed?
                                    val heightDiff = nativeUiHeight - staticLayout.height
                                    // Spread the difference across the line gaps
                                    val lineSpacingAdjustment = heightDiff / (staticLayout.lineCount - 1)

                                    // 2nd Pass: Rebuild with the EXACT calculated line spacing
                                    staticLayout = StaticLayout.Builder.obtain(textOverlay.text, 0, textOverlay.text.length, textPaint, textWidth)
                                        .setAlignment(nativeAlignment)
                                        .setLineSpacing(lineSpacingAdjustment, 1.0f)
                                        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
                                        .setIncludePad(false)
                                        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                                        .build()
                                }

                                val centerX = bmpWidth / 2f
                                val centerY = bmpHeight / 2f
                                val textX = centerX + (textOverlay.offsetX * scaleX)
                                val textY = centerY + (textOverlay.offsetY * scaleY)

                                nativeCanvas.withSave {
                                    val translateX = textX - (textWidth / 2f)
                                    val translateY = textY - (nativeUiHeight / 2f)
                                    translate(translateX, translateY)

                                    val pivotX = textWidth / 2f
                                    val pivotY = nativeUiHeight / 2f

                                    rotate(textOverlay.rotation, pivotX, pivotY)
                                    scale(textOverlay.scale, textOverlay.scale, pivotX, pivotY)

                                    staticLayout.draw(this)
                                }
                            }
                        }

                        // --- PdfBox: স্বচ্ছ কাঁচটাকে আসল পিডিএফের ওপর বসানো হচ্ছে ---
                        val pdImage = LosslessFactory.createFromImage(document, overlayBitmap)

                        val contentStream = PDPageContentStream(
                            document,
                            page,
                            PDPageContentStream.AppendMode.APPEND,
                            true,
                            true
                        )
                        contentStream.drawImage(pdImage, 0f, 0f, pdfWidth, pdfHeight)
                        contentStream.close()

                        overlayBitmap.recycle()
                    }

                    // প্রগ্রেস আপডেট
                    val progress = ((i + 1).toFloat() / totalPages.toFloat())
                    _state.update { it.copy(savingProgress = progress) }
                }

                // ৩. এডিট এবং সেভ করার পর ফাইলের নামের আগে "Edited_" যোগ করা হবে
                val originalName = originalPdfUri.getFileName(context)
                val targetName = if (originalName.startsWith("Edited_")) {
                    originalName
                } else {
                    "Edited_${originalName}"
                }
                
                val internalFile = java.io.File(context.filesDir, targetName)
                fileName = internalFile.name

                java.io.FileOutputStream(internalFile).use { outputStream ->
                    document.save(outputStream) // PdfBox দিয়ে সেভ
                }
                finalUri = Uri.fromFile(internalFile)

                    } finally {
                        document.close() // ডকুমেন্ট ক্লোজ করা
                    }
                }

                withContext(Dispatchers.Main) {
                    finalUri?.let { savedUri ->
                        viewModelScope.launch {
                            repository.insertPdf(
                                RecentPdf(
                                    fileUri = savedUri.toString(),
                                    fileName = fileName,
                                    lastOpened = System.currentTimeMillis()
                                )
                            )
                        }
                        _state.update { it.copy(savedFileUri = savedUri) }
                    }
                    Toast.makeText(context, "Saved to Internal Storage!", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Issue 2 Fix: Safely close PdfRenderer resources with try-catch
        try {
            currentPage?.close()
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}