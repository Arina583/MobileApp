package com.example.mobileapp.logic

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mobileapp.R
import com.example.mobileapp.data.DangerTypeRepository
import com.example.mobileapp.data.Geo
import com.example.mobileapp.data.HazardRepository
import com.example.mobileapp.models.DangerType
import com.example.mobileapp.models.Hazard
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class Hazard (private val activity: AppCompatActivity) {

    var selectedHazardId: Int? = null
        private set

    var selectedLat: Double? = null
    var selectedLng: Double? = null

    private val hazardButtonsGreen = listOf(
        R.id.btnHazardGarbage,
        R.id.btnHazardAnimals,
        R.id.btnHazardTrees,
        R.id.btnHazardWater,
        R.id.btnHazardRocks,
        R.id.btnHazardFire,
        R.id.btnHazardArea
    )

    private val mapPickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
            val lng = result.data?.getDoubleExtra(MapPickerActivity.EXTRA_LNG, 0.0)
            if (lat != null && lng != null) {
                selectedLat = lat
                selectedLng = lng
                showCoordinates(lat, lng)
            }
        }
    }

    init {

        hazardButtonsGreen.forEach { id -> activity.findViewById<MaterialButton>(id).setOnClickListener { selectedHazard(id) } }

        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap();
        }

        activity.findViewById<View>(R.id.btnPickOnMap).setOnClickListener {
            mapPickerLauncher.launch(Intent(activity, MapPickerActivity::class.java))
        }

        //Обработка отправки
        activity.findViewById<View>(R.id.btnSubmit).setOnClickListener { submit() }

    }

    private fun submit() {
        val hazard = buildHazard()
        if (hazard == null) {
            // Разбираемся, чего именно не хватает, чтобы подсказать пользователю
            when {
                selectedHazardId == null -> Toast.makeText(activity, "Выберите тип опасности", Toast.LENGTH_SHORT).show()
                selectedLat == null || selectedLng == null -> Toast.makeText(activity, "Выберите место на карте", Toast.LENGTH_SHORT).show()
            }
            return
        }

        HazardRepository.add(hazard)
        Toast.makeText(activity, "Отметка сохранена", Toast.LENGTH_SHORT).show()
        resetForm()
        activity.finish()
    }

    private fun buildHazard(): Hazard? {
        val typeId = selectedHazardId ?: return null

        val lat = selectedLat ?: return null
        val lng = selectedLng ?: return null

        val description = activity.findViewById<TextInputEditText>(R.id.etDescription).text?.toString()?.trim()?.ifEmpty { null }

        return Hazard(
            areaId = 1L,
            dangerTypeId = typeFor(typeId).id,
            severity = Severity.MEDIUM,
            location = Geo.point(latitude = lat, longitude = lng),
            description = description
        )
    }

    private fun resetForm() {
        // сброс формы
        hazardButtonsGreen.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        selectedHazardId = null
        selectedLat = null
        selectedLng = null
        activity.findViewById<TextInputEditText>(R.id.etDescription).setText("")
        activity.findViewById<TextView>(R.id.tvCoordinates).visibility = View.GONE

        activity.finish()
    }

    private fun typeFor(buttonId: Int): DangerType = when (buttonId) {
        R.id.btnHazardGarbage -> DangerTypeRepository.GARBAGE
        R.id.btnHazardAnimals -> DangerTypeRepository.ANIMALS
        R.id.btnHazardTrees -> DangerTypeRepository.TREES
        R.id.btnHazardWater -> DangerTypeRepository.WATER
        R.id.btnHazardRocks -> DangerTypeRepository.ROCKS
        R.id.btnHazardFire -> DangerTypeRepository.FIRE
        R.id.btnHazardArea -> DangerTypeRepository.AREA
        else -> error("Неизвестная кнопка типа: $buttonId")
    }

    private fun selectedHazard(id: Int) {
        hazardButtonsGreen.forEach { activity.findViewById<MaterialButton>(it).isSelected = false }
        activity.findViewById<MaterialButton>(id).isSelected = true
        selectedHazardId = id
    }

    private fun showCoordinates(lat: Double, lng: Double) {
        val tv = activity.findViewById<TextView>(R.id.tvCoordinates)
        tv.text = String.format(Locale.US, "Выбранная точка: %.6f, %.6f", lat, lng)
        tv.visibility = View.VISIBLE
    }

    private fun backMap() {
        activity.finish()
    }
}