package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.PageContent
import com.iftekharrafi.asimplepdfeditor.domain.model.TextOverlay

// ১. নতুন: কোন টুল সিলেক্ট করা আছে সেটা বোঝার জন্য Enum
enum class EditorTool {
    NONE,       // কোনো টুল সিলেক্ট নেই (শুধু পিডিএফ দেখবে/স্ক্রল করবে)
    TEXT,       // টেক্সট লেখার টুল
    DRAW,       // ফ্রি-হ্যান্ড পেন্সিল ড্রইং
    ERASER      // মোছার টুল
}

data class PdfEditorState(
    val pdfBitmap: Bitmap? = null,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,

    // --- নতুন: পেজ নাম্বার অনুযায়ী ডেটা সেভ রাখার Map ---
    val pageContents: Map<Int, PageContent> = emptyMap(),

    // (পুরোনো textOverlay এবং drawnStrokes এখান থেকে মুছে ফেলবে)

    val isSaving: Boolean = false,
    val savingProgress: Float = 0f ,
    val selectedTool: EditorTool = EditorTool.NONE,
    val isBottomSheetVisible: Boolean = false,
    val brushColor: Color = Color.Red,
    val brushSize: Float = 8f,
    val pdfScale: Float = 1f,
    val pdfOffsetX: Float = 0f,
    val pdfOffsetY: Float = 0f,
    // --- নতুন: স্ক্রিনের সাইজ মেপে রাখার জন্য ---
    val canvasWidth: Float = 1f,
    val canvasHeight: Float = 1f
)