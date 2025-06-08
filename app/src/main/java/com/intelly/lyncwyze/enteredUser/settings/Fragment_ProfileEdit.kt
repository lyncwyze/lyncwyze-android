package com.intelly.lyncwyze.enteredUser.settings

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Location
import com.intelly.lyncwyze.Assest.modals.UserProfile
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class Fragment_ProfileEdit : Fragment() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var profileInfo: UserProfile

    private lateinit var userImage: ImageView
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var email: TextInputEditText

    private lateinit var ccpEditCountryCode: CountryCodePicker
    private lateinit var inputMobileNumber: EditText

    // Address
    private lateinit var line1: MaterialAutoCompleteTextView
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var locationAutoComplete: List<Location> = listOf()
    private lateinit var locationAdapter: ArrayAdapter<String> // Initialize adapter here
    private lateinit var line2: TextInputEditText
    private lateinit var city: TextInputEditText
    private lateinit var state: TextInputEditText
    private lateinit var landmark: TextInputEditText
    private lateinit var pinCode: TextInputEditText
    private lateinit var save: Button


    private lateinit var galleryForProfileImage: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null
    private var isSettingAddress = false // Add this flag at class level
    private var textWatcher: TextWatcher? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment__profile_edit, container, false)
        galleryForProfileImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let {    // Uri
                    selectedImageUri = it
                    Glide.with(requireContext())
                        .load(it)
                        .circleCrop()
                        .error(R.drawable.default_user)
                        .into(userImage)
                    confirmUploadNewImage()
                }
            }
        }

        // Setup UI
        userImage = view.findViewById(R.id.userImage)
        userImage.setOnClickListener {
            showImageOptionsDialog()
        }
        firstName = view.findViewById(R.id.firstName)
        lastName = view.findViewById(R.id.lastName)
        email = view.findViewById(R.id.email)

        ccpEditCountryCode = view.findViewById(R.id.ccpEditCountryCode)
        inputMobileNumber = view.findViewById(R.id.editECMobileNumber)

        line1 = view.findViewById(R.id.line1)
        line2 = view.findViewById(R.id.line2)
        city = view.findViewById(R.id.city)
        state = view.findViewById(R.id.state)
        landmark = view.findViewById(R.id.landmark)
        pinCode = view.findViewById(R.id.pinCode)
        save = view.findViewById(R.id.save)
        save.setOnClickListener { getGeocodeAndSave() }

        // Set functionality
        // Initialize the ArrayAdapter here with an empty list initially
        locationAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        line1.setAdapter(locationAdapter)
        line1.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    if (s.toString().isNotEmpty()) {
                        logger.info { s }
                        val query = s.toString().trim()
                        fetchLocationSuggestions(query)
                    }
                }
                handler.postDelayed(searchRunnable!!, 250)
            }
            override fun afterTextChanged(s: Editable?) { }
        })
        line1.setOnItemClickListener { _, _, position, _ ->
            val selectedAddress = locationAutoComplete[position].description
            fillTheSuggestedLocation(selectedAddress)
            line1.dismissDropDown()
        }

        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpEditCountryCode.setCountryForNameCode(defaultCountryCode)
                ccpEditCountryCode.setCcpClickable(false)
            }
        }

        // Set CountryCodePicker text colors based on theme
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val themeTextColor = if (isDarkMode) Color.WHITE else Color.BLACK
        ccpEditCountryCode.apply {
            setContentColor(themeTextColor)
            setDialogTextColor(themeTextColor)
            setDialogBackgroundColor(if (isDarkMode) Color.parseColor("#121212") else Color.WHITE)
            setFlagBorderColor(themeTextColor)
            setArrowColor(themeTextColor)
        }

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchProfileDetails()
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
                else showToast(requireContext(), "${getString(R.string.error_txt)}: ${response.code()}")
            } catch (e: Exception) {
                showToast(requireContext(), "${getString(R.string.exception_collen)}: ${e.message}")
            }
        }
    }
    private fun updateAutoCompleteSuggestions() {
        val descriptions = locationAutoComplete.map { it.description }
        locationAdapter.clear()
        locationAdapter.addAll(descriptions)
        locationAdapter.notifyDataSetChanged()
        line1.showDropDown()
    }

    private fun fillTheSuggestedLocation(suggestedAddress: String) {
        // Split the address into components based on commas
        val addressComponents = suggestedAddress.split(",").map { it.trim() }

        when (addressComponents.size) {
            0 -> { }
            1 -> {
                line1.setText(addressComponents[0])
            }
            2 -> {
                line1.setText(addressComponents[0])
                city.setText(addressComponents[1])
            }
            3 -> {
                line1.setText(addressComponents[0])
                city.setText(addressComponents[1])
                state.setText(addressComponents[2])
            }
            4 -> {
                line1.setText(addressComponents[0])
                city.setText(addressComponents[1])
                state.setText(addressComponents[2])
                landmark.setText(addressComponents[3])
            }
            else -> {
                line1.setText(addressComponents[0])
                city.setText(addressComponents[addressComponents.size - 2]) // Second last as city
                landmark.setText(addressComponents[4])
                state.setText(addressComponents[addressComponents.size - 3]) // Third last as state
            }
        }
        // Assuming last component is the pinCode
        val zipCode: String = addressComponents.lastOrNull()?.takeLastWhile { it.isDigit() } ?: ""
        if (zipCode.isNotEmpty()) {
            pinCode.setText(zipCode)
        } else {
            pinCode.setText("")
        }

        // Hide keyboard and clear focus
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
        line1.clearFocus()
        city.clearFocus()
        state.clearFocus()
        landmark.clearFocus()
        pinCode.clearFocus()
    }

    private fun showImageOptionsDialog() {
        val options = profileInfo.image?.let { arrayOf("Upload New Image", "Delete Image") } ?: arrayOf("Upload New Image")
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.profile_image_option))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addNewImage()
                    1 -> deleteImage()
                }
            }.show()
    }
    private fun confirmUploadNewImage() {
        val options = arrayOf(getString(R.string.ok_Txt), getString(R.string.change_txt))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.want_to_change_profile_image))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> uploadImage()
                    1 -> addNewImage()
                }
            }.show()
    }
    private fun addNewImage() {
        galleryForProfileImage.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
    }
    private fun uploadImage() {
        lifecycleScope.launch {
            try {
                selectedImageUri?.let { uri ->
                    // Compress the image first
                    val compressedFile = Utilities.compressImage(requireContext(), uri)
                    if (compressedFile == null) {
                        showToast(requireContext(), "Failed to compress image")
                        return@launch
                    }

                    val reqBody = compressedFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", compressedFile.name, reqBody)

                    loader.showLoader()
                    val response = NetworkManager.apiService.addProfileImage(file = part)
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        showToast(requireContext(), getString(R.string.image_uploaded_successfully))
                        // Clean up the compressed file
                        compressedFile.delete()
                    } else {
                        loader.hideLoader()
                        showToast(requireContext(), "${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                }
            } catch (e: Exception) {
                loader.hideLoader()
                showToast(requireContext(), "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }

    private fun deleteImage() {
        lifecycleScope.launch {
            try {
                loader.showLoader()
                val response = NetworkManager.apiService.deleteProfileImage(SharedPreferencesManager.getDecodedAccessToken()!!.userId)
                if (response.isSuccessful) {
                    loader.hideLoader()
                    response.body()?.let {
                        profileInfo.image = null
                        displayInfo()
                    }
                } else {
                    loader.hideLoader()
                    showToast(requireContext(), "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader()
                showToast(requireContext(), e.message.toString())
            }
        }
    }

    private fun fetchProfileDetails() {
        lifecycleScope.launch {
            try {
                if (!isAdded) return@launch
                loader.showLoader()
                val response = NetworkManager.apiService.getUserById(SharedPreferencesManager.getDecodedAccessToken()!!.userId)
                if (!isAdded) return@launch
                if (response.isSuccessful) {
                    if (view != null) loader.hideLoader()
                    response.body()?.let {
                        profileInfo = it
                        displayInfo()
                    }
                } else {
                    if (view != null) loader.hideLoader()
                    if (context != null) {
                        showToast(requireContext(), "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                }
            } catch (e: Exception) {
                if (view != null) loader.hideLoader()
                if (context != null) {
                    showToast(requireContext(), e.message.toString())
                }
            }
        }
    }
    private fun displayInfo() {
        firstName.setText(profileInfo.firstName)
        lastName.setText(profileInfo.lastName)
        email.setText(profileInfo.email)

        val parts = profileInfo.mobileNumber.removePrefix("-").split("-", limit = 2).toMutableList()
        if (profileInfo.mobileNumber.startsWith("-"))
            parts[0] = "-${parts[0]}"
        ccpEditCountryCode.setCountryForPhoneCode(parts[0].toIntOrNull() ?: 0)
        inputMobileNumber.setText(parts.getOrNull(1) ?: parts.getOrNull(0) ?: "")

        profileInfo.image?.let { endpoint ->
            lifecycleScope.launch {
                ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                    try {
                        val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        Glide.with(requireContext())
                            .load(bitmap)
                            .circleCrop()
                            .error(R.drawable.default_user)
                            .into(userImage)
                    } catch (e: Exception) {
                        logger.error { "Error loading profile image: ${e.message}" }
                        Glide.with(requireContext())
                            .load(R.drawable.default_user)
                            .circleCrop()
                            .into(userImage)
                    }
                } ?: run { 
                    Glide.with(requireContext())
                        .load(R.drawable.default_user)
                        .circleCrop()
                        .into(userImage)
                }
            }
        } ?: Glide.with(requireContext())
            .load(R.drawable.default_user)
            .circleCrop()
            .into(userImage)
        if (profileInfo.addresses.isNotEmpty()) {
            val address = profileInfo.addresses[0]
            line1.setText(address.addressLine1)
            line2.setText(address.addressLine2 ?: "")
            city.setText(address.city)
            state.setText(address.state)
            landmark.setText(address.landMark ?: "")
            pinCode.setText(address.pincode.toString())
        }
    }

    private fun getGeocodeAndSave() {
        profileInfo.firstName = firstName.text.toString().trim()
        profileInfo.lastName = lastName.text.toString().trim()
        profileInfo.email = email.text.toString().trim()
        profileInfo.mobileNumber = inputMobileNumber.text.toString().trim()
        profileInfo.addresses[0].addressLine1 = line1.text.toString().trim()
        profileInfo.addresses[0].addressLine2 = line2.text.toString().trim()
        profileInfo.addresses[0].city = city.text.toString().trim()
        profileInfo.addresses[0].state = state.text.toString().trim()
        profileInfo.addresses[0].landMark = landmark.text.toString().trim()
        
        // Handle pin code safely
        val pinCodeStr = pinCode.text.toString().trim()
        profileInfo.addresses[0].pincode = if (pinCodeStr.isNotEmpty()) {
            try {
                pinCodeStr.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }

        validate(profileInfo)?.let {
            profileInfo.mobileNumber = "${ccpEditCountryCode.selectedCountryCodeWithPlus}-${inputMobileNumber.text.toString().trim()}"
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val fullAddress = "${profileInfo.addresses[0].addressLine1}, ${profileInfo.addresses[0].addressLine2}, ${profileInfo.addresses[0].city}, ${profileInfo.addresses[0].state}, ${profileInfo.addresses[0].pincode}"
                    val response = NetworkManager.apiService.getGeoCode(fullAddress)
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        response.body()?.let {
                            profileInfo.addresses[0].location.coordinates = it[0].location.coordinates
                            profileInfo.addresses[0].location.type = it[0].location.type
                            updateData()
                        } ?: showToast(requireContext(), getString(R.string.warn_invalid_address))
                    }
                    else {
                        loader.hideLoader()
                        showToast(requireContext(), "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader()
                    showToast(requireContext(), "${getString(R.string.exception_collen)} ${e.message}")
                }
            }
        }
    }
    private fun updateData() {
        lifecycleScope.launch {
            try {
                loader.showLoader()
                println(SharedPreferencesManager.gson.toJson(profileInfo))
                val response = NetworkManager.apiService.updateUser(SharedPreferencesManager.getDecodedAccessToken()!!.user_name!!, profileInfo)
                if (response.isSuccessful) {
                    loader.hideLoader()
                    response.body()?.let {
                        showToast(requireContext(), getString(R.string.update_successful))
                        requireActivity().onBackPressed()
                    }
                }
                else {
                    loader.hideLoader()
                    println(response)
                    showToast(requireContext(), "${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader()
                showToast(requireContext(), e.message.toString())
            }
    }
    }
    private fun validate(data: UserProfile): UserProfile? {
        // First Name validation
        if (data.firstName.isBlank()) {
            firstName.error = "First name is required"
            firstName.requestFocus()
            return null
        }

        // Last Name validation
        if (data.lastName.isBlank()) {
            lastName.error = "Last name is required"
            lastName.requestFocus()
            return null
        }

        // Email validation
        if (data.email.isBlank()) {
            email.error = "Email is required"
            email.requestFocus()
            return null
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) {
            email.error = "Please enter a valid email address"
            email.requestFocus()
            return null
        }

        // Mobile number validation
        if (data.mobileNumber.isBlank()) {
            inputMobileNumber.error = "Phone number is required"
            inputMobileNumber.requestFocus()
            return null
        }

        if (data.addresses[0].addressLine1 == null){
            data.addresses[0].addressLine1 = ""
        }
        // Address Line 1 validation
        if (data.addresses[0].addressLine1!!.isBlank()) {
            line1.error = "Address line 1 is required"
            line1.requestFocus()
            return null
        }

        // City validation
        if (data.addresses[0].city.isBlank()) {
            city.error = "City is required"
            city.requestFocus()
            return null
        }

        // State validation
        if (data.addresses[0].state.isBlank()) {
            state.error = "State is required"
            state.requestFocus()
            return null
        }

        // Pin code validation
        val pinCodeStr = pinCode.text.toString().trim()
        if (pinCodeStr.isBlank()) {
            pinCode.error = "Pin code is required"
            pinCode.requestFocus()
            return null
        }
        try {
            val pinCodeInt = pinCodeStr.toInt()
            if (pinCodeInt <= 0 || pinCodeStr.length != 6) {
                pinCode.error = "Please enter a valid 6-digit pin code"
                pinCode.requestFocus()
                return null
            }
            data.addresses[0].pincode = pinCodeInt
        } catch (e: NumberFormatException) {
            pinCode.error = "Please enter a valid pin code"
            pinCode.requestFocus()
            return null
        }

        return data
    }
}