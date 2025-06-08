package com.intelly.lyncwyze.enteredUser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.modals.ProfileStatus
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.LoggedInDataKey
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.UserRequiredDataCount
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.UpdateDialog
import com.intelly.lyncwyze.enteredUser.childActivity.Activity_Activities_ChildList
import com.intelly.lyncwyze.enteredUser.scheduleRide.Activity_ScheduleRide_ChildList
import com.intelly.lyncwyze.enteredUser.children.Activity_ChildrenList
import com.intelly.lyncwyze.enteredUser.children.Activity_Child_Add
import com.intelly.lyncwyze.enteredUser.emergencyContacts.Activity_EmergencyContact_Add
import com.intelly.lyncwyze.enteredUser.emergencyContacts.Activity_EmergencyContact_List
import com.intelly.lyncwyze.enteredUser.profileSetup.Activity_PS_Step1
import com.intelly.lyncwyze.enteredUser.profileSetup.Activity_PS_Step2_Address
import com.intelly.lyncwyze.enteredUser.profileSetup.Activity_PS_Step3
import com.intelly.lyncwyze.enteredUser.profileSetup.Activity_PS_Step4
import com.intelly.lyncwyze.enteredUser.records.Activity_CO2Saved
import com.intelly.lyncwyze.enteredUser.records.Activity_OnGoingRide
import com.intelly.lyncwyze.enteredUser.records.Activity_RidesGiven
import com.intelly.lyncwyze.enteredUser.records.Activity_RidesTaken
import com.intelly.lyncwyze.enteredUser.records.Activity_RidesUpcoming
import com.intelly.lyncwyze.enteredUser.settings.Activity_Menu
import com.intelly.lyncwyze.enteredUser.vehicles.Activity_VehicleList
import com.intelly.lyncwyze.enteredUser.vehicles.Activity_Vehicle_Add
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import kotlinx.coroutines.launch
import mu.KotlinLogging


