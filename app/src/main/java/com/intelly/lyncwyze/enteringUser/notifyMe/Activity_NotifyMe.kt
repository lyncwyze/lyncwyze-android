package com.intelly.lyncwyze.enteringUser.notifyMe

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_NotifyMe: AppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var backButton: ImageButton
    private lateinit var fullNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phone_input: EditText
    private lateinit var countryCodePicker: CountryCodePicker
    private lateinit var newsletterSubscription: CheckBox
    private lateinit var submitNotifyButton: Button
    private var successDialog: SweetAlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notify_me)
        setUpUI()
        setUpFunctionality()
        setupDarkModeAwareUI()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupDarkModeAwareUI() {
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        
        // Update status bar and navigation bar colors
        window.statusBarColor = ContextCompat.getColor(this, if (isDarkMode) R.color.primary_bg else R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, if (isDarkMode) R.color.primary_bg else R.color.white)
        
        // Update status bar icons color
        if (!isDarkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        // Update SweetAlert dialog theme
        SweetAlertDialog.DARK_STYLE = isDarkMode

        // Update Country Code Picker colors
        updateCountryCodePickerColor()
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
        setupDarkModeAwareUI()
    }

    private fun setUpUI() {
        backButton = findViewById(R.id.aclBackButton)
        fullNameInput = findViewById(R.id.full_name_input)
        emailInput = findViewById(R.id.email_input)
        phone_input = findViewById(R.id.phone_input)
        countryCodePicker = findViewById(R.id.ccp)
        newsletterSubscription = findViewById(R.id.newsletterSubscription)
        submitNotifyButton = findViewById(R.id.submit_Notify_button)

        // Set default country code
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {
                countryCodePicker.setCountryForNameCode(defaultCountryCode)
                countryCodePicker.setCcpClickable(false)
            }
        }
    }

    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        submitNotifyButton.setOnClickListener {
            val fullName = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phoneNumber = phone_input.text.toString().trim()

            if (validateInputs(fullName, email, phoneNumber))
                saveNotifyMe(fullName, email, phoneNumber, newsletterSubscription.isChecked)
        }
    }

    private fun validateInputs(fullName: String, email: String, phoneNumber: String): Boolean {
        var isValidData = true;
        if (fullName.isBlank()) {
            showToast(this, getString(R.string.want_enter_full_name))
            fullNameInput.requestFocus()
            isValidData = false
        }
        else if (email.isBlank()) {
            showToast(this, getString(R.string.warn_enter_email))
            emailInput.requestFocus()
            isValidData = false
        }
        else if (email.isBlank() || !Utilities.isValidEmail(email)) {
            showToast(this, getString(R.string.warn_invalid_email))
            emailInput.requestFocus()
            isValidData = false
        }
        else if (phoneNumber.isNotBlank() && !Utilities.isPhoneNumberValid(phoneNumber)) {
            showToast(this, getString(R.string.warn_invalid_phone))
            phone_input.requestFocus()
            isValidData = false
        }
        return isValidData
    }

    override fun onDestroy() {
        super.onDestroy()
        successDialog?.dismiss()
        successDialog = null
    }

    private fun saveNotifyMe(fullName: String, email: String, phoneNumber: String, newsletterSubscription: Boolean) {
        lifecycleScope.launch {
            try {
                val formattedPhoneNumber = if (phoneNumber.isBlank()) {
                    ""
                } else {
                    "${countryCodePicker.selectedCountryCodeWithPlus}-$phoneNumber"
                }

                val body = NotifyMeRequest(
                    name = fullName, 
                    email = email,
                    mobileNumber = formattedPhoneNumber,
                    newsletterSubscription = newsletterSubscription
                )
                val response = NetworkManager.apiService.saveNotifyMe(request = body)
                if (response.isSuccessful) {
                    response.body()?.let {
                        logger.info { "data: $it" }
                        if (it.id.isNotEmpty()) {
                            Utilities.clearEditTexts(fullNameInput, emailInput, phone_input)
                            successDialog = SweetAlertDialog(this@Activity_NotifyMe, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText(getString(R.string.notified_availability_ms))
                                .setContentText(getString(R.string.notified_availability_message))
                                .setConfirmText(getString(R.string.thank_you))
                                .setConfirmClickListener { 
                                    successDialog?.dismiss()
                                    this@Activity_NotifyMe.finish() 
                                }
                            successDialog?.show()
                        } else
                            showToast(this@Activity_NotifyMe, getString(R.string.unable_to_save))
                    } ?: showToast(this@Activity_NotifyMe, getString(R.string.unable_to_save))
                } else
                    showToast(this@Activity_NotifyMe, "${getString(R.string.error_txt)}: ${response.code()}")
            } catch (e: Exception) {
                Toast.makeText(this@Activity_NotifyMe, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class NotifyMeRequest(
    val name: String,
    val email: String,
    val mobileNumber: String,
    var newsletterSubscription: Boolean
)
data class NotifyMeResponse(
    val id: String,
    val name: String,
    val email: String,
    val mobileNumber: String,
    var newsletterSubscription: Boolean
)