package com.example.mobileapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mobileapp.models.Hazard
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.io.WKTReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HazardRepository(context: Context) {

    private val db: SQLiteDatabase =
        DatabaseHelper.getInstance(context).writableDatabase

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val wktReader = WKTReader(Geo.factory)

    /** Добавление отметки об опасности в таблицу Hazard. */
    fun add(hazard: Hazard): Hazard {
        // ВРЕМЕННАЯ ДИАГНОСТИКА FK: проверяем каждую ссылку до вставки
        listOf(
            "Area" to hazard.areaId,
            "Danger_type" to hazard.dangerTypeId,
            "Status" to 1L
        ).forEach { (table, id) ->
            db.rawQuery("SELECT id FROM $table WHERE id = ?", arrayOf(id.toString())).use { c ->
                if (!c.moveToFirst()) {
                    Log.e("HazardRepository", "FK СЛОМАН: в таблице $table нет строки id=$id")
                }
            }
        }

        val values = ContentValues().apply {
            put("area_id", hazard.areaId)
            put("danger_type_id", hazard.dangerTypeId)
            put("severity", hazard.severity)
            put("location", toWkt(hazard.location))   // WKT: POINT(lon lat)
            put("radius", 0.0)
            put("status_id", 1)                       // «новая»
            put("description", hazard.description)
            put("reported_at", dateFormat.format(Date()))
            put("sequence_number", -1)                // не выгружено на сервер
        }

        val newId = db.insert("Hazard", null, values)

        Log.d("HazardRepository", "Сохранено #$newId: тип=${hazard.dangerTypeId}, " +
                "точка=(${hazard.location.coordinate.y}, ${hazard.location.coordinate.x}), " +
                "описание=${hazard.description ?: "—"}")

        return hazard.copy(id = newId, reportedAt = Date())
    }

    /** Все отметки, кроме устранённых. */
    fun getAll(): List<Hazard> {
        val result = mutableListOf<Hazard>()
        db.rawQuery(
            """SELECT h.id, h.area_id, h.danger_type_id, h.severity,
                      h.location, h.radius, h.status_id, h.description, h.reported_at
               FROM Hazard h
               JOIN Status s ON s.id = h.status_id
               WHERE s.name != 'устранена'
               ORDER BY h.reported_at DESC""",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val hazard = readHazard(cursor)
                if (hazard != null) result += hazard
            }
        }
        return result
    }

    fun byArea(areaId: Long): List<Hazard> = getAll().filter { it.areaId == areaId }

    fun byType(typeId: Long): List<Hazard> = getAll().filter { it.dangerTypeId == typeId }

    fun count(): Int = getAll().size

    /** Опасности рядом с точкой — расстояние считаем формулой гаверсинуса. */
    fun near(lat: Double, lng: Double, radiusKm: Double = 2.0): List<Hazard> =
        getAll().filter { h ->
            val d = haversineKm(lat, lng, h.location.coordinate.y, h.location.coordinate.x)
            d <= radiusKm + h.radius / 1000.0
        }

    // ---------- служебные функции ----------

    private fun readHazard(cursor: android.database.Cursor): Hazard? {
        val id = cursor.getLong(0)
        val areaId = cursor.getLong(1)
        val dangerTypeId = cursor.getLong(2)
        val severity = cursor.getInt(3)
        val wkt = cursor.getString(4)
        val radius = cursor.getDouble(5)
        val statusId = cursor.getLong(6)
        val description = cursor.getString(7)
        val reportedAt = cursor.getString(8)

        val point = parseWkt(wkt) ?: return null

        return Hazard(
            id = id,
            areaId = areaId,
            dangerTypeId = dangerTypeId,
            severity = severity,
            location = point,
            description = description,
            reportedAt = reportedAt?.let { dateFormat.parse(it) }
        )
    }

    /** JTS Point -> WKT-строка для SQLite. Coordinate(longitude, latitude) -> POINT(lon lat). */
    private fun toWkt(geometry: Geometry): String =
        "POINT(${geometry.coordinate.x} ${geometry.coordinate.y})"

    /** WKT-строка из базы -> JTS Point. */
    private fun parseWkt(wkt: String): Point? = try {
        (wktReader.read(wkt) as? Point)   // обычное приведение с проверкой
    } catch (e: Exception) {
        Log.e("HazardRepository", "Не удалось разобрать WKT: $wkt", e)
        null
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * r * Math.asin(Math.sqrt(a))
    }
}
