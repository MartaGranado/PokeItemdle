package com.example.pokeitemdle

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.example.pokeitemdle.R
import com.example.pokeitemdle.database.DatabaseHelper
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        val changeLanguageButton: Button = findViewById(R.id.changeLanguageButton)
        changeLanguageButton.setOnClickListener {
            showLanguageDialog()
        }

        val db = DatabaseHelper(this)
        db.dropMove()
        db.dropObjectTable()

        val loginButton: Button = findViewById(R.id.loginButton)
        val registerButton: Button = findViewById(R.id.registerButton)
        val guestButton: Button = findViewById(R.id.guestButton)

        // Login button click listener
        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()

            // Try to log in with the entered credentials
            if (DatabaseHelper(this).loginUser(email, password.hashCode())) {
                Toast.makeText(this, R.string.successful_login, Toast.LENGTH_SHORT).show()
                navigateToPokeItemdle(email) // User is logged in
            } else {
                Toast.makeText(this, R.string.unsuccessful_login, Toast.LENGTH_SHORT).show()
            }
        }

        // Register button click listener
        registerButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()
            if(validateInputs(email,password)){
                if (DatabaseHelper(this).registerUser(email, password.hashCode())) {
                    Toast.makeText(this, R.string.successful_register, Toast.LENGTH_SHORT).show()
                    navigateToPokeItemdle(email)
                } else {
                    Toast.makeText(this, R.string.unsuccessful_register, Toast.LENGTH_SHORT).show()
                }
            }
            // Try to register the user
        }

        // Guest button click listener
        guestButton.setOnClickListener {
            Toast.makeText(this, R.string.no_account, Toast.LENGTH_SHORT).show()
            navigateToPokeItemdle("") // User is a guest
        }
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

        // Reinicia la actividad para aplicar los cambios
        recreate()
    }

    // This method will navigate to PokeItemdleActivity and pass whether the user is logged in
    private fun navigateToPokeItemdle(email: String) {
        val intent = Intent(this, PokeItemdleActivity::class.java)
        intent.putExtra("email", email) // Passing login status
        startActivity(intent)
        finish() // Close the LoginActivity
    }

    private fun validateInputs(email:String, password: String): Boolean {
        // Validación del email
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@gmail\\.com\$")
        if (!emailRegex.matches(email)) {
            Toast.makeText(this,R.string.wrong_email, Toast.LENGTH_SHORT).show()
            return false
        }

        // Validación de la contraseña
        if (password.length < 6) {
            Toast.makeText(this,R.string.wrong_password, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}
