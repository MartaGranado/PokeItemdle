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
        val email = intent.getStringExtra("email")

        // If the user is logged in, navigate to MainActivity

        classicButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            if(email.isNullOrEmpty()){
                intent.putExtra("email", email)
            }
            startActivity(intent)
        }
    }
}
