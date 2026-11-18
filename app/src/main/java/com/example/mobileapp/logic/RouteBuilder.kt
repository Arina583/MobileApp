package com.example.mobileapp.logic

import android.util.Log
import com.example.mobileapp.data.RouteResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RouteBuilder {

    /**
     * Строит маршрут между двумя координатами по графу троп.
     * Возвращает null, если точки не связаны сетью.
     */
    fun buildRoute(
        graph: TrailGraph,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? {
        val s = nearestNode(graph, startLat, startLon)
        val e = nearestNode(graph, endLat, endLon)

        Log.d("RouteBuilder", "s=$s (соседей=${graph.neighborsOf(s).count()}), " +
                "e=$e (соседей=${graph.neighborsOf(e).count()})")

        if (s == -1 || e == -1) {
            val dist = haversine(startLat, startLon, endLat, endLon).toFloat()
            return RouteResult(
                distanceMeters = dist,
                points = listOf(startLat to startLon, endLat to endLon)
            )
        }

        val route = AStar.findRoute(graph, s, e) ?: return null
        val (meters, indices) = route

        val points = indices.map { graph.lat[it].toDouble() to graph.lon[it].toDouble() }

        // Прилепляем крайние точки к реальным координатам пользователя,
        // чтобы линия не обрывалась в десятках метров от точки
        val fullPoints = ArrayList<Pair<Double, Double>>(points.size + 2)
        fullPoints.add(startLat to startLon)
        fullPoints.addAll(points)
        fullPoints.add(endLat to endLon)

        return RouteResult(distanceMeters = meters, points = fullPoints)
    }

    /**
     * Индекс ближайшего узла графа к точке (lat, lon).
     */
    fun nearestNode(graph: TrailGraph, lat: Double, lon: Double): Int {
        var best = -1
        var bestD = Double.MAX_VALUE
        val latF = lat.toFloat()
        val lonF = lon.toFloat()
        val cosLat = cos(Math.toRadians(lat))
        for (i in 0 until graph.nodeCount) {
            // изолированные узлы не годятся: из них не построить маршрут
            if (graph.neighborsOf(i).isEmpty()) continue
            val dy = (graph.lat[i] - latF).toDouble()
            val dx = (graph.lon[i] - lonF).toDouble() * cosLat
            val d = dy * dy + dx * dx
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        return best
    }

    /** Расстояние по прямой между двумя точками в метрах. */
    fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}