package com.cs646.program.courseprojecttetris

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class InstructionsActivity : AppCompatActivity() {

    lateinit var mainMenuButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        this.supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructions)

        mainMenuButton = findViewById(R.id.main_menu_button)

        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}