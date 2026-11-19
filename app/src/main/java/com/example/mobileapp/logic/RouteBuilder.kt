package com.example.mobileapp.logic

import android.util.Log
import com.example.mobileapp.data.RouteResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RouteBuilder {

    /** Порог: ближе — точка считается стоящей на дороге (сплошная линия),
     *  дальше — вне сети (пунктирный хвост). */
    private const val ON_ROAD_THRESHOLD_M = 30.0

    /** Проекция точки на ребро графа. */
    private data class EdgeProjection(
        val edgeIndex: Int,
        val u: Int,          // начало ребра
        val v: Int,          // конец ребра
        val t: Float,        // 0..1 — положение проекции вдоль ребра от u к v
        val lat: Double,     // координаты проекции
        val lon: Double,
        val distMeters: Double // расстояние от исходной точки до проекции
    )

    /**
     * Строит маршрут между двумя координатами по графу.
     * Точки проецируются на ближайшие рёбра: если пользователь стоит на дороге,
     * маршрут начинается прямо с его точки; если в стороне — рисуется пунктирный хвост.
     */
    fun buildRoute(
        graph: TrailGraph,
        mode: Int,
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): RouteResult? {
        val projS = nearestEdge(graph, mode, startLat, startLon) ?: return null
        val projE = nearestEdge(graph, mode, endLat, endLon) ?: return null

        Log.d("RouteBuilder", "mode=$mode, старт: ребро ${projS.u}->${projS.v}, t=${"%.2f".format(projS.t)}, " +
                "до дороги ${"%.0f".format(projS.distMeters)} м; финиш: ребро ${projE.u}->${projE.v}, " +
                "t=${"%.2f".format(projE.t)}, до дороги ${"%.0f".format(projE.distMeters)} м")

        // Выбор легального узла выхода из стартовой проекции.
        // Движение по ребру u->v из P в сторону v всегда легально;
        // в сторону u — только если существует обратное ребро v->u (важно для односторонних).
        val revS = hasReverse(graph, mode, projS.u, projS.v)
        val (startNode, startShare) = if (projS.t <= 0.5f) {
            if (revS) projS.u to projS.t else projS.v to (1f - projS.t)
        } else {
            projS.v to (1f - projS.t)
        }

        // Вход в конечную проекцию: со стороны u — всегда легально, со стороны v — только при обратном ребре
        val revE = hasReverse(graph, mode, projE.u, projE.v)
        val (endNode, endShare) = if (projE.t <= 0.5f) {
            projE.u to projE.t
        } else {
            if (revE) projE.v to (1f - projE.t) else projE.u to projE.t
        }

        val route = AStar.findRoute(graph, mode, startNode, endNode) ?: return null
        val (baseSeconds, indices) = route

        // Время на частях рёбер от проекций до выбранных узлов
        val startW = graph.weight(mode, projS.edgeIndex)
        val endW = graph.weight(mode, projE.edgeIndex)
        val totalSeconds = baseSeconds + startShare * startW + endShare * endW

        // Точки по графу: проекция старта -> путь -> проекция финиша
        val graphPoints = ArrayList<Pair<Double, Double>>(indices.size + 2)
        graphPoints.add(projS.lat to projS.lon)
        for (idx in indices) {
            graphPoints.add(graph.lat[idx].toDouble() to graph.lon[idx].toDouble())
        }
        graphPoints.add(projE.lat to projE.lon)

        // Хвосты: от реальных точек пользователя до проекций на дорогу
        val startTail = haversine(startLat, startLon, projS.lat, projS.lon)
        val endTail = haversine(endLat, endLon, projE.lat, projE.lon)

        // Полный список: реальный старт, дорога, реальный финиш
        val fullPoints = ArrayList<Pair<Double, Double>>(graphPoints.size + 2)
        fullPoints.add(startLat to startLon)
        fullPoints.addAll(graphPoints)
        fullPoints.add(endLat to endLon)

        // Метры — по итоговой полилинии, включая хвосты
        var meters = 0.0
        for (k in 1 until fullPoints.size) {
            meters += haversine(
                fullPoints[k - 1].first, fullPoints[k - 1].second,
                fullPoints[k].first, fullPoints[k].second
            )
        }

        Log.d("RouteBuilder", "маршрут: ${"%.0f".format(meters)} м, ${"%.1f".format(totalSeconds / 60f)} мин, " +
                "точек: ${fullPoints.size}, хвосты: ${"%.0f".format(startTail)}/${"%.0f".format(endTail)} м")

        return RouteResult(
            distanceMeters = meters.toFloat(),
            points = fullPoints,
            startTailMeters = if (startTail > ON_ROAD_THRESHOLD_M) startTail.toFloat() else 0f,
            endTailMeters = if (endTail > ON_ROAD_THRESHOLD_M) endTail.toFloat() else 0f
        )
    }

    /**
     * Ближайшее ребро режима к точке: перебор рёбер с проекцией на отрезок
     * в сплюснутых координатах (долгота сжата на cos широты).
     */
    private fun nearestEdge(graph: TrailGraph, mode: Int, lat: Double, lon: Double): EdgeProjection? {
        val latF = lat.toFloat()
        val lonF = lon.toFloat()
        val cosLat = cos(Math.toRadians(lat)).toFloat()

        var best: EdgeProjection? = null
        var bestD = Float.MAX_VALUE

        for (node in 0 until graph.nodeCount) {
            val ax = graph.lon[node] * cosLat
            val ay = graph.lat[node]
            for (i in graph.neighborsOf(mode, node)) {
                val v = graph.target(mode, i)
                val bx = graph.lon[v] * cosLat
                val by = graph.lat[v]

                val abx = bx - ax
                val aby = by - ay
                val len2 = abx * abx + aby * aby
                if (len2 <= 0f) continue

                val px = lonF * cosLat
                val py = latF
                var t = ((px - ax) * abx + (py - ay) * aby) / len2
                t = t.coerceIn(0f, 1f)

                val cx = ax + t * abx
                val cy = ay + t * aby
                val dx = px - cx
                val dy = py - cy
                val d2 = dx * dx + dy * dy

                if (d2 < bestD) {
                    bestD = d2
                    // расстояние пересчитываем честно, гаверсинусом
                    val projLat = cy.toDouble()
                    val projLon = (cx / cosLat).toDouble()
                    best = EdgeProjection(
                        edgeIndex = i,
                        u = node,
                        v = v,
                        t = t,
                        lat = projLat,
                        lon = projLon,
                        distMeters = haversine(lat, lon, projLat, projLon)
                    )
                }
            }
        }
        return best
    }

    /** Существует ли обратное ребро v->u (то есть можно ли двигаться против ребра u->v). */
    private fun hasReverse(graph: TrailGraph, mode: Int, u: Int, v: Int): Boolean {
        for (i in graph.neighborsOf(mode, v)) {
            if (graph.target(mode, i) == u) return true
        }
        return false
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