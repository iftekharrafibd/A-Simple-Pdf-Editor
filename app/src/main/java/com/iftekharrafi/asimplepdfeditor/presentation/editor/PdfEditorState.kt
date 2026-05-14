package com.iftekharrafi.asimplepdfeditor.presentation.editor

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.iftekharrafi.asimplepdfeditor.domain.model.DrawingStroke
import com.iftekharrafi.asimplepdfeditor.domain.model.TextOverlay

// ১. নতুন: কোন টুল সিলেক্ট করা আছে সেটা বোঝার জন্য Enum
enum class EditorTool {
    NONE,       // কোনো টুল সিলেক্ট নেই (শুধু পিডিএফ দেখবে/স্ক্রল করবে)
    TEXT,       // টেক্সট লেখার টুল
    DRAW,       // ফ্রি-হ্যান্ড পেন্সিল ড্রইং
    ERASER      // মোছার টুল
}

// ২. PdfEditorState আপডেট করা
data class PdfEditorState(
    val pdfBitmap: Bitmap? = null,
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val textOverlay: TextOverlay = TextOverlay(),
    val isSaving: Boolean = false,

    // --- নতুন স্টেটগুলো ---
    val selectedTool: EditorTool = EditorTool.NONE,
    val isBottomSheetVisible: Boolean = false,
    val drawnStrokes: List<DrawingStroke> = emptyList(), // আঁকা লাইনগুলো
    val brushColor: Color = Color.Red,                   // বর্তমান ব্রাশ কালার
    val brushSize: Float = 8f
)