package com.intelly.lyncwyze.enteredUser.childActivity

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.TimePicker
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.ChildActivityDC
import com.intelly.lyncwyze.Assest.modals.DailySchedule
import com.intelly.lyncwyze.Assest.modals.DataCountResp
import com.intelly.lyncwyze.Assest.modals.Providers
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.CHILD_NAME
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.SelectYourProvider
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.UserRequiredDataCount
import com.intelly.lyncwyze.Assest.utilities.Utilities
import com.intelly.lyncwyze.Assest.utilities.convert24to12HourFormat
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.roundToNearestFiveMinutes
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.ProviderCustomSpinnerAdapter
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.ProviderAdaptorData
import com.intelly.lyncwyze.enteredUser.vehicles.Activity_Vehicle_Add
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.Calendar


class Activity_Activities_Add : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}
    private val loader = Loader(this)

    private var childActivity = ChildActivityDC.default()
    private lateinit var childName: String

    private lateinit var backButton: ImageButton
    private lateinit var logoutButton: ImageButton

    private lateinit var activityType: TextView
    private lateinit var activitySubType: TextView
    private lateinit var availableProviders: Spinner
    private var providerAutoComplete: MutableList<Providers> = mutableListOf()
    private var selectedProvider: Providers? = null

    // Activity Time
    private lateinit var acDayMonday: Button
    private lateinit var acDayTuesday: Button
    private lateinit var acDayWednesday: Button
    private lateinit var acDayThursday: Button
    private lateinit var acDayFriday: Button
    private lateinit var acDaySaturday: Button
    private lateinit var acDaySunday: Button

    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_activities_add)
        appCheck()
        setupUI()
        setUpFunctionality()
        fetchProviderSuggestions()
        setupDarkModeAwareUI()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun appCheck() {
        intent.getStringExtra(CHILD_ID)?.let {
            childActivity.childId = it
            childActivity.address.location.type = "Point"
            childName = intent.getStringExtra(CHILD_NAME) ?: ""
        } ?: {
            this@Activity_Activities_Add.finish()
            showToast(this, getString(R.string.error_getting_child_id))
        }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.aclBackButton)
        logoutButton = findViewById(R.id.logoutButton)
        logoutButton.visibility = View.GONE

        findViewById<TextView>(R.id.textChildAndActivitiesName).text = if (childName.isNotEmpty())  "$childName - ${getString(R.string.activity_details)}" else getString(R.string.activity_details)

        activityType = findViewById(R.id.activitiesType)
        activitySubType = findViewById(R.id.activitySubType)
        availableProviders = findViewById(R.id.acaMyAddress)

        // Activity Time -- what is the time of class
        acDayMonday = findViewById(R.id.acDayMonday)
        acDayTuesday = findViewById(R.id.acDayTuesday)
        acDayWednesday = findViewById(R.id.acDayWednesday)
        acDayThursday = findViewById(R.id.acDayThursday)
        acDayFriday = findViewById(R.id.acDayFriday)
        acDaySaturday = findViewById(R.id.acDaySaturday)
        acDaySunday = findViewById(R.id.acDaySunday)

        // Setup time text click listeners
        findViewById<TextView>(R.id.acDayMondayTime).setOnClickListener { editDaySchedule(acDayMonday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.MONDAY) }
        findViewById<TextView>(R.id.acDayTuesdayTime).setOnClickListener { editDaySchedule(acDayTuesday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.TUESDAY) }
        findViewById<TextView>(R.id.acDayWednesdayTime).setOnClickListener { editDaySchedule(acDayWednesday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.WEDNESDAY) }
        findViewById<TextView>(R.id.acDayThursdayTime).setOnClickListener { editDaySchedule(acDayThursday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.THURSDAY) }
        findViewById<TextView>(R.id.acDayFridayTime).setOnClickListener { editDaySchedule(acDayFriday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.FRIDAY) }
        findViewById<TextView>(R.id.acDaySaturdayTime).setOnClickListener { editDaySchedule(acDaySaturday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.SATURDAY) }
        findViewById<TextView>(R.id.acDaySundayTime).setOnClickListener { editDaySchedule(acDaySunday, it as TextView, com.intelly.lyncwyze.Assest.modals.WeekDays.SUNDAY) }

        continueButton = findViewById(R.id.aclContinueBtn)
        if (SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!.activity < 1) {
            continueButton.text = "Next: Add Vehicle (6/8)"
            logoutButton.visibility = View.VISIBLE
        }
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        logoutButton.setOnClickListener {
            Utilities.letLogout(this)
        }

        // Activity time
        acDayMonday.setOnClickListener { selectOptions(acDayMonday, findViewById(R.id.acDayMondayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.MONDAY) }
        acDayTuesday.setOnClickListener { selectOptions(acDayTuesday, findViewById(R.id.acDayTuesdayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.TUESDAY) }
        acDayWednesday.setOnClickListener { selectOptions(acDayWednesday, findViewById(R.id.acDayWednesdayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.WEDNESDAY) }
        acDayThursday.setOnClickListener { selectOptions(acDayThursday, findViewById(R.id.acDayThursdayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.THURSDAY) }
        acDayFriday.setOnClickListener { selectOptions(acDayFriday, findViewById(R.id.acDayFridayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.FRIDAY) }
        acDaySaturday.setOnClickListener { selectOptions(acDaySaturday, findViewById(R.id.acDaySaturdayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.SATURDAY) }
        acDaySunday.setOnClickListener { selectOptions(acDaySunday, findViewById(R.id.acDaySundayTime), com.intelly.lyncwyze.Assest.modals.WeekDays.SUNDAY) }

        continueButton.setOnClickListener { saveActivity() }
    }


    private fun fetchProviderSuggestions() {
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getProviders(pageSize = 100, offSet = 0)
                if (response.isSuccessful) {
                    val providerList = response.body()
                    providerList?.let {
                        providerAutoComplete.addAll(it.data)
                        populateProviders()
                    }
                } else
                    showToast(this@Activity_Activities_Add, "${getString(R.string.error_txt)}: ${response.code()}")

            } catch (e: Exception) {
                showToast(this@Activity_Activities_Add, "${getString(R.string.exception_collen)}: ${e.message}")
            }
        }
    }
    private fun populateProviders() {
        val providers: MutableList<ProviderAdaptorData> = mutableListOf(ProviderAdaptorData(SelectYourProvider, "", ""))
        providers.addAll(providerAutoComplete.map {
            ProviderAdaptorData(it.name, it.address.state, it.address.city)
        })
        availableProviders.adapter = ProviderCustomSpinnerAdapter(this, providers)
        availableProviders.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedProvider = if (position > 0) {  // Ignore the first item
                    val dataX = providerAutoComplete[position - 1]
                    activityType.text = dataX.type;
                    activitySubType.text = dataX.subType
                    dataX
                }
                else null
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
    }

    private fun selectOptions(button: Button, timeView: TextView, day: com.intelly.lyncwyze.Assest.modals.WeekDays) {
        if (button.tag == null) {
            // Check if we have any existing schedule to use as default
            val firstSchedule = childActivity.schedulePerDay.values.firstOrNull()
            
            if (firstSchedule != null) {
                // Use the first schedule's values
                childActivity.schedulePerDay[day.name] = DailySchedule(
                    startTime = firstSchedule.startTime,
                    endTime = firstSchedule.endTime,
                    preferredPickupTime = firstSchedule.preferredPickupTime,
                    pickupRole = firstSchedule.pickupRole,
                    dropoffRole = firstSchedule.dropoffRole
                )
                
                // Update UI
                if (firstSchedule.preferredPickupTime > 0) {
                    timeView.text = "${convert24to12HourFormat(firstSchedule.startTime)} \n - \n ${convert24to12HourFormat(firstSchedule.endTime)} \n - \n ${getLocalizedText(this@Activity_Activities_Add, firstSchedule.pickupRole)} \n -\n ${firstSchedule.preferredPickupTime} min"
                } else {
                    timeView.text = "${convert24to12HourFormat(firstSchedule.startTime)} \n - \n ${convert24to12HourFormat(firstSchedule.endTime)} \n - \n ${getLocalizedText(this@Activity_Activities_Add, firstSchedule.pickupRole)}"
                }
                button.tag = getString(R.string.select)
                button.background = resources.getDrawable(R.drawable.circular_button_background_selected, null)
                timeView.visibility = View.VISIBLE
                logger.info { childActivity }
            } else {
                // No existing schedule, show dialogs
                showTimeAndPreferenceDialogs(button, timeView, day)
            }
        } else {
            button.background = resources.getDrawable(R.drawable.circular_button_background, null)
            timeView.visibility = View.GONE
            timeView.text = ""
            button.tag = null
            childActivity.schedulePerDay.remove(day.name)
        }
    }

    private fun setupDarkModeAwareUI() {
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        
        // Update status bar and navigation bar colors
        window.statusBarColor = ContextCompat.getColor(this, if (isDarkMode) R.color.primary_bg else R.color.white)
        window.navigationBarColor = ContextCompat.getColor(this, if (isDarkMode) R.color.primary_bg else R.color.white)
        
        // Update status bar icons color
        if (!isDarkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        // Update back button color based on theme
        val backButton = findViewById<ImageButton>(R.id.aclBackButton)
        backButton.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, if (isDarkMode) R.color.textColor else R.color.iconColor))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setupDarkModeAwareUI()
    }

    private fun showTimeAndPreferenceDialogs(button: Button, timeView: TextView, day: com.intelly.lyncwyze.Assest.modals.WeekDays) {
        val timerLayoutStart = layoutInflater.inflate(R.layout.layout_timer,null)
        val dialogStartTime = AlertDialog.Builder(this)
            .setView(timerLayoutStart)
            .create()

        val captionStart = timerLayoutStart.findViewById<TextView>(R.id.theCaption)
        captionStart.text = getString(R.string.start_time)

        val closeStart = timerLayoutStart.findViewById<Button>(R.id.closeButton)
        closeStart.setOnClickListener { dialogStartTime.dismiss() }

        val okStart = timerLayoutStart.findViewById<Button>(R.id.okButton)

        okStart.setOnClickListener {
            val timePickerStart = timerLayoutStart.findViewById<TimePicker>(R.id.timePicker)
            val hour = timePickerStart.hour
            val minute = timePickerStart.minute
            val startTime = String.format("%02d:%02d", hour, minute)
            dialogStartTime.dismiss()

            // End time
            val timerLayoutEnd = layoutInflater.inflate(R.layout.layout_timer, null)
            val dialogEndTime = AlertDialog.Builder(this)
                .setView(timerLayoutEnd)
                .create()

            val captionEnd = timerLayoutEnd.findViewById<TextView>(R.id.theCaption)
            captionEnd.text = getString(R.string.end_time)

            val closeEnd = timerLayoutEnd.findViewById<Button>(R.id.closeButton)
            closeEnd.setOnClickListener { dialogEndTime.dismiss() }

            val okEnd = timerLayoutEnd.findViewById<Button>(R.id.okButton)

            okEnd.setOnClickListener {
                val timePickerEnd = timerLayoutEnd.findViewById<TimePicker>(R.id.timePicker)
                val hour2 = timePickerEnd.hour
                val minute2 = timePickerEnd.minute
                val endTime = String.format("%02d:%02d", hour2, minute2)
                dialogEndTime.dismiss()

                val dialogRidePreference = layoutInflater.inflate(R.layout.dialog_ride_preferences, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogRidePreference)
                    .create()

                val rideTakerOptions = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRPType)

                val rideGiver = dialogRidePreference.findViewById<RadioButton>(R.id.rideGiver)
                rideGiver.setOnClickListener { rideTakerOptions.visibility = View.GONE }

                val rideTaker = dialogRidePreference.findViewById<RadioButton>(R.id.rideTaker)
                rideTaker.setOnClickListener { rideTakerOptions.visibility = View.VISIBLE }

                val btnSave = dialogRidePreference.findViewById<Button>(R.id.btnSaveRP)
                btnSave.setOnClickListener {
                    val ridePreOptionID = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRP).checkedRadioButtonId
                    if (ridePreOptionID != -1) {
                        var rp = ""
                        val riseSelectedRadioBtn = dialogRidePreference.findViewById<RadioButton>(ridePreOptionID)  // GIVER/TAKER
                        if (riseSelectedRadioBtn.tag == RiderType.GIVER.name) {
                            rp = riseSelectedRadioBtn.tag.toString()
                        }
                        else {    // TAKER
                            rp = ""
                            val rideTakerOptionsId = rideTakerOptions.checkedRadioButtonId
                            if (rideTakerOptionsId != -1) {
                                rp = dialogRidePreference.findViewById<RadioButton>(rideTakerOptionsId).tag as String    // Taker options
                            }
                            else
                                showToast(this@Activity_Activities_Add, getString(R.string.select_an_ride_taker_option_to_proceed))
                        }

                        if (rp != "") {
                            dialog.dismiss()
                            if (rp == RiderType.GIVER.name) {
                                childActivity.schedulePerDay[day.name] = DailySchedule(
                                    startTime = startTime,
                                    endTime = endTime,
                                    preferredPickupTime = 0,
                                    pickupRole = rp,
                                    dropoffRole = rp
                                )
                                // View
                                timeView.text = "${convert24to12HourFormat(startTime)} \n - \n ${convert24to12HourFormat(endTime)} \n - \n ${getLocalizedText(this@Activity_Activities_Add, rp)}"
                                button.tag = getString(R.string.select)
                                button.background = resources.getDrawable(R.drawable.circular_button_background_selected, null)
                                timeView.visibility = View.VISIBLE
                                logger.info { childActivity }
                            }
                            else {    // TAKER and its values
                                val timeDialog = AlertDialog.Builder(this)
                                    .setView(layoutInflater.inflate(R.layout.dialog_radio_buttons, null))
                                    .create()
                                timeDialog.show()

                                val radioGroupOptions = timeDialog.findViewById<RadioGroup>(R.id.radioGroupOptions)
                                val timeSaveBtn = timeDialog.findViewById<Button>(R.id.btnSave)
                                timeSaveBtn.setOnClickListener {
                                    val timeSelectID = radioGroupOptions.checkedRadioButtonId
                                    if (timeSelectID != -1) {
                                        val selectedTime = radioGroupOptions.findViewById<RadioButton>(timeSelectID)
                                        val selectedText = selectedTime.tag.toString()
                                        val minutesValue = selectedText.split(" ")[0].toInt()
                                        "$minutesValue ${getString(R.string.min)}".also {
                                            timeView.text = it
                                        }
                                        childActivity.schedulePerDay[day.name] = DailySchedule(
                                            startTime = startTime,
                                            endTime = endTime,
                                            preferredPickupTime = minutesValue,
                                            pickupRole = rp,
                                            dropoffRole = rp
                                        )
                                        timeView.text = "${convert24to12HourFormat(startTime)} \n - \n ${convert24to12HourFormat(endTime)} \n - \n ${getLocalizedText(this@Activity_Activities_Add, rp)} \n -\n $minutesValue min"
                                        button.tag = getString(R.string.select)
                                        button.background = resources.getDrawable(R.drawable.circular_button_background_selected, null)
                                        timeView.visibility = View.VISIBLE
                                        timeDialog.dismiss()
                                        logger.info { childActivity }
                                    } else
                                        showToast(this@Activity_Activities_Add, getString(R.string.please_select_an_option))
                                }
                            }
                        }
                    } else
                        showToast(this, getString(R.string.please_select_an_option))
                }
                dialog.show()
            }
            dialogEndTime.show()
        }
        dialogStartTime.show()
    }

    private fun editDaySchedule(button: Button, timeView: TextView, day: com.intelly.lyncwyze.Assest.modals.WeekDays) {
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        
        val schedule = childActivity.schedulePerDay[day.name] ?: return
        
        // Start time dialog
        val timerLayoutStart = layoutInflater.inflate(R.layout.layout_timer,null)
        val dialogStartTime = AlertDialog.Builder(this)
            .setView(timerLayoutStart)
            .create()

        val captionStart = timerLayoutStart.findViewById<TextView>(R.id.theCaption)
        captionStart.text = getString(R.string.start_time)

        val closeStart = timerLayoutStart.findViewById<Button>(R.id.closeButton)
        closeStart.setOnClickListener { dialogStartTime.dismiss() }

        val timePickerStart = timerLayoutStart.findViewById<TimePicker>(R.id.timePicker)
        // Set current start time
        val currentStartTime = schedule.startTime.substring(0, 5).split(":")
        timePickerStart.hour = currentStartTime[0].toInt()
        timePickerStart.minute = currentStartTime[1].toInt()

        val okStart = timerLayoutStart.findViewById<Button>(R.id.okButton)
        okStart.setOnClickListener {
            val hour = timePickerStart.hour
            val minute = timePickerStart.minute
            val startTime = String.format("%02d:%02d", hour, minute)
            dialogStartTime.dismiss()

            // End time dialog
            val timerLayoutEnd = layoutInflater.inflate(R.layout.layout_timer, null)
            val dialogEndTime = AlertDialog.Builder(this)
                .setView(timerLayoutEnd)
                .create()

            val captionEnd = timerLayoutEnd.findViewById<TextView>(R.id.theCaption)
            captionEnd.text = getString(R.string.end_time)

            val closeEnd = timerLayoutEnd.findViewById<Button>(R.id.closeButton)
            closeEnd.setOnClickListener { dialogEndTime.dismiss() }

            val timePickerEnd = timerLayoutEnd.findViewById<TimePicker>(R.id.timePicker)
            // Set current end time
            val currentEndTime = schedule.endTime.substring(0, 5).split(":")
            timePickerEnd.hour = currentEndTime[0].toInt()
            timePickerEnd.minute = currentEndTime[1].toInt()

            val okEnd = timerLayoutEnd.findViewById<Button>(R.id.okButton)
            okEnd.setOnClickListener {
                val hour2 = timePickerEnd.hour
                val minute2 = timePickerEnd.minute
                val endTime = String.format("%02d:%02d", hour2, minute2)
                dialogEndTime.dismiss()

                // Ride preference dialog
                val dialogRidePreference = layoutInflater.inflate(R.layout.dialog_ride_preferences, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogRidePreference)
                    .create()

                val rideTakerOptions = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRPType)
                rideTakerOptions.visibility = View.GONE // Hide ride taker options initially

                val rideGiver = dialogRidePreference.findViewById<RadioButton>(R.id.rideGiver)
                val rideTaker = dialogRidePreference.findViewById<RadioButton>(R.id.rideTaker)
                
                // Don't pre-select any option
                rideGiver.isChecked = false
                rideTaker.isChecked = false

                rideGiver.setOnClickListener { rideTakerOptions.visibility = View.GONE }
                rideTaker.setOnClickListener { rideTakerOptions.visibility = View.VISIBLE }

                val btnSave = dialogRidePreference.findViewById<Button>(R.id.btnSaveRP)
                btnSave.setOnClickListener {
                    val ridePreOptionID = dialogRidePreference.findViewById<RadioGroup>(R.id.radioGroupOptionsRP).checkedRadioButtonId
                    if (ridePreOptionID != -1) {
                        var rp = ""
                        val riseSelectedRadioBtn = dialogRidePreference.findViewById<RadioButton>(ridePreOptionID)  // GIVER/TAKER
                        if (riseSelectedRadioBtn.tag == com.intelly.lyncwyze.Assest.modals.RiderType.GIVER.name) {
                            rp = riseSelectedRadioBtn.tag.toString()
                            dialog.dismiss()
                            
                            // Update schedule for GIVER
                            childActivity.schedulePerDay[day.name] = DailySchedule(
                                startTime = startTime,
                                endTime = endTime,
                                preferredPickupTime = 0,
                                pickupRole = rp,
                                dropoffRole = rp
                            )
                            
                            // Update UI
                            timeView.text = "${convert24to12HourFormat(startTime)} \n - \n ${convert24to12HourFormat(endTime)} \n - \n ${getLocalizedText(this, rp)}"
                        }
                        else {    // TAKER
                            rp = ""
                            val rideTakerOptionsId = rideTakerOptions.checkedRadioButtonId
                            if (rideTakerOptionsId != -1) {
                                rp = dialogRidePreference.findViewById<RadioButton>(rideTakerOptionsId).tag as String    // Taker options
                                dialog.dismiss()
                                
                                // Show pickup time dialog
                                val timeDialog = AlertDialog.Builder(this)
                                    .setView(layoutInflater.inflate(R.layout.dialog_radio_buttons, null))
                                    .create()
                                timeDialog.show()

                                val radioGroupOptions = timeDialog.findViewById<RadioGroup>(R.id.radioGroupOptions)
                                val timeSaveBtn = timeDialog.findViewById<Button>(R.id.btnSave)
                                
                                // Set current pickup time if exists
                                if (schedule.preferredPickupTime > 0) {
                                    val pickupTimeOption = "${schedule.preferredPickupTime} min"
                                    for (i in 0 until radioGroupOptions.childCount) {
                                        val radioButton = radioGroupOptions.getChildAt(i) as RadioButton
                                        if (radioButton.tag.toString() == pickupTimeOption) {
                                            radioButton.isChecked = true
                                            break
                                        }
                                    }
                                }
                                
                                timeSaveBtn.setOnClickListener {
                                    val timeSelectID = radioGroupOptions.checkedRadioButtonId
                                    if (timeSelectID != -1) {
                                        val selectedTime = radioGroupOptions.findViewById<RadioButton>(timeSelectID)
                                        val selectedText = selectedTime.tag.toString()
                                        val minutesValue = selectedText.split(" ")[0].toInt()
                                        
                                        // Update schedule for TAKER
                                        childActivity.schedulePerDay[day.name] = DailySchedule(
                                            startTime = startTime,
                                            endTime = endTime,
                                            preferredPickupTime = minutesValue,
                                            pickupRole = rp,
                                            dropoffRole = rp
                                        )
                                        
                                        // Update UI
                                        timeView.text = "${convert24to12HourFormat(startTime)} \n - \n ${convert24to12HourFormat(endTime)} \n - \n ${getLocalizedText(this, rp)} \n -\n $minutesValue min"
                                        timeDialog.dismiss()
                                    } else
                                        showToast(this, getString(R.string.please_select_an_option))
                                }
                            }
                            else
                                showToast(this, getString(R.string.select_an_ride_taker_option_to_proceed))
                        }
                    } else
                        showToast(this, getString(R.string.please_select_an_option))
                }
                dialog.show()
            }
            dialogEndTime.show()
        }
        dialogStartTime.show()
    }

    private fun saveActivity() {
        if (validateData()) {
            lifecycleScope.launch {
                try {
                    childActivity.address.addressLine1 = selectedProvider!!.address.addressLine1
                    childActivity.address.addressLine2 = selectedProvider!!.address.addressLine2
                    childActivity.address.landMark = selectedProvider!!.address.landMark
                    childActivity.address.state = selectedProvider!!.address.state
                    childActivity.address.city = selectedProvider!!.address.city
                    childActivity.address.location.coordinates = selectedProvider!!.address.location.coordinates
                    childActivity.address.location.type= selectedProvider!!.address.location.type
                    childActivity.type = activityType.text.trim().toString()
                    childActivity.subType = activitySubType.text.trim().toString()

                    loader.showLoader(this@Activity_Activities_Add)
                    val response = NetworkManager.apiService.addChildActivity(body = childActivity)
                    if (response.isSuccessful) {
                        loader.hideLoader(this@Activity_Activities_Add)
                        response.body()?.let {
                            showToast(this@Activity_Activities_Add, getString(R.string.activity_saved_successfully))
                            val dataCount = SharedPreferencesManager.getObject<DataCountResp>(UserRequiredDataCount)!!
                            if (dataCount.vehicle < 1) {
                                dataCount.activity += 1;
                                SharedPreferencesManager.saveObject(UserRequiredDataCount, dataCount)
                                startActivity(Intent(this@Activity_Activities_Add, Activity_Vehicle_Add::class.java))
                            } else this@Activity_Activities_Add.finish()
                        } ?: showToast(this@Activity_Activities_Add, getString(R.string.fetching_error))
                    } else {
                        loader.hideLoader(this@Activity_Activities_Add)
                        showToast(this@Activity_Activities_Add, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                    }
                    } catch (e: Exception) {
                        loader.hideLoader(this@Activity_Activities_Add)
                        logger.error { e.message }
                        showToast(this@Activity_Activities_Add, "${e.message}")
                    }
            }
        }
    }
    private fun validateData(): Boolean {
        if (selectedProvider == null) {
            showToast(this@Activity_Activities_Add, getString(R.string.please_add_the_activity_location))
            availableProviders.performClick()
            return false
        }
        if (childActivity.schedulePerDay.isEmpty()) {
            showToast(this@Activity_Activities_Add, getString(R.string.please_add_activity_days_and_time))
            return false
        }
        return true
    }
}