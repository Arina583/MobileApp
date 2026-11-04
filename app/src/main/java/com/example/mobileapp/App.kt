package com.example.mobileapp

import android.app.Application
import android.content.Context
import org.osmdroid.config.Configuration

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().load(
            this,
            getSharedPreferences("main", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue =
            "ForestRouteApp/1.0 (student project; orlovaarina742@gmail.com)"
    }
}