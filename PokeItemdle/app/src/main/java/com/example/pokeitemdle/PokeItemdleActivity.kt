package com.example.pokeitemdle;

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pokeitemdle.MainActivity
import com.example.pokeitemdle.R

class PokeItemdleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pokeitemdle)


        val classicButton: Button = findViewById(R.id.classicButton)
        val descButton: Button = findViewById(R.id.descriptionButton)
        // Get the login status from the intent
        val email = intent.getStringExtra("email")
        // If the user is logged in, navigate to MainActivity

        classicButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)

            intent.putExtra("email", email)

            startActivity(intent)
        }
        descButton.setOnClickListener {
            val intent = Intent(this, DescriptionActivity::class.java)

            intent.putExtra("email", email)

            startActivity(intent)
        }
    }
}
