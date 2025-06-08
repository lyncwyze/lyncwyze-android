package com.intelly.lyncwyze.enteredUser.profileSetup

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.modals.ProfileStatus
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CAMERA_REQUEST_CODE
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.LoggedInDataKey
import com.intelly.lyncwyze.Assest.utilities.PICK_IMAGE_REQUEST
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


class Activity_PS_Step3 : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
    private lateinit var logoutButton: ImageButton

    private lateinit var userImage: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step3)
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
        userImage = findViewById(R.id.userImage)
        continueButton = findViewById(R.id.aclContinueBtn)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }

        userImage.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Choose Image Source")
                .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                    }
                }.show()
        }
        continueButton.setOnClickListener { saveData() }
        setupActivityResultLaunchers()
    }
    private fun setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? Bitmap
                bitmap?.let {
                    val uri = ImageUtilities.saveBitmapToCache(this@Activity_PS_Step3, it)
                    selectedImageUri = uri
                    userImage.setImageURI(uri)
                }
            }
        }
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    selectedImageUri = it
                    userImage.setImageURI(it)
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
    private fun openGallery() { galleryLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
             data.data?.let {
                 selectedImageUri = it
             }
             userImage.setImageURI(selectedImageUri)
        }
    }

    private fun saveData() {
        lifecycleScope.launch {
            try {
                selectedImageUri?.let { uri ->
                    // Compress the image first
                    val compressedFile = Utilities.compressImage(applicationContext, uri)
                    if (compressedFile == null) {
                        showToast(this@Activity_PS_Step3, "Failed to compress image")
                        return@launch
                    }

                    val reqBody = compressedFile!!.asRequestBody("image/*".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", compressedFile!!.name, reqBody)

                    loader.showLoader(this@Activity_PS_Step3)
                    val response = NetworkManager.apiService.addProfileImage(file = part)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_PS_Step3)
                        val responseString = response.body()?.string()
                        logger.info { "responseString: $responseString" }     // Eg --- PROFILE/677a9c7c62546e0f99a7a772/new.png
                        showToast(this@Activity_PS_Step3, "Image uploaded successfully")

                        // Clean up the compressed file
                        compressedFile.delete()

                        val loginRes =
                            SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                        if (loginRes != null) {
                            loginRes.profileStatus = ProfileStatus.POLICY.value
                            SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes)
                        }
                        this@Activity_PS_Step3.finish()
                    }
                    else {
                        loader.hideLoader(this@Activity_PS_Step3)
                        showToast(
                            this@Activity_PS_Step3,
                            "Error: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}"
                        )
                    }
                } ?: showToast(this@Activity_PS_Step3, "Please Add Image")
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_PS_Step3)
                showToast(this@Activity_PS_Step3, "${getString(R.string.exception_collen)} ${e.message}")
            }
        }
    }
}