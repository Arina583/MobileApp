package com.example.mobileapp.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RouteMapActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ROUTE_LAT = "route_points_lat"
        const val EXTRA_ROUTE_LNG = "route_points_lng"
        const val EXTRA_DISTANCE = "route_distance_meters"
        const val EXTRA_START_LAT = "route_start_lat"
        const val EXTRA_START_LNG = "route_start_lng"
        const val EXTRA_END_LAT = "route_end_lat"
        const val EXTRA_END_LNG = "route_end_lng"
        const val EXTRA_MODE = "route_mode" // 0=пешком, 1=вело, 2=машина

        // Для пунктира (если маршрут выходит за граф)
        const val EXTRA_TAIL_START = "route_tail_start_m"
        const val EXTRA_TAIL_END = "route_tail_end_m"
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

    // Состояния экрана
    private lateinit var cardBanner: View
    private lateinit var cardRouteInfo: View
    private lateinit var cardNavigation: View
    private lateinit var tvManeuverText: TextView
    private lateinit var tvManeuverDistance: TextView
    private lateinit var tvTimeLeft: TextView
    private lateinit var tvDistanceLeft: TextView

    // Геометрия маршрута для расчёта прогресса
    private var routePoints: Array<GeoPoint> = emptyArray()
    private var cumulativeDist = floatArrayOf() // метры от старта до точки i
    private var totalDistance = 0.0
    private var mode = 0 // 0=пешком, 1=вело, 2=машина

    // Средние скорости режимов, м/с (для оценки оставшегося времени)
    private val avgSpeed: Double
        get() = when (mode) {
            1 -> 4.0   // вело ~14 км/ч
            2 -> 15.0  // авто ~54 км/ч
            else -> 1.39 // пешком 5 км/ч
        }

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

        cardBanner = findViewById(R.id.cardBanner)
        cardRouteInfo = findViewById(R.id.cardRouteInfo)
        cardNavigation = findViewById(R.id.cardNavigation)
        tvManeuverText = findViewById(R.id.tvManeuverText)
        tvManeuverDistance = findViewById(R.id.tvManeuverDistance)
        tvTimeLeft = findViewById(R.id.tvTimeLeft)
        tvDistanceLeft = findViewById(R.id.tvDistanceLeft)

        findViewById<ImageButton>(R.id.btnCloseBanner).setOnClickListener { stopTracking() }
        findViewById<MaterialButton>(R.id.btnFinishRoute).setOnClickListener { stopTracking() }

        mapView = findViewById(R.id.mapViewRoute)
        mapApi = MapApi(this)

        mapApi.setupMap(mapView)
        mapController = mapView.controller
        mapApi.showHazards(mapView)

        findViewById<MaterialButton>(R.id.btnStartRoute).setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                startLocationUpdates()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            cardRouteInfo.visibility = View.GONE
            cardNavigation.visibility = View.VISIBLE
            cardBanner.visibility = View.VISIBLE
        }

        SyncManager.getInstance(this).syncNow {
            runOnUiThread { mapApi.showHazards(mapView) }
        }

        val latArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LAT) ?: doubleArrayOf()
        val lngArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_LNG) ?: doubleArrayOf()
        val distanceMeters = intent.getDoubleExtra(EXTRA_DISTANCE, 0.0)
        mode = intent.getIntExtra(EXTRA_MODE, 0)

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

        val startTail = intent.getFloatExtra(EXTRA_TAIL_START, 0f)
        val endTail = intent.getFloatExtra(EXTRA_TAIL_END, 0f)

        if (latArray.size >= 2 && latArray.size == lngArray.size) {
            drawRoute(latArray, lngArray, startTail, endTail)
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun drawRoute(lats: DoubleArray, lngs: DoubleArray, startTail: Float, endTail: Float) {
        val points = lats.zip(lngs) { lat, lng -> GeoPoint(lat, lng) }
        routePoints = points.toTypedArray()

        // накопленные расстояния от старта — для расчёта «осталось»
        cumulativeDist = FloatArray(points.size)
        for (i in 1 until points.size) {
            cumulativeDist[i] = cumulativeDist[i - 1] +
                    haversine(
                        points[i - 1].latitude, points[i - 1].longitude,
                        points[i].latitude, points[i].longitude
                    ).toFloat()
        }
        totalDistance = cumulativeDist.lastOrNull()?.toDouble() ?: 0.0

        // индексы, с которых начинается/заканчивается «внеграфовая» часть
        val firstSolid = if (startTail > 0 && points.size >= 2) 1 else 0
        val lastSolid = if (endTail > 0 && points.size >= 2) points.size - 2 else points.size - 1

        // сплошная линия — только по графу
        if (lastSolid > firstSolid) {
            val solid = Polyline(mapView)
            solid.setPoints(points.subList(firstSolid, lastSolid + 1))
            solid.outlinePaint.apply {
                color = Color.parseColor("#1E6FE0")
                strokeWidth = 12f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            mapView.overlays.add(solid)
        }

        // пунктирные хвосты — путь вне дороги
        val dash = DashPathEffect(floatArrayOf(24f, 18f), 0f)
        if (startTail > 0) addDashed(points.subList(0, firstSolid + 1), dash)
        if (endTail > 0) addDashed(points.subList(lastSolid, points.size), dash)

        // маркеры начала и конца
        addPointMarker(mapView, points.first(), Color.parseColor("#2E7D32"))
        addPointMarker(mapView, points.last(), Color.parseColor("#C62828"))

        // масштаб, чтобы весь маршрут был виден
        val bounds = BoundingBox.fromGeoPoints(points).increaseByScale(1.25f)
        mapView.post {
            mapView.zoomToBoundingBox(bounds, false, 40)
            mapView.invalidate()
        }
    }

    private fun addDashed(points: List<GeoPoint>, dash: DashPathEffect) {
        val line = Polyline(mapView)
        line.setPoints(points)
        line.outlinePaint.apply {
            color = Color.parseColor("#1E6FE0")
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            pathEffect = dash
        }
        mapView.overlays.add(line)
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

    private fun formatTime(seconds: Double): String {
        val totalMin = (seconds / 60).toInt()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}ч $m мин" else "$m мин"
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

        updateProgress(loc)
        mapView.invalidate()
    }

    /**
     * Прогресс по маршруту: ближайшая точка, осталось метров/минут, манёвр впереди.
     */
    private fun updateProgress(loc: Location) {
        if (routePoints.size < 2) return

        // ближайшая точка маршрута (линейный поиск; при 1 Гц это дёшево)
        var best = 0
        var bestD = Double.MAX_VALUE
        for (i in routePoints.indices) {
            val d = haversine(loc.latitude, loc.longitude,
                routePoints[i].latitude, routePoints[i].longitude)
            if (d < bestD) { bestD = d; best = i }
        }

        val passed = cumulativeDist[best].toDouble()
        val remaining = (totalDistance - passed).coerceAtLeast(0.0)
        val remainingSec = remaining / avgSpeed

        tvDistanceLeft.text = "Осталось ${formatDistance(remaining)}"
        tvTimeLeft.text = formatTime(remainingSec)

        // манёвр: смотрим на участок впереди, ищем заметный поворот
        tvManeuverText.text = maneuverAhead(best)
        tvManeuverDistance.text = "Ещё ${formatDistance(remaining)}"
    }

    /**
     * Текст манёвра: идём вперёд по маршруту, пока направление не изменится заметно.
     */
    private fun maneuverAhead(idx: Int): String {
        val n = routePoints.size
        if (idx >= n - 2) return "Вы на месте"

        val a = routePoints[idx]
        var j = idx + 1
        // пропускаем почти нулевые участки
        while (j < n - 1 &&
            haversine(routePoints[j - 1].latitude, routePoints[j - 1].longitude,
                routePoints[j].latitude, routePoints[j].longitude) < 5.0) {
            j++
        }
        val b = routePoints[j]
        val c = routePoints[minOf(j + 1, n - 1)]

        // азимуты участков
        val brg1 = bearingDeg(a, b)
        val brg2 = bearingDeg(b, c)
        var delta = brg2 - brg1
        while (delta > 180) delta -= 360
        while (delta < -180) delta += 360

        return when {
            abs(delta) < 30 -> "Идите прямо"
            delta > 0 -> if (abs(delta) > 120) "Резко направо" else "Поверните направо"
            else -> if (abs(delta) > 120) "Резко налево" else "Поверните налево"
        }
    }

    private fun bearingDeg(a: GeoPoint, b: GeoPoint): Double {
        val la1 = Math.toRadians(a.latitude)
        val la2 = Math.toRadians(b.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val y = sin(dLon) * cos(la2)
        val x = cos(la1) * sin(la2) - sin(la1) * cos(la2) * cos(dLon)
        return Math.toDegrees(atan2(y, x))
    }

    private fun stopTracking() {
        tracking = false
        if (::locationManager.isInitialized) locationManager.removeUpdates(locationListener)
        cardNavigation.visibility = View.GONE
        cardBanner.visibility = View.GONE
        cardRouteInfo.visibility = View.VISIBLE
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
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