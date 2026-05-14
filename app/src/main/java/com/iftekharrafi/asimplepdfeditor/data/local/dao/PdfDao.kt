package com.iftekharrafi.asimplepdfeditor.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    // সব রিসেন্ট ফাইলগুলো নিয়ে আসবে (সবচেয়ে নতুনটা আগে থাকবে)
    // Flow ব্যবহার করছি যেন ডেটাবেস আপডেট হলে সাথে সাথে UI আপডেট হয়!
    @Query("SELECT * FROM recent_pdfs ORDER BY lastOpened DESC")
    fun getAllRecentPdfs(): Flow<List<PdfEntity>>

    // নতুন ফাইল ইনসার্ট করা বা আপডেট করা
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfEntity)

    // চাইলে কোনো ফাইল হিস্ট্রি থেকে মুছে ফেলা
    @Delete
    suspend fun deletePdf(pdf: PdfEntity)
}