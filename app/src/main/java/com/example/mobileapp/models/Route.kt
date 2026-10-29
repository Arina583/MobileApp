package com.example.mobileapp.models

import org.locationtech.jts.geom.Geometry
import java.util.Date

data class Route(
    val id: Long? = null,
    val areaId: Long? = null,
    val startLocation: Geometry,
    val endLocation: Geometry,
    val totalLength: Double,
    val estimatedTime: Int,
    val safetyScore: Double,
    val segment: String,
    val generatedAt: Date,
    val preferenceApplied: String,
    val sequenceNumber: Int,
)