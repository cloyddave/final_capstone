package com.group5.safehomenotifier

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface HistoryImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: HistoryImage)

    @Query("SELECT * FROM history_images ORDER BY timestamp DESC")
    suspend fun getAllImages(): List<HistoryImage>

    @Delete
    suspend fun deleteImage(image: HistoryImage)

    @Query("DELETE FROM history_images")
    suspend fun clearHistory()
}