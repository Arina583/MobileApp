package com.example.mobileapp.api

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.util.Log
import com.example.mobileapp.data.DangerTypeRepository
import com.example.mobileapp.data.HazardRepository
import com.example.mobileapp.models.Hazard
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapApi (context: Context) {
    val tileSource = CartoTileSource("cb1_52a2_1_523fd23a1b11d498da8f10a4")

    private var hazardMarkers: List<Marker> = emptyList()

    private var hazardRepository = HazardRepository(context)

    fun setupMap(mapView: MapView) {
        mapView.setTileSource(tileSource)

        mapView.setMultiTouchControls(true)
        mapView.isTilesScaledToDpi = true

        val startPoint = GeoPoint(56.129057, 40.406635) // Владимир
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)
    }

    fun showHazards(mapView: MapView) {

        // 1. Удаляем старые маркеры опасностей
        hazardMarkers.forEach { mapView.overlays.remove(it) }
        hazardMarkers = emptyList()

        // 2. Проходим по всем отметкам из базы
        val newMarkers = hazardRepository.getAll().mapNotNull { hazard ->
            val point = hazard.location as? org.locationtech.jts.geom.Point
                ?: return@mapNotNull null

            val marker = Marker(mapView)

            // GeoPoint ждёт (lat, lon), а JTS хранит (x=lon, y=lat) — порядок обратный
            marker.position = GeoPoint(point.coordinate.y, point.coordinate.x)

            val typeName = DangerTypeRepository.nameFor(hazard.dangerTypeId)
            marker.title = typeName
            marker.snippet = hazard.description ?: ""

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            mapView.overlays.add(marker)
            marker
        }

        // 3. Запоминаем созданные маркеры для последующей очистки
        hazardMarkers = newMarkers

        mapView.invalidate()
    }
}
