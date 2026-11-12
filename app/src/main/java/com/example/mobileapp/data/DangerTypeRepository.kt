package com.example.mobileapp.data

import com.example.mobileapp.models.DangerType

object DangerTypeRepository {

    val GARBAGE = DangerType(id = 1L, name = "Мусор")
    val ANIMALS = DangerType(id = 2L, name = "Дикие животные")
    val TREES = DangerType(id = 3L, name = "Поваленные деревья")
    val WATER = DangerType(id = 4L, name = "Продавины с водой")
    val ROCKS = DangerType(id = 5L, name = "Камнепад")
    val FIRE = DangerType(id = 6L, name = "Риск пожара")
    val AREA = DangerType(id = 7L, name = "Опасный участок")

    private val all = listOf(GARBAGE, ANIMALS, TREES, WATER, ROCKS, FIRE, AREA)

    fun getAll(): List<DangerType> = all

    fun byId(id: Long?): DangerType? = all.find { it.id == id }

    fun nameFor(id: Long?): String = byId(id)?.name ?: "Не указано"
}