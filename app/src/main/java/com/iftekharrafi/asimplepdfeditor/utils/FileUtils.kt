package com.iftekharrafi.asimplepdfeditor.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

// URI থেকে ফাইলের আসল নাম বের করার এক্সটেনশন ফাংশন
fun Uri.getFileName(context: Context): String {
    var result: String? = null
    if (this.scheme == "content") {
        val cursor = context.contentResolver.query(this, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = this.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Unknown_Document.pdf"
}

// URI থেকে অ্যাপের অভ্যন্তরীণ স্টোরেজে কপি করার ফাংশন
fun Uri.copyToInternalStorage(context: Context): Uri? {
    return try {
        val fileName = this.getFileName(context)
        val cleanName = fileName.replace(" ", "_")
        val destFile = java.io.File(context.filesDir, cleanName)
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            destFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        Uri.fromFile(destFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}