package com.example.mobileapp.logic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R

class RouteActivity: AppCompatActivity() {

    private lateinit var route: Route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        route = Route(this)

    }
}