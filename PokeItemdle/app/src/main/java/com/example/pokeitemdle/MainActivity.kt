package com.example.pokeitemdle

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.example.pokeitemdle.networking.RemoteAPI
import com.example.pokeitemdle.utils.ItemDetailsFormatter
import org.json.JSONObject
import android.text.InputType
import com.example.pokeitemdle.database.DatabaseHelper
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var loadingScreen: FrameLayout
    private lateinit var mainContent: ConstraintLayout
    private var randomItem: String? = null
    private var attemptsLeft = 3 // Número de intentos iniciales hasta primera pista
    private var randomItemDetails: JSONObject? = null // Para guardar los detalles del objeto random
    private var gameOver = false // Bandera para finalizar el juego
    private var userEmail: String? = null
    private var userAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userEmail = intent.getStringExtra("email")
        if (userEmail.isNullOrEmpty()) userEmail=null
        Log.d("MainActivity", "onCreate() called")

        // Configurar el Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "PokeItemdle"

        toolbar.setOnClickListener {
            // Al hacer clic en el título del Toolbar, lanzar PokeItemdleActivity
            val intent = Intent(this, PokeItemdleActivity::class.java)
            startActivity(intent)
        }

        loadingScreen = findViewById(R.id.loadingScreen)
        mainContent = findViewById(R.id.mainContent)

        val attemptsTextView = findViewById<TextView>(R.id.attemptsTextView)
        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.PruebaTextView)
        val fetchButton = findViewById<Button>(R.id.fetchButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)
        val itemImageView = findViewById<ImageView>(R.id.itemImageView)

        val remoteAPI = RemoteAPI()

        mainContent.visibility = View.GONE
        loadingScreen.visibility = View.VISIBLE

        remoteAPI.getAllItems(
            onSuccess = { items ->
                runOnUiThread {
                    Log.d("MainActivity", "Fetched items successfully: ${items.size}")
                    if (items.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            items
                        )
                        autoCompleteTextView.setAdapter(adapter)
                        randomItem = items.random()
                        fetchRandomItemDetails(randomItem ?: "", remoteAPI)

                        // Ocultar pantalla de carga y mostrar contenido principal
                        loadingScreen.visibility = View.GONE
                        mainContent.visibility = View.VISIBLE
                    } else {
                        showToast("No se encontraron objetos.")
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("MainActivity", "Error fetching items: $errorMessage")
                showToast("Error: $errorMessage")
                loadingScreen.visibility = View.GONE // Ocultar pantalla de carga en caso de error
            }
        )

        setupAutoCompleteTextView(remoteAPI, autoCompleteTextView)
        setupFetchButton(fetchButton, autoCompleteTextView, resultTextView, itemImageView, attemptsTextView, remoteAPI)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("MainActivity", "onCreateOptionsMenu() called")
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.findItem(R.id.action_login)?.title = userEmail ?: "Login"
        menu?.findItem(R.id.action_register)?.isVisible = userEmail == null // Hide "Registrar" if logged in
        return true
    }

    private fun showRegisterDialog(onRegister: (Boolean) -> Unit) {
        Log.d("MainActivity", "showRegisterDialog: called")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registrar Usuario")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val emailInput = EditText(this).apply {
            hint = "Correo Electrónico"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = "Contraseña"
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton("Registrar") { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDB = password.hashCode();
            if (email.isNotEmpty() && password.isNotEmpty()) {
                val dbHelper = DatabaseHelper(this)
                val success = dbHelper.registerUser(email, passwordDB)
                onRegister(success)
                userEmail = email
            } else {
                showToast("Por favor completa todos los campos.")
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    private fun showLanguageDialog() {
        val languages = arrayOf("Español", "Inglés", "Francés") // Los idiomas disponibles
        AlertDialog.Builder(this)
            .setTitle("Selecciona un idioma")
            .setItems(languages) { _, which ->
                when (which) {
                    0 -> setLocale("es") // Cambiar a Español
                    1 -> setLocale("en") // Cambiar a Inglés
                    2 -> setLocale("fr") // Cambiar a Francés
                }
            }
            .create()
            .show()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate() // Recargar la actividad para aplicar el cambio de idioma
    }

    // Manejar clics en el menú
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("MainActivity", "onOptionsItemSelected: called")
        return when (item.itemId) {
            R.id.action_language -> {
                // Mostrar un cuadro de diálogo para seleccionar el idioma
                showLanguageDialog()
                true
            }
            R.id.action_login -> {
                if (userEmail.isNullOrEmpty()) {
                    showLoginDialog { email ->
                        userEmail = email
                        invalidateOptionsMenu()
                        showToast("Sesión iniciada como $userEmail")
                    }
                } else {
                    showAverageAttemptsDialog() // Mostrar promedio al hacer clic en el correo
                }
                true
            }
            R.id.action_register -> {
                if (userEmail == null) {
                    showRegisterDialog { success ->
                        if (success) showToast("Usuario registrado exitosamente.") else showToast("Error: el usuario ya existe.")
                    }
                }
                true
            }
            R.id.action_logout -> {
                userEmail = null
                invalidateOptionsMenu() // Refresh the menu
                showToast("Sesión cerrada.")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun showAverageAttemptsDialog() {
        if (!userEmail.isNullOrEmpty()) {
            val dbHelper = DatabaseHelper(this)
            val average = dbHelper.getAverageAttempts(userEmail!!)
            val message = if (average > 0) {
                "Tu número medio de intentos: %.2f".format(average)
            } else {
                "Aún no tienes suficientes datos para calcular un promedio."
            }
            AlertDialog.Builder(this)
                .setTitle("Tu promedio de intentos")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            showToast("Debes iniciar sesión para ver tu promedio de intentos.")
        }
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_register)?.isVisible = userEmail == null // Show "Registrar" if not logged in
        menu?.findItem(R.id.action_logout)?.isVisible = userEmail != null // Show "Cerrar sesión" if logged in
        menu?.findItem(R.id.action_login)?.title = userEmail ?: "Login" // Update login title
        return super.onPrepareOptionsMenu(menu)
    }


    // Function to display the login dialog
    private fun showLoginDialog(onLogin: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Iniciar Sesión")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val emailInput = EditText(this).apply {
            hint = "Correo Electrónico"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = "Contraseña"
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton("Iniciar") { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDb = password.hashCode();
            val dbHelper = DatabaseHelper(this)
            val validUser = dbHelper.loginUser(email, passwordDb)
            if (validUser) {
                onLogin(email)
            } else {
                showToast("Credenciales inválidas. Inténtalo de nuevo.")
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    private fun setupFetchButton(
        fetchButton: Button,
        autoCompleteTextView: AutoCompleteTextView,
        resultTextView: TextView,
        itemImageView: ImageView,
        attemptsTextView: TextView,
        remoteAPI: RemoteAPI
    ) {
        fetchButton.setOnClickListener {
            Log.d("MainActivity", "Fetch button clicked")
            if (gameOver) return@setOnClickListener // Si el juego ha terminado, no hacer nada
            userAttempts++

            val selectedItem = autoCompleteTextView.text.toString()
            if (selectedItem.isNotEmpty()) {
                if (selectedItem.equals(randomItem, ignoreCase = true)) {
                    // Mostrar detalles del objeto correcto
                    Log.d("MainActivity", "User selected correct item: $selectedItem")
                    fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)

                    // Guardar los intentos en la base de datos si el usuario ha iniciado sesión
                    Log.d("MainActivity", "User email: $userEmail")
                    if (!userEmail.isNullOrEmpty()) {
                        val dbHelper = DatabaseHelper(this)

                        val name = selectedItem
// Asegurarse de que randomCost no sea nulo, se establece en 0 si es nulo
                        val randomCost = randomItemDetails?.optInt("cost", 0) ?: 0
                        val randomCategory = randomItemDetails?.optJSONObject("category")?.optString("name", "Unknown") ?: "Unknown"
                        val randomFlingPower = randomItemDetails?.optInt("fling_power", 0) ?: 0

                        givenNameRecieveDetails(name, remoteAPI) { cost, category, flingPower, description ->
                            // Asegúrate de que cost es un valor Int no nulo, y compararlo con randomCost
                            val costComparison = when {
                                cost < randomCost -> "cost ^"
                                cost > randomCost -> "cost v"
                                else -> "fetchedCost" // Asegúrate de que "fetchedCost" sea una variable válida
                            }

                            // Log para depuración antes de insertar en la base de datos
                            Log.d("MainActivity", "Inserting attempt with cost: $cost, category: $category, flingPower: $flingPower, description: $description")

                            val insertSuccess = dbHelper.insertAttempt(
                                userEmail = userEmail,
                                itemName = name,
                                cost = cost.toString(), // Convierte cost a String
                                costCorrect = cost.toInt() == randomCost,
                                category = category,
                                categoryCorrect = category == randomCategory,
                                flingPower = flingPower.toString(), // Convierte flingPower a String
                                flingPowerCorrect = flingPower == randomFlingPower,
                                description = description,
                                descriptionCorrect = description == randomItemDetails?.optString("description", "")
                            )

                            // Verifica si la inserción fue exitosa
                            if (insertSuccess) {
                                Log.d("MainActivity", "Attempt inserted successfully")
                            } else {
                                Log.e("MainActivity", "Failed to insert attempt")
                            }
                        }



                        val previousAttempts = dbHelper.getTotalAttempts(userEmail!!)
                        val totalAttempts = previousAttempts + userAttempts
                        val success = dbHelper.updateGameStats(userEmail!!, totalAttempts)
                        if (!success) {
                            Log.e("MainActivity", "Error saving game stats to the database")
                            showToast("Error al guardar los datos en la base de datos.")
                        }

                        // Incrementar el total de juegos
                        dbHelper.incrementTotalGames(userEmail!!)
                    }

                    // Mostrar diálogo y finalizar juego
                    Log.d("MainActivity", "User won with $userAttempts attempts") // Log statement
                    runOnUiThread {
                        showWinningDialog(userAttempts) // Ensure this is on the UI thread
                    }
                    showToast("¡Felicidades! Has acertado.")
                    fetchButton.isEnabled = false // Deshabilitar botón tras acertar
                    gameOver = true
                } else {
                    Log.d("MainActivity", "User selected incorrect item: $selectedItem")
                    // Reducir intentos si quedan
                    if (attemptsLeft > 0) {
                        attemptsLeft -= 1
                    }

                    if (attemptsLeft > 0) {
                        val text = "Intentos restantes: $attemptsLeft"
                        attemptsTextView.text = text
                        fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)
                    } else {
                        // Dividir las pistas en parte1 y parte2
                        val effectEntries = randomItemDetails?.let { details ->
                            details.optJSONArray("effect_entries")?.let { effects ->
                                (0 until effects.length()).joinToString("\n") { index ->
                                    val effectText = effects.getJSONObject(index).optString("effect", "No effect available.")
                                    effectText.split(":").getOrNull(1)?.trim() ?: "No clue available."
                                }
                            } ?: "No clues available."
                        } ?: "No clues available."
                        val text = "Pista: $effectEntries"
                        attemptsTextView.text = text
                        fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)
                        showToast("Intento incorrecto. Sigue intentando.")
                    }
                }
                // Limpiar el texto en AutoCompleteTextView
                autoCompleteTextView.setText("")
            } else {
                showToast("Por favor, selecciona un objeto primero.")
            }
        }
    }

    private fun setupAutoCompleteTextView(
        remoteAPI: RemoteAPI,
        autoCompleteTextView: AutoCompleteTextView
    ) {
        // Already called in onCreate(), no need to call again here.
    }

    private fun fetchRandomItemDetails(itemName: String, remoteAPI: RemoteAPI) {
        Log.d("MainActivity", "Fetching details for item: $itemName")
        remoteAPI.getItemDetails(
            itemName = itemName,
            onSuccess = { details -> Log.d("MainActivity", "Fetched details for $itemName successfully")
                randomItemDetails = details
            },

            onError = { errorMessage -> Log.e("MainActivity", "Error fetching random item details: $errorMessage") }
        )
    }

    private fun givenNameRecieveDetails(itemName: String, remoteAPI: RemoteAPI, onSuccess: (Double, String, Int, String) -> Unit) {
        remoteAPI.getItemDetails(
            itemName = itemName,
            onSuccess = { itemDetails ->  // Suponiendo que itemDetails es un JSONObject
                // Extraemos los valores del JSONObject
                val cost = itemDetails.optDouble("cost")
                val category = itemDetails.optString("category")
                val flingPower = itemDetails.optInt("fling-power")
                val description = itemDetails.optString("description")

                // Llamamos al callback onSuccess con los valores extraídos
                onSuccess(cost, category, flingPower, description)
            },
            onError = { error ->
                // Manejo de errores si es necesario
                println("Error: $error")
            }
        )
    }

    private fun fetchItemDetails(
        itemName: String,
        resultTextView: TextView,
        itemImageView: ImageView,
        remoteAPI: RemoteAPI
    ) {
        Log.d("MainActivity", "Fetching item details for: $itemName")
        remoteAPI.getItemDetails(
            itemName = itemName,
            onSuccess = { details ->
                runOnUiThread {
                    Log.d("MainActivity", "Successfully fetched item details for: $itemName")
                    val randomCost = randomItemDetails?.optInt("cost", -1) ?: -1
                    val fetchedCost = details.optInt("cost", -1)

                    val formattedDetails = formatDetailsWithColors(details, randomCost, fetchedCost, resultTextView.text)
                    resultTextView.text = formattedDetails

                    val spriteUrl = details.optJSONObject("sprites")?.optString("default", "")
                    if (!spriteUrl.isNullOrEmpty()) {
                        Glide.with(this).load(spriteUrl).into(itemImageView)
                    } else {
                        itemImageView.setImageResource(R.drawable.ic_launcher_background)
                    }
                }
            },

            onError = { errorMessage ->  Log.e("MainActivity", "Error fetching item details: $errorMessage")
                showToast("Error: $errorMessage") }
        )
    }

    private fun formatDetailsWithColors(
        details: JSONObject,
        randomCost: Int,
        fetchedCost: Int,
        previousTries: CharSequence
    ): SpannableString {
        val name = details.optString("name", "Unknown")
        val cost = details.optInt("cost", 0)
        val category = details.optJSONObject("category")?.optString("name", "Unknown") ?: "Unknown"
        val randomCategory = randomItemDetails?.optJSONObject("category")?.optString("name", "Unknown") ?: "Unknown"

        val fetchedFlingPower = details.optInt("fling_power", 0)
        val randomFlingPower = randomItemDetails?.optInt("fling_power", 0) ?: 0

        val costComparison = when {
            fetchedCost < randomCost -> "$fetchedCost ^"
            fetchedCost > randomCost -> "$fetchedCost v"
            else -> "$fetchedCost"
        }

        val flingComparison = when {
            fetchedFlingPower < randomFlingPower -> "$fetchedFlingPower ^"
            fetchedFlingPower > randomFlingPower -> "$fetchedFlingPower v"
            else -> "$fetchedFlingPower"
        }

        val fetchedEffectEntries = details.optJSONArray("effect_entries")?.let { effects ->
            (0 until effects.length()).joinToString("\n") { index ->
                effects.getJSONObject(index).optString("effect", "No description available.").split(":").getOrNull(0)?.trim()
                    ?: "No description available."
            }
        } ?: "No description available."

        val randomEffectEntries = randomItemDetails?.optJSONArray("effect_entries")?.let { effects ->
            (0 until effects.length()).joinToString("\n") { index ->
                effects.getJSONObject(index).optString("effect", "No description available.").split(":").getOrNull(0)?.trim()
                    ?: "No description available."
            }
        } ?: "No description available."

        val effectEntriesColor = if (fetchedEffectEntries == randomEffectEntries) Color.GREEN else Color.RED

        // Comparaciones y símbolos ✅/❌
        val costSymbol = if (fetchedCost == randomCost) "✅" else "❌"
        val categorySymbol = if (category == randomCategory) "✅" else "❌"
        val flingSymbol = if (fetchedFlingPower == randomFlingPower) "✅" else "❌"
        val descriptionSymbol = if (fetchedEffectEntries == randomEffectEntries) "✅" else "❌"

        val formattedText = """
    Nombre: $name
    Coste: $costComparison $costSymbol
    Categoría: $category $categorySymbol
    Fling-power: $fetchedFlingPower $flingSymbol
    Descripción: $fetchedEffectEntries $descriptionSymbol
    _____________________________
    Intentos anteriores: $previousTries
    """.trimIndent()

        val spannable = SpannableString(formattedText)

        // Aplicar color a la categoría
        val categoryStartIndex = formattedText.indexOf("Categoría:") + "Categoría: ".length
        val categoryEndIndex = categoryStartIndex + category.length
        val categoryColor = if (category == randomCategory) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(categoryColor),
            categoryStartIndex,
            categoryEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color al coste
        val costStartIndex = formattedText.indexOf(costComparison)
        val costEndIndex = costStartIndex + costComparison.length
        val costColor = if (fetchedCost == randomCost) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(costColor),
            costStartIndex,
            costEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color al Fling-power
        val flingStartIndex = formattedText.indexOf("Fling-power:") + "Fling-power: ".length
        val flingEndIndex = flingStartIndex + flingComparison.length
        val flingColor = if (fetchedFlingPower == randomFlingPower) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(flingColor),
            flingStartIndex,
            flingEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color a la descripción
        val descriptionStartIndex = formattedText.indexOf("Descripción:") + "Descripción: ".length
        val descriptionEndIndex = descriptionStartIndex + fetchedEffectEntries.length
        spannable.setSpan(
            ForegroundColorSpan(effectEntriesColor),
            descriptionStartIndex,
            descriptionEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color a los intentos anteriores
        val previousTriesStartIndex = formattedText.indexOf("Intentos anteriores:") + "Intentos anteriores: ".length
        val previousTriesEndIndex = previousTriesStartIndex + previousTries.length
        spannable.setSpan(
            ForegroundColorSpan(Color.BLACK), // Optional color for previous tries
            previousTriesStartIndex,
            previousTriesEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }



    private fun showWinningDialog(attempts: Int) {
        Log.d("MainActivity", "Showing winning dialog with $attempts attempts")
        runOnUiThread {
            val dialog = AlertDialog.Builder(this)
                .setTitle("¡Ganaste!")
                .setMessage("Lo lograste en $attempts intentos.")
                .setPositiveButton("OK") { _, _ ->
                    // Reiniciar juego
                    Log.d("MainActivity", "User clicked OK to restart the game")
                    attemptsLeft = 3
                    fetchRandomItemDetails(randomItem ?: "", RemoteAPI())
                }
                .create()
            dialog.show()
        }
    }

}
