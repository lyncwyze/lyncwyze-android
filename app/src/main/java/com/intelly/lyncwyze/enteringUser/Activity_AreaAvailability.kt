package com.intelly.lyncwyze.enteringUser

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.intelly.lyncwyze.Assest.utilities.IS_AVAILABLE
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteringUser.notifyMe.Activity_NotifyMe
import com.intelly.lyncwyze.enteringUser.signUp.Activity_RegisterWithEmail

class Activity_AreaAvailability : AppCompatActivity() {
    private lateinit var backBtn: ImageButton
    private lateinit var continueBtn: Button
    private lateinit var heading: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_area_availability2)
        setUpUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setUpUI() {
        backBtn = findViewById(R.id.acl2BackButton)
        heading = findViewById(R.id.tvCenteredText)
        continueBtn = findViewById(R.id.continueBtn)
        continueBtn.text = if (intent.getBooleanExtra(IS_AVAILABLE, false)) getString(R.string.continueTxt) else getString(R.string.notify_me)
    }
    private fun setUpFunctionality() {
        val isAvailable = intent.getBooleanExtra(IS_AVAILABLE, false)

        backBtn.setOnClickListener { this.onBackPressed() }
        if (isAvailable) {
            heading.text = "Great news!! We are servicing your area"
//            continueBtn.setOnClickListener { startActivity(Intent(this@Activity_AreaAvailability, Activity_LoginWithNumber::class.java)) }
            continueBtn.setOnClickListener { startActivity(Intent(this@Activity_AreaAvailability, Activity_RegisterWithEmail::class.java)) }
        } else {
            heading.text = "Sorry, We are not currently in your area"
            continueBtn.setOnClickListener { startActivity(Intent(this@Activity_AreaAvailability, Activity_NotifyMe::class.java)) }
            findViewById<LinearLayout>(R.id.realTimeUserBase).visibility = View.GONE
            findViewById<LinearLayout>(R.id.noOfCarPoolSchedule).visibility = View.GONE
            findViewById<LinearLayout>(R.id.co2_saved).visibility = View.GONE
        }

        val webView: WebView = findViewById(R.id.videoView)
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        val videoUrl = "https://www.youtube.com/embed/Rwe5Aw3KPHY"
        webView.webViewClient = WebViewClient() // To open URLs in WebView itself
        webView.loadUrl(videoUrl)
    }
}