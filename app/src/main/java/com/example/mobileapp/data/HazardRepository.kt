package com.example.mobileapp.data

import android.util.Log
import com.example.mobileapp.models.Hazard
import java.util.Date

object HazardRepository {
    private val hazards = mutableListOf<Hazard>()
    private var nextId: Long = 1 //имитации автоматического id

    fun add(hazard: Hazard): Hazard {
        val saved = hazard.copy(id = nextId, reportedAt = Date())
        nextId++
        hazards.add(hazard)
        Log.d("HazardRepository", "Сохранено #${saved.id}: тип=${saved.dangerTypeId}, " +
                "точка=(${saved.location.coordinate.y}, ${saved.location.coordinate.x}), " +
                "описание=${saved.description ?: "—"}")
        return saved
    }

    fun getAll(): List<Hazard> = hazards.toList()

    fun byArea(areaId: Long): List<Hazard> = hazards.filter { it.areaId == areaId }

    fun byType(typeId: Long): List<Hazard> = hazards.filter { it.dangerTypeId == typeId }

    fun count(): Int = hazards.size

    fun clear() {
        hazards.clear()
        nextId = 1
    }
}