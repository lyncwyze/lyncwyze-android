package com.intelly.lyncwyze.enteredUser.profileSetup

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Address2
import com.intelly.lyncwyze.Assest.modals.Location
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


class Activity_PS_Step2_Address : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var addressLine1: AutoCompleteTextView
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var locationAutoComplete: List<Location> = listOf()

    private lateinit var addressLine2: EditText
    private lateinit var addressLandmark: EditText
    private lateinit var addressState: EditText
    private lateinit var addressCity: EditText
    private lateinit var addressPinCode: EditText

    private lateinit var addressContinueBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step2_address)
        setUpUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setUpUI() {
        backButton = findViewById(R.id.addressBackButton)
        logoutButton = findViewById(R.id.logoutButton)

        addressLine1 = findViewById(R.id.addressLine1)
        addressLine2 = findViewById(R.id.addressLine2)
        addressLandmark = findViewById(R.id.addressLandmark)
        addressState = findViewById(R.id.addressState)
        addressCity = findViewById(R.id.addressCity)
        addressPinCode = findViewById(R.id.addressPinCode)
        addressContinueBtn = findViewById(R.id.addressContinueBtn)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }

        addressLine1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    if (s.toString().isNotEmpty()) {
                        val query = s.toString().trim()
                        fetchLocationSuggestions(query)
                    }
                }
                handler.postDelayed(searchRunnable!!, 250)
            }
            override fun afterTextChanged(s: Editable?) { }
        })
        addressLine1.setOnItemClickListener { _, _, position, _ ->
            fillTheSuggestedLocation(locationAutoComplete[position].description)
        }
        addressContinueBtn.setOnClickListener { getGeocodeAndSave() }
    }

    private fun getGeocodeAndSave() {
        validate()?.let { address ->
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val response = NetworkManager.apiService.getGeoCode(address = "${address.addressLine1}, ${address.addressLine2}, ${address.city}, ${address.state}, ${address.pincode}")
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        response.body()?.let {
                            address.location.coordinates = it[0].location.coordinates
                            address.location.type = it[0].location.type
                            saveHomeAddress(address)
                        } ?: showToast(this@Activity_PS_Step2_Address, getString(R.string.warn_invalid_address))
                    } else {
                        loader.hideLoader()
                        showToast(this@Activity_PS_Step2_Address, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader()
                    showToast(this@Activity_PS_Step2_Address, "${getString(R.string.exception_collen)} ${e.message}")
                }
            }
        }
    }
    private fun saveHomeAddress(address: Address2) {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_PS_Step2_Address)
                val response = NetworkManager.apiService.addAddress(body = address)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_PS_Step2_Address)
                    response.body()?.let {
                        logger.info { "Saved Address: $it" }

                        val loginRes = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                        if (loginRes != null) {
                            loginRes.profileStatus = ProfileStatus.PHOTO.value;
                            SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes)
                            showToast(this@Activity_PS_Step2_Address, getString(R.string.saved_successfully))
                            this@Activity_PS_Step2_Address.finish()
                        }
                    } ?: showToast(this@Activity_PS_Step2_Address, getString(R.string.unable_to_save))
                } else {
                    loader.hideLoader(this@Activity_PS_Step2_Address)
                    showToast(this@Activity_PS_Step2_Address, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_PS_Step2_Address)
                showToast(this@Activity_PS_Step2_Address, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
    private fun validate(): Address2? {
        val pinCodeValue = try { addressPinCode.text.toString().trim().toInt() } catch (e: NumberFormatException) { 0 }
        val address = Address2(
            userId = null,
            addressLine1 = addressLine1.text.trim().toString(),
            addressLine2 = addressLine2.text.trim().toString(),
            landMark = addressLandmark.text.trim().toString(),
            city = addressCity.text.trim().toString(),
            state = addressState.text.trim().toString(),
            pincode = pinCodeValue,
            location = Location2(coordinates = listOf(0.00, 0.00), type = "")
        )
        if (address.addressLine1.isBlank()) {
            showToast(this, getString(R.string.warn_invalid_address))
//            addressLine1.error = getString(R.string.invalid_address)
            addressLine1.requestFocus()
            return null
        }
        if (address.city.isBlank()) {
            showToast(this, getString(R.string.warn_empty_city))
//            addressCity.error = getString(R.string.warn_empty_city)
            addressCity.requestFocus()
            return null
        }
        if (address.state.isBlank()) {
            showToast(this, getString(R.string.warn_empty_state))
//            addressState.error = getString(R.string.warn_empty_state)
            addressState.requestFocus()
            return null
        }
        if (address.pincode <= 0) {
            showToast(this, getString(R.string.warn_invalid_zipcode))
            addressPinCode.error = getString(R.string.warn_invalid_zipcode) // Set error on EditText
            addressPinCode.requestFocus()
            return null
        }
        return address
    }

    private fun fetchLocationSuggestions(query: String) {
        logger.info { "fetchLocationSuggestions $query" }
        lifecycleScope.launch {
            try {
                val response = if (locationAutoComplete.isNotEmpty())
                    NetworkManager.apiService.getLocations(query, sessionToken = locationAutoComplete[0].sessionToken)
                else NetworkManager.apiService.getLocations(query)
                if (response.isSuccessful) {
                    response.body()?.let {
                        logger.info { "fetchLocationSuggestions $it" }
                        locationAutoComplete = it
                        updateAutoCompleteSuggestions()
                    }
                }
                else showToast(this@Activity_PS_Step2_Address, "${getString(R.string.error_txt)}: ${response.code()}")
            } catch (e: Exception) {
                showToast(this@Activity_PS_Step2_Address, "${getString(R.string.exception_collen)}: ${e.message}")
            }
        }
    }
    private fun updateAutoCompleteSuggestions() {
        logger.info { "updateAutoCompleteSuggestions: $locationAutoComplete" }
        val descriptions = locationAutoComplete.map { it.description }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, descriptions)
        addressLine1.setAdapter(adapter)
        adapter.notifyDataSetChanged()
    }
    private fun fillTheSuggestedLocation(suggestedAddress: String) {
        // Split the address into components based on commas
        val addressComponents = suggestedAddress.split(",").map { it.trim() }

        logger.info { addressComponents.size }

        // Extract city, state, and address lines based on your logic
        when (addressComponents.size) {
            0 -> { }
            1 -> {
                addressLine1.setText(addressComponents[0])
            }
            2 -> {
                addressLine1.setText(addressComponents[0])
                addressCity.setText(addressComponents[1])
            }
            3 -> {
                addressLine1.setText(addressComponents[0])
                addressCity.setText(addressComponents[1])
                addressState.setText(addressComponents[2])
            }
            4 -> {
                addressLine1.setText(addressComponents[0])
                addressCity.setText(addressComponents[1])
                addressState.setText(addressComponents[2])
                addressLandmark.setText(addressComponents[3])
            }
            else -> {
                addressLine1.setText(addressComponents[0])
                addressCity.setText(addressComponents[addressComponents.size - 2]) // Second last as city
                addressLandmark.setText(addressComponents[4])
                addressState.setText(addressComponents[addressComponents.size - 3]) // Third last as state
            }
        }
        // Assuming last component is the pinCode
        val pincode = addressComponents.lastOrNull()?.takeLastWhile { it.isDigit() } ?: ""
        if (pincode.isNotEmpty())
            addressPinCode.setText(pincode)

        addressLine1.requestFocus()
    }
}