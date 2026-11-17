package com.example.mobileapp.logic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R

class CloudyActivity: AppCompatActivity() {

    private lateinit var cloudy: Cloudy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloudy)

        cloudy = Cloudy(this)
    }
}