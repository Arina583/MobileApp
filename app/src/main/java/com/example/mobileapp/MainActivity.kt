package com.example.mobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.api.MapApi
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var mapApi = MapApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView2)
        mapApi.setupMap(mapView)
        mapApi.addTrailPath(mapView)          // <-- добавить тропы
//        mapApi.addDangerMarker(mapView, ...) // <-- и опасные участки
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach() // Освобождаем ресурсы
    }
}