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
    @Query("SELECT * FROM recent_pdfs ORDER BY lastOpened DESC")
    fun getAllRecentPdfs(): Flow<List<PdfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfEntity)

    @Delete
    suspend fun deletePdf(pdf: PdfEntity)
}