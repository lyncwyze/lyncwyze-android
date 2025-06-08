package com.intelly.lyncwyze.enteredUser.profileSetup

import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.CreateProfileRequestBodyFromEmail
import com.intelly.lyncwyze.Assest.modals.CreateProfileRequestBodyFromMobile
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.modals.ProfileStatus
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.LoggedInDataKey
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import okhttp3.internal.concurrent.TaskRunner.Companion.logger


class Activity_PS_Step1 : AppCompatActivity() {
    private val loader = Loader(this)

    private var userData = SharedPreferencesManager.getDecodedAccessToken();

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var inputFName: EditText
    private lateinit var inputLName: EditText

    private lateinit var theNumberBlock: LinearLayout
    private lateinit var email_container: LinearLayout
    private lateinit var emailInput: EditText
    private lateinit var ccpAddress: CountryCodePicker
    private lateinit var inputMobileNo: EditText

    private lateinit var inputPassword: EditText
    private lateinit var togglePasswordVisibility: ImageView
    private var isPasswordVisible = false

    private lateinit var inputConfirmPassword: EditText
    private lateinit var toggleConfirmPasswordVisibility: ImageView
    private var isConfirmPasswordVisible = false

    private lateinit var passStrength: TextView
    private lateinit var passStrengthImg: ImageView
    private lateinit var containUpperCase: TextView
    private lateinit var containUpperCaseImg: ImageView
    private lateinit var lengthLeast8: TextView
    private lateinit var lengthLeast8Img: ImageView
    private lateinit var containNumberSpecialChar: TextView
    private lateinit var containNumberSpecialCharImg: ImageView

    private lateinit var createProfileButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step1)
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
        logoutButton = findViewById(R.id.logoutButton)

        inputFName = findViewById(R.id.input_fName)
        inputLName = findViewById(R.id.input_lName)

        theNumberBlock = findViewById(R.id.phone_no_container)
        email_container = findViewById(R.id.email_container)
        emailInput = findViewById(R.id.email_input)

        ccpAddress = findViewById(R.id.ccpAddress)
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpAddress.setCountryForNameCode(defaultCountryCode)
                ccpAddress.setCcpClickable(false)
            }
        }
        // Set text color based on night mode
        val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            ccpAddress.setContentColor(ContextCompat.getColor(this, android.R.color.white))
            ccpAddress.setDialogTextColor(ContextCompat.getColor(this, android.R.color.white))
        }
        inputMobileNo = findViewById(R.id.inputMobileNo)

        userData?.let {
            if (Utilities.isValidEmail(it.user_name)) {
                emailInput.setText(it.user_name)
                email_container.visibility = View.GONE
                theNumberBlock.visibility = View.VISIBLE
            } else {
                inputMobileNo.setText(it.user_name)
                theNumberBlock.visibility = View.GONE
                email_container.visibility = View.VISIBLE
            }
        }

