package com.example.mobileapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.FileOutputStream

class DatabaseHelper private constructor(private var context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "forest_route_db.db"
        const val DATABASE_VERSION = 1

        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
    }

    init {
        // Копируем готовую базу ДО того, как helper создаст и откроет файл
        copyDatabaseFromAssetsIfNeeded()
    }

    override fun onCreate(db: SQLiteDatabase) {
        // База уже скопирована из assets в init — здесь делать ничего не нужно
        Log.d("DatabaseHelper", "onCreate вызван, база уже на месте")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "Обновление базы: $oldVersion -> $newVersion")
        // Пока схема готовая из assets — здесь ничего не делаем.
        // При изменении схемы: поднять DATABASE_VERSION и скопировать новый файл (см. примечание ниже)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
    }

    private fun copyDatabaseFromAssetsIfNeeded() {
        val outFile = context.getDatabasePath(DATABASE_NAME)

        if (outFile.exists()) {
            Log.d("DatabaseHelper", "База уже существует: ${outFile.absolutePath} (${outFile.length()} байт)")
            return
        }

        outFile.parentFile?.mkdirs()

        try {
            context.assets.open(DATABASE_NAME).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output, bufferSize = 1024 * 512)
                }
            }
            Log.d("DatabaseHelper", "База скопирована из assets: ${outFile.absolutePath}, ${outFile.length()} байт")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Не удалось скопировать базу из assets", e)
            outFile.delete() // не оставляем битый пустой файл
            throw e
        }
    }
}
