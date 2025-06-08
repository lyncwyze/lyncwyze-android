package com.intelly.lyncwyze.enteredUser.profileSetup

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.LoggedInDataKey
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_PS_Step4 : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var backButton: ImageView
//    private lateinit var logoutButton: ImageButton

    private lateinit var declineButton: Button
    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ps_step4)
        setUpUI()
        setUpFunctionality()
        showToast(this, "Next: Add An Child (4/8)")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun setUpUI() {
        backButton = findViewById(R.id.aclBackButton)
//        logoutButton = findViewById(R.id.logoutButton)
        declineButton = findViewById(R.id.btnDecline)
        continueButton = findViewById(R.id.btnAccept)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        
//        logoutButton.setOnClickListener {
//            Utilities.letLogout(this)
//        }

        declineButton.setOnClickListener { this.onBackPressed() }
        continueButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = NetworkManager.apiService.acceptTermsAndPrivacyPolicy(acceptTermsAndPrivacyPolicy = true)
                    loader.showLoader(this@Activity_PS_Step4)

                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_PS_Step4)
                        val body = response.body()
                        if (body != null && body) {
                            val loginRes = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)
                            if (loginRes != null) {
                                loginRes.profileStatus = ""
                                loginRes.profileComplete = true
                                loginRes.acceptTermsAndPrivacyPolicy = true
                                SharedPreferencesManager.saveObject(LoggedInDataKey, loginRes);
                                this@Activity_PS_Step4.finish()
                            }
                        } else
                            showToast(this@Activity_PS_Step4, "Error in accepting! Please try later.");
                    } else {
                        loader.hideLoader(this@Activity_PS_Step4)
                        showToast(this@Activity_PS_Step4, "${getString(R.string.error_txt)}: ${response.code()}");
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Activity_PS_Step4)
                    Toast.makeText(this@Activity_PS_Step4, "${R.string.exception_collen} ${e.message}", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}