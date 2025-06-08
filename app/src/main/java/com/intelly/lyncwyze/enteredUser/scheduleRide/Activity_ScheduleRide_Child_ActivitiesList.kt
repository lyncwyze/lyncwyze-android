package com.intelly.lyncwyze.enteredUser.scheduleRide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.ChildActivityDC
import com.intelly.lyncwyze.Assest.modals.WeekDays
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_DAY
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_ID
import com.intelly.lyncwyze.Assest.utilities.CHILD_ID
import com.intelly.lyncwyze.Assest.utilities.CHILD_NAME
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.IS_VALID_DAY
import com.intelly.lyncwyze.Assest.utilities.getLocalizedText
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.view.isNotEmpty

class Activity_ScheduleRide_Child_ActivitiesList : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    private lateinit var childId: String
    private lateinit var childName: String

    private lateinit var backButton: ImageView
    private lateinit var textChildAndActivitiesName: TextView

    private lateinit var dayMonday: Button
    private lateinit var dayTuesday: Button
    private lateinit var dayWednesday: Button
    private lateinit var dayThursday: Button
    private lateinit var dayFriday: Button
    private lateinit var daySaturday: Button
    private lateinit var daySunday: Button
    private var selectedDay: String = ""
    private var validDays: List<String> = emptyList()

    private lateinit var scrollView: ScrollView
    private lateinit var theListNoContent: LinearLayout

    private var activities: MutableList<ChildActivityDC> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule_ride_child_activities_list)
        activityCheck()
        setupUI()
        setUpFunctionality()
        highlightTodayButton()
        fetchValidDays()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        activityCheck()
        fetchChildActivities()
    }

    private fun activityCheck() {
        val forChildId = intent.getStringExtra(CHILD_ID)
        if (forChildId.isNullOrBlank()) {
            SweetAlertDialog(this@Activity_ScheduleRide_Child_ActivitiesList, SweetAlertDialog.ERROR_TYPE)
                .setTitleText(getString(R.string.error_getting_child_id))
                .setConfirmText(getString(R.string.ok_Txt))
                .setConfirmClickListener { sDialog ->
                    sDialog.dismissWithAnimation()
                    this@Activity_ScheduleRide_Child_ActivitiesList.finish()
                }
                .show()
        } else {
            childId = forChildId
            childName = intent.getStringExtra(CHILD_NAME) ?: ""
        }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.aclBackButton)
        textChildAndActivitiesName = findViewById(R.id.textChildAndActivitiesName)
        textChildAndActivitiesName.text = if (childName.isNotEmpty()) "$childName - ${getString(R.string.activities)}" else getString(R.string.activity_details)

        dayMonday = findViewById(R.id.dayMonday)
        dayTuesday = findViewById(R.id.dayTuesday)
        dayWednesday = findViewById(R.id.dayWednesday)
        dayThursday = findViewById(R.id.dayThursday)
        dayFriday = findViewById(R.id.dayFriday)
        daySaturday = findViewById(R.id.daySaturday)
        daySunday = findViewById(R.id.daySunday)

        scrollView = findViewById(R.id.aclScrollView)
        theListNoContent = findViewById(R.id.aclNoList)

    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }

        dayMonday.setOnClickListener { onDayButtonClick( WeekDays.MONDAY.toString(), dayMonday) }
        dayTuesday.setOnClickListener { onDayButtonClick( WeekDays.TUESDAY.toString(), dayTuesday) }
        dayWednesday.setOnClickListener { onDayButtonClick( WeekDays.WEDNESDAY.toString(), dayWednesday) }
        dayThursday.setOnClickListener { onDayButtonClick( WeekDays.THURSDAY.toString(), dayThursday) }
        dayFriday.setOnClickListener { onDayButtonClick( WeekDays.FRIDAY.toString(), dayFriday) }
        daySaturday.setOnClickListener { onDayButtonClick( WeekDays.SATURDAY.toString(), daySaturday) }
        daySunday.setOnClickListener { onDayButtonClick( WeekDays.SUNDAY.toString(), daySunday) }
    }

    private fun onDayButtonClick(day: String, button: Button) {
        resetButtonStyles()
        setSelectedStyle(button, day)
        displayChildActivities()
    }
    private fun highlightTodayButton() {
        resetButtonStyles()
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        when (currentDay.uppercase()) {
            WeekDays.MONDAY.name -> setSelectedStyle(dayMonday, WeekDays.MONDAY.name)
            WeekDays.TUESDAY.name -> setSelectedStyle(dayTuesday, WeekDays.TUESDAY.name)
            WeekDays.WEDNESDAY.name -> setSelectedStyle(dayWednesday, WeekDays.WEDNESDAY.name)
            WeekDays.THURSDAY.name -> setSelectedStyle(dayThursday, WeekDays.THURSDAY.name)
            WeekDays.FRIDAY.name -> setSelectedStyle(dayFriday, WeekDays.FRIDAY.name)
            WeekDays.SATURDAY.name -> setSelectedStyle(daySaturday, WeekDays.SATURDAY.name)
            WeekDays.SUNDAY.name -> setSelectedStyle(daySunday, WeekDays.SUNDAY.name)
        }
    }
    private fun resetButtonStyles() {
        val buttons = listOf(dayMonday, dayTuesday, dayWednesday, dayThursday, dayFriday, daySaturday, daySunday)
        for (button in buttons)
            button.background = resources.getDrawable(R.drawable.circular_button_background, null)
    }
    private fun setSelectedStyle(button: Button, day: String) {
        button.background = resources.getDrawable(R.drawable.circular_button_background_selected, null)
        selectedDay = day
    }

    private fun fetchValidDays() {
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getValidDays()
                if (response.isSuccessful) {
                    validDays = response.body() ?: emptyList()
                    fetchChildActivities()
                } else {
                    showToast(this@Activity_ScheduleRide_Child_ActivitiesList, "Error fetching valid days")
                }
            } catch (e: Exception) {
                showToast(this@Activity_ScheduleRide_Child_ActivitiesList, "Exception: ${e.message}")
            }
        }
    }

    private fun fetchChildActivities() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_ScheduleRide_Child_ActivitiesList)
                val response = NetworkManager.apiService.getChildActivities(childId = childId, pageSize = 1000, offSet = 0, sortOrder = "ASC")
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_ScheduleRide_Child_ActivitiesList)
                    val childActivities = response.body()
                    if (childActivities != null) {
                        activities = childActivities.data.toMutableList()
                        if (activities.isNotEmpty()) {
                            displayChildActivities()
                        } else {
                            showToast(this@Activity_ScheduleRide_Child_ActivitiesList, getString(R.string.found_nothing_to_list))
                            scrollView.visibility = View.INVISIBLE
                            theListNoContent.visibility = View.VISIBLE
                        }
                    } else {
                        showToast(this@Activity_ScheduleRide_Child_ActivitiesList, "Fetching error!")
                    }
                } else {
                    loader.hideLoader(this@Activity_ScheduleRide_Child_ActivitiesList)
                    showToast(this@Activity_ScheduleRide_Child_ActivitiesList, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_ScheduleRide_Child_ActivitiesList)
                showToast(this@Activity_ScheduleRide_Child_ActivitiesList, "Exception: ${e.message}")
            }
        }
    }
    private fun displayChildActivities() {
        val childActivitiesLayout = findViewById<LinearLayout>(R.id.theChildActivities)
        childActivitiesLayout.removeAllViews()

        if (activities.isNotEmpty()) {
            for (eachActivity in activities) {
                for ((day, schedule) in eachActivity.schedulePerDay) {
                    if (selectedDay.isBlank() || selectedDay.uppercase() == day.uppercase()) {
                        val childActEachDayLayout = layoutInflater.inflate(R.layout.layout_each_day_activity, childActivitiesLayout, false)

                        val imageViewChild = childActEachDayLayout.findViewById<ImageView>(R.id.childImgViewEachDay)
                        if (eachActivity.image != null) Glide.with(this).load(eachActivity.image).into(imageViewChild)
                        else imageViewChild.setImageResource(R.drawable.user)

                        val classTime = "${schedule.startTime.subSequence(0, 5)} - ${schedule.endTime.subSequence(0, 5)}"
                        val pickUpTime = "${schedule.preferredPickupTime} ${getString(R.string.min)}"
                        childActEachDayLayout.findViewById<TextView>(R.id.textActivityType).text = eachActivity.type
                        childActEachDayLayout.findViewById<TextView>(R.id.textClassName).text = eachActivity.subType
                        childActEachDayLayout.findViewById<TextView>(R.id.textClassTiming).text = classTime
                        childActEachDayLayout.findViewById<TextView>(R.id.pickUpTime).text = pickUpTime
                        childActEachDayLayout.findViewById<TextView>(R.id.driveType).text = getLocalizedText(this, schedule.dropoffRole)
                        childActEachDayLayout.findViewById<ImageView>(R.id.editActivity).visibility = View.GONE
                        childActEachDayLayout.setOnClickListener {
                            val intent = Intent(this, Activity_ConfirmActivity::class.java)
                                .apply {
                                    putExtra(CHILD_ACTIVITY_ID, eachActivity.id)
                                    putExtra(CHILD_ACTIVITY_DAY, day)
                                    putExtra(IS_VALID_DAY, validDays.contains(day.uppercase()))
                                }
                            startActivity(intent)
                        }
                        childActivitiesLayout.addView(childActEachDayLayout)
                    }
                }
            }
        }

        if (childActivitiesLayout.isNotEmpty()) {
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.INVISIBLE
        } else {
            showToast(this@Activity_ScheduleRide_Child_ActivitiesList, getString(R.string.found_nothing_to_list))
            scrollView.visibility = View.INVISIBLE
            theListNoContent.visibility = View.VISIBLE
//            findViewById<TextView>(R.id.aclNoContentText).text = "${getString(R.string.no_activity_found_for)} ${selectedDay.lowercase().replaceFirstChar { it.uppercase() }}"
        }
    }
}