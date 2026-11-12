package com.example.mobileapp.logic

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.carto.layers.CartoBaseMapStyle
import com.carto.layers.CartoOnlineVectorTileLayer
import com.carto.ui.MapClickInfo
import com.carto.ui.MapEventListener
import com.carto.ui.MapView
import com.example.mobileapp.R
import com.google.android.material.button.MaterialButton

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        // Лицензию регистрируют до работы с картой
        MapView.registerLicense("cb1_52a2_1_523fd23a1b11d498da8f10a4", applicationContext)

        mapView = findViewById(R.id.map_view)
        mapView.layers.add(CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_POSITRON))

        findViewById<MaterialButton>(R.id.btnCancelPick).setOnClickListener { finish() }

        mapView.setMapEventListener(object : MapEventListener() {
            override fun onMapClicked(clickInfo: MapClickInfo?) {
                val pos = clickInfo?.clickPos ?: return
                // clickPos приходит в базовой проекции — переводим в WGS84.
                // Внимание: в CARTO MapPos хранится как x = долгота, y = широта
                val wgs = mapView.options.baseProjection.toWgs84(pos)
                returnResult(latitude = wgs.y, longitude = wgs.x)
            }
        })
    }


    // Опционально: остановить подкачку тайлов, пока экран выбора точки не виден
    override fun onPause() {
        super.onPause()
        if (::mapView.isInitialized) mapView.cancelAllTasks()
    }

    private fun returnResult(latitude: Double, longitude: Double) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(EXTRA_LAT, latitude)
            putExtra(EXTRA_LNG, longitude)
        })
        finish()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
    }
}