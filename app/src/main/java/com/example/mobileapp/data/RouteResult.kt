package com.example.mobileapp.data

data class RouteResult(
    val distanceMeters: Float,                          // длина маршрута в метрах
    val points: List<Pair<Double, Double>>
)
