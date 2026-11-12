package com.example.mobileapp.logic

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale

class Hazard (private val activity: AppCompatActivity) {

    var selectedLat: Double? = null
        private set
    var selectedLng: Double? = null
        private set

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

    private val hazardButtonsGreen = listOf(
            R.id.btnHazardGarbage,
            R.id.btnHazardAnimals,
            R.id.btnHazardTrees,
            R.id.btnHazardWater,
            R.id.btnHazardRocks,
            R.id.btnHazardFire,
            R.id.btnHazardArea
    )

    var selectedHazardId: Int? = null
        private set

    init {

        hazardButtonsGreen.forEach { id -> activity.findViewById<MaterialButton>(id).setOnClickListener { selectedHazard(id) } }

        activity.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            backMap();
        }

        activity.findViewById<View>(R.id.btnPickOnMap).setOnClickListener {
            mapPickerLauncher.launch(Intent(activity, MapPickerActivity::class.java))
        }

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