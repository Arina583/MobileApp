package com.example.mobileapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mobileapp.models.Hazard
import org.json.JSONObject
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
            put("uuid", hazard.uuid)
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
                      h.location, h.radius, h.status_id, h.description, h.reported_at, h.uuid
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

    private fun readHazard(cursor: android.database.Cursor): Hazard? {
        fun col(name: String) = cursor.getColumnIndexOrThrow(name)

        val id = cursor.getLong(col("id"))
        val areaId = cursor.getLong(col("area_id"))
        val dangerTypeId = cursor.getLong(col("danger_type_id"))
        val severity = cursor.getInt(col("severity"))
        val wkt = cursor.getString(col("location"))
        val radius = cursor.getDouble(col("radius"))
        val statusId = cursor.getLong(col("status_id"))
        val description = cursor.getString(col("description"))
        val reportedAt = cursor.getString(col("reported_at"))
        val uuid = cursor.getString(col("uuid"))

        val point = parseWkt(wkt) ?: return null

        return Hazard(
            id = id,
            areaId = areaId,
            dangerTypeId = dangerTypeId,
            severity = severity,
            location = point,
            description = description,
            reportedAt = reportedAt?.let { dateFormat.parse(it) },
            uuid = uuid
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

    //Для работы с сервером
    /** Записи, ещё не выгруженные на сервер. */
    fun unsynced(): List<Hazard> {
        val result = mutableListOf<Hazard>()
        db.rawQuery("""SELECT id, area_id, danger_type_id, severity, location,
                          radius, status_id, description, reported_at, uuid
                   FROM Hazard WHERE sequence_number = -1
                   ORDER BY id""", null).use { c ->
            while (c.moveToNext()) readHazard(c)?.let { result += it }
        }
        return result
    }

    /** Пометить запись как выгруженную (серверный номер вместо -1). */
    fun markSynced(localId: Long, sequence: Long) {
        db.execSQL("UPDATE Hazard SET sequence_number = ? WHERE id = ?",
            arrayOf(sequence, localId))
    }

    /** Есть ли локально запись, пришедшая с сервера (по uuid). */
    fun localIdByUuid(uuid: String): Long? =
        db.rawQuery("SELECT id FROM Hazard WHERE uuid = ?", arrayOf(uuid)).use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }

    /** Вставка чужой метки, полученной с сервера. */
    fun insertFromServer(h: JSONObject, sequence: Long): Long {
        val values = ContentValues().apply {
            put("area_id", h.getLong("area_id"))
            put("danger_type_id", h.getLong("danger_type_id"))
            put("severity", h.getInt("severity"))
            put("location", h.getString("location"))
            put("radius", h.getDouble("radius"))
            put("status_id", h.getInt("status_id"))
            put("description", h.optString("description", ""))
            put("reported_at", h.getString("reported_at"))
            put("sequence_number", sequence)
            put("uuid", h.getString("uuid"))
        }
        return db.insert("Hazard", null, values)
    }

    /** Удаляет локальные записи, отсутствующие в наборе uuid сервера.
     *  Несинхронизированные локальные записи (sequence_number = -1) не трогаем —
     *  они уйдут на сервер при следующем синке. */
    fun deleteNotIn(serverUuids: Set<String>) {
        val unsyncedIds = unsynced().map { it.id }.toSet()
        val toDelete = ArrayList<Long>()

        db.rawQuery("SELECT id, uuid FROM Hazard", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val uuid = c.getString(1)
                if (id !in unsyncedIds && (uuid == null || uuid !in serverUuids)) {
                    toDelete.add(id)
                }
            }
        }

        for (id in toDelete) {
            db.delete("Hazard", "id = ?", arrayOf(id.toString()))
        }
        Log.d("HazardRepository", "Зеркальная синхронизация: удалено устаревших записей: ${toDelete.size}")
    }


}
