package com.intelly.lyncwyze.enteredUser.emergencyContacts

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.EmergencyContact
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CONTACT_ID
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Activity_EmergencyContact_Edit : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private var loader = Loader(this)

    private lateinit var ecId: String

    private lateinit var backButton: ImageView
    private lateinit var inputFirstName: EditText
    private lateinit var inputLastName: EditText

    private lateinit var ccpEditCountryCode: CountryCodePicker
    private lateinit var inputMobileNumber: EditText

    private lateinit var inputEmail: EditText

    private lateinit var ecEditUpdate: Button
    private lateinit var ecEditDelete: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergency_contact_edit)
        setupActivity()
        setupUI()
        setupFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupActivity() {
        intent.getStringExtra(CONTACT_ID)?.let {
            ecId = it
            fetchContact()
        } ?: {
            showToast(this, getString(R.string.error_getting_contact_id))
        }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.viewECBackButton)

        inputFirstName = findViewById(R.id.editECFName)
        inputLastName = findViewById(R.id.editECLName)

        ccpEditCountryCode = findViewById(R.id.ccpEditCountryCode)
        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpEditCountryCode.setCountryForNameCode(defaultCountryCode)
                ccpEditCountryCode.setCcpClickable(false)
            }
        }
        // Set CountryCodePicker text colors based on theme
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        if (isDarkMode) {
            ccpEditCountryCode.setContentColor(Color.WHITE)
            ccpEditCountryCode.setDialogTextColor(Color.WHITE)
            ccpEditCountryCode.setDialogBackgroundColor(Color.parseColor("#1C1B1F"))
        } else {
            ccpEditCountryCode.setContentColor(Color.BLACK)
            ccpEditCountryCode.setDialogTextColor(Color.BLACK)
            ccpEditCountryCode.setDialogBackgroundColor(Color.WHITE)
        }

        inputMobileNumber = findViewById(R.id.editECMobileNumber)
        inputEmail = findViewById(R.id.editECEmail)

        ecEditUpdate = findViewById(R.id.ecEditUpdate)
        ecEditDelete = findViewById(R.id.ecEditDelete)
    }
    private fun setupFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        ecEditUpdate.setOnClickListener { updateContact() }
        ecEditDelete.setOnClickListener { deleteContact() }
    }
    private fun fetchContact() {
        loader.showLoader(this@Activity_EmergencyContact_Edit)
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getEmergencyContactByID(emergencyContactId = ecId)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_EmergencyContact_Edit)
                    val contact = response.body()
                    if (contact != null)
                        displayEmergencyContact(contact)
                    else
                        showToast(this@Activity_EmergencyContact_Edit, getString(R.string.fetching_error))
                } else {
                    loader.hideLoader(this@Activity_EmergencyContact_Edit)
                    showToast(this@Activity_EmergencyContact_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_EmergencyContact_Edit)
                showToast(this@Activity_EmergencyContact_Edit, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun displayEmergencyContact(contact: EmergencyContact) {
        inputFirstName.setText(contact.firstName ?: "")
        inputLastName.setText(contact.lastName ?: "")

        val parts = contact.mobileNumber.removePrefix("-").split("-", limit = 2).toMutableList()
        if (contact.mobileNumber.startsWith("-"))
            parts[0] = "-${parts[0]}"
        ccpEditCountryCode.setCountryForPhoneCode(parts[0].toIntOrNull() ?: 0)
        inputMobileNumber.setText(parts.getOrNull(1) ?: parts.getOrNull(0) ?: "")

        inputEmail.setText(contact.email ?: "")
    }
    private fun updateContact() {
        val firstName = inputFirstName.text.trim().toString();
        val lastName = inputLastName.text.trim().toString();
        val phoneNumber = inputMobileNumber.text.trim().toString();
        val email = inputEmail.text.trim().toString();

        if(validateData(firstName, lastName, phoneNumber, email)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val body = EmergencyContact(id = ecId, name = null, firstName = firstName, lastName = lastName, email = email, mobileNumber = "${ccpEditCountryCode.selectedCountryCodeWithPlus}-$phoneNumber")

                    val response = NetworkManager.apiService.updateEmergencyContact(body)
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        val contact = response.body()
                        if (contact != null) {
                            showToast(this@Activity_EmergencyContact_Edit, "Updated successfully")
                            this@Activity_EmergencyContact_Edit.finish()
                        } else
                            showToast(this@Activity_EmergencyContact_Edit, getString(R.string.fetching_error))
                    } else {
                        loader.hideLoader()
                        showToast(this@Activity_EmergencyContact_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader()
                    showToast(
                        this@Activity_EmergencyContact_Edit,
                        "${getString(R.string.exception_collen)} ${e.message}"
                    )
                }
            }
        }
    }
    private fun validateData(fName: String, lName: String, pNo: String, email: String): Boolean {
        if (fName.isBlank()) {
            showToast(this, getString(R.string.warn_empty_first_name))
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
            showToast(this, getString(R.string.warn_empty_phone))
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
    private fun deleteContact() {
        loader.showLoader(this@Activity_EmergencyContact_Edit)
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.deleteEmergencyContact(emergencyContactId = ecId)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_EmergencyContact_Edit)
                    val contact = response.body()
                    if (contact != null && contact) {
                        showToast(this@Activity_EmergencyContact_Edit, "Deleted successfully")
                        this@Activity_EmergencyContact_Edit.finish()
                    } else
                        showToast(this@Activity_EmergencyContact_Edit, getString(R.string.fetching_error))
                } else {
                    loader.hideLoader(this@Activity_EmergencyContact_Edit)
                    showToast(this@Activity_EmergencyContact_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_EmergencyContact_Edit)
                showToast(this@Activity_EmergencyContact_Edit, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
}