package com.example.mobileapp.logic

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R
import com.example.mobileapp.data.DangerTypeRepository
import com.example.mobileapp.models.DangerType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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