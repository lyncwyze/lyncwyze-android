package com.intelly.lyncwyze.enteredUser.profileSetup

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Address2
import com.intelly.lyncwyze.Assest.modals.BackgroundVerificationRequestBody
import com.intelly.lyncwyze.Assest.modals.Location2
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
import mu.KotlinLogging
import java.util.Calendar


class Activity_PS_Step2 : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var inputSSN: EditText
    private lateinit var licensedState: EditText
    private lateinit var licenseNumber: EditText
    private lateinit var inputExpDate: EditText
    private lateinit var inputHomeAdd: EditText
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step2)
        setUpUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setUpUI() {
        findViewById<TextView>(R.id.work_title).text = if (resources.getString(R.string.profile_setup_2_isbackgroundverification).toBoolean()) "Background verification" else "License details"
        backButton = findViewById(R.id.aclBackButton)
        logoutButton = findViewById(R.id.logoutButton)
        inputSSN = findViewById(R.id.input_ssn)
        licensedState = findViewById(R.id.licensedState)
        licenseNumber = findViewById(R.id.licenseNumber)
        inputExpDate = findViewById(R.id.input_expDate)
        inputHomeAdd = findViewById(R.id.input_homeAdd)
        continueButton = findViewById(R.id.aclContinueBtn)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }
        inputExpDate.setOnClickListener { showDatePickerDialog() }
        continueButton.setOnClickListener { saveAPI() }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            inputExpDate.setText(String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay))
        }, year, month, day).show()
    }
    private fun saveAPI() {
        val ssn = inputSSN.text.toString().trim()
        val state = licensedState.text.toString().trim()
        val lNum = licenseNumber.text.toString().trim()
        val expDate = inputExpDate.text.toString().trim()
        val homeAdd = inputHomeAdd.text.toString().trim()
        if (validateInputs(ssn, state, lNum, expDate, homeAdd))
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_PS_Step2)
                    // TODO: Add address suggestion (not mandatory to select from it)
                    val response = NetworkManager.apiService.getGeoCode(address = homeAdd)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_PS_Step2)
                        val body = response.body()
                        if (!body.isNullOrEmpty()) {
                            val address = Address2(addressLine1 = homeAdd, addressLine2 = null, landMark = null, pincode = 11111, state = state, city = "test city", userId = null, location = Location2(coordinates = body[0].location.coordinates, type = "Point"))
                            val apiBody = BackgroundVerificationRequestBody(ssn = ssn, licenseNumber = lNum, licenseState = state, licenseExpiredDate = expDate, address = address)
                            hitBackgroundVerification(apiBody)
                        } else {
                            loader.hideLoader(this@Activity_PS_Step2)
                            showToast(this@Activity_PS_Step2, getString(R.string.warn_invalid_address))
                        }
                    } else {
                        loader.hideLoader(this@Activity_PS_Step2)
                        showToast(this@Activity_PS_Step2, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Activity_PS_Step2)
                    showToast(this@Activity_PS_Step2, "${getString(R.string.exception_collen)} ${e.message}")
                }
            }
    }
    private fun validateInputs(ssn: String, state: String, lNum: String, expDate: String, homeAdd: String): Boolean {
        if (ssn.isEmpty() || ssn.length != 10) { // Assuming SSN should be 10 digits
            showToast(this, "Please enter a valid SSN (10 digits).")
            inputSSN.requestFocus()
            return false
        }
        if (state.isEmpty()) {
            showToast(this, "Please enter a valid state.")
            licensedState.requestFocus()
            return false
        }
        if (lNum.isEmpty()) {
            showToast(this, "Please enter a valid license number.")
            licenseNumber.requestFocus()
            return false
        }
        if (expDate.isEmpty()) {
            showToast(this, "Please enter a valid expiration date.")
            inputExpDate.performClick()
            return false
        }
        if (homeAdd.isEmpty()) {
            showToast(this, "Please enter a valid home address.")
            inputHomeAdd.requestFocus()
            return false
        }
        return true
    }
    private fun hitBackgroundVerification(apiBody: BackgroundVerificationRequestBody) {
        lifecycleScope.launch {
            try {
                logger.info { "API body $apiBody" }
                loader.showLoader(this@Activity_PS_Step2)
                // addUserAddress
                val response = NetworkManager.apiService.backgroundVerification(body = apiBody)

                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_PS_Step2)
                    val body = response.body()
                    if (body != null) {
                        val loginRes = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                        if (loginRes != null) {
                            loginRes.profileStatus = ProfileStatus.PHOTO.value
                            SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes)
                        }
                        SweetAlertDialog(this@Activity_PS_Step2, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText(getString(R.string.details_added))
                            .setContentText("Please proceed to add a profile image")
                            .setConfirmText(getString(R.string.ok_Txt))
                            .setConfirmClickListener { sDialog ->
                                sDialog.dismissWithAnimation()
                                this@Activity_PS_Step2.finish()
                            }
                            .show()

                    }
                } else {
                    loader.hideLoader(this@Activity_PS_Step2)
                    showToast(this@Activity_PS_Step2, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                logger.error { e.message }
                loader.hideLoader(this@Activity_PS_Step2)
                showToast(this@Activity_PS_Step2, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
}

