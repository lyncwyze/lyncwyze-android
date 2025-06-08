package com.intelly.lyncwyze.enteredUser.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.R


class Activity_Menu : AppCompatActivity() {
    private lateinit var fragmentContainer: View

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (!permissions.all { it.value })    // in case want to show pop-up in loop
//                requestCameraAndAlbumPermissions()
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainSetting)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<ImageButton>(R.id.settingBackBtn).setOnClickListener { this.onBackPressed() }
        fragmentContainer = findViewById(R.id.fragmentContainer)
        openSettingFragment()
//        requestCameraAndAlbumPermissions()
    }
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()
        else super.onBackPressed()
    }
    private fun openSettingFragment() {
        val useName = SharedPreferencesManager.getDecodedAccessToken()!!.emailId.toString()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, Fragment_Settings.newInstance(useName))
            .commit()
    }
    private fun requestCameraAndAlbumPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        } else {
            openSettingFragment()
        }
    }
}