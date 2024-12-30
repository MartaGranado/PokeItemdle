package com.example.pokeitemdle.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 6

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
        // Agregar esta tabla en onCreate de la base de datos
        val createTableQuery2 = """
    CREATE TABLE IF NOT EXISTS attempts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_email TEXT,
        item_name TEXT,
        cost TEXT,
        category TEXT,
        fling_power TEXT,
        description TEXT
    )
""".trimIndent()
        db.execSQL(createTableQuery2)

        val createTableQuery3 = """
    CREATE TABLE IF NOT EXISTS attemptsMoves (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        user_email TEXT,
        move_name TEXT,
        type_name TEXT,
        power TEXT,
        accuracy TEXT,
        damage_class TEXT,
        description TEXT
    )
""".trimIndent()
        db.execSQL(createTableQuery3)

        val createTableQuery4 = """
    CREATE TABLE IF NOT EXISTS objectTable (
        object TEXT,
        attempts INTEGER
    )
""".trimIndent()
        db.execSQL(createTableQuery4)

        val createTableQuery5 = """
    CREATE TABLE IF NOT EXISTS move (
        move TEXT, 
        attempts INTEGER
    )
""".trimIndent()
        db.execSQL(createTableQuery5)
    }

    fun insertObject(
        objectToInsert: String?,
    ) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply { put("object",objectToInsert)}
        db.insert("objectTable", null,  contentValues)
    }

    fun insertAttemptsObject(
        attemptsToInsert: Int,
        object2: String?
        ){
        val db = this.writableDatabase
        val contentValues = ContentValues().apply { put("attempts", attemptsToInsert) }
        db.update("objectTable", contentValues, "object = ?", arrayOf(object2))

    }

    fun insertMove(
        moveToInsert:String?,
    ) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply { put("move",moveToInsert)}
        db.insert("move", null,  contentValues)
    }

    fun insertAttemptsMove(
        attemptsToInsert: Int,
        move2: String?
    ){
        val db = this.writableDatabase
        val contentValues = ContentValues().apply { put("attempts", attemptsToInsert) }
        db.update("move", contentValues, "move = ?", arrayOf(move2))

    }

    fun getObject(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM objectTable", null)
        return if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null // Si no hay registros, retornar 0
        }.also {
            cursor.close()
        }
    }

    fun getAttemptsObject(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT attempts FROM objectTable", null)
        return if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else {
            0 // Si no hay registros, retornar 0
        }.also {
            cursor.close()
        }
    }

    fun getMove(): String? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM move", null)
        return if (cursor.moveToFirst()) {
            cursor.getString(0)
        } else {
            null // Si no hay registros, retornar 0
        }.also {
            cursor.close()
        }
    }

    fun getAttemptsMove(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT attempts FROM move", null)
        return if (cursor.moveToFirst()) {
            cursor.getInt(0)
        } else {
            -1 // Si no hay registros, retornar -1
        }.also {
            cursor.close()
        }
    }

    fun dropObjectTable(){
        val db = this.writableDatabase
        db.execSQL("DELETE FROM objectTable")
    }

    fun dropMove(){
        val db = this.writableDatabase
        db.execSQL("DELETE FROM move")
    }
    // Método para insertar un intento
    fun insertAttempt(
        userEmail: String?,
        itemName: String,
        cost: String,
        costCorrect: Boolean,
        category: String,
        categoryCorrect: Boolean,
        flingPower: String,
        flingPowerCorrect: Boolean,
        description: String,
        descriptionCorrect: Boolean
    ): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("user_email", userEmail)
            put("item_name", itemName)
            put("cost", cost)
            put("cost_correct", if (costCorrect) "✅" else "❌")
            put("category", category)
            put("category_correct", if (categoryCorrect) "✅" else "❌")
            put("fling_power", flingPower)
            put("fling_power_correct", if (flingPowerCorrect) "✅" else "❌")
            put("description", description)
            put("description_correct", if (descriptionCorrect) "✅" else "❌")
        }
        val result = db.insert("attempts", null, contentValues)
        return result != -1L
    }

    fun insertAttemptMoves(
        userEmail: String?,
        moveName: String,
        type: String,
        typeCorrect: Boolean,
        power: String,
        powerCorrect: Boolean,
        accuracy: String,
        accuracyCorrect: Boolean,
        damageClass: String,
        damageClassCorrect: Boolean,
        description: String,
        descriptionCorrect: Boolean
    ): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("user_email", userEmail)
            put("move_name", moveName)
            put("type", type)
            put("type_correct", if (typeCorrect) "✅" else "❌")
            put("power", power)
            put("power_correct", if (powerCorrect) "✅" else "❌")
            put("accuracy", accuracy)
            put("accuracy_correct", if (accuracyCorrect) "✅" else "❌")
            put("damage_class", damageClass)
            put("damage_class_correct", if (damageClassCorrect) "✅" else "❌")
            put("description", description)
            put("description_correct", if (descriptionCorrect) "✅" else "❌")
        }
        val result = db.insert("attempts", null, contentValues)
        return result != -1L
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
    fun clearAttempts() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM attempts")
    }


    fun incrementTotalGames(email: String) {
        val db = this.writableDatabase
        db.execSQL("UPDATE users SET games_played = games_played + 1 WHERE email = ?", arrayOf(email))
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
        if (oldVersion < 2) {
            val createTableQuery2 = """
        CREATE TABLE IF NOT EXISTS attempts (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_email TEXT,
            item_name TEXT,
            cost TEXT,
            category TEXT,
            fling_power TEXT,
            description TEXT
        )
        """.trimIndent()
            db.execSQL(createTableQuery2)
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE attempts ADD COLUMN cost_correct TEXT DEFAULT '❌'")
            db.execSQL("ALTER TABLE attempts ADD COLUMN category_correct TEXT DEFAULT '❌'")
            db.execSQL("ALTER TABLE attempts ADD COLUMN fling_power_correct TEXT DEFAULT '❌'")
            db.execSQL("ALTER TABLE attempts ADD COLUMN description_correct TEXT DEFAULT '❌'")
        }
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
