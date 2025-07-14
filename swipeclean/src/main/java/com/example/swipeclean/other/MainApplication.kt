package com.example.swipeclean.other

import android.app.Application
import com.example.swipeclean.business.AlbumController
import com.google.android.material.color.DynamicColors

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        AlbumController.init(applicationContext)
    }
}