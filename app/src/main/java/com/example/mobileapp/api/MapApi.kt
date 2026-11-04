package com.example.mobileapp.api

import android.graphics.Color
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapApi {
    val tileSource = CartoTileSource("cb1_52a2_1_523fd23a1b11d498da8f10a4")

    fun setupMap(mapView: MapView) {
        mapView.setTileSource(tileSource)

        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true

        val startPoint = GeoPoint(56.129057, 40.406635) // Владимир
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)
    }

    fun addTrailPath(mapView: MapView) {
        val trailPoints = listOf(
            GeoPoint(56.129057, 40.406635),
            GeoPoint(56.130100, 40.410200),
            GeoPoint(56.131500, 40.412300),
            GeoPoint(56.133000, 40.415000)
        )

        val polyline = org.osmdroid.views.overlay.Polyline(mapView)
        polyline.setPoints(trailPoints)
        polyline.color = Color.parseColor("#2E7D32")       // Тёмно-зелёный
        polyline.width = 8f                                 // Толщина линии
//        polyline. = 0.9f                              // Прозрачность
        polyline.title = "Лесная тропа (тестовая)"

        mapView.overlays.add(polyline)
        mapView.invalidate()
    }


//    fun addDangerMarker(
//        mapView: MapView,
//        latitude: Double,
//        longitude: Double,
//        title: String? = "Опасный участок"
//    ) {
//        val marker = Marker(mapView)
//        marker.position = GeoPoint(latitude, longitude)
//        marker.title = title
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
//        mapView.overlays.add(marker)
//        mapView.invalidate()
//    }
}
