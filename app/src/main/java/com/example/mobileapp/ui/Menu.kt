package com.example.mobileapp.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import com.example.mobileapp.MainActivity
import com.example.mobileapp.R
import com.example.mobileapp.logic.HazardActivity
import com.example.mobileapp.logic.RouteActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Menu(private val activity: Activity) {

    private val fabMenu: FloatingActionButton =
        activity.findViewById(R.id.fabMenu)
    private val actionRow: LinearLayout =
        activity.findViewById(R.id.actionRow)

    private var isMenuOpen = false
    private val menuButtons = mutableListOf<View>()

    init {
        // Заполняем список кнопок и задаём стартовую позицию панели
        actionRow.post {
            for (i in 0 until actionRow.childCount) {
                menuButtons.add(actionRow.getChildAt(i))
            }

            // Точка роста — левый край панели (у самой FAB)
            actionRow.pivotX = 0f
            actionRow.pivotY = actionRow.height / 2f
            actionRow.scaleX = 0f
            actionRow.alpha = 0f

            for (btn in menuButtons) {
                btn.alpha = 0f
                btn.visibility = View.INVISIBLE
            }
            actionRow.visibility = View.VISIBLE
        }

        fabMenu.setOnClickListener {
            if (!isMenuOpen) openMenu() else closeMenu()
        }

        // Обработчики кнопок
        activity.findViewById<ImageButton>(R.id.btnMap).setOnClickListener {
            closeMenu()
            openMap()
        }
        activity.findViewById<ImageButton>(R.id.btnHazards).setOnClickListener {
            closeMenu()
            openHazard()
        }
        activity.findViewById<ImageButton>(R.id.btnRoute).setOnClickListener {
            closeMenu()
            openRoute()
        }
        activity.findViewById<ImageButton>(R.id.btnCloudy).setOnClickListener {
            closeMenu()
            openCloudy()
        }
        activity.findViewById<ImageButton>(R.id.btnHistory).setOnClickListener {
            closeMenu()
            openHistory()
        }
    }

    private fun openMenu() {
        isMenuOpen = true

        // Панель разворачивается из кнопки вправо
        actionRow.animate()
            .scaleX(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Кнопки проявляются каскадом
        menuButtons.forEachIndexed { index, btn ->
            btn.visibility = View.VISIBLE
            btn.animate()
                .alpha(1f)
                .setStartDelay(index * 60L)
                .setDuration(200)
                .start()
        }
    }

    private fun closeMenu() {
        isMenuOpen = false

        menuButtons.reversed().forEachIndexed { index, btn ->
            btn.animate()
                .alpha(0f)
                .setStartDelay(index * 30L)
                .setDuration(150)
                .withEndAction {
                    if (!isMenuOpen) btn.visibility = View.INVISIBLE
                }
                .start()
        }

        actionRow.animate()
            .scaleX(0f)
            .alpha(0f)
            .setStartDelay(80L)
            .setDuration(200)
            .setInterpolator(AccelerateInterpolator())
            .start()
    }

    private fun openMap() {
        val intent = Intent(activity, MainActivity::class.java)
        activity.startActivity(intent)
    }

    private fun openRoute() {
        val intent = Intent(activity, RouteActivity::class.java)
        activity.startActivity(intent)
    }

    private fun openHazard() {
        val intent = Intent(activity, HazardActivity::class.java)
        activity.startActivity(intent)
    }

    private fun openCloudy() {
        val intent = Intent(activity, ActivityCloudy::class.java)
        activity.startActivity(intent)
    }

    private fun openHistory() {
        val intent = Intent(activity, ActivityHistory::class.java)
        activity.startActivity(intent)
    }
}