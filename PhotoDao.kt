package com.autophoto.ai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    /** Flow: UI otomatis ter-update saat data berubah. Terbaru di atas. */
    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun getById(id: Long): PhotoEntity?

    @Delete
    suspend fun delete(photo: PhotoEntity)
}
