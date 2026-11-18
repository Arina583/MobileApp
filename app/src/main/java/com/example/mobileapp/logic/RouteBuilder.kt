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
     * mode — TrailGraph.MODE_WALK / MODE_BIKE / MODE_CAR.
     * Возвращает null, если точки не связаны сетью для этого режима.
     */
    fun buildRoute(
        graph: TrailGraph,
        mode: Int,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? {
        val s = nearestNode(graph, mode, startLat, startLon)
        val e = nearestNode(graph, mode, endLat, endLon)

        Log.d("RouteBuilder", "mode=$mode, s=$s (соседей=${if (s == -1) 0 else graph.neighborsOf(mode, s).count()}), " +
                "e=$e (соседей=${if (e == -1) 0 else graph.neighborsOf(mode, e).count()})")

        if (s == -1 || e == -1) return null

        val route = AStar.findRoute(graph, mode, s, e) ?: return null
        val (seconds, indices) = route

        val points = indices.map { graph.lat[it].toDouble() to graph.lon[it].toDouble() }

        // Метры по пути — расстояния теперь сами считаем по маршруту
        var meters = 0.0
        for (k in 1 until indices.size) {
            meters += haversine(
                graph.lat[indices[k - 1]].toDouble(), graph.lon[indices[k - 1]].toDouble(),
                graph.lat[indices[k]].toDouble(), graph.lon[indices[k]].toDouble()
            )
        }

        // Прилепляем крайние точки к реальным координатам пользователя
        val fullPoints = ArrayList<Pair<Double, Double>>(points.size + 2)
        fullPoints.add(startLat to startLon)
        fullPoints.addAll(points)
        fullPoints.add(endLat to endLon)

        Log.d("RouteBuilder", "маршрут: ${"%.0f".format(meters)} м, ${"%.1f".format(seconds / 60f)} мин, точек: ${fullPoints.size}")
        return RouteResult(distanceMeters = meters.toFloat(), points = fullPoints)
    }

    fun nearestNode(graph: TrailGraph, mode: Int, lat: Double, lon: Double): Int {
        var best = -1
        var bestD = Double.MAX_VALUE
        val latF = lat.toFloat()
        val lonF = lon.toFloat()
        val cosLat = cos(Math.toRadians(lat))
        for (i in 0 until graph.nodeCount) {
            if (graph.neighborsOf(mode, i).isEmpty()) continue
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
