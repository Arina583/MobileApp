package com.example.mobileapp.models

import org.locationtech.jts.geom.Geometry
import java.util.Date

data class Hazard(
    val id: Long? = null,
    val areaId: Long? = null,
    val dangerTypeId: Long? = null,
    val severity: Int,
    val location: Geometry,
    val radius: Double? = null,
    val statusId: Long? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val reportedAt: Date? = null,
    val confirmedByUser: Date? = null,
    val sequenceNumber: Int? = null,
)