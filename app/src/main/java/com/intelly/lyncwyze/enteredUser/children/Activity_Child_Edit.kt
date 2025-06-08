package com.intelly.lyncwyze.enteredUser.children

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Gender
import com.intelly.lyncwyze.Assest.modals.SaveChild
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CAMERA_REQUEST_CODE
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.convertFromFormattedToIso
import com.intelly.lyncwyze.Assest.utilities.convertToFormattedDate
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale


class Activity_Child_Edit : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private var loader = Loader(this)

    private lateinit var editChildBack: ImageButton

    private lateinit var childId: String
    private lateinit var childDetails: SaveChild

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var editChildImage: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var editFirstName: EditText
    private lateinit var editLastName: EditText
    private lateinit var editChildDOB: EditText
    private lateinit var editChildGenderSpinner: Spinner
    private lateinit var editChildPhoneNumberCode: CountryCodePicker
    private lateinit var editChildPhoneNumber: EditText
    private lateinit var editChildFrontRide: LinearLayout
    private lateinit var editChildFrontRideSwitch: SwitchCompat

    private lateinit var editChildBoosterSeat: LinearLayout
    private lateinit var editChildBoosterSeatSwitch: SwitchCompat

    private lateinit var editChildSaveChild: Button
    private lateinit var cvButtonDelete: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_child)
        setupUI()
        setupFunctionality()
        appCheck()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun appCheck() {
        val forChildId = intent.getStringExtra(CHILD_ID)
        if (forChildId.isNullOrBlank()) {
            showToast(this@Activity_Child_Edit, getString(R.string.error_getting_child_id))
            this.onBackPressed()
        } else {
            childId = forChildId
            fetchChild()
        }
        setupActivityResultLaunchers()
    }
    private fun setupUI() {
        editChildBack = findViewById(R.id.editChildBack)
        editChildImage = findViewById(R.id.editChildImage)
        editFirstName = findViewById(R.id.editFirstName)
        editLastName = findViewById(R.id.editLastName)
        editChildDOB = findViewById(R.id.editChildDOB)
        editChildGenderSpinner = findViewById(R.id.editChildGenderSpinner)
        editChildPhoneNumberCode = findViewById(R.id.editChildPhoneNumberCode)
        editChildPhoneNumber = findViewById(R.id.editChildPhoneNumber)
        editChildFrontRide = findViewById(R.id.editChildFrontRide)
        editChildFrontRideSwitch = findViewById(R.id.editChildFrontRideSwitch)
        editChildBoosterSeat = findViewById(R.id.editChildBoosterSeat)
        editChildBoosterSeatSwitch = findViewById(R.id.editChildBoosterSeatSwitch)
        editChildSaveChild = findViewById(R.id.editChildSaveChild)
        cvButtonDelete = findViewById(R.id.cvButtonDelete)

        // Handle dark mode
        updateUIForTheme()
    }

    private fun updateUIForTheme() {
        val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        // Set tint for back button and default user image
        val tintColor = if (isNightMode) {
            ContextCompat.getColor(this, android.R.color.white)
        } else {
            ContextCompat.getColor(this, android.R.color.black)
        }
        
        editChildBack.setColorFilter(tintColor)

        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                editChildPhoneNumberCode.setCountryForNameCode(defaultCountryCode)
                editChildPhoneNumberCode.setCcpClickable(false)
            }
        }

        // Set CountryCodePicker colors for dark mode
        if (isNightMode) {
            editChildPhoneNumberCode.setContentColor(ContextCompat.getColor(this, android.R.color.white))
            editChildPhoneNumberCode.setDialogTextColor(ContextCompat.getColor(this, android.R.color.white))
            
            // Only tint the default user image if it's being used
            if (editChildImage.drawable.constantState == ContextCompat.getDrawable(this, R.drawable.default_user)?.constantState) {
                // editChildImage.setColorFilter(tintColor)
            }
        } else {
            editChildImage.clearColorFilter()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateUIForTheme()
    }

    private fun setupFunctionality() {
        editChildBack.setOnClickListener { this.onBackPressed() }
        editChildImage.setOnClickListener { showImageOptions() }

        val genderArray = Gender.entries.map { getLocalizedText(context = this, it.name) }.toTypedArray()
        ArrayAdapter(this, android.R.layout.simple_spinner_item, genderArray)
        .also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            editChildGenderSpinner.adapter = adapter
        }
        cvButtonDelete.setOnClickListener { deleteChild() }
        editChildSaveChild.setOnClickListener { updateChild() }
        editChildDOB.setOnClickListener { showDatePickerDialog() }
    }


    private fun fetchChild() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_Child_Edit)
                val response = NetworkManager.apiService.getChildById(childId = childId)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_Child_Edit)
                    response.body()?.let {
                        childDetails = it
                        childDetails.image?.let { detail ->
                            lifecycleScope.launch {
                                try {
                                    ImageUtilities.imageFullPath(detail)?.let { base64Data ->
                                        try {
                                            val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                                            val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                            Glide.with(this@Activity_Child_Edit)
                                                .load(bitmap)
                                                .circleCrop()
                                                .error(R.drawable.default_user)
                                                .into(editChildImage)
                                        } catch (e: Exception) {
                                            logger.error { "Error decoding image: ${e.message}" }
                                            editChildImage.setImageResource(R.drawable.default_user)
                                        }
                                    } ?: run {
                                        editChildImage.setImageResource(R.drawable.default_user)
                                    }
                                } catch (e: Exception) {
                                    logger.error { "Error loading image: ${e.message}" }
                                    editChildImage.setImageResource(R.drawable.default_user)
                                }
                            }
                        } ?: run {
                            editChildImage.setImageResource(R.drawable.default_user)
                        }
                        editChildImage.visibility = View.VISIBLE
                        
                        editFirstName.setText(it.firstName)
                        editLastName.setText(it.lastName)
                        
                        // Convert yyyy-MM-dd to dd-MMM-yyyy
                        val dateParts = it.dateOfBirth.split("-")
                        if (dateParts.size == 3) {
                            val year = dateParts[0]
                            val month = dateParts[1]
                            val day = dateParts[2]
                            editChildDOB.setText(convertToFormattedDate("$day-$month-$year"))
                        } else
                            editChildDOB.setText(it.dateOfBirth)

                        val genderArray = Gender.entries.map { gen -> getLocalizedText(context = this@Activity_Child_Edit, gen.name) }.toTypedArray()
                        ArrayAdapter(this@Activity_Child_Edit, android.R.layout.simple_spinner_item, genderArray)
                        .also { adapter ->
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            val spinnerPosition = adapter.getPosition(getLocalizedText(this@Activity_Child_Edit, it.gender.name))
                            if (spinnerPosition >= 0)
                                editChildGenderSpinner.setSelection(spinnerPosition)
                        }

                        val parts = it.mobileNumber?.let { number ->
                            number.removePrefix("-").split("-", limit = 2).toMutableList()
                        } ?: mutableListOf("0", "")

                        if (it.mobileNumber?.startsWith("-") == true) {
                            parts[0] = "-${parts[0]}"
                        }
                        editChildPhoneNumberCode.setCountryForPhoneCode(parts[0].toIntOrNull() ?: 0)
                        editChildPhoneNumber.setText(parts.getOrNull(1) ?: parts.getOrNull(0) ?: "")


                        editChildFrontRideSwitch.isChecked = it.rideInFront
                        editChildBoosterSeatSwitch.isChecked = it.boosterSeatRequired
                    } ?: showToast(this@Activity_Child_Edit, "Fetching error!")
                } else {
                    loader.hideLoader(this@Activity_Child_Edit)
                    showToast(this@Activity_Child_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_Child_Edit)
                logger.error { e.message }
                showToast(this@Activity_Child_Edit, "Exception: ${e.message}")
            }
        }
    }
    private fun updateChild() {
        val firstName = editFirstName.text.toString().trim()
        val lastName = editLastName.text.toString().trim()
        val dateOfBirth = convertFromFormattedToIso(editChildDOB.text.toString().trim())

        val mobileNumber = editChildPhoneNumber.text.toString().trim()

        val selectedGenderPosition = editChildGenderSpinner.selectedItemPosition
        val gender: Gender? = if (selectedGenderPosition != 0) Gender.fromString(editChildGenderSpinner.getItemAtPosition(selectedGenderPosition).toString()) else null
        val rideInFront = editChildFrontRideSwitch.isChecked
        val boosterSeatRequired = editChildBoosterSeatSwitch.isChecked

        if (validateData(firstName, lastName, dateOfBirth, mobileNumber, gender)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader(this@Activity_Child_Edit)

                    childDetails.id = childId
                    childDetails.firstName = firstName
                    childDetails.lastName = lastName
                    childDetails.dateOfBirth = dateOfBirth
                    childDetails.gender = gender!!
                    childDetails.mobileNumber = "${editChildPhoneNumberCode.selectedCountryCodeWithPlus}-$mobileNumber"
                    childDetails.rideInFront = rideInFront
                    childDetails.boosterSeatRequired = boosterSeatRequired

                    val requestBody = Gson().toJson(childDetails).toRequestBody("application/json".toMediaTypeOrNull())

                    var imagePart: MultipartBody.Part? = null
                    var compressedFile: File? = null
                    selectedImageUri?.let { uri ->
                        // Compress the image first
                        compressedFile = Utilities.compressImage(applicationContext, uri)
                        if (compressedFile == null) {
                            showToast(this@Activity_Child_Edit, "Failed to compress image")
                            return@launch
                        }

                        val reqBody = compressedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("file", compressedFile!!.name, reqBody)
                        imagePart = part
                    }

                    val response = NetworkManager.apiService.updateChild(child = requestBody, image = imagePart)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_Child_Edit)
                        val body = response.body()
                        if (body != null) {
                            logger.info { "Saved child: $body" }
                            // Clean up the compressed file
                            compressedFile?.delete()
                            showToast(this@Activity_Child_Edit, "Details updated!")
                            this@Activity_Child_Edit.onBackPressed()
                        }
                    } else {
                        loader.hideLoader(this@Activity_Child_Edit)
                        showToast(this@Activity_Child_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    logger.error { e.message }
                    loader.hideLoader(this@Activity_Child_Edit)
                    showToast(this@Activity_Child_Edit, e.message.toString())
                }
            }
        }
    }
    private fun validateData(firstName: String, lastName: String, dob: String, mobileNumber: String, gender: Gender?): Boolean {
        if (firstName.isEmpty()) {
            showToast(this, "First name cannot be empty")
            editFirstName.requestFocus()
            return false
        }
        if (lastName.isEmpty()) {
            showToast(this, "Last name cannot be empty")
            editLastName.requestFocus()
            return false
        }
        if (dob.isEmpty() || Utilities.isFutureDate(dob)) {
            showToast(this, "Date of birth is required and must be in past")
            editChildDOB.performClick()
            return false
        }
        if (mobileNumber.isNotEmpty()) {
            if (!Utilities.isPhoneNumberValid(mobileNumber)) {
                showToast(this, "Phone number not valid")
                editChildPhoneNumber.requestFocus()
                return false
            }
        }
        if (gender == null) {
            showToast(this, "Please select a gender")
            editChildGenderSpinner.performClick()
            return false
        }
        return true
    }
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val dobText = editChildDOB.text.toString()
        if (dobText.isNotBlank()) {
            try {
                val parsedDate = LocalDate.parse(dobText, DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH))
                calendar.set(parsedDate.year, parsedDate.monthValue - 1, parsedDate.dayOfMonth)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val rawDate = "$selectedDay-${selectedMonth + 1}-$selectedYear"
            editChildDOB.setText(convertToFormattedDate(rawDate))
        }, year, month, day)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showImageOptions () {
        AlertDialog.Builder(this)
            .setTitle("Choose Image Source")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }.show()
    }
    private fun setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    val uri = ImageUtilities.saveBitmapToCache(this@Activity_Child_Edit, it)
                    selectedImageUri = uri
                    editChildImage.setImageURI(uri)
                }
            }
        }
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedImageUri = it
                    editChildImage.setImageURI(it)
                }
            }
        }
    }
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        else
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    private fun deleteChild() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_Child_Edit)
                val response = NetworkManager.apiService.deleteChild(childId = childId)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_Child_Edit)
                    val deleted = response.body()
                    if (deleted != null && deleted == true)  {
                        showToast(this@Activity_Child_Edit, getString(R.string.child_deleted_successfully))
                        this@Activity_Child_Edit.onBackPressed()
                    } else showToast(this@Activity_Child_Edit, getString(R.string.unable_to_delete_child))
                } else {
                    loader.hideLoader(this@Activity_Child_Edit)
                    showToast(this@Activity_Child_Edit, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_Child_Edit)
                logger.error { e.message }
                showToast(this@Activity_Child_Edit, e.message.toString())
            }
        }
    }
}