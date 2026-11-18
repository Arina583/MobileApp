package com.example.mobileapp.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.carto.core.MapPos
import com.carto.datasources.HTTPTileDataSource
import com.carto.layers.RasterTileLayer
import com.carto.projections.Projection
import com.carto.ui.MapClickInfo
import com.carto.ui.MapEventListener
import com.carto.ui.MapView
import com.example.mobileapp.R
import com.example.mobileapp.data.GraphRepository
import com.example.mobileapp.logic.TrailGraph
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var projection: Projection

    private var graph: TrailGraph? = null
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            centerOnMyLocation()
        }
        // если пользователь отказался — карта остаётся на стандартном центре
    }

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

        // 4. Центрирование на текущем местоположении
        if (hasLocationPermission()) {
            centerOnMyLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val g = GraphRepository.openGraph(applicationContext)
            withContext(Dispatchers.Main) {
                graph = g
                drawRoute()   // метод, который строит и рисует маршрут
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // вызывается только после проверки разрешений
    private fun centerOnMyLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        var last: Location? = null
        for (provider in providers) {
            try {
                val loc = lm.getLastKnownLocation(provider)
                if (loc != null && (last == null || loc.time > last.time)) last = loc
            } catch (e: IllegalArgumentException) {
                // провайдер недоступен — пробуем следующий
            }
        }

        last?.let { loc ->
            val point = projection.fromWgs84(MapPos(loc.longitude, loc.latitude))
            mapView.setFocusPos(point, 0f)
            mapView.setZoom(15f, 0f)
        }
    }

    private fun drawRoute() {
        val g = graph ?: return   // граф ещё не готов — метод вызовется снова из корутины
        // здесь ваш существующий код: buildRoute, полилиния, дистанция
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