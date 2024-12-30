package com.example.pokeitemdle

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pokeitemdle.R
import com.example.pokeitemdle.database.DatabaseHelper

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
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
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                navigateToPokeItemdle(email) // User is logged in
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        // Register button click listener
        registerButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.emailEditText).text.toString()
            val password = findViewById<EditText>(R.id.passwordEditText).text.toString()

            // Try to register the user
            if (DatabaseHelper(this).registerUser(email, password.hashCode())) {
                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                navigateToPokeItemdle(email)
            } else {
                Toast.makeText(this, "El usuario ya existe", Toast.LENGTH_SHORT).show()
            }
        }

        // Guest button click listener
        guestButton.setOnClickListener {
            Toast.makeText(this, "Tu progreso no será guardado", Toast.LENGTH_SHORT).show()
            navigateToPokeItemdle("") // User is a guest
        }
    }

    // This method will navigate to PokeItemdleActivity and pass whether the user is logged in
    private fun navigateToPokeItemdle(email: String) {
        val intent = Intent(this, PokeItemdleActivity::class.java)
        intent.putExtra("email", email) // Passing login status
        startActivity(intent)
        finish() // Close the LoginActivity
    }
}
