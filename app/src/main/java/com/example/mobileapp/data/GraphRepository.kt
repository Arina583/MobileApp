package com.example.mobileapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mobileapp.logic.TrailGraph
import java.io.File

object GraphRepository {
    private const val DB_NAME = "trails.db"
    private var db: SQLiteDatabase? = null

    @Volatile
    private var graph: TrailGraph? = null

    fun open(context: Context): SQLiteDatabase {
        db?.let { return it }
        val outFile = File(context.filesDir, DB_NAME)

        context.assets.open(DB_NAME).use { input ->
            outFile.outputStream().use { input.copyTo(it) }
        }
        Log.d("GraphRepository", "База скопирована из assets: ${outFile.length()} байт")

        db = SQLiteDatabase.openDatabase(outFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        return db!!
    }


    /**
     * Возвращает граф, загружая его из базы один раз за жизнь процесса.
     * Безопасен при параллельных вызовах: двойная проверка под synchronized.
     */
    fun openGraph(context: Context): TrailGraph {
        graph?.let { return it }
        return synchronized(this) {
            graph ?: TrailGraph.load(open(context)).also { graph = it }
        }
    }
}