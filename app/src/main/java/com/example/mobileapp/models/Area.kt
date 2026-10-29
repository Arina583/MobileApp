package com.example.mobileapp.models

import org.locationtech.jts.geom.Geometry

data class Area(
    val id: Long? = null,
    val boundary: Geometry,
    val description: String,
)