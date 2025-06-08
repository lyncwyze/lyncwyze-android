package com.intelly.lyncwyze.enteredUser.records

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.PaginatedResponse
import com.intelly.lyncwyze.Assest.modals.EachRide
import com.intelly.lyncwyze.Assest.modals.FeedBackPreReq
import com.intelly.lyncwyze.Assest.modals.RideStatus
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.modals.feedBackRequired
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.convertToMiles
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.feedback.Activity_RideFeedback
import kotlinx.coroutines.launch
import mu.KotlinLogging

class Activity_RidesGiven : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private var loader = Loader(this)

    private lateinit var eachRide: PaginatedResponse<EachRide>

    private lateinit var backButton: ImageView
    private lateinit var scrollView: ScrollView
    private lateinit var rideRecordsLL: LinearLayout
    private lateinit var theListNoContent: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rides_given)
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        backButton = findViewById(R.id.rideRecordBackButton)
        scrollView = findViewById(R.id.rideRecordSV)
        rideRecordsLL = findViewById(R.id.rideRecordsLL)
        theListNoContent = findViewById(R.id.rideRecordNoList)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        fetchRides()
    }
    private fun fetchRides() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_RidesGiven)
                val response = NetworkManager.apiService.getUpcomingRides(pageSize = 100, status = RideStatus.COMPLETED.name, role = RiderType.GIVER.name)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_RidesGiven)
                    response.body()?.let {
                        eachRide = it
                        displayRides()
                    } ?: showToast(this@Activity_RidesGiven, "No upcoming rides found!")
                } else {
                    loader.hideLoader(this@Activity_RidesGiven)
                    showToast(this@Activity_RidesGiven, "${getString(R.string.error_txt)}: ${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            } catch (e: Exception) {
                loader.hideLoader(this@Activity_RidesGiven)
                logger.error { e.message }
                showToast(this@Activity_RidesGiven, "Exception: ${e.message}")
            }
        }
    }
    private fun displayRides() {
        rideRecordsLL.removeAllViews()
        if (eachRide.data.isNotEmpty()) {
            logger.info { eachRide.data }
            for (rideD in eachRide.data) {
                val thisUserIsTracker = rideD.rideTakers.map { it.userId }.contains(SharedPreferencesManager.getDecodedAccessToken()!!.userId)

                val rideGiverItemView = layoutInflater.inflate(R.layout.layout_ride_record_item, rideRecordsLL, false)

                rideGiverItemView.findViewById<TextView>(R.id.otherPartyName).text =
                    if (thisUserIsTracker) "${rideD.userFirstName} ${rideD.userLastName}"
                    else "${rideD.rideTakers[0].userFirstName} ${rideD.rideTakers[0].userLastName}"


                rideGiverItemView.findViewById<ImageView>(R.id.callPerson).setOnClickListener {
                    val number = if (thisUserIsTracker) rideD.mobileNumber else rideD.rideTakers[0].mobileNumber
                    number?.let { startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$it") })
                    } ?: showToast(this, "Phone number is missing!")
                }
                rideGiverItemView.findViewById<ImageView>(R.id.messagePerson).setOnClickListener {
                    val number = if (thisUserIsTracker) rideD.mobileNumber else rideD.rideTakers[0].mobileNumber
                    number?.let {
                        val messageIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$it") // Use "smsto:" for SMS
                            putExtra("sms_body", " ")
                        }
                        startActivity(messageIntent)
                    } ?: showToast(this, "Phone number is missing!")
                }


                rideGiverItemView.findViewById<TextView>(R.id.distanceTravel).text = convertToMiles(rideD.rideTakers[0].distance)
                rideGiverItemView.findViewById<TextView>(R.id.otherPartySuccessRecord).text  = "Successfuly completed ${rideD.noOfCompletedRides} rides"
                rideGiverItemView.findViewById<TextView>(R.id.communityText).text  = "Community Text"
                rideGiverItemView.findViewById<TextView>(R.id.fromLocationText).text  = "${rideD.pickupAddress.addressLine1}"
                rideGiverItemView.findViewById<TextView>(R.id.toLocation).text  = "${rideD.dropoffAddress.addressLine1}"

                val personImg = rideGiverItemView.findViewById<ImageView>(R.id.personImg)
                rideD.rideTakers[0].userImage?.let { endpoint ->
                    lifecycleScope.launch {
                        ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                            try {
                                val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                                val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                                Glide.with(this@Activity_RidesGiven)
                                    .load(bitmap)
                                    .into(personImg)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                personImg.setImageResource(R.drawable.user)
                            }
                        } ?: run { personImg.setImageResource(R.drawable.user) }
                    }
                }
                rideGiverItemView.setOnClickListener {
                    val data = FeedBackPreReq(
                        rideId = rideD.id,
                        fromUserId = rideD.userId,
                        fromUserName = rideD.userFirstName + " " + rideD.userLastName,
                        forUserId = rideD.rideTakers[0].userId,
                        forUserName = rideD.rideTakers[0].userFirstName + " " + rideD.rideTakers[0].userLastName,
                        riderType =  RiderType.GIVER
                    )

                    SharedPreferencesManager.saveObject(feedBackRequired, data)
                    val intent = Intent(this, Activity_RideFeedback::class.java)
                    intent.putExtra("ride_date", rideD.date)
                    startActivity(intent)
                }
                rideRecordsLL.addView(rideGiverItemView)
            }
            scrollView.visibility = View.VISIBLE
            theListNoContent.visibility = View.GONE
        } else {
            scrollView.visibility = View.GONE
            theListNoContent.visibility = View.VISIBLE
            showToast(this@Activity_RidesGiven, getString(R.string.found_nothing))
        }
    }
}