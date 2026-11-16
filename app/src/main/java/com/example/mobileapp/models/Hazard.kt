package com.example.mobileapp.models

import org.locationtech.jts.geom.Geometry
import java.util.Date

data class Hazard(
    val id: Long? = null,
    val areaId: Long = 1,
    val dangerTypeId: Long,
    val severity: Int,
    val location: Geometry,
    val radius: Double = 0.0,
    val statusId: Long = 1,
    val description: String? = null,
    val photoUrl: String? = null,
    val reportedAt: Date? = null,
    val confirmedByUser: Date? = null,
    val sequenceNumber: Int? = null,
    val uuid: String?,
)