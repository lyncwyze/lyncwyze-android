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
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.EmailNumb_ToValidate
import com.intelly.lyncwyze.Assest.utilities.FCMTokenCallback
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.getPlatform
import com.intelly.lyncwyze.Assest.utilities.getVersion
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import kotlinx.coroutines.launch
import mu.KotlinLogging
import retrofit2.Response

class Activity_RegisterWithEmail : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var inputEmail: EditText
    private lateinit var continueButton: Button
    private lateinit var registerWithMobileNo: Button
    private lateinit var alreadyHaveAccount: TextView
    private lateinit var workTitleMessage: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_with_email)
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
        inputEmail = findViewById(R.id.user_email_input)
        continueButton = findViewById(R.id.aclContinueBtn)
        registerWithMobileNo = findViewById(R.id.registerWithMobileNo)
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount)
        workTitleMessage = findViewById(R.id.work_title_message)

        resources.getString(R.string.isValidOtp).let { isValidOtp ->
            if (isValidOtp == "false" || isValidOtp == "") {  // Available in local props
                workTitleMessage.visibility = View.GONE
            }
        }
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        continueButton.setOnClickListener {
            resources.getString(R.string.isValidOtp).let { isValidOtp ->
                if (isValidOtp == "true") {  // Available in local props
                    createAcc()
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
        registerWithMobileNo.setOnClickListener { startActivity(Intent(this@Activity_RegisterWithEmail, Activity_RegisterWithNumber::class.java)) }
        alreadyHaveAccount.setOnClickListener { startActivity(Intent(this@Activity_RegisterWithEmail, Activity_LoginWithEmail::class.java)) }
    }
    private fun createAcc() {
        val emailTxt = inputEmail.text.toString().trim()
        if (validateAndContinue(emailTxt)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_RegisterWithEmail)
                    val response = NetworkManager.apiService.generateOtpFromEmail(email = emailTxt)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_RegisterWithEmail)
                        response.body()?.let {
                            val intent = Intent(this@Activity_RegisterWithEmail, Activity_ValidateOTP::class.java)
                                .apply { putExtra(EmailNumb_ToValidate, emailTxt) }
                            startActivity(intent)
                        }
                    } else {
                        loader.hideLoader(this@Activity_RegisterWithEmail)
                        showToast(this@Activity_RegisterWithEmail, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@Activity_RegisterWithEmail, "Exception: ${e.message}", Toast.LENGTH_SHORT).show()
                    loader.hideLoader(this@Activity_RegisterWithEmail)
                }
            }
        }
    }

    private fun createUser(deviceToken: String) {
        val emailTxt = inputEmail.text.toString().trim()
        if (validateAndContinue(emailTxt)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_RegisterWithEmail)
                    val response: Response<LoginResponse> =
                        NetworkManager.apiService.createUserUsingEmail(email = emailTxt, token = deviceToken, platform = getPlatform(), version = getVersion())
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_RegisterWithEmail)
                        response.body()?.let {

                            SharedPreferencesManager.saveAuthDetails(it)

                            val intent = Intent(this@Activity_RegisterWithEmail, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                            startActivity(intent)
                        } ?: {
                            showToast(this@Activity_RegisterWithEmail, getString(R.string.credentials_not_found))
                        }
                    } else {
                        loader.hideLoader(this@Activity_RegisterWithEmail)
                        showToast(this@Activity_RegisterWithEmail, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.error { "${R.string.exception_collen} ${e.message}" }
                    Toast.makeText(this@Activity_RegisterWithEmail, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show()
                    loader.hideLoader(this@Activity_RegisterWithEmail)
                }
            }
        }
    }


    private fun validateAndContinue(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                showToast(this, getString(R.string.warn_empty_email))
                inputEmail.requestFocus()
                false
            }
            !Utilities.isValidEmail(email) -> {
                showToast(this, getString(R.string.warn_invalid_email))
                inputEmail.requestFocus()
                false
            }
            else -> true
        }
    }
}