package com.example.swipeclean.business

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.swipeclean.model.Image

@Database(entities = [Image::class], version = 1, exportSchema = false)
abstract class ImageDataBase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
}