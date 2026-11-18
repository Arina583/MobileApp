package com.example.mobileapp.logic

import android.database.sqlite.SQLiteDatabase
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
            val idToIndex = HashMap<Long, Int>(nodeCount * 2) // только на время загрузки

            db.rawQuery("SELECT id, lat, lon FROM nodes", null).use { c ->
                var i = 0
                while (c.moveToNext()) {
                    idToIndex[c.getLong(0)] = i
                    lat[i] = c.getDouble(1).toFloat()
                    lon[i] = c.getDouble(2).toFloat()
                    i++
                }
            }

            // рёбра
            val edgeCount = db.rawQuery("SELECT COUNT(*) FROM edges", null)
                .use { it.moveToFirst(); it.getInt(0) }
            val edgeU = IntArray(edgeCount)
            val edgeV = IntArray(edgeCount)
            val edgeW = FloatArray(edgeCount)

            db.rawQuery("SELECT u, v, length FROM edges", null).use { c ->
                var i = 0
                while (c.moveToNext()) {
                    edgeU[i] = idToIndex[c.getLong(0)] ?: continue // висячее ребро — пропускаем
                    edgeV[i] = idToIndex[c.getLong(1)] ?: continue
                    edgeW[i] = c.getDouble(2).toFloat()
                    i++
                }
            }

            // CSR: считаем степени, строим смещения
            val offsets = IntArray(nodeCount + 1)
            for (i in 0 until edgeCount) offsets[edgeU[i] + 1]++
            for (i in 1..nodeCount) offsets[i] += offsets[i - 1]

            val targets = IntArray(edgeCount)
            val weights = FloatArray(edgeCount)
            val cursor = offsets.copyOf()
            for (i in 0 until edgeCount) {
                val pos = cursor[edgeU[i]]++
                targets[pos] = edgeV[i]
                weights[pos] = edgeW[i]
            }

            return TrailGraph(lat, lon, offsets, targets, weights)
        }
    }

}
