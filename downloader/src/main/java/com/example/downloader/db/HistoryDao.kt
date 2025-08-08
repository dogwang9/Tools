package com.example.downloader.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.downloader.model.TaskHistory

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistory(taskHistory: TaskHistory)

    @Query("SELECT * FROM task ORDER BY downloadTime")
    fun queryAll(): List<TaskHistory>

    @Query("SELECT * FROM task WHERE source LIKE :source ORDER BY downloadTime")
    fun queryBySource(source: String): List<TaskHistory>

    @Update
    fun updateHistory(taskHistory: TaskHistory)

    @Delete
    fun deleteHistory(taskHistory: TaskHistory)

    @Query("DELETE FROM task WHERE id = :id")
    fun deleteHistory(id: Long)

    @Query("DELETE FROM task")
    fun deleteAll()
}