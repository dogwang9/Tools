package com.example.downloader.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.downloader.model.TaskHistory

@Database(entities = [TaskHistory::class], version = 2, exportSchema = false)
abstract class MyDataBase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}