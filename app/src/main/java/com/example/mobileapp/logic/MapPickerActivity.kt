package com.example.mobileapp.logic

import android.content.Intent
import android.os.Bundle
import com.carto.core.MapPos
import androidx.appcompat.app.AppCompatActivity
import com.carto.datasources.HTTPTileDataSource
import com.carto.layers.RasterTileLayer
import com.carto.projections.Projection
import com.carto.ui.MapClickInfo
import com.carto.ui.MapEventListener
import com.carto.ui.MapView
import com.example.mobileapp.R
import com.google.android.material.button.MaterialButton

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var projection: Projection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.map_view)

        // 1. Растровый слой
        val dataSource = HTTPTileDataSource(
            0, 20,
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png"
        )
        mapView.layers.add(RasterTileLayer(dataSource))

        // 2. Стартовая точка и зум (Владимир, z=10)
        projection = mapView.options.baseProjection
        // В MapApi: GeoPoint(56.129057, 40.406635) — это (lat, lon).
        // Здесь x = долгота, y = широта, поэтому порядок обратный.
        val start = projection.fromWgs84(MapPos(40.406635, 56.129057))
        mapView.setFocusPos(start, 0f)
        mapView.setZoom(10f, 0f)

        findViewById<MaterialButton>(R.id.btnCancelPick).setOnClickListener { finish() }

        // 3. Клик по карте — возвращаем координаты в WGS84
        mapView.setMapEventListener(object : MapEventListener() {
            override fun onMapClicked(clickInfo: MapClickInfo?) {
                val pos = clickInfo?.clickPos ?: return
                val wgs = projection.toWgs84(pos)
                returnResult(latitude = wgs.y, longitude = wgs.x)
            }
        })
    }

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
