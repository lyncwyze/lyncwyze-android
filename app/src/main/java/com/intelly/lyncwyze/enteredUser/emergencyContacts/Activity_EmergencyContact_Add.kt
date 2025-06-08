package com.intelly.lyncwyze.enteredUser.emergencyContacts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.DataCountResp
import com.intelly.lyncwyze.Assest.modals.EmergencyContact
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.UserRequiredDataCount
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import kotlinx.coroutines.launch
import mu.KotlinLogging
import android.content.res.Configuration
import android.graphics.Color

class Activity_EmergencyContact_Add : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private var loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var inputFirstName: EditText
    private lateinit var inputLastName: EditText
    private lateinit var ccpAddContact: CountryCodePicker
    private lateinit var inputMobileNumber: EditText
    private lateinit var inputEmail: EditText
    private lateinit var SaveContact: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergency_contact_add)
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun setupUI() {
        backButton = findViewById(R.id.aclBackButton)
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.visibility = View.GONE

        inputFirstName = findViewById(R.id.editTextFirstName)
        inputLastName = findViewById(R.id.editTextLastName)

        ccpAddContact = findViewById(R.id.ccpAddContact)
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpAddContact.setCountryForNameCode(defaultCountryCode)
                ccpAddContact.setCcpClickable(false)
            }
        }
        
        // Set CountryCodePicker text colors based on theme
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (isDarkMode) {
            ccpAddContact.setContentColor(Color.WHITE)
            ccpAddContact.setDialogTextColor(Color.WHITE)
            ccpAddContact.setDialogBackgroundColor(Color.parseColor("#1C1B1F"))
        } else {
            ccpAddContact.setContentColor(Color.BLACK)
            ccpAddContact.setDialogTextColor(Color.BLACK)
            ccpAddContact.setDialogBackgroundColor(Color.WHITE)
        }

        inputMobileNumber = findViewById(R.id.mobileNumber)
        inputEmail = findViewById(R.id.editTextEmail)
        SaveContact = findViewById(R.id.buttonSaveContact)
        if (SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!.emergencyContact < 1) {
            SaveContact.text = "Next: You are ready to schedule ride (8/8)"
            logoutButton.visibility = View.VISIBLE
        }
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }
        SaveContact.setOnClickListener { saveContact()}
    }

    private fun saveContact() {
        val firstName = inputFirstName.text.trim().toString()
        val lastName = inputLastName.text.trim().toString()
        var phoneNumber = inputMobileNumber.text.trim().toString()
        val email = inputEmail.text.trim().toString()

        if (validateData(firstName, lastName, phoneNumber, email)) {
            phoneNumber =  "${ccpAddContact.selectedCountryCodeWithPlus}-$phoneNumber"
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val body = EmergencyContact(id = null, name= null, firstName, lastName, phoneNumber, email)

                    val response = NetworkManager.apiService.addEmergencyContact(body)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_EmergencyContact_Add)
                        response.body()?.let {
                            val dataCount = SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!
                            showToast(this@Activity_EmergencyContact_Add, getString(R.string.saved_successfully))
                            if (dataCount.emergencyContact < 1) {
                                dataCount.emergencyContact += 1
                                SharedPreferencesManager.saveObject(UserRequiredDataCount, dataCount)

                                val intent = Intent(this@Activity_EmergencyContact_Add, Dashboard::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
                                startActivity(intent)
                            }
                            else this@Activity_EmergencyContact_Add.finish()
                        }
                    } else {
                        val error = HttpUtilities.parseError(response)
                        loader.hideLoader(this@Activity_EmergencyContact_Add)
                        showToast(this@Activity_EmergencyContact_Add, "${getString(R.string.error_txt)}: ${response.code()}, ${error?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Activity_EmergencyContact_Add)
                    showToast(this@Activity_EmergencyContact_Add, "Exception: ${e.message}")
                }
            }
        }
    }
    private fun validateData (fName: String, lName: String, pNo: String, email: String): Boolean {
        if (fName.isBlank()) {
            showToast(this, getString(R.string.warn_empty_last_name))
            inputFirstName.requestFocus()
            return false
        }
        if (lName.isBlank()) {
            showToast(this, getString(R.string.warn_empty_last_name))
            inputLastName.requestFocus()
            return false
        }
        if (pNo.isBlank()) {
            showToast(this, getString(R.string.warn_empty_phone))
            inputMobileNumber.requestFocus()
            return false
        }
        if (!Utilities.isPhoneNumberValid(pNo)) {
            showToast(this, getString(R.string.warn_invalid_phone))
            inputMobileNumber.requestFocus()
            return false
        }
        if (email.isNotBlank() && !Utilities.isValidEmail(email)) {
            showToast(this, getString(R.string.warn_invalid_email))
            inputEmail.requestFocus()
            return false
        }
        return true
    }
}