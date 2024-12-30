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
import org.json.JSONObject
import android.text.InputType
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import com.example.pokeitemdle.database.DatabaseHelper
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private lateinit var loadingScreen: FrameLayout
    private lateinit var mainContent: ConstraintLayout
    private var randomItem: String? = null
    private var attemptsLeft = 3
    private var randomItemDetails: JSONObject? = null
    private var gameOver = false
    private var userEmail: String? = null
    private var userAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userEmail = intent.getStringExtra("email")
        if (userEmail.isNullOrEmpty()) userEmail = null
        Log.d("MainActivity", "onCreate() called")

        // Toolbar setup
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "PokeItemdle"

        toolbar.setOnClickListener {
            val dbHelper = DatabaseHelper(this)
            dbHelper.insertAttemptsObject(userAttempts, randomItem)
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

        var isActionPerformed = false

        autoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && !isActionPerformed) {
                isActionPerformed = true
                val adapter = autoCompleteTextView.adapter
                if (adapter != null && adapter.count > 0) {
                    val firstSuggestion = adapter.getItem(0) as? String
                    if (!firstSuggestion.isNullOrEmpty() && autoCompleteTextView.text.toString() != firstSuggestion) {
                        autoCompleteTextView.setText(firstSuggestion)
                        autoCompleteTextView.setSelection(firstSuggestion.length)
                    }
                }

                autoCompleteTextView.dismissDropDown()
                fetchButton.performClick()

                autoCompleteTextView.postDelayed({
                    isActionPerformed = false
                }, 300)

                true
            } else {
                false
            }
        }

        val remoteAPI = RemoteAPI()

        mainContent.visibility = View.GONE
        loadingScreen.visibility = View.VISIBLE

        // Start rotation animation on loading screen image
        val loadingImageView = findViewById<ImageView>(R.id.loadingImageView)
        val rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.rotation_animation)
        loadingImageView.startAnimation(rotationAnimation)

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
                        val dbHelper = DatabaseHelper(this)
                        if (dbHelper.getObject() != null) {
                            randomItem = dbHelper.getObject()
                            userAttempts = dbHelper.getAttemptsObject()
                            if (userAttempts >= attemptsLeft) {
                                fetchRandomItemDetails(randomItem ?: "", remoteAPI)
                                attemptsLeft = 0
                            } else {
                                attemptsLeft -= userAttempts
                            }
                        } else {
                            randomItem = items.random()
                            dbHelper.insertObject(randomItem)
                        }
                        if(attemptsLeft != 0) attemptsTextView.text = String.format(getString(R.string.remaining_attempts), attemptsLeft)
                        fetchRandomItemDetails(randomItem ?: "", remoteAPI)

                        // Hide loading screen and show main content
                        loadingScreen.visibility = View.GONE
                        mainContent.visibility = View.VISIBLE
                    } else {
                        showToast(getString(R.string.no_found))
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("MainActivity", "Error fetching items: $errorMessage")
                showToast("Error: $errorMessage")
                loadingScreen.visibility = View.GONE // Hide loading screen on error
            }
        )

        setupAutoCompleteTextView(remoteAPI, autoCompleteTextView)
        setupFetchButton(fetchButton, autoCompleteTextView, resultTextView, itemImageView, attemptsTextView, remoteAPI)
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("MainActivity", "onCreateOptionsMenu() called")
        menuInflater.inflate(R.menu.main_menu, menu)
        menu?.findItem(R.id.action_login)?.title = userEmail ?: getString(R.string.login_button)
        menu?.findItem(R.id.action_register)?.isVisible = userEmail == null // Hide "Registrar" if logged in
        return true
    }

    private fun showRegisterDialog(onRegister: (Boolean) -> Unit) {
        Log.d("MainActivity", "showRegisterDialog: called")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.register_button)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val emailInput = EditText(this).apply {
            hint = R.string.hint_email.toString()
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = R.string.hint_password.toString()
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton(R.string.register_button) { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDB = password.hashCode();
            if(validateInputs(email, password)){
                val dbHelper = DatabaseHelper(this)
                val success = dbHelper.registerUser(email, passwordDB)
                onRegister(success)
                userEmail = email
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }


    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.spanish), getString(R.string.english), getString(R.string.french)) // Los idiomas disponibles
        AlertDialog.Builder(this)
            .setTitle(R.string.select_language)
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
                        showToast(String.format(getString(R.string.Session_started), userEmail))
                    }
                } else {
                    showAverageAttemptsDialog() // Mostrar promedio al hacer clic en el correo
                }
                true
            }
            R.id.action_register -> {
                if (userEmail == null) {
                    showRegisterDialog { success ->
                        if (success) showToast(getString(R.string.toast_registered)) else showToast(getString(R.string.toast_user_exists))
                    }
                }
                true
            }
            R.id.action_logout -> {
                userEmail = null
                invalidateOptionsMenu() // Refresh the menu
                showToast(getString(R.string.toast_logged_out))
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
                String.format(getString(R.string.average_attempts), average)
            } else {
                getString(R.string.not_enough_data)
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.average_tries)
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            showToast(getString(R.string.toast_must_log))
        }
    }
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_register)?.isVisible = userEmail == null // Show "Registrar" if not logged in
        menu?.findItem(R.id.action_logout)?.isVisible = userEmail != null // Show "Cerrar sesión" if logged in
        menu?.findItem(R.id.action_login)?.title = userEmail ?: getString(R.string.login_button) // Update login title
        return super.onPrepareOptionsMenu(menu)
    }


    // Function to display the login dialog
    private fun showLoginDialog(onLogin: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.login_button))

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val emailInput = EditText(this).apply {
            hint = getString(R.string.hint_email)
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = getString(R.string.hint_password)
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton(getString(R.string.login_button)) { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDb = password.hashCode()
            val dbHelper = DatabaseHelper(this)
            val validUser = dbHelper.loginUser(email, passwordDb)
            if (validUser) {
                onLogin(email)
            } else {
                showToast(getString(R.string.error_invalid_credentials))
            }
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }

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
            if (gameOver) return@setOnClickListener // Si el juego ha terminado, no hacer nad

            val selectedItem = autoCompleteTextView.text.toString()

            if (selectedItem.isNotEmpty()) {
                userAttempts++
                if (selectedItem.equals(randomItem, ignoreCase = true)) {
                    // Mostrar detalles del objeto correcto
                    Log.d("MainActivity", "User selected correct item: $selectedItem")
                    fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)

                    // Guardar los intentos en la base de datos si el usuario ha iniciado sesión
                    Log.d("MainActivity", "User email: $userEmail")
                    if (!userEmail.isNullOrEmpty()) {
                        val dbHelper = DatabaseHelper(this)

                        // Asegurarse de que randomCost no sea nulo, se establece en 0 si es nulo
                        val randomCost = randomItemDetails?.optInt("cost", 0) ?: 0
                        val randomCategory = randomItemDetails?.optJSONObject("category")?.optString("name", getString(R.string.unknown)) ?: getString(R.string.unknown)
                        val randomFlingPower = randomItemDetails?.optInt("fling_power", 0) ?: 0

                        givenNameRecieveDetails(
                            selectedItem,
                            remoteAPI
                        ) { cost, category, flingPower, description ->
                            // Asegúrate de que cost es un valor Int no nulo, y compararlo con randomCost
                            val costComparison = when {
                                cost < randomCost -> getString(R.string.cost) + " ^"
                                cost > randomCost -> getString(R.string.cost) + " v"
                                else -> "fetchedCost" // Asegúrate de que "fetchedCost" sea una variable válida
                            }

                            // Log para depuración antes de insertar en la base de datos
                            Log.d(
                                "MainActivity",
                                "Inserting attempt with cost: $cost, category: $category, flingPower: $flingPower, description: $description"
                            )

                            val insertSuccess = dbHelper.insertAttempt(
                                userEmail = userEmail,
                                itemName = selectedItem,
                                cost = cost.toString(), // Convierte cost a String
                                costCorrect = cost.toInt() == randomCost,
                                category = category,
                                categoryCorrect = category == randomCategory,
                                flingPower = flingPower.toString(), // Convierte flingPower a String
                                flingPowerCorrect = flingPower == randomFlingPower,
                                description = description,
                                descriptionCorrect = description == randomItemDetails?.optString(
                                    "description",
                                    ""
                                )
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
                            showToast(getString(R.string.error_data))
                        }

                        // Incrementar el total de juegos
                        dbHelper.incrementTotalGames(userEmail!!)
                    }

                    // Mostrar diálogo y finalizar juego
                    Log.d("MainActivity", "User won with $userAttempts attempts") // Log statement
                    runOnUiThread {
                        showWinningDialog(userAttempts) // Ensure this is on the UI thread
                    }
                    showToast(getString(R.string.you_won))
                    fetchButton.isEnabled = false // Deshabilitar botón tras acertar
                    gameOver = true
                } else {
                    Log.d("MainActivity", "User selected incorrect item: $selectedItem")
                    // Reducir intentos si quedan
                    if (attemptsLeft > 0) {
                        attemptsLeft -= 1
                    }

                    if (attemptsLeft > 0) {
                        attemptsTextView.text = String.format(getString(R.string.remaining_attempts), attemptsLeft)
                        fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)
                    } else {
                        // Dividir las pistas en parte1 y parte2
                        val effectEntries = randomItemDetails?.let { details ->
                            details.optJSONArray("effect_entries")?.let { effects ->
                                (0 until effects.length()).joinToString("\n") { index ->
                                    val effectText = effects.getJSONObject(index).optString("effect", "No effect available.")
                                    effectText.split(":").getOrNull(1)?.trim() ?: getString(R.string.no_clue)
                                }
                            } ?: getString(R.string.no_clue)
                        } ?: getString(R.string.no_clue)
                        val text = getString(R.string.Clue) + ": $effectEntries"
                        attemptsTextView.text = text
                        fetchItemDetails(selectedItem, resultTextView, itemImageView, remoteAPI)
                        showToast(getString(R.string.try_again))
                    }
                }
                // Limpiar el texto en AutoCompleteTextView
                autoCompleteTextView.setText("")
            } else {
                showToast(getString(R.string.select_object))
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

        val formattedText = getString(
            R.string.formatted_text_description,
            name,
            costComparison, costSymbol,
            category, categorySymbol,
            flingComparison, flingSymbol,
            fetchedEffectEntries, descriptionSymbol,
            previousTries
        )

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
        val flingStartIndex = formattedText.indexOf(flingComparison)
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
                .setTitle(R.string.you_won)
                .setMessage(getString(R.string.winning_dialog))
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

    private fun validateInputs(email:String, password: String): Boolean {
        // Validación del email
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com\$")
        if (!emailRegex.matches(email)) {
            Toast.makeText(this,R.string.wrong_email, Toast.LENGTH_SHORT).show()
            return false
        }

        // Validación de la contraseña
        val passwordRegex = Regex("^(?=.*[A-Z])(?=.*\\d).{6,}\$")
        if (!passwordRegex.matches(password)) {
            Toast.makeText(this,R.string.wrong_password, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}
