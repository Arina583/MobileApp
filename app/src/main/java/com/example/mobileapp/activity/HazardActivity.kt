package com.example.mobileapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.logic.Hazard

class HazardActivity: AppCompatActivity() {

    private lateinit var hazard: Hazard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard)

        hazard = Hazard(this)

    }
}