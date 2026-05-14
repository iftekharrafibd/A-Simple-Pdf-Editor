package com.iftekharrafi.asimplepdfeditor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iftekharrafi.asimplepdfeditor.data.local.dao.PdfDao
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity

@Database(
    entities = [PdfEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract val pdfDao: PdfDao

    // উল্লেখ্য: আমরা এখানে কোনো companion object (Singleton) রাখছি না।
    // কারণ প্রোডাকশন লেভেলে এই কাজটা আমরা Dagger-Hilt (DI) দিয়ে হ্যান্ডেল করব!
}