//        inputSsn4 = findViewById(R.id.input_ssn4)
        inputPassword = findViewById(R.id.input_password)
        inputConfirmPassword = findViewById(R.id.input_ConfirmPassword)

        // Show/hide password
        togglePasswordVisibility = findViewById(R.id.toggle_PasswordVisibility)
        toggleConfirmPasswordVisibility = findViewById(R.id.toggle_ConfirmPasswordVisibility)

        // Color and icon change
        passStrength = findViewById(R.id.passStrength)
        passStrengthImg = findViewById(R.id.passStrengthImg)
        containUpperCase = findViewById(R.id.containUpperCase)
        containUpperCaseImg = findViewById(R.id.containUpperCaseImg)
        lengthLeast8 = findViewById(R.id.lengthLeast8)
        lengthLeast8Img = findViewById(R.id.lengthLeast8Img)
        containNumberSpecialChar = findViewById(R.id.containNumberSpecialChar)
        containNumberSpecialCharImg = findViewById(R.id.containNumberSpecialCharImg)

        // Save data
        createProfileButton = findViewById(R.id.aclContinueBtn)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }

        togglePasswordVisibility.setOnClickListener {
            if (isPasswordVisible) {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//                togglePasswordVisibility.setImageResource(R.drawable.co2_saved)
                isPasswordVisible = false
            } else {
                inputPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
//                togglePasswordVisibility.setImageResource(R.drawable.co2_saved)
                isPasswordVisible = true
            }
            inputPassword.setSelection(inputPassword.text.length)
        }
        toggleConfirmPasswordVisibility.setOnClickListener {
            if (isConfirmPasswordVisible) {
                inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//                toggleConfirmPasswordVisibility.setImageResource(R.drawable.co2_saved)
                isConfirmPasswordVisible = false
            } else {
                inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
//                toggleConfirmPasswordVisibility.setImageResource(R.drawable.co2_saved)
                isConfirmPasswordVisible = true
            }
            inputConfirmPassword.setSelection(inputConfirmPassword.text.length)
        }
        inputPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { checkPasswordStrength(inputPassword.text.toString().trim()) }
        })

        createProfileButton.setOnClickListener {
            userData?.let {

                if (Utilities.isValidEmail(it.user_name)) {
                    val data =  CreateProfileRequestBodyFromMobile(
                        firstName = inputFName.text.toString().trim(),
                        lastName = inputLName.text.toString().trim(),
                        mobileNumber = inputMobileNo.text.toString().trim(),
                        password = inputPassword.text.toString().trim(),
                        confirmPassword = inputConfirmPassword.text.toString().trim(),
//                ssnLast4 = inputSsn4.text.toString().trim()
                    )
                    if (validateInputsFromMobile(data)) {
                        if (!data.mobileNumber.startsWith('+')){
                            data.mobileNumber =
                                "${ccpAddress.selectedCountryCodeWithPlus}-${data.mobileNumber}"
                        }
                        lifecycleScope.launch {
                            try {
                                loader.showLoader(this@Activity_PS_Step1)
                                val response = NetworkManager.apiService.createProfileFromMobile(body = data)
                                if (response.isSuccessful) {
                                    loader.hideLoader(this@Activity_PS_Step1)
                                    val body = response.body()
                                    if (body != null) {
                                        val loginRes = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                                        if (loginRes != null) {
                                            loginRes.profileStatus = ProfileStatus.BACKGROUND.value;
                                            SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes)
                                            this@Activity_PS_Step1.finish()
                                        }
//                                SweetAlertDialog(this@Activity_PS_Step1, SweetAlertDialog.SUCCESS_TYPE)
//                                    .setTitleText(getString(R.string.profile_successfully_created))
//                                    .setContentText("Please proceed to add a valid document")
//                                    .setConfirmText("OK")
//                                    .setConfirmClickListener { sDialog ->
//                                        sDialog.dismissWithAnimation()
//                                        this@Activity_PS_Step1.finish()
//                                    }.show()
                                    }
                                } else {
                                    val error =  HttpUtilities.parseError(response)
                                    loader.hideLoader(this@Activity_PS_Step1)
                                    showToast(this@Activity_PS_Step1, "${getString(R.string.error_txt)}: ${response.code()}, ${error?.errorInformation?.errorDescription}")
                                }
                            } catch (e: Exception) {
                                loader.hideLoader(this@Activity_PS_Step1)
                                showToast(this@Activity_PS_Step1, "Exception: ${e.message}")
                            }
                        }
                    }

                } else {
                    val data =  CreateProfileRequestBodyFromEmail(
                        firstName = inputFName.text.toString().trim(),
                        lastName = inputLName.text.toString().trim(),
                        email = emailInput.text.toString().trim(),
                        password = inputPassword.text.toString().trim(),
                        confirmPassword = inputConfirmPassword.text.toString().trim(),
//                ssnLast4 = inputSsn4.text.toString().trim()
                    )
                    if (validateInputsFromEmail(data)) {
                        lifecycleScope.launch {
                            try {
                                loader.showLoader(this@Activity_PS_Step1)
                                val response = NetworkManager.apiService.createProfileFromEmail(body = data)
                                if (response.isSuccessful) {
                                    loader.hideLoader(this@Activity_PS_Step1)
                                    val body = response.body()
                                    if (body != null) {
                                        val loginRes = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                                        if (loginRes != null) {
                                            loginRes.profileStatus = ProfileStatus.BACKGROUND.value;
                                            SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes)
                                            this@Activity_PS_Step1.finish()
                                        }
//                                SweetAlertDialog(this@Activity_PS_Step1, SweetAlertDialog.SUCCESS_TYPE)
//                                    .setTitleText(getString(R.string.profile_successfully_created))
//                                    .setContentText("Please proceed to add a valid document")
//                                    .setConfirmText("OK")
//                                    .setConfirmClickListener { sDialog ->
//                                        sDialog.dismissWithAnimation()
//                                        this@Activity_PS_Step1.finish()
//                                    }.show()
                                    }
                                } else {
                                    val error =  HttpUtilities.parseError(response)
                                    loader.hideLoader(this@Activity_PS_Step1)
                                    showToast(this@Activity_PS_Step1, "${getString(R.string.error_txt)}: ${response.code()}, ${error?.errorInformation?.errorDescription}")
                                }
                            } catch (e: Exception) {
                                loader.hideLoader(this@Activity_PS_Step1)
                                showToast(this@Activity_PS_Step1, "Exception: ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validateInputsFromMobile(data: CreateProfileRequestBodyFromMobile): Boolean {
        var number: String = ""
        if (data.mobileNumber != "") {
            val parts = data.mobileNumber.removePrefix("-").split("-", limit = 2).toMutableList()
            if (data.mobileNumber.startsWith("-"))
                parts[0] = "-${parts[0]}"
            number = parts.getOrNull(1) ?: parts.getOrNull(0) ?: ""
        }

        return when {
            data.firstName.isEmpty() -> {
                showToast(this, getString(R.string.warn_empty_first_name))
                inputFName.requestFocus()
                false
            }
            data.lastName.isEmpty() -> {
                showToast(this, getString(R.string.warn_empty_last_name))
                inputLName.requestFocus()
                false
            }
            !Utilities.isPhoneNumberValid(number) -> {
                showToast(this, getString(R.string.warn_invalid_phone))
                inputMobileNo.requestFocus()
                false
            }
//            data.ssnLast4.length != 4 -> { // Assuming SSN should be 4 digits long
//                showToast(this, getString(R.string.warn_last_four_digits_of_ssn))
//                inputSsn4.requestFocus()
//                false
//            }
            data.password.isBlank() -> {
                showToast(this, getString(R.string.warn_empty_password))
                inputPassword.requestFocus()
                false
            }
            !checkPasswordStrength(data.password) -> {
                showToast(this, getString(R.string.warn_invalid_password))
                inputPassword.requestFocus()
                false
            }
            data.password != data.confirmPassword -> {
                showToast(this, getString(R.string.warn_confirm_password_no_match))
                inputConfirmPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun validateInputsFromEmail(data: CreateProfileRequestBodyFromEmail): Boolean {
        return when {
            data.firstName.isEmpty() -> {
                showToast(this, getString(R.string.warn_empty_first_name))
                inputFName.requestFocus()
                false
            }
            data.lastName.isEmpty() -> {
                showToast(this, getString(R.string.warn_empty_last_name))
                inputLName.requestFocus()
                false
            }
            !Utilities.isValidEmail(data.email) -> {
                showToast(this, getString(R.string.warn_invalid_email))
                emailInput.requestFocus()
                false
            }
//            data.ssnLast4.length != 4 -> { // Assuming SSN should be 4 digits long
//                showToast(this, getString(R.string.warn_last_four_digits_of_ssn))
//                inputSsn4.requestFocus()
//                false
//            }
            data.password.isBlank() -> {
                showToast(this, getString(R.string.warn_empty_password))
                inputPassword.requestFocus()
                false
            }
            !checkPasswordStrength(data.password) -> {
                showToast(this, getString(R.string.warn_invalid_password))
                inputPassword.requestFocus()
                false
            }
            data.password != data.confirmPassword -> {
                showToast(this, getString(R.string.warn_confirm_password_no_match))
                inputConfirmPassword.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun checkPasswordStrength(password: String): Boolean {val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val isValidLength = password.length >= 8
        val hasDigitOrSpecialChar = password.any { !it.isLetterOrDigit() || it.isDigit() }


        if (hasUpperCase) {
            containUpperCase.setTextColor(ContextCompat.getColor(this, R.color.primary))
            containUpperCaseImg.setImageResource(R.drawable.tick)
        } else {
            containUpperCase.setTextColor(ContextCompat.getColor(this, cn.pedant.SweetAlert.R.color.red_btn_bg_color))
            containUpperCaseImg.setImageResource(R.drawable.cross_icon)
        }

        if (isValidLength) {
            lengthLeast8.setTextColor(ContextCompat.getColor(this, R.color.primary))
            lengthLeast8Img.setImageResource(R.drawable.tick)
        } else {
            lengthLeast8.setTextColor(ContextCompat.getColor(this, cn.pedant.SweetAlert.R.color.red_btn_bg_color))
            lengthLeast8Img.setImageResource(R.drawable.cross_icon)
        }

        if (hasDigitOrSpecialChar) {
            containNumberSpecialChar.setTextColor(ContextCompat.getColor(this, R.color.primary))
            containNumberSpecialCharImg.setImageResource(R.drawable.tick)
        } else {
            containNumberSpecialChar.setTextColor(ContextCompat.getColor(this, cn.pedant.SweetAlert.R.color.red_btn_bg_color))
            containNumberSpecialCharImg.setImageResource(R.drawable.cross_icon)
        }

        return if (isValidLength && hasUpperCase && hasDigitOrSpecialChar) {
            passStrength.text = buildString {
                append(getString(R.string.password_strength))
                append(": ")
                append(getString(R.string.strong))
            }
            passStrength.setTextColor(ContextCompat.getColor(this, R.color.primary))
            passStrengthImg.setImageResource(R.drawable.tick)
            true
        } else {
            passStrength.setTextColor(ContextCompat.getColor(this, cn.pedant.SweetAlert.R.color.red_btn_bg_color))
            passStrengthImg.setImageResource(R.drawable.cross_icon)
            false
        }
    }
}