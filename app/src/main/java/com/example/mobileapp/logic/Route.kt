package com.example.mobileapp.logic

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R

class Route (private val activity: Activity) {

    init {
        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap();
        }
    }

    private fun backMap() {
        activity.finish()
    }
}