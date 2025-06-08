package com.intelly.lyncwyze

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.Location
import com.intelly.lyncwyze.Assest.modals.Providers
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.IS_AVAILABLE
import com.intelly.lyncwyze.Assest.utilities.SelectYourProvider
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.notification
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.enteredUser.Dashboard
import com.intelly.lyncwyze.enteringUser.Activity_AreaAvailability
import com.intelly.lyncwyze.enteringUser.notifyMe.Activity_NotifyMe
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import com.intelly.lyncwyze.enteringUser.signUp.Activity_RegisterWithEmail
import com.intelly.lyncwyze.enteringUser.signUp.Activity_RegisterWithNumber
import kotlinx.coroutines.launch
import mu.KotlinLogging


class MainActivity : ComponentActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    // Update check Functionality
    private var retryCount = 0
    private var updateDialog: UpdateDialog? = null
    private var hasShownToast = false // Flag to ensure toast is shown only once

    // Referrals
    private val haveReferral: Boolean = false
    private lateinit var referralText: TextView

    // Address
    private lateinit var byAddSuggestionLinearLayout: LinearLayout
    private lateinit var addressAdaptor: AutoCompleteTextView
    private var searchRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())
    private var locationAutoComplete: MutableList<Location> = mutableListOf()
    private var selectedLocation: Location? = null
    private lateinit var inputIcon: ImageView

    // Predefined provider
    private var providerAutoComplete: MutableList<Providers> = mutableListOf()
    private var selectedProvider: Providers? = null

    private lateinit var availableProviders: Spinner
    private lateinit var providersLoader: ProgressBar

    private lateinit var continueCheckAvailability: Button
    private lateinit var providerNotFoundLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        requestAllPermissions()
        appInit()
        checkForUpdate()
        setUpUI()
        setUpFunctionality()
        fetchProviderSuggestions()
    }
    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
