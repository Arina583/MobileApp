package com.example.mobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.api.MapApi
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mapApi: MapApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Берём MapView, который уже есть в layout
        mapView = findViewById(R.id.mapView2)

        // Инициализируем интеграционный класс
        mapApi = MapApi(this)

        // Настраиваем существующий mapView (зум, центр, тайлы)
        mapApi.setupMap(mapView)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        mapView.destroy() // Освобождаем ресурсы Osmdroid
//    }
}