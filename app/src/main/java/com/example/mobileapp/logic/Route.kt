package com.example.mobileapp.logic

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R
import com.example.mobileapp.activity.MapPickerActivity
import com.example.mobileapp.data.DangerTypeRepository
import com.example.mobileapp.models.DangerType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class Route (private val activity: AppCompatActivity) {

    // id выбранного типа опасности
    var selectedRouteId: Int? = null
        private set

    // широта выбранной точки на карте
    var selectedLat: Double? = null
    // долгота выбранной точки на карте
    var selectedLng: Double? = null

    //список кнопок (id) типов передвижения
    private val routeButtons = listOf(
        R.id.btnModeWalk,
        R.id.btnModeBike,
        R.id.btnModeCar,
    )

    init {
        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap();
        }

        routeButtons.forEach { id -> activity.findViewById<MaterialButton>(id).setOnClickListener { selectedRoute(id) } }

        activity.findViewById<View>(R.id.btnStartPickOnMap).setOnClickListener {
            mapPickerLauncherStart.launch(Intent(activity, MapPickerActivity::class.java))
        }

        activity.findViewById<View>(R.id.btnEndPickOnMap).setOnClickListener {
            mapPickerLauncherEnd.launch(Intent(activity, MapPickerActivity::class.java))
        }
    }


    //Лаунчер экрана выбора начальной точки на карте
    //При успехе - сохранение координат и показ их на экране
    private val mapPickerLauncherStart = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0)
            if (lat != null && lng != null) {
                selectedLat = lat
                selectedLng = lng
                showStartCoordinates(lat, lng)
            }
        }
    }

    //Лаунчер экрана выбора конечной точки на карте
    //При успехе - сохранение координат и показ их на экране
    private val mapPickerLauncherEnd = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0)
            if (lat != null && lng != null) {
                selectedLat = lat
                selectedLng = lng
                showEndCoordinates(lat, lng)
            }
        }
    }

    //Показ выбранной начальной точки с координатами
    private fun showStartCoordinates(lat: Double, lng: Double) {
        val tv = activity.findViewById<TextView>(R.id.etStartCoords)
        tv.text = String.format(Locale.US, "%.6f, %.6f", lat, lng)
        tv.visibility = View.VISIBLE
    }

    //Показ выбранной точки с координатами
    private fun showEndCoordinates(lat: Double, lng: Double) {
        val tv = activity.findViewById<TextView>(R.id.etEndCoords)
        tv.text = String.format(Locale.US, "%.6f, %.6f", lat, lng)
        tv.visibility = View.VISIBLE
    }

    //Обработка нажатия кнопки вида передвижения
    private fun selectedRoute(id: Int) {
        routeButtons.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        activity.findViewById<MaterialButton>(id).isSelected = true
        selectedRouteId = id
    }

    //сброс формы в исходное состояние
    //снимаем выделение с кнопок, очищается форма
    private fun resetForm() {
        routeButtons.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        selectedRouteId = null
        selectedLat = null
        selectedLng = null
        activity.findViewById<TextInputEditText>(R.id.etDescription).setText("")
        activity.findViewById<TextView>(R.id.tvCoordinates).visibility = View.GONE

        activity.finish()
    }

    private fun backMap() {
        activity.finish()
    }
}