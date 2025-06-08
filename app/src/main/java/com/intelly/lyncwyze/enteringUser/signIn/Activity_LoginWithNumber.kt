package com.intelly.lyncwyze.enteringUser.signIn

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
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
import android.content.res.Configuration
import androidx.core.content.ContextCompat

class Activity_LoginWithNumber : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var inputPhoneNumber: EditText
    private lateinit var continueButton: Button
    private lateinit var continueWithEmail: Button
    private lateinit var createNewAccLink: TextView
    private lateinit var inputPasswor: EditText
    private lateinit var togglePasswordNumberVisibility: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_with_number)
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

        countryCodePicker = findViewById(R.id.ccp)
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                countryCodePicker.setCountryForNameCode(defaultCountryCode)
                countryCodePicker.setCcpClickable(false)
            }
        }
        
        // Set text color based on theme
        updateCountryCodePickerColor()

        inputPhoneNumber = findViewById(R.id.phone_number_input)
        inputPasswor = findViewById(R.id.passwordForNum)
        togglePasswordNumberVisibility = findViewById(R.id.togglePasswordNumberVisibility)

        continueButton = findViewById(R.id.aclContinueBtn)
        continueWithEmail = findViewById(R.id.continueWithEmail)
        createNewAccLink = findViewById(R.id.createNewAccLink)
    }

    private fun updateCountryCodePickerColor() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val textColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            ContextCompat.getColor(this, android.R.color.white)
        } else {
            ContextCompat.getColor(this, android.R.color.black)
        }
        countryCodePicker.setNumberAutoFormattingEnabled(false)
        countryCodePicker.contentColor = textColor
        countryCodePicker.setDialogTextColor(textColor)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCountryCodePickerColor()
    }

    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        togglePasswordNumberVisibility.setOnClickListener {
            if (inputPasswor.inputType == InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                inputPasswor.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                inputPasswor.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            }
            inputPasswor.setSelection(inputPasswor.text.length)
        }

        continueButton.setOnClickListener {
            Utilities.getFCMToken(object : FCMTokenCallback {
                override fun onTokenReceived(token: String) {
                    callAPI(token)
                }
                override fun onTokenError() {
                    logger.info("Failed to retrieve FCM Token")
                    callAPI("")
                }
            })
        }
        continueWithEmail.setOnClickListener { startActivity(Intent(this@Activity_LoginWithNumber, Activity_LoginWithEmail::class.java)) }
        createNewAccLink.setOnClickListener { startActivity(Intent(this, Activity_RegisterWithEmail::class.java)) }
    }

    private fun callAPI(deviceToken: String) {
        val phoneNumber = inputPhoneNumber.text.toString().trim()
        val fullPhoneNumber = "${countryCodePicker.selectedCountryCodeWithPlus}-${inputPhoneNumber.text.toString().trim()}"
        val password = inputPasswor.text.toString().trim()
        val encodedAuthString = Base64.encodeToString("$fullPhoneNumber:$password".toByteArray(), Base64.NO_WRAP)

        if (validateAndContinue(phoneNumber, password)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_LoginWithNumber)
                    val response = NetworkManager.apiService.loginUser(
                        authType = "app",
                        token = deviceToken,
                        platform = getPlatform(),
                        version = getVersion(),
                        Authorization = "Basic $encodedAuthString"
                    )
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_LoginWithNumber)
                        response.body()?.let {
                            logger.info { "Number login data: $it" }
                            Utilities.clearEditTexts(inputPhoneNumber, inputPasswor)

                            SharedPreferencesManager.saveAuthDetails(it)

                            val intent = Intent(this@Activity_LoginWithNumber, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                            startActivity(intent)
                        } ?: {
                            showToast(this@Activity_LoginWithNumber,  getString(R.string.credentials_not_found))
                        }
                    } else {
                        loader.hideLoader(this@Activity_LoginWithNumber)
                        showToast(this@Activity_LoginWithNumber,  getString(R.string.credentials_not_found))
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Activity_LoginWithNumber)
                    showToast(this@Activity_LoginWithNumber, "Exception in login with number: ${e.message}" )
                }
            }
        }
    }
    private fun validateAndContinue(phoneNumber: String, password: String): Boolean {
        return when {
            phoneNumber.isBlank() -> {
                showToast(this, getString(R.string.warn_empty_phone))
                inputPhoneNumber.requestFocus()
                false
            }
            !Utilities.isPhoneNumberValid(phoneNumber) -> {
                showToast(this, getString(R.string.warn_invalid_phone))
                inputPhoneNumber.requestFocus()
                false
            }
            password.isBlank() -> {
                showToast(this, getString(R.string.warn_empty_password))
                inputPasswor.requestFocus()
                false
            }
            !Utilities.isPasswordValid(password) -> {
                showToast(this, getString(R.string.warn_invalid_password))
                inputPasswor.requestFocus()
                false
            }
            else -> true
        }
    }
}