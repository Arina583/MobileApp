package com.example.mobileapp.data

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

object Geo {
    val factory = GeometryFactory(PrecisionModel(), 4326)

    fun point(latitude: Double, longitude: Double): Point =
        factory.createPoint(Coordinate(longitude, latitude))
}