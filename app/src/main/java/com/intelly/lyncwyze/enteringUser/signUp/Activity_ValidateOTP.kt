package com.intelly.lyncwyze.enteringUser.signUp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
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
import kotlinx.coroutines.launch
import mu.KotlinLogging
import retrofit2.Response


class Activity_ValidateOTP : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)
    private var emailNumToValidate: String = ""

    private lateinit var backButton: ImageView
    private lateinit var displayEmailOrNumber: TextView
    private lateinit var otpInput1: EditText
    private lateinit var otpInput2: EditText
    private lateinit var otpInput3: EditText
    private lateinit var otpInput4: EditText
    private lateinit var noOtpReceivedResend: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_validate_otp)
        preRequisite()
        setUpUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun preRequisite() {
        emailNumToValidate = intent.getStringExtra(EmailNumb_ToValidate).toString()
    }
    private fun setUpUI() {
        backButton = findViewById(R.id.aclBackButton)
        displayEmailOrNumber = findViewById(R.id.displayEmailOrNumber)
        otpInput1 = findViewById(R.id.otp_input_1)
        otpInput2 = findViewById(R.id.otp_input_2)
        otpInput3 = findViewById(R.id.otp_input_3)
        otpInput4 = findViewById(R.id.otp_input_4)
        noOtpReceivedResend = findViewById(R.id.noOtpReceivedResend)

        // Set message based on whether it's email or phone number
        val messageText = if (emailNumToValidate.contains("@")) {
            "Sent on your email: $emailNumToValidate"
        } else {
            "Sent on your phone number: $emailNumToValidate"
        }
        findViewById<TextView>(R.id.work_title_message).text = messageText
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }

        val editTexts = listOf(otpInput1, otpInput2, otpInput3, otpInput4)
        for (editText in editTexts) {
            editText.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus)
                    (view as EditText).setText("")
            }
        }
        otpInput1.performClick()
        otpInput1.addTextChangedListener(OtpTextWatcher(otpInput2))
        otpInput2.addTextChangedListener(OtpTextWatcher(otpInput3))
        otpInput3.addTextChangedListener(OtpTextWatcher(otpInput4))

        // Handle backspace behavior
        otpInput2.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_DEL && otpInput2.text.isEmpty())
                otpInput1.requestFocus()
            false
        }
        otpInput3.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_DEL && otpInput3.text.isEmpty())
                otpInput2.requestFocus()
            false
        }
        otpInput4.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_DEL && otpInput4.text.isEmpty())
                otpInput3.requestFocus()
            false
        }
        otpInput4.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s?.length == 1) {
                    Utilities.getFCMToken(object : FCMTokenCallback {
                        override fun onTokenReceived(token: String) {
                            logger.info("FCM Token: $token")
                            callAPI(token)
                        }
                        override fun onTokenError() {
                            logger.info("Failed to retrieve FCM Token")
                            callAPI("token")
                        }
                    })
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        noOtpReceivedResend.setOnClickListener { triggerReSendOtp() }
    }

    private class OtpTextWatcher(private val nextEditText: EditText) : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (s?.length == 1)
                nextEditText.requestFocus()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    private fun callAPI(deviceToken: String) {
        val enteredOtp = "${otpInput1.text}${otpInput2.text}${otpInput3.text}${otpInput4.text}"
        if(enteredOtp.length == 4 && emailNumToValidate.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_ValidateOTP)
                    val response: Response<LoginResponse> =
                        if (Utilities.isValidEmail(emailNumToValidate))
                            NetworkManager.apiService.verifyEmailOtpAndCreateUser(otp = enteredOtp, email = emailNumToValidate, token = deviceToken, platform = getPlatform(), version = getVersion())
                        else
                            NetworkManager.apiService.verifyNumberOtpAndCreateUser(otp = enteredOtp, mobileNumber = emailNumToValidate, token = deviceToken, platform = getPlatform(), version = getVersion())


                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_ValidateOTP)
                        response.body()?.let {

                            SharedPreferencesManager.saveAuthDetails(it)

                            val intent = Intent(this@Activity_ValidateOTP, Dashboard::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                            startActivity(intent)
                        } ?: {
                            showToast(this@Activity_ValidateOTP, getString(R.string.credentials_not_found))
                        }
                    } else {
                        loader.hideLoader(this@Activity_ValidateOTP)
                        logger.error { "Error: ${response.code()} ${response.body()}" }
                        showToast(this@Activity_ValidateOTP, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.error { "${R.string.exception_collen} ${e.message}" }
                    Toast.makeText(this@Activity_ValidateOTP, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show()
                    loader.hideLoader(this@Activity_ValidateOTP)
                }
            }
        }
    }
    private fun triggerReSendOtp() {
        lifecycleScope.launch {
            try {
                showToast(this@Activity_ValidateOTP, "Resending...")
                val response = if (emailNumToValidate.split("-").last().isDigitsOnly())
                    NetworkManager.apiService.retryPhoneOtp(mobileNumber = emailNumToValidate)
                else
                    NetworkManager.apiService.retryEmailOtp(email = emailNumToValidate)
                if (response.isSuccessful) {
                    response.body()?.let {
                        showToast(this@Activity_ValidateOTP,
                            if (it) getString(R.string.new_opt_send_successfully)
                            else getString(R.string.unable_to_sent_new_otp)
                        )
                    } ?: showToast(this@Activity_ValidateOTP, getString(R.string.error_sending_new_otp))
                } else showToast(this@Activity_ValidateOTP, response.code().toString())
            } catch (e: Exception) {
                Toast.makeText(this@Activity_ValidateOTP, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}