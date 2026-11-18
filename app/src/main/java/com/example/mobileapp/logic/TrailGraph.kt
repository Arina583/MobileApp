package com.example.mobileapp.logic

import android.database.sqlite.SQLiteDatabase
import android.util.Log

class TrailGraph private constructor(
    val lat: FloatArray,
    val lon: FloatArray,
    private val offsets: Array<IntArray>,   // [режим][узел] — диапазон рёбер узла
    private val targets: Array<IntArray>,   // [режим][ребро] — конечный узел
    private val weights: Array<FloatArray>  // [режим][ребро] — время проезда в секундах
) {
    val nodeCount get() = lat.size

    fun neighborsOf(mode: Int, node: Int): IntRange =
        offsets[mode][node] until offsets[mode][node + 1]

    fun target(mode: Int, edgeIndex: Int): Int = targets[mode][edgeIndex]

    fun weight(mode: Int, edgeIndex: Int): Float = weights[mode][edgeIndex]

    companion object {
        const val MODE_WALK = 0
        const val MODE_BIKE = 1
        const val MODE_CAR = 2

        fun load(db: SQLiteDatabase): TrailGraph {
            // ---- узлы ----
            val nodeCount = db.rawQuery("SELECT COUNT(*) FROM nodes", null)
                .use { it.moveToFirst(); it.getInt(0) }
            val lat = FloatArray(nodeCount)
            val lon = FloatArray(nodeCount)

            db.rawQuery("SELECT id, lat, lon FROM nodes", null).use { c ->
                while (c.moveToNext()) {
                    val i = c.getLong(0).toInt()
                    if (i < 0 || i >= nodeCount) continue
                    lat[i] = c.getDouble(1).toFloat()
                    lon[i] = c.getDouble(2).toFloat()
                }
            }

            // ---- рёбра: каждый режим — своим запросом ----
            val specs = listOf(
                Triple(MODE_WALK, "foot", "w_foot"),
                Triple(MODE_BIKE, "bike", "w_bike"),
                Triple(MODE_CAR, "car", "w_car")
            )

            val offsets = arrayOfNulls<IntArray>(3)
            val targets = arrayOfNulls<IntArray>(3)
            val weights = arrayOfNulls<FloatArray>(3)

            for ((mode, flagCol, wCol) in specs) {
                val (u, v, w, m) = loadMode(db, nodeCount, flagCol, wCol)
                val (off, tgt, wgt) = buildCsr(nodeCount, u, v, w, m)
                offsets[mode] = off
                targets[mode] = tgt
                weights[mode] = wgt

                var connected = 0
                for (i in 0 until nodeCount) if (off[i] != off[i + 1]) connected++
                Log.d("TrailGraph", "режим $mode: рёбер=$m, узлов с рёбрами=$connected")
            }

            Log.d("TrailGraph", "первый узел: (${lat[0]}, ${lon[0]}), последний: (${lat[nodeCount - 1]}, ${lon[nodeCount - 1]})")

            @Suppress("UNCHECKED_CAST")
            return TrailGraph(lat, lon, offsets as Array<IntArray>, targets as Array<IntArray>, weights as Array<FloatArray>)
        }

        /** Читает направленные рёбра одного режима. Возвращает массивы и реальное число рёбер. */
        private data class ModeEdges(val u: IntArray, val v: IntArray, val w: FloatArray, val m: Int)

        private fun loadMode(db: SQLiteDatabase, nodeCount: Int, flagCol: String, wCol: String): ModeEdges {
            val count = db.rawQuery("SELECT COUNT(*) FROM edges WHERE $flagCol=1", null)
                .use { it.moveToFirst(); it.getInt(0) }

            val u = IntArray(count)
            val v = IntArray(count)
            val w = FloatArray(count)
            var m = 0

            db.rawQuery("SELECT u, v, $wCol FROM edges WHERE $flagCol=1", null).use { c ->
                while (c.moveToNext()) {
                    val uu = c.getLong(0).toInt()
                    val vv = c.getLong(1).toInt()
                    if (uu == vv || uu < 0 || uu >= nodeCount || vv < 0 || vv >= nodeCount) continue
                    u[m] = uu
                    v[m] = vv
                    w[m] = c.getFloat(2)
                    m++
                }
            }
            return ModeEdges(u, v, w, m)
        }

        /** CSR: смещения, конечные узлы, веса. */
        private fun buildCsr(
            nodeCount: Int, u: IntArray, v: IntArray, w: FloatArray, m: Int
        ): Triple<IntArray, IntArray, FloatArray> {
            val offsets = IntArray(nodeCount + 1)
            for (i in 0 until m) offsets[u[i] + 1]++
            for (i in 1..nodeCount) offsets[i] += offsets[i - 1]

            val targets = IntArray(m)
            val weights = FloatArray(m)
            val cursor = offsets.copyOf()
            for (i in 0 until m) {
                val pos = cursor[u[i]]++
                targets[pos] = v[i]
                weights[pos] = w[i]
            }
            return Triple(offsets, targets, weights)
        }
    }
}
