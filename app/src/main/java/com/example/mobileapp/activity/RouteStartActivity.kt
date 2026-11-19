package com.example.mobileapp.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class RouteStartActivity : AppCompatActivity() {

    private lateinit var mapView: MapView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_start)

        mapView = findViewById(R.id.mapViewPreview)

        val mapApi = com.example.mobileapp.api.MapApi(this)
        mapApi.setupMap(mapView)   // та же настройка, что и на экране навигации

        mapView.setMultiTouchControls(false)   // без зума пальцами

        val latArray = intent.getDoubleArrayExtra(RouteMapActivity.EXTRA_ROUTE_LAT) ?: doubleArrayOf()
        val lngArray = intent.getDoubleArrayExtra(RouteMapActivity.EXTRA_ROUTE_LNG) ?: doubleArrayOf()

        if (latArray.size >= 2 && latArray.size == lngArray.size) {
            val points = latArray.mapIndexed { i, lat -> GeoPoint(lat, lngArray[i]) }

            val line = Polyline(mapView)
            line.setPoints(points)
            line.outlinePaint.apply {
                color = Color.parseColor("#1E6FE0")
                strokeWidth = 12f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            mapView.overlays.add(line)

            val bounds = BoundingBox.fromGeoPoints(points).increaseByScale(1.25f)
            mapView.post {
                mapView.zoomToBoundingBox(bounds, false, 40)
                mapView.invalidate()
            }
        }



        val distanceMeters = intent.getDoubleExtra(RouteMapActivity.EXTRA_DISTANCE, 0.0)
        val mode = intent.getIntExtra(RouteMapActivity.EXTRA_MODE, 0)
        val startLat = intent.getDoubleExtra(RouteMapActivity.EXTRA_START_LAT, 0.0)
        val startLng = intent.getDoubleExtra(RouteMapActivity.EXTRA_START_LNG, 0.0)
        val endLat = intent.getDoubleExtra(RouteMapActivity.EXTRA_END_LAT, 0.0)
        val endLng = intent.getDoubleExtra(RouteMapActivity.EXTRA_END_LNG, 0.0)

        findViewById<TextView>(R.id.tvDistance).text = formatDistance(distanceMeters)
        findViewById<TextView>(R.id.tvTime).text = formatTime(estimateSeconds(distanceMeters, mode))
        findViewById<TextView>(R.id.tvComplexity).text = complexity(distanceMeters)

        findViewById<TextView>(R.id.tvStartCoords).text =
            String.format(Locale.US, "%.6f, %.6f", startLat, startLng)
        findViewById<TextView>(R.id.tvEndCoords).text =
            String.format(Locale.US, "%.6f, %.6f", endLat, endLng)

        findViewById<MaterialCardView>(R.id.cardHazards).setOnClickListener {
            // пока просто сообщение; сюда можно повесить список опасностей вдоль маршрута
            Toast.makeText(this, "Опасности вдоль маршрута", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnHelp).setOnClickListener {
            Toast.makeText(this, "Маршрут построен по безопасным тропам", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnStartRoute).setOnClickListener {
            val intent = Intent(this, RouteMapActivity::class.java)
            intent.putExtras(this.intent.getExtras() ?: Bundle()) // пересылаем все экстра как есть
            startActivity(intent)
            finish()
        }
    }

    /** Средняя скорость по режиму, м/с — как в RouteMapActivity. */
    private fun estimateSeconds(distance: Double, mode: Int): Double {
        val speed = when (mode) {
            1 -> 4.0   // вело ~14 км/ч
            2 -> 15.0  // авто ~54 км/ч
            else -> 1.39 // пешком 5 км/ч
        }
        return distance / speed
    }

    private fun complexity(distance: Double): String = when {
        distance < 3000 -> "Низкая"
        distance < 10000 -> "Средняя"
        else -> "Высокая"
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000)
            String.format(Locale.US, "%.2f км", meters / 1000)
        else
            String.format(Locale.US, "%.0f м", meters)

    private fun formatTime(seconds: Double): String {
        val totalMin = (seconds / 60).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}ч $m мин" else "$m мин"
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
        mapView.onDetach()
        super.onDestroy()
    }

}
