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
fun PdfFont.toNativeTypeface(context: Context, isBold: Boolean, isItalic: Boolean = false): Typeface {
    val style = when {
        isBold && isItalic -> Typeface.BOLD_ITALIC
        isBold -> Typeface.BOLD
        isItalic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }
    return when (this) {
        PdfFont.DEFAULT -> Typeface.create(Typeface.DEFAULT, style)
        PdfFont.SERIF -> Typeface.create(Typeface.SERIF, style)
        PdfFont.CURSIVE -> Typeface.create("cursive", style)
        PdfFont.KALPANA -> {
            val customTypeface = ResourcesCompat.getFont(context, R.font.kalpana)
            // ফন্টটা লোড হলে সেটাকে সঠিক স্টাইল করে দিচ্ছি, না পেলে ডিফল্টটা দেখাবে
            Typeface.create(customTypeface, style) ?: Typeface.create(Typeface.DEFAULT, style)
        }
    }
}