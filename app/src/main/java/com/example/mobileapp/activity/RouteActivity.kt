package com.example.mobileapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.logic.Route

class RouteActivity: AppCompatActivity() {

    private lateinit var route: Route

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route)

        route = Route(this)

    }
}