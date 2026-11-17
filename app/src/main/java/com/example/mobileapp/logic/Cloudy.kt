package com.example.mobileapp.logic

import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R

class Cloudy(private val activity: AppCompatActivity) {
    init {
        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap()
        }
    }
    private fun backMap() {
        activity.finish()
    }
}