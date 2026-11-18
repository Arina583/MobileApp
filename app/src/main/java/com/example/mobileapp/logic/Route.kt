package com.example.mobileapp.logic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R
import com.example.mobileapp.activity.MapPickerActivity
import com.example.mobileapp.data.DangerTypeRepository
import com.example.mobileapp.data.GraphRepository
import com.example.mobileapp.data.RouteResult
import com.example.mobileapp.models.DangerType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

class Route (private val activity: AppCompatActivity) {

    // id выбранного типа опасности
    var selectedRouteId: Int? = null
        private set

    // широта выбранной точки на карте
    var selectedLat: Double? = null
    // долгота выбранной точки на карте
    var selectedLng: Double? = null

    //список кнопок (id) типов передвижения
    private val routeButtons = listOf(
        R.id.btnModeWalk,
        R.id.btnModeBike,
        R.id.btnModeCar,
    )

    init {
        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap();
        }

        routeButtons.forEach { id -> activity.findViewById<MaterialButton>(id).setOnClickListener { selectedRoute(id) } }

        activity.findViewById<View>(R.id.btnStartPickOnMap).setOnClickListener {
            mapPickerLauncherStart.launch(Intent(activity, MapPickerActivity::class.java))
        }

        activity.findViewById<View>(R.id.btnEndPickOnMap).setOnClickListener {
            mapPickerLauncherEnd.launch(Intent(activity, MapPickerActivity::class.java))
        }

