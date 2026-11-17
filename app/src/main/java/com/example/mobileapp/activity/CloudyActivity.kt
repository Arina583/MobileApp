package com.example.mobileapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.logic.Cloudy

class CloudyActivity: AppCompatActivity() {

    private lateinit var cloudy: Cloudy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloudy)

        cloudy = Cloudy(this)
    }
}