package com.example.mobileapp.logic

import com.example.mobileapp.data.RouteResult
import kotlin.math.cos

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
        if (s == -1 || e == -1) return null

        val route = AStar.findRoute(graph, s, e) ?: return null
        val (meters, indices) = route

        val points = indices.map { graph.lat[it].toDouble() to graph.lon[it].toDouble() }
        return RouteResult(distanceMeters = meters, points = points)
    }

    /**
     * Индекс ближайшего узла графа к точке (lat, lon).
     * Линейный перебор ~455 тыс. узлов занимает единицы миллисекунд.
     */
    fun nearestNode(graph: TrailGraph, lat: Double, lon: Double): Int {
        var best = -1
        var bestD = Double.MAX_VALUE
        val latF = lat.toFloat(); val lonF = lon.toFloat()
        val cosLat = cos(Math.toRadians(lat))
        for (i in 0 until graph.nodeCount) {
            val dy = (graph.lat[i] - latF).toDouble()
            val dx = (graph.lon[i] - lonF).toDouble() * cosLat
            val d = dy * dy + dx * dx   // сравниваем квадраты — без sqrt
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }
}