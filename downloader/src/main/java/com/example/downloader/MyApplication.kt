package com.example.downloader

import android.app.Application
import androidx.room.Room
import com.example.downloader.business.LibHelper
import com.example.downloader.db.MyDataBase
import com.google.android.material.color.DynamicColors

class MyApplication : Application() {

    companion object {
        lateinit var database: MyDataBase
    }

    override fun onCreate() {
        super.onCreate()
        // 开启动态配色
        DynamicColors.applyToActivitiesIfAvailable(this)
        // 初始化资源
        LibHelper.init(applicationContext)
        // 初始化数据库
        database = Room
            .databaseBuilder(this, MyDataBase::class.java, "DownloaderDB")
            .fallbackToDestructiveMigration()
            .build()
    }
}