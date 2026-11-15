package com.amrreda.downloader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DownloadEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloads(): DownloadDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "downloads.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()

        /** v1 -> v2: adds video-engine columns and keeps the user's existing history. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN kind TEXT NOT NULL DEFAULT 'DIRECT'")
                listOf("title", "thumbnailUrl", "formatSelector", "formatSort", "audioFormat", "qualityLabel", "platform")
                    .forEach { db.execSQL("ALTER TABLE downloads ADD COLUMN $it TEXT") }
            }
        }
    }
}
