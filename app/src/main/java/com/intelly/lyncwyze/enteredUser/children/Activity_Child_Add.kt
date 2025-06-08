package com.intelly.lyncwyze.enteredUser.children

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.google.gson.Gson
import com.hbb20.CountryCodePicker
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.DataCountResp
import com.intelly.lyncwyze.Assest.modals.Gender
import com.intelly.lyncwyze.Assest.modals.SaveChildRequest
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CAMERA_REQUEST_CODE
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.CHILD_NAME
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.PICK_IMAGE_REQUEST
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.UserRequiredDataCount
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.convertFromFormattedToIso
import com.intelly.lyncwyze.Assest.utilities.convertToFormattedDate
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.childActivity.Activity_Activities_Add
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import com.bumptech.glide.Glide
import android.widget.ImageButton
import androidx.transition.Visibility


class Activity_Child_Add : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private var loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var childDP: ImageView
    private lateinit var adAddDPBtn: Button
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    private lateinit var editTextFirstName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var inputDOB: EditText
    private lateinit var spinnerGender: Spinner

    private lateinit var ccpAddChildCcd: CountryCodePicker
    private lateinit var phoneNumber: EditText

    private lateinit var frontRideSwitch: SwitchCompat
    private lateinit var boosterSeatSwitch: SwitchCompat
    private lateinit var saveChildButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_children_add)
        activityCheck()
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        activityCheck()
    }

    private fun activityCheck() {}
    private fun setupUI() {
        backButton = findViewById(R.id.acBackButton)
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.visibility = View.GONE

        childDP = findViewById(R.id.adAddDP)
        adAddDPBtn = findViewById(R.id.adAddDPBtn)

        editTextFirstName = findViewById(R.id.acFirstName)
        editTextLastName = findViewById(R.id.acLastName)
        inputDOB = findViewById(R.id.acInputDOB)
        spinnerGender = findViewById(R.id.spinnerGender)

        ccpAddChildCcd = findViewById(R.id.ccpAddChildCcd)
        
        // Set text color based on night mode
        val isNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (isNightMode) {
            ccpAddChildCcd.setContentColor(ContextCompat.getColor(this, android.R.color.white))
            ccpAddChildCcd.setDialogTextColor(ContextCompat.getColor(this, android.R.color.white))
        }

        resources.getString(R.string.defaultCountryCode).let { defaultCountryCode ->
            if (defaultCountryCode.isNotBlank()) {  // Available in local props
                ccpAddChildCcd.setCountryForNameCode(defaultCountryCode)
                ccpAddChildCcd.setCcpClickable(false)
            }
        }
        phoneNumber = findViewById(R.id.phoneNumber)

        frontRideSwitch = findViewById(R.id.frontRideSwitch)
        boosterSeatSwitch = findViewById(R.id.boosterSeatSwitch)

        saveChildButton = findViewById(R.id.saveChild)
        if (SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!.child < 1) {
            saveChildButton.text = "Next: Add Child Activity (5/8)"
            logoutButton.visibility = View.VISIBLE
        }
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }
        childDP.setOnClickListener { showImageOptions() }
        adAddDPBtn.setOnClickListener { showImageOptions() }
        inputDOB.setOnClickListener { showDatePickerDialog() }

        val genderArray = Gender.entries.map { getLocalizedText(context = this, it.name) }.toTypedArray()
        ArrayAdapter(this, android.R.layout.simple_spinner_item, genderArray)
        .also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGender.adapter = adapter
        }
        saveChildButton.setOnClickListener { saveChildNow() }
        setupActivityResultLaunchers()
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
                    val uri = ImageUtilities.saveBitmapToCache(this@Activity_Child_Add, it)
                    selectedImageUri = uri
                    Glide.with(this@Activity_Child_Add)
                        .load(uri)
                        .circleCrop()
                        .error(R.drawable.default_user)
                        .into(childDP)
                }
            }
        }
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedImageUri = it
                    Glide.with(this@Activity_Child_Add)
                        .load(it)
                        .circleCrop()
                        .error(R.drawable.default_user)
                        .into(childDP)
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

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val dobText = inputDOB.text.toString()
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
            inputDOB.setText(convertToFormattedDate(rawDate))
        }, year, month, day)
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun saveChildNow() {
        val firstName = editTextFirstName.text.toString().trim()
        val lastName = editTextLastName.text.toString().trim()
        val dateOfBirth = convertFromFormattedToIso(inputDOB.text.toString().trim())
        val phoneNumber = phoneNumber.text.toString().trim()
        val selectedGenderPosition = spinnerGender.selectedItemPosition
        val gender = if (selectedGenderPosition > 0) Gender.entries[selectedGenderPosition].name else ""
        val rideInFront = frontRideSwitch.isChecked
        val boosterSeatRequired = boosterSeatSwitch.isChecked

        if (validateData(firstName, lastName, dateOfBirth, phoneNumber, gender)) {
            lifecycleScope.launch {
                try {
                    loader.showLoader()
                    val body = SaveChildRequest(id = null, firstName = firstName, lastName = lastName,
                        dateOfBirth = dateOfBirth, gender = gender, mobileNumber = "${ccpAddChildCcd.selectedCountryCodeWithPlus}-$phoneNumber",
                        rideInFront = rideInFront, boosterSeatRequired = boosterSeatRequired)
                    val gson = Gson()
                    val jsonBody = gson.toJson(body)
                    val requestBody: RequestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
                    var imagePart: MultipartBody.Part? = null
                    var compressedFile: File? = null
                    selectedImageUri?.let { uri ->
                        // Compress the image first
                        compressedFile = Utilities.compressImage(applicationContext, uri)
                        if (compressedFile == null) {
                            showToast(this@Activity_Child_Add, "Failed to compress image")
                            return@launch
                        }

                        val reqBody = compressedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("file", compressedFile!!.name, reqBody)
                        imagePart = part
                    }

                    val response = NetworkManager.apiService.addChild(child = requestBody, image = imagePart)
                    if (response.isSuccessful) {
                        loader.hideLoader()
                        response.body()?.let {
                            showToast(this@Activity_Child_Add,"Child saved successfully!")
                            // Clean up the compressed file
                            compressedFile?.delete()

                            if (SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!.activity < 1) {
                                val c = Intent(this@Activity_Child_Add, Activity_Activities_Add::class.java)
                                    .apply {
                                        putExtra(CHILD_ID, it.id)
                                        putExtra(CHILD_NAME, it.firstName + " " + it.lastName)
                                    }
                                startActivity(c)
                            } else this@Activity_Child_Add.finish()
                        }
                    } else {
                        loader.hideLoader()
                        showToast(this@Activity_Child_Add, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                } catch (e: Exception) {
                    logger.error { e.message }
                    loader.hideLoader()
                    showToast(this@Activity_Child_Add, "${getString(R.string.exception_collen)} ${e.message}")
                }
            }
        }
    }
    private fun validateData(firstName: String, lastName: String, dob: String, mobileNumber: String, gender: String): Boolean {
        if (firstName.isEmpty()) {
            showToast(this, "First name is required")
            editTextFirstName.requestFocus()
            return false
        }
        if (lastName.isEmpty()) {
            showToast(this, "Last name is required")
            editTextLastName.requestFocus()
            return false
        }
        if (dob.isEmpty() || Utilities.isFutureDate(dob)) {
            showToast(this, "Date of birth is required and should be past date")
            inputDOB.performClick()
            return false
        }
        if (mobileNumber.isNotEmpty()) {
            if (!Utilities.isPhoneNumberValid(mobileNumber)) {
                showToast(this, "Phone number not valid")
                phoneNumber.requestFocus()
                return false
            }
        }
        if (gender.isEmpty()) {
            showToast(this, "Please select a gender")
            spinnerGender.performClick()
            return false
        }

        return true
    }
    private fun getFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
        return null
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            data.data?.let {
                selectedImageUri = it
                childDP.setImageURI(selectedImageUri)
            }
        }
    }
}