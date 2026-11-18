package com.example.mobileapp.activity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.api.MapApi
import com.example.mobileapp.data.SyncManager
import com.example.mobileapp.ui.Menu
import org.osmdroid.config.Configuration
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class RouteMapActivity: AppCompatActivity() {
    companion object {
        const val EXTRA_ROUTE_LAT = "route_points_lat"
        const val EXTRA_ROUTE_LNG = "route_points_lng"
        const val EXTRA_DISTANCE = "route_distance_meters"
        const val EXTRA_START_LAT = "route_start_lat"
        const val EXTRA_START_LNG = "route_start_lng"
        const val EXTRA_END_LAT = "route_end_lat"
        const val EXTRA_END_LNG = "route_end_lng"
    }

    private lateinit var mapView: MapView

    private lateinit var mapApi: MapApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_route_map)

        mapView = findViewById(R.id.mapViewRoute)
        mapApi = MapApi(this)

        mapApi.setupMap(mapView)
        mapApi.showHazards(mapView)       // <-- и опасные участки

        SyncManager.getInstance(this).syncNow {
            runOnUiThread { mapApi.showHazards(mapView) }
        }

        val latArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LAT) ?: doubleArrayOf()
        val lngArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LNG) ?: doubleArrayOf()
        val distanceMeters = intent.getDoubleExtra(EXTRA_DISTANCE, 0.0)

        val tvDistance = findViewById<TextView>(R.id.tvRouteDistance)
        val tvPoints = findViewById<TextView>(R.id.tvRoutePoints)

        tvDistance.text = "Длина маршрута: " + formatDistance(distanceMeters)
        tvPoints.text = "Точек в маршруте: ${latArray.size}"

        if (latArray.size >= 2 && latArray.size == lngArray.size) {
            drawRoute(latArray, lngArray)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun drawRoute(lats: DoubleArray, lngs: DoubleArray) {
        val points = lats.zip(lngs) { lat, lng -> GeoPoint(lat, lng) }

        val polyline = Polyline(mapView)
        polyline.setPoints(points)
        polyline.outlinePaint.apply {
            color = Color.parseColor("#E53935")
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        mapView.overlays.add(polyline)

        // маркеры начала и конца — те же цвета, что и на экране настроек
        addPointMarker(mapView,points.first(), Color.parseColor("#2E7D32"))
        addPointMarker(mapView,points.last(), Color.parseColor("#C62828"))

        // масштаб, чтобы весь маршрут был виден
        val bounds = BoundingBox.fromGeoPoints(points).increaseByScale(1.25f)
        mapView.post {
            mapView.zoomToBoundingBox(bounds, false, 40)
            mapView.invalidate()
        }
    }

    private fun addPointMarker(mapView: MapView, point: GeoPoint, color: Int): Marker {
        val marker = Marker(mapView)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon = makeCircleIcon(mapView.context, color)
        mapView.overlays.add(marker)
        return marker
    }

    private fun makeCircleIcon(context: Context, color: Int): Drawable {
        val size = 24
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint().apply { this.color = color; isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format(java.util.Locale.US, "%.1f км", meters / 1000)
        } else {
            String.format(java.util.Locale.US, "%.0f м", meters)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}