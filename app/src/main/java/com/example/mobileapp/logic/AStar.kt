package com.example.mobileapp.logic

import java.util.PriorityQueue
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object AStar {

    private const val EARTH_R = 6371000.0

    fun findRoute(graph: TrailGraph, start: Int, goal: Int): Pair<Float, IntArray>? {
        val n = graph.nodeCount
        val gScore = FloatArray(n) { Float.POSITIVE_INFINITY }
        val prev = IntArray(n) { -1 }
        val closed = BooleanArray(n)

        data class Entry(val node: Int, val f: Float)
        val pq = PriorityQueue<Entry>(compareBy { it.f })

        gScore[start] = 0f
        pq.add(Entry(start, heuristic(graph, start, goal)))

        while (pq.isNotEmpty()) {
            val (cur, f) = pq.poll()
            if (closed[cur]) continue
            if (f > gScore[cur] + heuristic(graph, cur, goal)) continue // устаревшая запись
            closed[cur] = true
            if (cur == goal) break

            for (i in graph.neighborsOf(cur)) {
                val next = graph.target(i)
                val tentative = gScore[cur] + graph.weight(i)
                if (tentative < gScore[next]) {
                    gScore[next] = tentative
                    prev[next] = cur
                    pq.add(Entry(next, tentative + heuristic(graph, next, goal)))
                }
            }
        }

        if (gScore[goal] == Float.POSITIVE_INFINITY) return null

        val path = ArrayList<Int>()
        var node = goal
        while (node != -1) { path.add(node); node = prev[node] }
        path.reverse()
        return gScore[goal] to path.toIntArray()
    }

    private fun heuristic(g: TrailGraph, a: Int, b: Int): Float {
        val dLat = Math.toRadians((g.lat[b] - g.lat[a]).toDouble())
        val dLon = Math.toRadians((g.lon[b] - g.lon[a]).toDouble())
        val h = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(g.lat[a].toDouble())) *
                cos(Math.toRadians(g.lat[b].toDouble())) *
                sin(dLon / 2).pow(2)
        return (2 * EARTH_R * asin(sqrt(h))).toFloat()
    }
}
