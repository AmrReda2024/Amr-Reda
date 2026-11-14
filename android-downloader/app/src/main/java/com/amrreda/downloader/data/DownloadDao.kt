package com.amrreda.downloader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'RUNNING' OR status = 'QUEUED'")
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun get(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String): List<DownloadEntity>

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, speedBps = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, bytes: Long, speed: Long)

    @Query("UPDATE downloads SET status = 'QUEUED', speedBps = 0 WHERE status = 'RUNNING'")
    suspend fun requeueInterrupted()

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM downloads WHERE status = 'COMPLETED' OR status = 'CANCELLED'")
    suspend fun clearFinished()
}
