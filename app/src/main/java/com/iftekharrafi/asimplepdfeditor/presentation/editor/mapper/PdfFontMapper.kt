package com.iftekharrafi.asimplepdfeditor.presentation.editor.mapper

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.core.content.res.ResourcesCompat
import com.iftekharrafi.asimplepdfeditor.R
import com.iftekharrafi.asimplepdfeditor.domain.model.PdfFont

// ১. Compose UI er jonno mapper
fun PdfFont.toComposeFontFamily(): FontFamily {
    return when (this) {
        PdfFont.DEFAULT -> FontFamily.Default
        PdfFont.SERIF -> FontFamily.Serif
        PdfFont.CURSIVE -> FontFamily.Cursive
        PdfFont.KALPANA -> FontFamily(Font(R.font.kalpana))
    }
}

// ২. Native Android Canvas (Paint) er jonno mapper
fun PdfFont.toNativeTypeface(context: Context): Typeface {
    return when (this) {
        PdfFont.DEFAULT -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        PdfFont.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
        PdfFont.CURSIVE -> Typeface.create("cursive", Typeface.BOLD)
        PdfFont.KALPANA -> {
            val customTypeface = ResourcesCompat.getFont(context, R.font.kalpana)
            // ফন্টটা লোড হলে সেটাকে বোল্ড স্টাইল করে দিচ্ছি, না পেলে ডিফল্টটা দেখাবে
            Typeface.create(customTypeface, Typeface.BOLD) ?: Typeface.DEFAULT_BOLD
        }
    }
}