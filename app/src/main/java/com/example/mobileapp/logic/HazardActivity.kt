package com.example.mobileapp.logic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.ui.Menu

class HazardActivity: AppCompatActivity() {

    private lateinit var hazard: Hazard
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard)

        hazard = Hazard(this)

    }
}