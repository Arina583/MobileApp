package com.example.mobileapp.api

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex

class CartoTileSource(apiKey: String) : OnlineTileSourceBase(
    "CARTO", 0, 19, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/"
    )
) {
    private val key = apiKey

    override fun getTileURLString(pMapTileIndex: Long): String {
        return getBaseUrl() +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) +
                mImageFilenameEnding + "?key=$key"
    }
}
