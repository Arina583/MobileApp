package com.example.mobileapp.models

data class RouteSegment(
    val id: Long? = null,
    val routeId: Long? = null,
    val trailId: Long? = null,
    val orderIndex: Int,
    val segmentLength: Double,
    val segmentPassafityScore: Double,
    val hasDangerNeardy: Boolean,
)