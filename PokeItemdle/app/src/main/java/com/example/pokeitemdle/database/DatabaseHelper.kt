package com.example.pokeitemdle.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 2

        // Table and column names
        const val TABLE_USERS = "users"
        const val COLUMN_ID = "id"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_GAMES_PLAYED = "games_played"
        const val COLUMN_TOTAL_PLAYED = "total_attempts"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
    CREATE TABLE $TABLE_USERS (
        $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COLUMN_EMAIL TEXT UNIQUE,
        $COLUMN_PASSWORD INTEGER,
        $COLUMN_GAMES_PLAYED INTEGER DEFAULT 0,
        $COLUMN_TOTAL_PLAYED INTEGER DEFAULT 0
    )
""".trimIndent()

        db.execSQL(createTableQuery)
    }
    fun getTotalAttempts(email: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT total_attempts FROM users WHERE email = ?", arrayOf(email))
        return if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else {
            0 // Si no hay registros, retornar 0
        }.also {
            cursor.close()
        }
    }

    fun incrementTotalGames(email: String) {
        val db = this.writableDatabase
        db.execSQL("UPDATE users SET games_played = games_played + 1 WHERE email = ?", arrayOf(email))
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }
    // Función para actualizar estadísticas del juego
    fun updateGameStats(email: String, attempts: Int): Boolean {
        val db = writableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        if (cursor.moveToFirst()) {
            val gamesPlayed = cursor.getInt(cursor.getColumnIndexOrThrow("games_played"))
            val totalAttempts = cursor.getInt(cursor.getColumnIndexOrThrow("total_attempts"))
            val updatedGamesPlayed = gamesPlayed + 1
            val updatedTotalAttempts = totalAttempts + attempts

            val values = ContentValues().apply {
                put("games_played", updatedGamesPlayed)
                put("total_attempts", updatedTotalAttempts)
            }

            val rowsUpdated = db.update(TABLE_USERS, values, "$COLUMN_EMAIL = ?", arrayOf(email))
            cursor.close()
            return rowsUpdated > 0
        } else {
            cursor.close()
            return false
        }
    }

    fun getUserStats(email: String): Pair<Int, Int> {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        return if (cursor.moveToFirst()) {
            val gamesPlayed = cursor.getInt(cursor.getColumnIndexOrThrow("games_played"))
            val totalAttempts = cursor.getInt(cursor.getColumnIndexOrThrow("total_attempts"))
            Pair(gamesPlayed, totalAttempts)
        } else {
            Pair(0, 0)
        }.also { cursor.close() }
    }

    fun getAverageAttempts(email: String): Float {
        val db = readableDatabase
        val query = "SELECT total_attempts, games_played FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        var average = 0f
        if (cursor.moveToFirst()) {
            val totalAttempts = cursor.getInt(0)
            val gamesPlayed = cursor.getInt(1)
            if (gamesPlayed > 0) {
                average = totalAttempts.toFloat() / gamesPlayed
            }
        } else {
            average = 0f // Manejo cuando no se encuentran datos
        }
        cursor.close()
        return average
    }


    // Insert a new user
    fun registerUser(email: String, password: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_EMAIL, email)
        values.put(COLUMN_PASSWORD, password)

        return try {
            db.insertOrThrow(TABLE_USERS, null, values)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Validate login
    fun loginUser(email: String, password: Int): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(email, password.toString()))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}
