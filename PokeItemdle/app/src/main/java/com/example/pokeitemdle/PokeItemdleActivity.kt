package com.example.pokeitemdle;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.pokeitemdle.MainActivity
import com.example.pokeitemdle.R

class PokeItemdleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokeitemdle)

        val classicButton: Button = findViewById(R.id.classicButton)
        // Get the login status from the intent
        val isLoggedIn = intent.getBooleanExtra("IS_LOGGED_IN", false) // Default to false if not passed

        // If the user is logged in, navigate to MainActivity
        if (isLoggedIn) {
            navigateToMain(isLoggedIn = true)
        }

        classicButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun navigateToMain(isLoggedIn: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("IS_LOGGED_IN", isLoggedIn) // Passing login status
        startActivity(intent)
        finish() // Close the LoginActivity
    }
}
