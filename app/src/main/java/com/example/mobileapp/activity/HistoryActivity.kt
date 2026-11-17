package com.example.mobileapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.logic.History

class HistoryActivity: AppCompatActivity() {

    private lateinit var history: History

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        history = History(this)
    }
}