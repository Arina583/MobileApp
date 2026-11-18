package com.example.mobileapp.logic

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlin.math.cos

class TrailGraph private constructor(
    val lat: FloatArray,
    val lon: FloatArray,
    private val offsets: IntArray,   // offsets[i]..offsets[i+1] — рёбра узла i
    private val targets: IntArray,   // конечный узел ребра
    private val weights: FloatArray  // длина ребра в метрах
) {
    val nodeCount get() = lat.size

    // Диапазон индексов рёбер, исходящих из узла
    fun neighborsOf(node: Int): IntRange = offsets[node] until offsets[node + 1]

    // Конечный узел ребра с индексом edgeIndex
    fun target(edgeIndex: Int): Int = targets[edgeIndex]

    // Вес (длина в метрах) ребра с индексом edgeIndex
    fun weight(edgeIndex: Int): Float = weights[edgeIndex]

    companion object {
        fun load(db: SQLiteDatabase): TrailGraph {
            // узлы
            val nodeCount = db.rawQuery("SELECT COUNT(*) FROM nodes", null)
                .use { it.moveToFirst(); it.getInt(0) }
            val lat = FloatArray(nodeCount)
            val lon = FloatArray(nodeCount)


            db.rawQuery("SELECT id, lat, lon FROM nodes", null).use { c ->
                while (c.moveToNext()) {
                    val i = c.getLong(0).toInt()
                    lat[i] = c.getDouble(1).toFloat()
                    lon[i] = c.getDouble(2).toFloat()
                }
            }

            // рёбра
            val edgeCount = db.rawQuery("SELECT COUNT(*) FROM edges", null)
                .use { it.moveToFirst(); it.getInt(0) }
            val edgeU = IntArray(edgeCount)
            val edgeV = IntArray(edgeCount)
            val edgeW = FloatArray(edgeCount)

            var valid = 0
            db.rawQuery("SELECT u, v, length FROM edges", null).use { c ->
                var i = 0
                while (c.moveToNext()) {
                    val u = c.getLong(0).toInt()
                    val v = c.getLong(1).toInt()   // колонка 1 — это v
                    if (u == v) continue           // петли не нужны
                    if (u < 0 || u >= nodeCount || v < 0 || v >= nodeCount) continue
                    edgeU[i] = u
                    edgeV[i] = v
                    edgeW[i] = c.getDouble(2).toFloat()
                    valid++
                    i++
                }
            }

            val m = valid   // реальное число рёбер для CSR
            Log.d("TrailGraph", "узлов=$nodeCount, рёбер=$edgeCount (валидных=$m)")
            Log.d(
                "TrailGraph",
                "первый узел: (${lat[0]}, ${lon[0]}), последний: (${lat[nodeCount - 1]}, ${lon[nodeCount - 1]})"
            )

            // CSR: считаем степени, строим смещения
            val offsets = IntArray(nodeCount + 1)
            for (i in 0 until m) offsets[edgeU[i] + 1]++
            for (i in 1..nodeCount) offsets[i] += offsets[i - 1]

            val targets = IntArray(m)
            val weights = FloatArray(m)
            val cursor = offsets.copyOf()
            for (i in 0 until m) {
                val pos = cursor[edgeU[i]]++
                targets[pos] = edgeV[i]
                weights[pos] = edgeW[i]
            }

            return TrailGraph(lat, lon, offsets, targets, weights)
        }
    }

}
