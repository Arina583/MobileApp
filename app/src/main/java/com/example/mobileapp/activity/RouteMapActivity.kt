package com.example.mobileapp.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mobileapp.R
import com.example.mobileapp.api.MapApi
import com.example.mobileapp.data.SyncManager
import com.google.android.material.button.MaterialButton
import org.osmdroid.api.IMapController
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class RouteMapActivity : AppCompatActivity() {

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
    private lateinit var mapController: IMapController

    // Старт маршрута — для начальной позиции стрелки
    private var startLat = 0.0
    private var startLng = 0.0

    // Отслеживание
    private var tracking = false
    private var arrowMarker: Marker? = null
    private var lastBearing = 0f
    private lateinit var locationManager: LocationManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startLocationUpdates()
        else Toast.makeText(this, "Без разрешения на геолокацию маршрут не отслеживается", Toast.LENGTH_LONG).show()
    }

    private val locationListener = LocationListener { loc ->
        runOnUiThread { updateArrow(loc) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_route_map)

        mapView = findViewById(R.id.mapViewRoute)
        mapApi = MapApi(this)

        mapApi.setupMap(mapView)
        mapController = mapView.controller
        mapApi.showHazards(mapView)       // и опасные участки

        findViewById<MaterialButton>(R.id.btnStartRoute).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationUpdates()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        SyncManager.getInstance(this).syncNow {
            runOnUiThread { mapApi.showHazards(mapView) }
        }

        val latArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LAT) ?: doubleArrayOf()
        val lngArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LNG) ?: doubleArrayOf()
        val distanceMeters = intent.getDoubleExtra(EXTRA_DISTANCE, 0.0)

        startLat = intent.getDoubleExtra(
            EXTRA_START_LAT, if (latArray.isNotEmpty()) latArray.first() else 0.0
        )
        startLng = intent.getDoubleExtra(
            EXTRA_START_LNG, if (lngArray.isNotEmpty()) lngArray.first() else 0.0
        )

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
        addPointMarker(mapView, points.first(), Color.parseColor("#2E7D32"))
        addPointMarker(mapView, points.last(), Color.parseColor("#C62828"))

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
        val canvas = Canvas(bitmap)
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

    private fun startLocationUpdates() {
        if (tracking) return
        tracking = true

        if (arrowMarker == null) {
            val startPoint = GeoPoint(startLat, startLng)
            arrowMarker = Marker(mapView).apply {
                position = startPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = makeArrowIcon(0f)
            }
            mapView.overlays.add(arrowMarker)
            mapController.animateTo(startPoint)
            mapController.setZoom(17.0)
        }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000L, 1f, locationListener
            )
        } catch (e: SecurityException) {
            Toast.makeText(this, "Нет доступа к геолокации", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateArrow(loc: Location) {
        val marker = arrowMarker ?: return
        val pos = GeoPoint(loc.latitude, loc.longitude)

        // поворачиваем стрелку только когда пользователь реально движется
        if (loc.speed > 0.5f) {
            lastBearing = loc.bearing
            marker.icon = makeArrowIcon(lastBearing)
        }
        marker.position = pos
        mapController.animateTo(pos)
        mapView.invalidate()
    }

    private fun makeArrowIcon(bearing: Float): BitmapDrawable {
        val size = (24 * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
        }
        val arrow = Path().apply {
            moveTo(size / 2f, size * 0.08f)
            lineTo(size * 0.85f, size * 0.9f)
            lineTo(size / 2f, size * 0.72f)
            lineTo(size * 0.15f, size * 0.9f)
            close()
        }
        canvas.rotate(bearing, size / 2f, size / 2f)
        canvas.drawPath(arrow, paint)
        return BitmapDrawable(resources, bmp)
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
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
        super.onDestroy()
    }
}