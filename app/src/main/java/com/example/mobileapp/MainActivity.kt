package com.example.mobileapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.api.MapApi
import com.example.mobileapp.data.SyncManager
import com.example.mobileapp.logic.Hazard
import com.example.mobileapp.logic.Route
import com.example.mobileapp.ui.Menu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.osmdroid.views.MapView

class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var mapApi: MapApi

    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        mapView = findViewById(R.id.mapView2)
        mapApi = MapApi(this)             // контекст передаём
        mapApi.setupMap(mapView)
        mapApi.showHazards(mapView)       // <-- и опасные участки

        menu = Menu(this)

        SyncManager.getInstance(this).syncNow(
            onSuccess = {
                // сервер ответил — при необходимости обновить данные на экране
            },
            onError = {
                showServerUnavailableDialog()
            }
        )
    }

    private fun showServerUnavailableDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Нет связи с сервером")
            .setMessage("Сервер временно недоступен, данные могут быть не актуальными.")
            .setPositiveButton("Понятно", null)
            .show()
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
        mapApi.showHazards(mapView)

        SyncManager.getInstance(this).syncNow {
            runOnUiThread { mapApi.showHazards(mapView) }
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach() // Освобождаем ресурсы
    }
}