        activity.findViewById<MaterialButton>(R.id.btnBuildRoute).setOnClickListener { buildRoute() }
    }


    //Лаунчер экрана выбора начальной точки на карте
    //При успехе - сохранение координат и показ их на экране
    private val mapPickerLauncherStart = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0)
            if (lat != null && lng != null) {
                selectedLat = lat
                selectedLng = lng
                showStartCoordinates(lat, lng)
            }
        }
    }

    //Лаунчер экрана выбора конечной точки на карте
    //При успехе - сохранение координат и показ их на экране
    private val mapPickerLauncherEnd = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0)
            if (lat != null && lng != null) {
                selectedLat = lat
                selectedLng = lng
                showEndCoordinates(lat, lng)
            }
        }
    }

    //  Отдельно начало и конец
    var startLat: Double? = null
    var startLng: Double? = null
    var endLat: Double? = null
    var endLng: Double? = null


    //Показ выбранной начальной точки с координатами
    private fun showStartCoordinates(lat: Double, lng: Double) {
        startLat = lat
        startLng = lng
        val tv = activity.findViewById<TextInputEditText>(R.id.etStartCoords)
        tv.setText(String.format(Locale.US, "%.6f, %.6f", lat, lng))
    }

    //Показ выбранной точки с координатами
    private fun showEndCoordinates(lat: Double, lng: Double) {
        endLat = lat
        endLng = lng
        val tv = activity.findViewById<TextInputEditText>(R.id.etEndCoords)
        tv.setText(String.format(Locale.US, "%.6f, %.6f", lat, lng))
    }

    //Обработка нажатия кнопки вида передвижения
    private fun selectedRoute(id: Int) {
        routeButtons.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        activity.findViewById<MaterialButton>(id).isSelected = true
        selectedRouteId = id
    }

    //Валидация координат
    private fun hasValidCoordinates(): Boolean {
        return startLat != null && startLng != null && endLat != null && endLng != null
    }

    // Парсинг координат из строки
    private fun parseCoordinates(text: String): Pair<Double, Double>? {
        val parts = text.trim().split(",")
        if (parts.size != 2) return null

        return try {
            val lat = parts[0].trim().toDouble()
            val lng = parts[1].trim().toDouble()
            Pair(lat, lng)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun performRouteCalculation(startLat: Double, startLng: Double, endLat: Double, endLng: Double) {
        // Пока просто заглушка (показываем что маршрут построен)

        Toast.makeText(
            activity,
            "Маршрут построен: от ($startLat, $startLng) до ($endLat, $endLng)",
            Toast.LENGTH_LONG
        ).show()

        // Пример (переход на экран с картой и линией маршрута)
        val intent = Intent(activity, MainActivity::class.java) // или отдельный Activity/Fragment для карты
        intent.putExtra("route_start_lat", startLat)
        intent.putExtra("route_start_lng", startLng)
        intent.putExtra("route_end_lat", endLat)
        intent.putExtra("route_end_lng", endLng)
        activity.startActivity(intent)
    }

    private fun setRouteControlsEnabled(enabled: Boolean) {
        activity.findViewById<View>(R.id.btnBuildRoute).isEnabled = enabled
    }

    //Построение маршрута
    private fun buildRoute() {
        if (!hasValidCoordinates()) {
            Toast.makeText(activity, "Выберите обе точки маршрута", Toast.LENGTH_SHORT).show()
            return
        }

        val startText = activity.findViewById<TextInputEditText>(R.id.etStartCoords).text.toString()
        val endText = activity.findViewById<TextInputEditText>(R.id.etEndCoords).text.toString()

        val (startLat, startLng) = parseCoordinates(startText) ?: run {
            Toast.makeText(activity, "Некорректные координаты начала", Toast.LENGTH_SHORT).show()
            return
        }

        val (endLat, endLng) = parseCoordinates(endText) ?: run {
            Toast.makeText(activity, "Некорректные координаты конца", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("Route", "Строим маршрут: [$startLat, $startLng] -> [$endLat, $endLng]")

        activity.lifecycleScope.launch(Dispatchers.Main) {
            // блокируем кнопку, чтобы не запустить расчёт дважды
            setRouteControlsEnabled(false)

            val result: RouteResult? = withContext(Dispatchers.IO) {
                val graph = GraphRepository.openGraph(activity.applicationContext)
                RouteBuilder.buildRoute(graph, startLat, startLng, endLat, endLng)
            }

            setRouteControlsEnabled(true)

            when {
                result == null ->
                    Toast.makeText(activity, "Маршрут не проложен: точки не связаны сетью троп", Toast.LENGTH_LONG).show()
                result.points.size < 2 ->
                    Toast.makeText(activity, "Точки слишком близко друг к другу", Toast.LENGTH_SHORT).show()
                else -> {
                    Log.d("Route", "Найден маршрут: ${"%.0f".format(result.distanceMeters)} м, точек: ${result.points.size}")
                    drawRouteOnMap(result)
                }
            }
        }
    }

    private var routeOverlay: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private fun drawRouteOnMap(result: RouteResult) {
        val mapView = activity.findViewById<MapView>(R.id.mapView2)
        val points = result.points.map { GeoPoint(it.first, it.second) }

        // убираем прошлый маршрут, если он был
        routeOverlay?.let { mapView.overlays.remove(it) }
        startMarker?.let { mapView.overlays.remove(it) }
        endMarker?.let { mapView.overlays.remove(it) }

        val polyline = Polyline(mapView)
        polyline.setPoints(points)
        polyline.outlinePaint.apply {
            color = Color.parseColor("#E53935")
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        mapView.overlays.add(polyline)
        routeOverlay = polyline

        // маркеры начала и конца
        startMarker = addPointMarker(mapView, points.first(), Color.parseColor("#2E7D32"))
        endMarker = addPointMarker(mapView, points.last(), Color.parseColor("#C62828"))

        // масштабируем карту так, чтобы весь маршрут был виден
        val bounds = BoundingBox.fromGeoPoints(points)
            .increaseByScale(1.25f)
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


    //сброс формы в исходное состояние
    private fun resetForm() {
        routeButtons.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        selectedRouteId = null
        startLat = null
        startLng = null
        endLat = null
        endLng = null
    }

    private fun backMap() {
        activity.finish()
    }
}