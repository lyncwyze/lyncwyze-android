package com.intelly.lyncwyze.enteredUser.scheduleRide

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.ChildActivityDC
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_DAY
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_ID
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.IS_VALID_DAY
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_ConfirmActivity : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private lateinit var activityID: String
    private lateinit var activityDay: String
    private var isValidDay: Boolean = true
    private var activityDetail: ChildActivityDC? = null

    private lateinit var backBtn: ImageButton
    private lateinit var className: TextView
    private lateinit var classTiming: TextView
    private lateinit var confirmActivityPickUpTime: TextView
    private lateinit var midText: TextView
    private lateinit var changeRideType: LinearLayout
    private lateinit var actProviderAdd: TextView
    private lateinit var probabilityTxt: TextView

    private lateinit var confirmBtn: Button
    private lateinit var cancelBtn: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_confirm)
        activityCheck()
        uiSetUP()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }


    private fun activityCheck() {
        intent.getStringExtra(CHILD_ACTIVITY_ID)?.let { this.activityID = it } ?: {
            showToast(this, "Error getting activity ID")
            this.onBackPressed()
        }
        intent.getStringExtra(CHILD_ACTIVITY_DAY)?.let { this.activityDay = it } ?: {
            showToast(this, "Error getting activity day")
            this.onBackPressed()
        }
        this.isValidDay = intent.getBooleanExtra(IS_VALID_DAY, true)
    }
    private fun uiSetUP() {
        backBtn = findViewById(R.id.confirmActivityBackButton)

        className = findViewById(R.id.className)
        classTiming = findViewById(R.id.classTiming)
        confirmActivityPickUpTime = findViewById(R.id.confirmActivityPickUpTime)
        midText = findViewById(R.id.midText)
        changeRideType = findViewById(R.id.changeRideType)
        actProviderAdd = findViewById(R.id.actProviderAdd)
        probabilityTxt = findViewById(R.id.probabilityTxt)

        confirmBtn = findViewById(R.id.aconConfirm)
        cancelBtn = findViewById(R.id.aconCancel)
    }
    private fun setUpFunctionality() {
        backBtn.setOnClickListener { this.onBackPressed() }
        cancelBtn.setOnClickListener { this.onBackPressed() }
        confirmBtn.setOnClickListener { putRideScheduleInQueue() }
        changeRideType.setOnClickListener { showEditRideType()}
        
        // Hide probability text and confirm button if isValidDay is true
        if (!isValidDay) {
            changeRideType.visibility = View.GONE
            confirmBtn.visibility = View.GONE
            probabilityTxt.visibility = View.GONE
        }
        getTheActivity()
    }


    private fun getTheActivity() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_ConfirmActivity)
                val response = NetworkManager.apiService.getActivityById(activityID)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_ConfirmActivity)
                    response.body()?.let {
                        activityDetail = it
                        if (isValidDay) {
                            getProb()
                        }
                        displayActivity()
                    } ?: showToast(this@Activity_ConfirmActivity, "No details found!")
                } else {
                    loader.hideLoader(this@Activity_ConfirmActivity)
                    val code = response.code()
                    when (code) {
                        400 -> {
                            showToast(this@Activity_ConfirmActivity, "Server error getting the activity details!")
                            this@Activity_ConfirmActivity.finish()
                        }
                        else -> showToast(this@Activity_ConfirmActivity, "${getString(R.string.error_txt)}: $code, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_ConfirmActivity)
                showToast(this@Activity_ConfirmActivity, "Exception: ${e.message}")
            }
        }
    }
    private fun getProb () {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_ConfirmActivity)
                val response = NetworkManager.apiService.getProbabilityData(activityId = activityID, dayOfWeek = activityDay)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_ConfirmActivity)
                    response.body()?.let {
                        logger.info { "Probability: $it" }
                        probabilityTxt.text = "${getString(R.string.probability_of_getting_the_ride)} $it"
                        probabilityTxt.visibility = View.VISIBLE

                        if (it == "MEDIUM" || it == "HIGH")
                            changeRideType.visibility = View.GONE
                        when (it) {
                            "LOW" -> probabilityTxt.setTextColor(ContextCompat.getColor(this@Activity_ConfirmActivity, R.color.orangered))
                            "MEDIUM" -> probabilityTxt.setTextColor(ContextCompat.getColor(this@Activity_ConfirmActivity, R.color.orange))
                            "HIGH" -> probabilityTxt.setTextColor(ContextCompat.getColor(this@Activity_ConfirmActivity, R.color.green))
                            else -> probabilityTxt.setTextColor(ContextCompat.getColor(this@Activity_ConfirmActivity, R.color.black))
                        }
                    } ?: showToast(this@Activity_ConfirmActivity, getString(R.string.no_activity_found_for))
                } else {
                    loader.hideLoader(this@Activity_ConfirmActivity)
                    showToast(this@Activity_ConfirmActivity, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_ConfirmActivity)
                logger.error { e.message }
                showToast(this@Activity_ConfirmActivity, e.message.toString())
            }
        }
    }
    private fun displayActivity() {
        activityDetail?.schedulePerDay?.get(activityDay)?.let { it ->
            className.text = activityDetail?.type
            classTiming.text = "${getLocalizedText(this, activityDay)} \n${it.startTime.subSequence(0, 5)} - ${it.endTime.subSequence(0, 5)}"
            confirmActivityPickUpTime.text = "${it.preferredPickupTime} ${getString(R.string.min_early)}"
            midText.text = getLocalizedText(this, it.pickupRole)
            val add = activityDetail!!.address
            val addFinal = listOfNotNull(
                add.addressLine1?.takeIf { it.isNotBlank() },
                add.addressLine2?.takeIf { it.isNotBlank() },
                add.city.takeIf { it.isNotBlank() },
                add.state.takeIf { it.isNotBlank() },
                add.pincode.takeIf { it.toString().isNotBlank() && it > 0 }
            )
            actProviderAdd.text = addFinal.joinToString(separator = ", ")
        } ?: {
            showToast(this, "Error getting desired day details")
            this.onBackPressed()
        }
    }

    private fun putRideScheduleInQueue() {
        lifecycleScope.launch {
            try {
                val x = activityDetail?.schedulePerDay?.get(activityDay)!!
                loader.showLoader()
                val response = NetworkManager.apiService.addRideScheduleInQueue(activityId = activityID, dayOfWeek = activityDay, role = x.pickupRole)
                if (response.isSuccessful) {
                    loader.hideLoader()
                    response.body()?.let {
                        showToast(this@Activity_ConfirmActivity, "Schedule added in queue")
                        this@Activity_ConfirmActivity.onBackPressed()
                    } ?: showToast(this@Activity_ConfirmActivity, "No activity found!")
                }
                else {
                    loader.hideLoader()
                    showToast(this@Activity_ConfirmActivity, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader()
                logger.error { e.message }
                showToast(this@Activity_ConfirmActivity, "Exception: ${e.message}")
            }
        }
    }

    private fun showEditRideType() {
        logger.info { SharedPreferencesManager.gson.toJson(activityDetail) }

        val dialogRidePreference = layoutInflater.inflate(R.layout.dialog_ride_preferences, null)
        dialogRidePreference.findViewById<TextView>(R.id.textView5).text = getString(R.string.change_ride_preference)
        val dialog = AlertDialog.Builder(this).setView(dialogRidePreference).create()

        val rideTakerOptions = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRPType)

        val rideGiver = dialogRidePreference.findViewById<RadioButton>(R.id.rideGiver)
        rideGiver.setOnClickListener { rideTakerOptions.visibility = View.GONE }

        val rideTaker = dialogRidePreference.findViewById<RadioButton>(R.id.rideTaker)
        rideTaker.setOnClickListener { rideTakerOptions.visibility = View.VISIBLE }

        val btnSave = dialogRidePreference.findViewById<Button>(R.id.btnSaveRP)
        btnSave.setOnClickListener {
            val ridePreOptionID = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRP).checkedRadioButtonId
            logger.info { ridePreOptionID }
            if (ridePreOptionID != -1) {
                val riseSelectedRadioBtn = dialogRidePreference.findViewById<RadioButton>(ridePreOptionID)  // GIVER/TAKER
                var selectedTag: String = ""
                if (riseSelectedRadioBtn.tag == RiderType.GIVER.name) {
                     selectedTag = riseSelectedRadioBtn.tag as String
                } else if (riseSelectedRadioBtn.tag == RiderType.TAKER.name) {
                    val rideTakerOptionsId = rideTakerOptions.checkedRadioButtonId
                    if (rideTakerOptionsId != -1) {
                        selectedTag = dialogRidePreference.findViewById<RadioButton>(rideTakerOptionsId).tag as String   // DROP/PICK/DROP_PICK
                    }
                    else showToast(this, getString(R.string.select_an_ride_taker_option_to_proceed))
                }
                if (selectedTag.isNotBlank()) {
                    logger.info { selectedTag }
                    logger.info { activityDetail }
                    dialog.dismiss()
                    val data = activityDetail!!.schedulePerDay[activityDay]!!

                    data.pickupRole = selectedTag
                    data.dropoffRole = selectedTag
                    activityDetail!!.schedulePerDay.set(activityDay, data)
                    logger.info { activityDetail }
                    saveActivity()
                }
            }
            else showToast(this, "Please select an option!")
        }
        dialog.show()
    }
    private fun saveActivity() {
        displayActivity()
//        lifecycleScope.launch {
//            try {
//                loader.showLoader()
//                val response = NetworkManager.apiService.addChildActivity(activityDetail!!)
//                if (response.isSuccessful) {
//                    loader.hideLoader()
//                    response.body()?.let {
//                        showToast(this@Activity_ConfirmActivity, getString(R.string.activity_saved_successfully))
//                        displayActivity()
//                    } ?: showToast(this@Activity_ConfirmActivity, getString(R.string.saving_error))
//                } else {
//                    loader.hideLoader(this@Activity_ConfirmActivity)
//                    showToast(this@Activity_ConfirmActivity, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
//                }
//            } catch (e: Exception) {
//                loader.hideLoader(this@Activity_ConfirmActivity)
//                logger.error { e.message }
//                showToast(this@Activity_ConfirmActivity, "${e.message}")
//            }
//        }
    }
}