//            if (!permissions.all { it.value })
//                requestAllPermissions()
        }
    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            POST_NOTIFICATIONS,
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
            CAMERA,
            READ_MEDIA_IMAGES
        )
        val permissionsToRequest = permissions.filter {
            logger.info { "permission data here $it" }
            logger.info { ContextCompat.checkSelfPermission(this, it) }
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        logger.info { permissionsToRequest }
        if (permissionsToRequest.isNotEmpty())
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }
    private fun appInit() {
        SharedPreferencesManager.init(this)
        NetworkManager.init(application)

        if (SharedPreferencesManager.getRefreshToken() != null) {
            val intent = Intent(this, Dashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear the back stack
            startActivity(intent)
        }
        FirebaseMessaging.getInstance().subscribeToTopic(notification)
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

    private fun setUpUI() {
        referralText = findViewById(R.id.referral_from)

        // By address
        byAddSuggestionLinearLayout = findViewById(R.id.byAddressSuggestion)
        addressAdaptor = findViewById(R.id.homeAddressSuggestion)
        inputIcon = findViewById(R.id.input_icon)

        // By provider
        availableProviders = findViewById(R.id.availableProviders)
        providersLoader = findViewById(R.id.providersLoader)

        // Choice between the address and provider to work with
        if (resources.getString(R.string.app_home_by_provider).toBoolean()) {
            providersLoader.visibility = View.VISIBLE
            availableProviders.visibility = View.GONE
            byAddSuggestionLinearLayout.visibility = View.GONE
        } else {
            providersLoader.visibility = View.GONE
            availableProviders.visibility = View.GONE
            byAddSuggestionLinearLayout.visibility = View.VISIBLE
        }

        // Actions
        continueCheckAvailability = findViewById(R.id.continueBtnToAvailable)
        providerNotFoundLink = findViewById(R.id.providerNotFound)
    }
    private fun setUpFunctionality() {
        if (haveReferral) {
            referralText.visibility = View.VISIBLE
            referralText.text = buildString {
                append(getString(R.string.text_referral_from))
                append(": Ujjwal")
            }
        } else referralText.visibility = View.GONE

        // By address
        addressAdaptor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    if (s.toString().isNotEmpty())
                        fetchLocationSuggestions(s.toString())
                }
                handler.postDelayed(searchRunnable!!, 250)
            }
            override fun afterTextChanged(s: Editable?) { }
        })
        addressAdaptor.setOnItemClickListener { _, _, position, _ ->
            selectedLocation = locationAutoComplete[position]
        }
        inputIcon.setOnClickListener { addressAdaptor.requestFocus() }

        // Sign-up
        findViewById<TextView>(R.id.textSignIn).setOnClickListener { startActivity(Intent(this, Activity_LoginWithEmail::class.java)) }
        // Sing-in
        findViewById<TextView>(R.id.textSignUp).setOnClickListener { startActivity(Intent(this, Activity_RegisterWithEmail::class.java)) }

        continueCheckAvailability.setOnClickListener {
            if (resources.getString(R.string.app_home_by_provider).toBoolean()) {
                if (selectedProvider != null)
                    startActivity(Intent(this@MainActivity, Activity_RegisterWithNumber::class.java).apply { putExtra(IS_AVAILABLE, true) })
                else showToast(this@MainActivity, "Please select a provider")
            } else {
                if (selectedLocation != null) {
                    lifecycleScope.launch {
                        try {
                            val response = NetworkManager.apiService.checkServiceAvailability(placeId = selectedLocation!!.placeId)
                            if (response.isSuccessful)
                                startActivity(Intent(this@MainActivity, Activity_AreaAvailability::class.java).apply { putExtra(IS_AVAILABLE, response.body()) })
                            else
                                showToast(this@MainActivity, "${getString(R.string.error_txt)}: ${response.code()} -- ${response.message()}")
                        } catch (e: Exception) {
                            showToast(this@MainActivity, getString(R.string.exception_collen, e.message))
                        }
                    }
                } else showToast(this@MainActivity, getString(R.string.please_select_a_location))
            }
        }
        providerNotFoundLink.setOnClickListener { startActivity(Intent(this, Activity_NotifyMe::class.java)) }
    }

    private fun fetchProviderSuggestions() {
        lifecycleScope.launch {
            try {
                logger.info { "Get providers" }
                val response = NetworkManager.apiService.getProviders(pageSize = 100, offSet = 0)
                logger.info { response.isSuccessful }
                if (response.isSuccessful) {
                    response.body()?.let {
                        providerAutoComplete.clear()
                        providerAutoComplete.addAll(it.data)
                        populateProviders()
                    }
                }
                else showToast(this@MainActivity, "${getString(R.string.error_txt)}: ${response.code()}")
            } catch (e: Exception) {
                logger.error { e.message }
//                showToast(this@MainActivity, "${getString(R.string.exception_collen)}: ${e.message}")
            }
        }
    }
    private fun populateProviders() {
        logger.info { "populateProviders" }
        val providers: MutableList<ProviderAdaptorData> = mutableListOf(ProviderAdaptorData(SelectYourProvider, "", ""))
        providers.addAll(providerAutoComplete.map { ProviderAdaptorData(it.name, it.address.state, it.address.city) })
        availableProviders.adapter = ProviderCustomSpinnerAdapter(this, providers)
        availableProviders.isEnabled = true
        providersLoader.visibility = View.GONE
        availableProviders.visibility = View.VISIBLE
        availableProviders.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedProvider = if (position > 0) {  // Ignore the first item
                    providerAutoComplete[position - 1]
                }
                else null
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
    }

    private fun fetchLocationSuggestions(query: String) {
        lifecycleScope.launch {
            try {
                val response = if (locationAutoComplete.isNotEmpty())
                    NetworkManager.apiService.getLocations(query, sessionToken = locationAutoComplete[0].sessionToken)
                else NetworkManager.apiService.getLocations(query)

                if (response.isSuccessful) {
                    val providerList = response.body()
                    providerList?.let {
                        locationAutoComplete.clear();
                        locationAutoComplete.addAll(it)
                        updateLocationAutoCompleteSuggestions()
                    }
                } else showToast(this@MainActivity, "${getString(R.string.error_txt)}: ${response.code()}")
            } catch (e: Exception) {
                showToast(this@MainActivity, "${getString(R.string.exception_collen)}: ${e.message}")
            }
        }
    }
    private fun updateLocationAutoCompleteSuggestions() {
        if (locationAutoComplete.isNotEmpty()) {
            val stringList = locationAutoComplete.map { it.description }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, stringList)
            addressAdaptor.setAdapter(adapter)
            adapter.notifyDataSetChanged()
            populateProviders()
        }
    }
}