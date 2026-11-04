package com.example.mobileapp.api

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

class MapApi(private val context: Context) {
    // Метод для настройки уже существующего MapView
    fun setupMap(mapView: MapView) {
        // Конфигурация Osmdroid (вызывается один раз при старте приложения)
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("main", android.content.Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName

        // Путь к файлу в assets
        val mbtilesFile = File(context.filesDir, "forest_map_6-9.mbtiles")

//        mapView.setTileSource(TileSourceFactory.MAPNIK) // Позже будет заменен на офлайн-тайлы

        // Если файл ещё не скопирован из assets в filesDir — сделай это один раз при старте
        if (!mbtilesFile.exists()) {
            context.assets.open("forest_map_6-9.mbtiles").use { input ->
                mbtilesFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        // Подключаем офлайн-архив как источник тайлов
        if (ArchiveFileFactory.isFileExtensionRegistered("mbtiles")) {
            val tileProvider = OfflineTileProvider(
                SimpleRegisterReceiver(context),
                arrayOf(mbtilesFile)
            )
            mapView.tileProvider = tileProvider

            // Узнаём, какой tile source записан в архиве
            val archives = tileProvider.archives
            if (archives.isNotEmpty()) {
                val sources = archives[0].tileSources
                mapView.setTileSource(
                    if (sources.isNotEmpty()) FileBasedTileSource.getSource(sources.first())
                    else TileSourceFactory.DEFAULT_TILE_SOURCE
                )
            }
            mapView.setUseDataConnection(false) // работаем только офлайн
        } else {
            // Запасной вариант — онлайн-карта
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }

        val startPoint = GeoPoint(56.129057, 40.406635) // Владимир
        mapView.controller.setZoom(9.0)
        mapView.controller.setCenter(startPoint)
    }

    // Добавление маркера опасности
//    fun addDangerMarker(mapView: MapView, latitude: Double, longitude: Double, title: String? = null) {
//        val marker = Marker(mapView)
//        marker.position = GeoPoint(latitude, longitude)
//        marker.title = title
//        // Далее будет добавлена иконка, цвет, описание (возможно что-то еще)
//        mapView.overlays.add(marker)
//    }
}