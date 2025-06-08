package com.intelly.lyncwyze.enteredUser.profileSetup

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import mu.KotlinLogging


class Activity_PS_Step5 : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private lateinit var backButton: ImageView


    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step5)
        setUpUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun setUpUI() {
        backButton = findViewById(R.id.aclBackButton)

        continueButton = findViewById(R.id.button_continue)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        continueButton.setOnClickListener {
            logger.info { "Get the the normal work." }
            startActivity(Intent(this@Activity_PS_Step5, Dashboard::class.java))
        }
    }
}