package com.iftekharrafi.asimplepdfeditor.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_pdfs")
data class PdfEntity(
    @PrimaryKey // URI কেই প্রাইমারি কি বানালাম যেন ডুপ্লিকেট না হয়
    val fileUri: String,
    val fileName: String,
    val lastOpened: Long
)