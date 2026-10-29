package com.example.mobileapp.models

import java.util.Date

data class Trail(
    val id: Long? = null,
    val areaId: Long? = null,
//    val startPoint: Geometry,
//    val endPoint: Geometry,
    val length: Double,
    val passabilityScore: Double,
    val passabilityType: Long? = null,
    val seasonalFactory: Double,
    val lastUpdateAt: Date,
    val sequenceNumber: Long? = null,
)