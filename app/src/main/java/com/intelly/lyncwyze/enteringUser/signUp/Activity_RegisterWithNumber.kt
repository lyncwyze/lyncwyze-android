package com.intelly.lyncwyze.enteringUser.signUp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.EmailNumb_ToValidate
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import kotlinx.coroutines.launch
import mu.KotlinLogging
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.utilities.FCMTokenCallback
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.getPlatform
import com.intelly.lyncwyze.Assest.utilities.getVersion
import com.intelly.lyncwyze.enteredUser.Dashboard
import retrofit2.Response

class Activity_RegisterWithNumber : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView

    private lateinit var singInWithNumber: Button
    private lateinit var inputPhoneNumber: EditText
    private lateinit var ccpSignUp: CountryCodePicker
    private lateinit var registerWithEmail: Button
    private lateinit var alreadyHaveAccount: TextView
    private lateinit var workTitleMessage: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_with_number)
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
        singInWithNumber = findViewById(R.id.singInWithNumber)
        inputPhoneNumber = findViewById(R.id.phone_number_input)
        ccpSignUp = findViewById(R.id.ccpSignUp)
        workTitleMessage = findViewById(R.id.work_title_message)
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpSignUp.setCountryForNameCode(defaultCountryCode)
                ccpSignUp.setCcpClickable(false)
            }
        }
        
        // Set text color based on theme
        updateCountryCodePickerColor()

        registerWithEmail = findViewById(R.id.registerWithEmail)
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount)
        resources.getString(R.string.isValidOtp).let { isValidOtp ->
            if (isValidOtp == "false" || isValidOtp == "") {  // Available in local props
                workTitleMessage.visibility = View.GONE
            }
        }
    }

    private fun updateCountryCodePickerColor() {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val textColor = if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            ContextCompat.getColor(this, android.R.color.white)
        } else {
            ContextCompat.getColor(this, android.R.color.black)
        }
        ccpSignUp.setNumberAutoFormattingEnabled(false)
        ccpSignUp.contentColor = textColor
        ccpSignUp.setDialogTextColor(textColor)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCountryCodePickerColor()
    }

    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        singInWithNumber.setOnClickListener { 
            resources.getString(R.string.isValidOtp).let { isValidOtp ->
                if (isValidOtp == "true") {  // Available in local props
                    generateOTP()
                } else {
                    Utilities.getFCMToken(object : FCMTokenCallback {
                        override fun onTokenReceived(token: String) {
                            logger.info("FCM Token: $token")
                            createUser(token)
                        }
                        override fun onTokenError() {
                            logger.info("Failed to retrieve FCM Token")
                            createUser("token")
                        }
                    })
                }
            }
         }
        registerWithEmail.setOnClickListener { startActivity(Intent(this, Activity_RegisterWithEmail::class.java)) }
        alreadyHaveAccount.setOnClickListener { startActivity(Intent(this, Activity_LoginWithEmail::class.java)) }
    }
    private fun generateOTP() {
        var numWithPref = inputPhoneNumber.text.toString().trim()
        if (validateAndContinue(numWithPref)) {
            numWithPref = "${ccpSignUp.selectedCountryCodeWithPlus}-$numWithPref"
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_RegisterWithNumber)
                    val response = NetworkManager.apiService.generateOtpForNumber(mobileNumber = numWithPref)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_RegisterWithNumber)
                        response.body()?.let {
                            logger.info { it }
                            val intent = Intent(this@Activity_RegisterWithNumber, Activity_ValidateOTP::class.java)
                                .apply { putExtra(EmailNumb_ToValidate, numWithPref) }
                            startActivity(intent)
                        }
                    } else {
                        loader.hideLoader(this@Activity_RegisterWithNumber)
                        showToast(this@Activity_RegisterWithNumber, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@Activity_RegisterWithNumber, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                    loader.hideLoader(this@Activity_RegisterWithNumber)
                }
            }
        }
    }


    private fun createUser(deviceToken: String) {
        var numWithPref = inputPhoneNumber.text.toString().trim()
        if (validateAndContinue(numWithPref)) {
            numWithPref = "${ccpSignUp.selectedCountryCodeWithPlus}-$numWithPref"
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_RegisterWithNumber)
                    val response: Response<LoginResponse> =
                            NetworkManager.apiService.createUserUsingNumber(mobileNumber = numWithPref, token = deviceToken, platform = getPlatform(), version = getVersion())
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_RegisterWithNumber)
                        response.body()?.let {

                            SharedPreferencesManager.saveAuthDetails(it)

                            val intent = Intent(this@Activity_RegisterWithNumber, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                            startActivity(intent)
                        } ?: {
                            showToast(this@Activity_RegisterWithNumber, getString(R.string.credentials_not_found))
                        }
                    } else {
                        loader.hideLoader(this@Activity_RegisterWithNumber)
                        showToast(this@Activity_RegisterWithNumber, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.error { "${R.string.exception_collen} ${e.message}" }
                    Toast.makeText(this@Activity_RegisterWithNumber, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show()
                    loader.hideLoader(this@Activity_RegisterWithNumber)
                }
            }
        }
    }


    private fun validateAndContinue(phoneNumber: String): Boolean {
        return when {
            !Utilities.isPhoneNumberValid(phoneNumber) -> {
                showToast(this, getString(R.string.warn_invalid_phone))
                inputPhoneNumber.requestFocus()
                false
            }
            else -> true
        }
    }
}