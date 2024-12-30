package com.example.pokeitemdle

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.ContactsContract.Data
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
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import com.example.pokeitemdle.database.DatabaseHelper
import java.util.Locale


class DescriptionActivity : AppCompatActivity() {
    private lateinit var loadingScreen: FrameLayout
    private lateinit var mainContent: ConstraintLayout
    private var randomMove: String? = null
    private var randomMoveDetails: JSONObject? = null // Para guardar los detalles del movimiento random
    private var gameOver = false // Bandera para finalizar el juego
    private var userEmail: String? = null
    private var userAttempts = 0
    private lateinit var attemptsTextView: TextView
    private lateinit var hintCountdownTextView: TextView
    private var attemptsRemaining = 20
    private var attemptsUntilHint = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)
        userEmail = intent.getStringExtra("email")
        if (userEmail.isNullOrEmpty()) userEmail = null
        Log.d("DescriptionActivity", "onCreate() called")

        // Set up the Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "PokeItemdle"

        attemptsTextView = findViewById(R.id.attemptsTextView)
        hintCountdownTextView = findViewById(R.id.hintCountdownTextView)
        hintCountdownTextView.text = String.format(getString(R.string.intentos_restantes), attemptsRemaining)

        toolbar.setOnClickListener {
            val dbHelper = DatabaseHelper(this)
            dbHelper.insertAttemptsMove(userAttempts, randomMove)

            // Launch PokeItemdleActivity when the toolbar title is clicked
            val intent = Intent(this, PokeItemdleActivity::class.java)
            startActivity(intent)
        }

        loadingScreen = findViewById(R.id.loadingScreen)
        mainContent = findViewById(R.id.mainContent)

        val autoCompleteTextView = findViewById<AutoCompleteTextView>(R.id.PruebaTextView)
        val fetchButton = findViewById<Button>(R.id.fetchButton)
        val resultTextView = findViewById<TextView>(R.id.resultTextView)

        var isActionPerformed = false

        autoCompleteTextView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && !isActionPerformed) {
                isActionPerformed = true

                val adapter = autoCompleteTextView.adapter
                if (adapter != null && adapter.count > 0) {
                    val firstSuggestion = adapter.getItem(0) as? String

                    if (!firstSuggestion.isNullOrEmpty() && autoCompleteTextView.text.toString() != firstSuggestion) {
                        autoCompleteTextView.setText(firstSuggestion)
                        autoCompleteTextView.setSelection(firstSuggestion.length) // Move cursor to end
                    }
                }

                autoCompleteTextView.dismissDropDown()
                fetchButton.performClick()

                // Reset flag after a brief delay
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

        // Start the rotation animation
        val loadingImageView = findViewById<ImageView>(R.id.loadingImageView)
        val rotationAnimation = AnimationUtils.loadAnimation(this, R.anim.rotation_animation)
        loadingImageView.startAnimation(rotationAnimation)

        remoteAPI.getAllMoves(
            onSuccess = { moves ->
                runOnUiThread {
                    Log.d("DescriptionActivity", "Fetched moves successfully: ${moves.size}")
                    if (moves.isNotEmpty()) {
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            moves
                        )
                        val dbHelper = DatabaseHelper(this)
                        autoCompleteTextView.setAdapter(adapter)
                        if (dbHelper.getMove() != null) {
                            randomMove = dbHelper.getMove()
                            userAttempts = dbHelper.getAttemptsMove()
                            attemptsRemaining -= userAttempts
                            if (userAttempts > attemptsUntilHint){
                                attemptsUntilHint = 0
                                val randomType = randomMoveDetails?.optJSONObject("type")?.optString("name", "Unknown") ?: "Desconocido"
                                val typeHint = "Pista: El tipo del movimiento es $randomType."
                                hintCountdownTextView.text = typeHint
                            }
                            else attemptsUntilHint -= userAttempts
                        } else {
                            randomMove = moves.random()
                            dbHelper.insertMove(randomMove)
                        }
                        attemptsTextView.text = String.format(getString(R.string.remaining_attempts), attemptsRemaining)
                        hintCountdownTextView.text = String.format(getString(R.string.pista_disponible_en_5), attemptsUntilHint)
                        fetchRandomMoveDetails(randomMove ?: "", remoteAPI)

                        // Hide loading screen and show main content
                        loadingScreen.visibility = View.GONE
                        mainContent.visibility = View.VISIBLE
                    } else {
                        showToast(getString(R.string.no_found))
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("DescriptionActivity", "Error fetching moves: $errorMessage")
                showToast("Error: $errorMessage")
                loadingScreen.visibility = View.GONE // Hide loading screen on error
            }
        )

        setupAutoCompleteTextView(remoteAPI, autoCompleteTextView)
        setupFetchButton(fetchButton, autoCompleteTextView, resultTextView, remoteAPI)
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
        builder.setTitle(getString(R.string.register_button))

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val emailInput = EditText(this).apply {
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = getString(R.string.hint_password)
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton(getString(R.string.register_button)) { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDB = password.hashCode();
            if (email.isNotEmpty() && password.isNotEmpty()) {
                val dbHelper = DatabaseHelper(this)
                val success = dbHelper.registerUser(email, passwordDB)
                onRegister(success)
                userEmail = email
            } else {
                showToast(getString(R.string.complete))
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

        builder.show()
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
                        showToast(getString(R.string.Session_started) + " $userEmail")
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

    private fun showLanguageDialog() {
        val languages = arrayOf( getString(R.string.spanish),
            getString(R.string.english),
            getString(R.string.french)) // Los idiomas disponibles
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
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

    private fun showAverageAttemptsDialog() {
        if (!userEmail.isNullOrEmpty()) {
            val dbHelper = DatabaseHelper(this)
            val average = dbHelper.getAverageAttempts(userEmail!!)
            val message = if (average > 0) {
                getString(R.string.average_attempts).format(average)
            } else {
                getString(R.string.not_enough_data)
            }
            AlertDialog.Builder(this)
                .setTitle(R.string.average_attempts)
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
            hint = "Email"
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val passwordInput = EditText(this).apply {
            hint = getString(R.string.hint_password)
            inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        layout.addView(emailInput)
        layout.addView(passwordInput)
        builder.setView(layout)

        builder.setPositiveButton(getString(R.string.start)) { _, _ ->
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()
            val passwordDb = password.hashCode();
            val dbHelper = DatabaseHelper(this)
            val validUser = dbHelper.loginUser(email, passwordDb)
            if (validUser) {
                onLogin(email)
            } else {
                showToast(getString(R.string.error_invalid_credentials))
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    private fun setupFetchButton(
        fetchButton: Button,
        autoCompleteTextView: AutoCompleteTextView,
        resultTextView: TextView,
        remoteAPI: RemoteAPI
    ) {
        fetchButton.setOnClickListener {
            Log.d("MainActivity", "Fetch button clicked")
            if (gameOver) return@setOnClickListener // Si el juego ha terminado, no hacer nada


            if (userAttempts > 20) {
                showToast(getString(R.string.toast_maximum_attempts))
                fetchButton.isEnabled = false
                gameOver = true
                return@setOnClickListener
            }

            val selectedMove = autoCompleteTextView.text.toString()
            if (selectedMove.isNotEmpty()) {
                userAttempts++
                attemptsRemaining--
                attemptsUntilHint--
                attemptsTextView.text = String.format(getString(R.string.intentos_restantes), attemptsRemaining)
                hintCountdownTextView.text = String.format(getString(R.string.pista_disponible_en_5), attemptsUntilHint)
                if (selectedMove.equals(randomMove, ignoreCase = true)) {
                    Log.d("MainActivity", "User selected correct move: $selectedMove")
                    fetchMoveDetails(selectedMove, resultTextView, remoteAPI)

                    if (!userEmail.isNullOrEmpty()) {
                        saveGameStats(selectedMove, remoteAPI)
                    }

                    showWinningDialog(userAttempts)
                    fetchButton.isEnabled = false
                    gameOver = true
                } else {
                    Log.d("MainActivity", "User selected incorrect item: $selectedMove")

                    if (userAttempts >= 5) {
                        val randomType = randomMoveDetails?.optJSONObject("type")?.optString("name", getString(R.string.unknown))
                        val typeHint = getString(R.string.track_movement) + " $randomType."
                        hintCountdownTextView.text = typeHint
                    }

                    if(userAttempts >= 20){
                        showLosingDialog()
                        fetchButton.isEnabled = false
                        gameOver = true
                    }


                    fetchMoveDetails(selectedMove, resultTextView, remoteAPI)
                    showToast(getString(R.string.wrong_attempt))
                }
                autoCompleteTextView.setText("")
            } else {
                showToast(getString(R.string.select_movement))
            }
        }
    }

    private fun showLosingDialog() {
        runOnUiThread {
            val dialog = AlertDialog.Builder(this)
                .setTitle(String.format(getString(R.string.lost_game), randomMove))
                .setMessage(R.string.try_again)
                .setPositiveButton("OK") { _, _ ->
                    // Reiniciar juego
                    Log.d("MainActivity", "User clicked OK to restart the game")

                    fetchRandomMoveDetails(randomMove ?: "", RemoteAPI())
                }
                .create()
            dialog.show()
        }
    }

    private fun saveGameStats(selectedMove: String, remoteAPI: RemoteAPI) {
        val dbHelper = DatabaseHelper(this)
        val name = selectedMove
        val randomPower = randomMoveDetails?.optInt("power", 0) ?: 0
        val randomAccuracy = randomMoveDetails?.optInt("accuracy", 0) ?: 0
        val randomType = randomMoveDetails?.optJSONObject("type")?.optString("name", "Unknown") ?: getString(R.string.unknown)
        val randomDamageClass = randomMoveDetails?.optJSONObject("damage_class")?.optString("damage_class", getString(R.string.unknown)) ?: getString(R.string.unknown)

        givenNameRecieveDetails(name, remoteAPI) { power, accuracy, type, damageClass, description ->
            dbHelper.insertAttemptMoves(
                userEmail = userEmail,
                moveName = name,
                power = power.toString(),
                powerCorrect = power.toInt() == randomPower,
                accuracy = accuracy.toString(),
                accuracyCorrect = accuracy.toInt() == randomAccuracy,
                type = type,
                typeCorrect = type == randomType,
                damageClass = damageClass,
                damageClassCorrect = damageClass == randomDamageClass,
                description = description,
                descriptionCorrect = description == randomMoveDetails?.optString("description", "")
            )
            val previousAttempts = dbHelper.getTotalAttempts(userEmail!!)
            val totalAttempts = previousAttempts + userAttempts
            dbHelper.updateGameStats(userEmail!!, totalAttempts)
            dbHelper.incrementTotalGames(userEmail!!)
        }
    }


    private fun setupAutoCompleteTextView(
        remoteAPI: RemoteAPI,
        autoCompleteTextView: AutoCompleteTextView
    ) {
        // Already called in onCreate(), no need to call again here.
    }

    private fun fetchRandomMoveDetails(moveName: String, remoteAPI: RemoteAPI) {
        Log.d("MainActivity", "Fetching details for item: $moveName")
        remoteAPI.getMoveDetails(
            moveName = moveName,
            onSuccess = { details -> Log.d("MainActivity", "Fetched details for $moveName successfully")
                randomMoveDetails = details
            },

            onError = { errorMessage -> Log.e("MainActivity", "Error fetching random item details: $errorMessage") }
        )
    }

    private fun givenNameRecieveDetails(moveName: String, remoteAPI: RemoteAPI, onSuccess: (Double, Double, String, String, String) -> Unit) {
        remoteAPI.getMoveDetails(
            moveName = moveName,
            onSuccess = { moveDetails ->  // Suponiendo que itemDetails es un JSONObject
                // Extraemos los valores del JSONObject
                val power = moveDetails.optDouble("power")
                val accuracy = moveDetails.optDouble("accuracy")
                val type = moveDetails.optString("type")
                val damageClass = moveDetails.optString("damage_class")
                val description = moveDetails.optString("description")

                // Llamamos al callback onSuccess con los valores extraídos
                onSuccess(power, accuracy, type, damageClass, description)
            },
            onError = { error ->
                // Manejo de errores si es necesario
                println("Error: $error")
            }
        )
    }

    private fun fetchMoveDetails(
        moveName: String,
        resultTextView: TextView,
        remoteAPI: RemoteAPI
    ) {
        Log.d("MainActivity", "Fetching item details for: $moveName")
        remoteAPI.getMoveDetails(
            moveName = moveName,
            onSuccess = { details ->
                runOnUiThread {
                    Log.d("MainActivity", "Successfully fetched item details for: $moveName")
                    val randomPower = randomMoveDetails?.optInt("power", -1) ?: -1
                    val fetchedPower = details.optInt("power", -1)
                    val randomAccuracy = randomMoveDetails?.optInt("accuracy", -1) ?: -1
                    val fetchedAccuracy = details.optInt("accuracy", -1)

                    val formattedDetails = formatDetailsWithColors(details,randomPower, fetchedPower, randomAccuracy, fetchedAccuracy, resultTextView.text)
                    resultTextView.text = formattedDetails

                }
            },

            onError = { errorMessage ->  Log.e("MainActivity", "Error fetching item details: $errorMessage")
                showToast("Error: $errorMessage") }
        )
    }

    private fun formatDetailsWithColors(
        details: JSONObject,
        randomPower : Int,
        fetchedPower : Int,
        randomAccuracy: Int,
        fetchedAccuracy: Int,
        previousTries: CharSequence
    ): SpannableString {
        val name = details.optString("name", getString(R.string.unknown))
        val power = details.optInt("power", 0)
        val accuracy = details.optInt("accuracy", 0)
        val type = details.optJSONObject("type")?.optString("name", getString(R.string.unknown)) ?: getString(R.string.unknown)
        val randomType = randomMoveDetails?.optJSONObject("type")?.optString("name", getString(R.string.unknown)) ?: getString(R.string.unknown)
        val damageClass = details.optJSONObject("damage_class")?.optString("name", getString(R.string.unknown)) ?: getString(R.string.unknown)
        val randomDamageClass = randomMoveDetails?.optJSONObject("damage_class")?.optString("name", getString(R.string.unknown)) ?: getString(R.string.unknown)

        val powerComparison = when {
            fetchedPower < randomPower -> "$fetchedPower ^"
            fetchedPower > randomPower -> "$fetchedPower v"
            else -> "$fetchedPower"
        }

        val accuracyComparison = when {
            fetchedAccuracy < randomAccuracy -> "$fetchedAccuracy ^"
            fetchedAccuracy > randomAccuracy -> "$fetchedAccuracy v"
            else -> "$fetchedAccuracy"
        }

        val fetchedEffectEntries = details.optJSONArray("effect_entries")?.let { effects ->
            (0 until effects.length()).joinToString("\n") { index ->
                effects.getJSONObject(index).optString("effect", getString(R.string.no_description)).split(":").getOrNull(0)?.trim()
                    ?: getString(R.string.no_description)
            }
        } ?: getString(R.string.no_description)

        val randomEffectEntries = randomMoveDetails?.optJSONArray("effect_entries")?.let { effects ->
            (0 until effects.length()).joinToString("\n") { index ->
                effects.getJSONObject(index).optString("effect", getString(R.string.no_description)).split(":").getOrNull(0)?.trim()
                    ?: getString(R.string.no_description)
            }
        } ?: getString(R.string.no_description)

        val effectEntriesColor = if (fetchedEffectEntries == randomEffectEntries) Color.GREEN else Color.RED

        // Comparaciones y símbolos ✅/❌
        val powerSymbol = if (fetchedPower == randomPower) "✅" else "❌"
        val accuracySymbol = if (fetchedAccuracy == randomAccuracy) "✅" else "❌"
        val typeSymbol = if (type == randomType) "✅" else "❌"
        val damageClassSymbol = if (damageClass == randomDamageClass) "✅" else "❌"
        val descriptionSymbol = if (fetchedEffectEntries == randomEffectEntries) "✅" else "❌"

        val formattedText = getString(
            R.string.formatted_text,
            name,
            powerComparison, powerSymbol,
            accuracyComparison, accuracySymbol,
            type, typeSymbol,
            damageClass, damageClassSymbol,
            fetchedEffectEntries, descriptionSymbol,
            previousTries
        )

        val spannable = SpannableString(formattedText)

        // Aplicar color a la categoría
        val typeStartIndex = formattedText.indexOf("Tipo:") + "Tipo: ".length
        val typeEndIndex = typeStartIndex + type.length
        val typeColor = if (type == randomType) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(typeColor),
            typeStartIndex,
            typeEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color al power
        val powerStartIndex = formattedText.indexOf(powerComparison)
        val powerEndIndex = powerStartIndex + powerComparison.length
        val powerColor = if (fetchedPower == randomPower) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(powerColor),
            powerStartIndex,
            powerEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Aplicar color al accuracye
        val accuracyStartIndex = formattedText.indexOf(accuracyComparison)
        val accuracyEndIndex = accuracyStartIndex + accuracyComparison.length
        val accuracyColor = if (fetchedAccuracy == randomAccuracy) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(accuracyColor),
            accuracyStartIndex,
            accuracyEndIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        // Aplicar color a la categoría
        val damageClassStartIndex = formattedText.indexOf("Damage-class:") + "Damage-class: ".length
        val damageClassEndIndex = damageClassStartIndex + damageClass.length
        val damageClassColor = if (damageClass == randomDamageClass) Color.GREEN else Color.RED
        spannable.setSpan(
            ForegroundColorSpan(damageClassColor),
            damageClassStartIndex,
            damageClassEndIndex,
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
                .setMessage(String.format(getString(R.string.winning_dialog), attempts))
                .setPositiveButton("OK") { _, _ ->
                    // Reiniciar juego
                    Log.d("MainActivity", "User clicked OK to restart the game")

                    fetchRandomMoveDetails(randomMove ?: "", RemoteAPI())
                }
                .create()
            dialog.show()
        }
    }

}
