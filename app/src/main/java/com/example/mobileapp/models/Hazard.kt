package com.example.mobileapp.models

import java.util.Date

data class Hazard(
    val id: Long? = null,
    val areaId: Long? = null,
    val dangerTypeId: Long? = null,
    val severity: Int,
//    val location: Geometry,
    val radius: Double,
    val statusId: Long? = null,
    val description: String,
    val photoUrl: String,
    val reportedAt: Date,
    val confirmedByUser: Date,
    val sequenceNumber: Int,
)