class Dashboard : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    // Update check Functionality
    private var retryCount = 0
    private var updateDialog: UpdateDialog? = null
    private var hasShownToast = false // Flag to ensure toast is shown only once

    private lateinit var backButton: ImageView
    private lateinit var menu: ImageButton

    private lateinit var walletSection: LinearLayout
    private lateinit var goToChild: LinearLayout
    private lateinit var goToAllActivities: LinearLayout
    private lateinit var goToVehicles: LinearLayout

    private lateinit var scheduleARide: LinearLayout
    private lateinit var onGoingRide: LinearLayout

    private lateinit var ridesGiven: LinearLayout
    private lateinit var ridesTaken: LinearLayout
    private lateinit var ridesUpcoming: LinearLayout

    private var co2Saved: LinearLayout? = null
    private lateinit var myContacts: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        activityCheck()
        checkForUpdate()
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
        checkRequiredDetails()
    }

    private fun activityCheck() {
        SharedPreferencesManager.init(this)
        NetworkManager.init(application)
    }
    private fun checkRequiredDetails() {
        if (SharedPreferencesManager.getDecodedAccessToken() == null &&
            SharedPreferencesManager.getRefreshToken() == null) {
            showToast(this, getString(R.string.please_login))
            val intent = Intent(this, Activity_LoginWithEmail::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        else {
            val userDate = SharedPreferencesManager.getObject<LoginResponse>(LoggedInDataKey)!!
            if (!userDate.profileComplete) {
                when (userDate.profileStatus) {
                    ProfileStatus.PROFILE.name -> startActivity(Intent(this@Dashboard, Activity_PS_Step1::class.java))
                    ProfileStatus.BACKGROUND.name -> startActivity(Intent(this@Dashboard, Activity_PS_Step2_Address::class.java))
                    ProfileStatus.PHOTO.name -> startActivity(Intent(this@Dashboard, Activity_PS_Step3::class.java))
                    ProfileStatus.POLICY.name -> startActivity(Intent(this@Dashboard, Activity_PS_Step4::class.java))
                    else -> {}
                }
            }
            else if (!userDate.acceptTermsAndPrivacyPolicy)
                startActivity(Intent(this@Dashboard, Activity_PS_Step4::class.java))

            else lifecycleScope.launch {
                try {
                    loader.showLoader(this@Dashboard)
                    val response = NetworkManager.apiService.dataCount()
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Dashboard)
                        response.body()?.let { body ->
                            SharedPreferencesManager.saveObject(UserRequiredDataCount, body)

                            findViewById<TextView>(R.id.rideGivenText).text =
                                body.givenRides.toString()
                            findViewById<TextView>(R.id.rideTakenText).text =
                                body.takenRides.toString()
                            findViewById<TextView>(R.id.upcomingRidesText).text =
                                body.upcomingRides.toString()
                            findViewById<TextView>(R.id.onGoingText).text =
                                body.ongoingRides.toString()

                            if (body.child < 1) {
                                startActivity(Intent(this@Dashboard, Activity_Child_Add::class.java).apply { })
                                showToast(this@Dashboard, "Please add a child to explore")
                            } else if (body.activity < 1) {
                                showToast(this@Dashboard, "Please choose a child and add an activity")
                                startActivity(Intent(this@Dashboard, Activity_Activities_ChildList::class.java))
                            } else if (body.vehicle < 1) {
                                showToast(this@Dashboard, "Please add a vehicle")
                                startActivity(Intent(this@Dashboard, Activity_Vehicle_Add::class.java))
                            } else if (body.emergencyContact < 1) {
                                showToast(this@Dashboard, "Please add a emergency contact")
                                startActivity(Intent(this@Dashboard, Activity_EmergencyContact_Add::class.java))
                            }
                        }
                    } else {
                        loader.hideLoader(this@Dashboard)
                        showToast(this@Dashboard, "${getString(R.string.error_txt)}: ${response.code()}")
                    }
                } catch (e: Exception) {
                    loader.hideLoader(this@Dashboard)
                    Toast.makeText(this@Dashboard, "${getString(R.string.exception_collen)} ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.aclBackButton)

        menu = findViewById(R.id.profile_section)
        walletSection = findViewById(R.id.walletSection)
        goToChild = findViewById(R.id.goToChild)
        goToAllActivities = findViewById(R.id.goToAllActivities)
        goToVehicles = findViewById(R.id.goToVehicles)
        ridesGiven = findViewById(R.id.ride_given)
        ridesTaken = findViewById(R.id.ride_taken)
        ridesUpcoming = findViewById(R.id.ride_upcoming)

        co2Saved = findViewById(R.id.co2_saved)

        scheduleARide = findViewById(R.id.scheduleARide)
        onGoingRide = findViewById(R.id.onGoingRide)

        myContacts = findViewById(R.id.myContacts)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }

        menu.setOnClickListener { startActivity(Intent(this, Activity_Menu::class.java)) }

        goToChild.setOnClickListener { startActivity(Intent(this, Activity_ChildrenList::class.java)) }
        goToAllActivities.setOnClickListener { startActivity(Intent(this, Activity_Activities_ChildList::class.java)) }
        goToVehicles.setOnClickListener { startActivity(Intent(this, Activity_VehicleList::class.java)) }

        scheduleARide.setOnClickListener { startActivity(Intent(this, Activity_ScheduleRide_ChildList::class.java)) }
        onGoingRide.setOnClickListener { startActivity(Intent(this, Activity_OnGoingRide::class.java)) }

        ridesGiven.setOnClickListener { startActivity(Intent(this, Activity_RidesGiven::class.java)) }
        ridesTaken.setOnClickListener { startActivity(Intent(this, Activity_RidesTaken::class.java)) }
        ridesUpcoming.setOnClickListener { startActivity(Intent(this, Activity_RidesUpcoming::class.java)) }
        myContacts.setOnClickListener { startActivity(Intent(this, Activity_EmergencyContact_List::class.java)) }
        co2Saved?.setOnClickListener { startActivity(Intent(this, Activity_CO2Saved::class.java)) }
    }

    private fun checkForUpdate() {
        if (!hasShownToast)
            hasShownToast = true

        val updateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = updateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE)
                showUpdateDialog()
        }.addOnFailureListener {
            retryUpdateWithBackoff() // Retry on failure
        }
    }

    private fun retryUpdateWithBackoff() {
        if (retryCount >= 5) // Stop retrying after 5 attempts
            return
        val retryDelay = (Math.pow(2.0, retryCount.toDouble()) * 1000).toLong() // 2^retryCount * 1000ms
        retryCount++
        Handler(Looper.getMainLooper()).postDelayed({ checkForUpdate() }, retryDelay)
    }

    // Show update dialog using com.intelly.lyncwyze.UpdateDialog class
    private fun showUpdateDialog() {
        if (updateDialog != null && updateDialog!!.isShowing) return // Prevent multiple dialogs
        if (!UpdateDialog.shouldShowDialog()) return // Don't show if user clicked Later
        updateDialog = UpdateDialog(this)
        updateDialog!!.show()
    }
}