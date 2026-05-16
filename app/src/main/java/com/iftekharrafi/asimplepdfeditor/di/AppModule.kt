package com.iftekharrafi.asimplepdfeditor.di

import android.app.Application
import androidx.room.Room
import com.iftekharrafi.asimplepdfeditor.data.local.AppDatabase
import com.iftekharrafi.asimplepdfeditor.data.local.dao.PdfDao
import com.iftekharrafi.asimplepdfeditor.data.repository.PdfRepositoryImpl
import com.iftekharrafi.asimplepdfeditor.domain.repository.PdfRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "pdf_editor_db"
        ).build()
    }

    // ডেটাবেস থেকে Dao প্রোভাইড করা হচ্ছে
    @Provides
    @Singleton
    fun providePdfDao(db: AppDatabase): PdfDao {
        return db.pdfDao
    }
    @Provides
    @Singleton
    fun providePdfRepository(dao: PdfDao): PdfRepository {
        return PdfRepositoryImpl(dao)
    }
}