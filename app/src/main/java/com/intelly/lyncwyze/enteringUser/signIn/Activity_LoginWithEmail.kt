package com.intelly.lyncwyze.enteringUser.signIn

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.FCMTokenCallback
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.getPlatform
import com.intelly.lyncwyze.Assest.utilities.getVersion
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import com.intelly.lyncwyze.enteringUser.signUp.Activity_RegisterWithEmail
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Activity_LoginWithEmail : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var passwordInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var togglePasswordVisibility: ImageView

    private lateinit var continueWithNumber: Button
    private lateinit var createNewAccLink: TextView

    private lateinit var continueButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_with_email)
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
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.passwordEmail)
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility)
        continueButton = findViewById(R.id.aclContinueBtn)
        continueWithNumber = findViewById(R.id.continueWithNumber)
        createNewAccLink = findViewById(R.id.createNewAccLink)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        togglePasswordVisibility.setOnClickListener {
            if (passwordInput.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            passwordInput.setSelection(passwordInput.text.length)
        }
        continueButton.setOnClickListener {
            Utilities.getFCMToken(object : FCMTokenCallback {
                override fun onTokenReceived(token: String) {
                    logger.info("FCM Token: $token")
                    callLoginAPI(token)
                }
                override fun onTokenError() {
                    logger.info("Failed to retrieve FCM Token")
                    callLoginAPI("")
                }
            })
        }
        continueWithNumber.setOnClickListener { startActivity(Intent(this, Activity_LoginWithNumber::class.java)) }
        createNewAccLink.setOnClickListener { startActivity(Intent(this, Activity_RegisterWithEmail::class.java)) }
    }
    private fun callLoginAPI(deviceToken: String) {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        val encodedAuthString = Base64.encodeToString("$email:$password".toByteArray(), Base64.NO_WRAP)

        if(validateInputs(email, password)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val response = NetworkManager.apiService.loginUser(
                        authType = "app",
                        token = deviceToken,
                        platform = getPlatform(),
                        version = getVersion(),
                        Authorization = "Basic $encodedAuthString"
                    )
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        response.body()?.let {
                            Utilities.clearEditTexts(emailInput, passwordInput)

                            SharedPreferencesManager.saveAuthDetails(it)

                            val intent = Intent(this@Activity_LoginWithEmail, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                            startActivity(intent)
                        } ?: {
                            showToast(this@Activity_LoginWithEmail, getString(R.string.credentials_not_found))
                        }
                    } else {
                        loader.hideLoader()
                        showToast(this@Activity_LoginWithEmail,  getString(R.string.credentials_not_found))
                    }
                } catch (e: Exception) {
                    loader.hideLoader()
                    showToast(this@Activity_LoginWithEmail, "Exception in login with email: ${e.message}")
                }
            }
        }
    }
    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            (!Utilities.isValidEmail(email)) -> {
                emailInput.error = getString(R.string.warn_invalid_email)
                emailInput.requestFocus()
                false
            }
            (!Utilities.isPasswordValid(password)) -> {
                passwordInput.error = getString(R.string.warn_empty_password)
                passwordInput.requestFocus()
                false
            }
            else -> {
                true
            }
        }
    }
}