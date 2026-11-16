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
        // Эмулятор: 10.0.2.2 — это ваш компьютер.
        // Реальный телефон в одной Wi-Fi сети: IP компьютера из ipconfig.
        private const val SERVER_URL = "http://10.0.2.2:5000"
        /*private const val SERVER_URL = "http://192.168.0.16:5000"*/
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

    fun syncNow(onDone: (() -> Unit)? = null) {
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
                    return@execute
                }

                val resp = JSONObject(conn.inputStream.bufferedReader().readText())
                conn.disconnect()
                applyServerData(resp)
                onDone?.invoke()
            } catch (e: Exception) {
                Log.e("Sync", "Нет связи с сервером - работает локально", e)
            }
        }
    }

    private fun applyServerData(resp: JSONObject) {
        val hazards = resp.getJSONArray("hazards")
        for (i in 0 until hazards.length()) {
            val h = hazards.getJSONObject(i)
            val localId = repo.localIdByUuid(h.getString("uuid"))
            if (localId != null) {
                repo.markSynced(localId, h.getLong("sequence_number"))
            } else {
                repo.insertFromServer(h, h.getLong("sequence_number"))
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_SEQ, resp.getLong("last_sequence")).apply()
        Log.d("Sync", "Синхронизация завершена, получено записей: ${hazards.length()}")
    }

}