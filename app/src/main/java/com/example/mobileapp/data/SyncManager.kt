package com.example.mobileapp.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class SyncManager private constructor(private val context: Context) {

    companion object {
//        private const val SERVER_URL = "http://10.0.2.2:5000" //для запуска на эмуляторе
        private const val SERVER_URL = "http://192.168.31.81:5000" //для запуска на реальном телефоне
        private const val PREFS = "sync"
        private const val KEY_LAST_SEQ = "last_sequence"

        @Volatile private var instance: SyncManager? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: SyncManager(context.applicationContext).also { instance = it }
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val repo = HazardRepository(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val deviceId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "dev"

    private fun lastSequence(): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST_SEQ, 0)

    /** Быстрая проверка доступности сервера (HEAD-запрос с таймаутом). */
    fun checkServerAvailable(onResult: (available: Boolean) -> Unit) {
        executor.execute {
            val available = try {
                val conn = URL(SERVER_URL).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val code = conn.responseCode
                conn.disconnect()
                code in 200..399
            } catch (e: Exception) {
                Log.e("Sync", "Сервер недоступен", e)
                false
            }
            runOnMain { onResult(available) }
        }
    }

    /**
     * Полная синхронизация.
     * onSuccess — данные получены и применены;
     * onError — сервер не ответил: нужно показать сообщение пользователю.
     */
    fun syncNow(onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        executor.execute {
            try {
                val body = JSONObject().apply {
                    put("last_sequence", lastSequence())
                    put("uploaded", JSONArray().apply {
                        repo.unsynced().forEach { h ->
                            put(JSONObject().apply {
                                put("uuid", h.uuid ?: "$deviceId-${h.id}")
                                put("area_id", h.areaId)
                                put("danger_type_id", h.dangerTypeId)
                                put("severity", h.severity)
                                put(
                                    "location",
                                    "POINT(${h.location.coordinate.x} ${h.location.coordinate.y})"
                                )
                                put("radius", 0.0)
                                put("description", h.description ?: JSONObject.NULL)
                                put("reported_at", dateFormat.format(h.reportedAt ?: Date()))
                            })
                        }
                    })
                }
                val conn = URL("$SERVER_URL/sync").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                if (conn.responseCode != 200) {
                    Log.e("Sync", "Сервер ответил ${conn.responseCode}")
                    conn.disconnect()
                    runOnMain { onError?.invoke() }
                    return@execute
                }

                val resp = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()
                applyServerData(resp)
                runOnMain { onSuccess?.invoke() }
            } catch (e: Exception) {
                Log.e("Sync", "Нет связи с сервером - работает локально", e)
                runOnMain { onError?.invoke() }
            }
        }
    }

    // executor-поток не может трогать UI — перекидываем на главный
    private fun runOnMain(block: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(block)
    }

    private fun applyServerData(resp: JSONObject) {
        val hazards = resp.getJSONArray("hazards")

        // 1. Добавляем/обновляем всё, что есть на сервере (дедуп по uuid)
        val serverUuids = HashSet<String>()
        for (i in 0 until hazards.length()) {
            val h = hazards.getJSONObject(i)
            val uuid = h.getString("uuid")
            serverUuids.add(uuid)
            val localId = repo.localIdByUuid(uuid)
            if (localId != null) {
                repo.markSynced(localId, h.getLong("sequence_number"))
            } else {
                repo.insertFromServer(h, h.getLong("sequence_number"))
            }
        }

        // 2. Удаляем локальные записи, которых на сервере больше нет
        repo.deleteNotIn(serverUuids)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_SEQ, resp.optLong("last_sequence", 0L)).apply()

        Log.d("Sync", "Зеркальная синхронизация: на сервере ${hazards.length()} записей")
    }

}