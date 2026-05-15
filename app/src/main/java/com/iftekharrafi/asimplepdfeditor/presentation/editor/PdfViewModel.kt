package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withRotation
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    fun loadPdf(uri: Uri) {
        // লোডিং শুরু করার আগে আগের পিডিএফ ক্লিয়ার করে দাও
        _state.update { PdfEditorState() }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>().applicationContext

                fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: throw Exception("ফাইলটি খুঁজে পাওয়া যাচ্ছে না বা পারমিশন নেই")

                pdfRenderer = PdfRenderer(fileDescriptor!!)
                _state.update {
                    it.copy(
                        pageCount = pdfRenderer!!.pageCount,
                        currentPageIndex = 0
                    )
                }
                renderPage(0)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
            renderPage(nextIndex)
        }
    }

    fun previousPage() {
        val currentState = _state.value
        if (currentState.currentPageIndex > 0) {
            val prevIndex = currentState.currentPageIndex - 1
            _state.update { it.copy(currentPageIndex = prevIndex) }
            renderPage(prevIndex)
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
    }

    // --- টেক্সট কন্ট্রোল (আপডেটেড) ---
    fun onTextChanged(newText: String) {
        updateCurrentPageContent { content ->
            content.copy(textOverlay = content.textOverlay.copy(text = newText))
        }
    }

    fun onColorChanged(newColor: Color) {
        updateCurrentPageContent { content ->
            content.copy(textOverlay = content.textOverlay.copy(color = newColor))
        }
    }

    fun onTextTransformed(panX: Float, panY: Float, zoomChange: Float, rotationChange: Float) {
        updateCurrentPageContent { content ->
            val currentOverlay = content.textOverlay
            content.copy(
                textOverlay = currentOverlay.copy(
                    offsetX = currentOverlay.offsetX + panX,
                    offsetY = currentOverlay.offsetY + panY,
                    scale = (currentOverlay.scale * zoomChange).coerceIn(0.5f, 5f),
                    rotation = currentOverlay.rotation + rotationChange
                )
            )
        }
    }

    // --- ড্রয়িং কন্ট্রোল (আপডেটেড) ---
    fun addStroke(stroke: DrawingStroke) {
        updateCurrentPageContent { content ->
            content.copy(drawnStrokes = content.drawnStrokes + stroke)
        }
    }

    fun setBrushColor(color: Color) {
        // ব্রাশ কালার গ্লোবাল থাকবে, তাই সরাসরি _state আপডেট
        _state.update { it.copy(brushColor = color) }
    }

    fun undoLastStroke() {
        updateCurrentPageContent { content ->
            if (content.drawnStrokes.isNotEmpty()) {
                content.copy(drawnStrokes = content.drawnStrokes.dropLast(1))
            } else content
        }
    }
    fun setBrushSize(size: Float) {
        _state.update { it.copy(brushSize = size) }
    }

    // --- PDF Zoom & Pan Control ---
    fun onPdfTransformed(panX: Float, panY: Float, zoomChange: Float) {
        _state.update {
            val newScale = (it.pdfScale * zoomChange).coerceIn(1f, 5f)
            it.copy(
                pdfScale = newScale,
                pdfOffsetX = it.pdfOffsetX + panX,
                pdfOffsetY = it.pdfOffsetY + panY
            )
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
    }

    fun dismissBottomSheet() {
        _state.update {
            it.copy(isBottomSheetVisible = false)
        }
    }

    // --- মেগা ফিচার: Native Multiple Page Saving ---
    fun saveEntirePdf() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isSaving = true, savedFileUri = null) } // সেভিং শুরু, আগের URI ক্লিয়ার

            try {
                val context = getApplication<Application>().applicationContext
                val newPdfDocument = PdfDocument()
                val currentState = _state.value

                if (pdfRenderer == null) throw Exception("PDF রেন্ডারার চালু নেই")

                val totalPages = pdfRenderer!!.pageCount

                for (i in 0 until totalPages) {
                    val progress = (i.toFloat() / totalPages.toFloat())
                    _state.update { it.copy(savingProgress = progress) }

                    val originalPage = pdfRenderer!!.openPage(i)

                    val width = originalPage.width * 2
                    val height = originalPage.height * 2

                    val pageBitmap = createBitmap(width, height)
                    pageBitmap.eraseColor(android.graphics.Color.WHITE)
                    originalPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                    val pageContent = currentState.pageContents[i]
                    if (pageContent != null && (pageContent.drawnStrokes.isNotEmpty() || pageContent.textOverlay.text.isNotEmpty())) {

                        val nativeCanvas = android.graphics.Canvas(pageBitmap)
                        val scaleX = width.toFloat() / currentState.canvasWidth
                        val scaleY = height.toFloat() / currentState.canvasHeight

                        if (pageContent.drawnStrokes.isNotEmpty()) {
                            val drawingBitmap = createBitmap(width, height)
                            val drawingCanvas = android.graphics.Canvas(drawingBitmap)

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
                                    paint.color = stroke.color.toArgb()
                                }
                                val androidPath = android.graphics.Path(stroke.path.asAndroidPath())
                                androidPath.transform(matrix)
                                drawingCanvas.drawPath(androidPath, paint)
                            }
                            nativeCanvas.drawBitmap(drawingBitmap, 0f, 0f, null)
                            drawingBitmap.recycle()
                        }

                        val textOverlay = pageContent.textOverlay
                        if (textOverlay.text.isNotEmpty()) {
                            val textPaint = Paint().apply {
                                color = textOverlay.color.toArgb()
                                textSize = 60f * scaleX * textOverlay.scale
                                typeface = Typeface.DEFAULT_BOLD
                                isAntiAlias = true
                                textAlign = Paint.Align.CENTER
                            }
                            val centerX = width / 2f
                            val centerY = height / 2f
                            val textX = centerX + (textOverlay.offsetX * scaleX)
                            val textY = centerY + (textOverlay.offsetY * scaleY)

                            nativeCanvas.withRotation(textOverlay.rotation, textX, textY) {
                                drawText(textOverlay.text, textX, textY + (textPaint.textSize / 3), textPaint)
                            }
                        }
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, i + 1).create()
                    val page = newPdfDocument.startPage(pageInfo)
                    page.canvas.drawBitmap(pageBitmap, 0f, 0f, null)
                    newPdfDocument.finishPage(page)

                    originalPage.close()
                    pageBitmap.recycle()
                }

                val fileName = "AmarPDF_Pro_${System.currentTimeMillis()}.pdf"
                var finalUri: Uri? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AmarPDF")
                    }

                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            newPdfDocument.writeTo(outputStream)
                        }
                        finalUri = uri
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val folder = File(downloadsDir, "AmarPDF")
                    if (!folder.exists()) folder.mkdirs()

                    val file = File(folder, fileName)
                    FileOutputStream(file).use { outputStream ->
                        newPdfDocument.writeTo(outputStream)
                    }
                    finalUri = Uri.fromFile(file)
                }

                newPdfDocument.close()

                withContext(Dispatchers.Main) {
                    finalUri?.let { savedUri ->
                        // --- নতুন: ডাটাবেসে ফাইল আপডেট এবং শেয়ার URI স্টেট আপডেট ---
                        viewModelScope.launch {
                            repository.insertPdf(
                                PdfEntity(
                                    fileUri = savedUri.toString(),
                                    fileName = fileName,
                                    lastOpened = System.currentTimeMillis()
                                )
                            )
                        }
                        _state.update { it.copy(savedFileUri = savedUri) }
                    }
                    Toast.makeText(context, "Saved & Added to Recent!", Toast.LENGTH_LONG).show()
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
        currentPage?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}