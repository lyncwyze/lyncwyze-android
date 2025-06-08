package com.intelly.lyncwyze.enteredUser.rideTracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intelly.lyncwyze.Assest.Loader
import com.intelly.lyncwyze.Assest.modals.EachRide
import com.intelly.lyncwyze.Assest.modals.FeedBackPreReq
import com.intelly.lyncwyze.Assest.modals.RideStartEvent
import com.intelly.lyncwyze.Assest.modals.RideStatus
import com.intelly.lyncwyze.Assest.modals.RideTrack
import com.intelly.lyncwyze.Assest.modals.RideType
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.modals.WebSocketEvents
import com.intelly.lyncwyze.Assest.modals.WsEvent
import com.intelly.lyncwyze.Assest.modals.WsEventGeneral
import com.intelly.lyncwyze.Assest.modals.feedBackRequired
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.HttpUtilities
import com.intelly.lyncwyze.Assest.utilities.ImageUtilities
import com.intelly.lyncwyze.Assest.utilities.RideData
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import com.intelly.lyncwyze.Assest.utilities.formatDateTime
import com.intelly.lyncwyze.Assest.utilities.showBottomSheetDialog
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.Assest.utilities.wsUrl
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.feedback.Activity_RideFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.util.concurrent.TimeUnit

class Activity_RideTracking_Giver : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }
    private val loader = Loader(this)

    private lateinit var rideData: EachRide

    private lateinit var backButton: ImageView
    private lateinit var rideTypeInclude: LinearLayout

    private lateinit var onlyDrop: LinearLayout
    private lateinit var startedImg: ImageView
    private lateinit var startText: TextView
    private lateinit var startRideTime: TextView

    private lateinit var pickedImg: ImageView
    private lateinit var pickUpText: TextView
    private lateinit var pickUpLocationName: TextView
    private lateinit var pickupRideTime: TextView

    private lateinit var droppedImg: ImageView
    private lateinit var dropOffText: TextView
    private lateinit var dropOffLocation: TextView
    private lateinit var dropOffTime: TextView


    // PICK
    private lateinit var onlyPick: LinearLayout
    private lateinit var reStartedImg: ImageView
    private lateinit var reStartText: TextView
    private lateinit var reStartRideTime: TextView

    private lateinit var rePickedImg: ImageView
    private lateinit var rePickUpText: TextView
    private lateinit var rePickUpLocationName: TextView
    private lateinit var rePickupRideTime: TextView

    // drop home
    private lateinit var dropOffHomeImg: ImageView
    private lateinit var dropOffHomeTxt: TextView
    private lateinit var dropOffHomeLocation: TextView
    private lateinit var dropOffHomeTime: TextView

    private lateinit var userName: TextView

    private lateinit var messageText: EditText
    private lateinit var callPerson: ImageView
    private lateinit var messagePerson: ImageView

    // Button to update status
    private lateinit var notifyButton: Button

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var LOCATION_PERMISSION_REQUEST_CODE: Int = 1233456
    private var webSocket: WebSocket? = null
    private var pingPongJob: Job? = null
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var latLong: List<Double> = listOf()

    private var isCompletionDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ride_tracking_giver)
        activityCheck()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun onResume() {
        super.onResume()
        connectWebSocket()
        getRideTrackingAndUpdateUI()
    }
    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
        disconnectWebSocket()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                getCurrentLocation()
            else
                showToast(this, "Location permission is required")
        }
    }

    private var thisUserIsTracker = false
    private var thisRideType: RideType? = RideType.DROP
    private fun activityCheck() {
        SharedPreferencesManager.getObject<EachRide>(RideData)?.let {
            rideData = it
            thisUserIsTracker = it.rideTakers.map { me -> me.userId }.contains(SharedPreferencesManager.getDecodedAccessToken()!!.userId)
            thisRideType = rideData.rideTakers[0].role
            setupUI()
            setUpFunctionality()
        } ?: {
            this.onBackPressed()
            showToast(this, "Ride data not found!")
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    private fun setupUI() {
        backButton = findViewById(R.id.rideTrackBackButton)

        rideTypeInclude = findViewById(R.id.rideTypeInclude)
        rideTypeInclude.removeAllViews()

        // Ride stages
        val rideDrop = layoutInflater.inflate(R.layout.layout_ride, rideTypeInclude, false)

        // layout drop
        onlyDrop = rideDrop.findViewById(R.id.onlyDrop)
        // Start
        startedImg = rideDrop.findViewById(R.id.toActivityStartImg)
        startText = rideDrop.findViewById(R.id.toActivityStartText)
        startText.text = getString(R.string.start_text)
        startRideTime = rideDrop.findViewById(R.id.toActivityStartTime)

        // Picked
        pickedImg = rideDrop.findViewById(R.id.toActivityPickImg)
        pickUpText = rideDrop.findViewById(R.id.toActivityPickText)
        pickUpLocationName = rideDrop.findViewById(R.id.toActivityPickLocation)
        pickUpLocationName.text = rideData.pickupAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        pickupRideTime = rideDrop.findViewById(R.id.toActivityPickTime)

        // Dropped
        droppedImg = rideDrop.findViewById(R.id.toActivityDropImg)
        dropOffText = rideDrop.findViewById(R.id.toActivityDropText)
        dropOffLocation = rideDrop.findViewById(R.id.toActivityDropLocation)
        dropOffLocation.text = rideData.dropoffAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        dropOffTime = rideDrop.findViewById(R.id.toActivityDropTime)

        // layout drop
        onlyPick = rideDrop.findViewById(R.id.onlyPick)
        // Start to act
        reStartedImg = rideDrop.findViewById(R.id.fromActivityStartImg)
        reStartText = rideDrop.findViewById(R.id.fromActivityStartText)
        reStartRideTime = rideDrop.findViewById(R.id.fromActivityStartTime)

        // Pick from act
        rePickedImg = rideDrop.findViewById(R.id.fromActivityPickImg)
        rePickUpText = rideDrop.findViewById(R.id.fromActivityPickText)
        rePickUpLocationName = rideDrop.findViewById(R.id.fromActivityPickLocation)
        rePickUpLocationName.text = rideData.dropoffAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        rePickupRideTime = rideDrop.findViewById(R.id.fromActivityPickTime)

        // Drop at home
        dropOffHomeImg = rideDrop.findViewById(R.id.fromActivityDropHomeImg)
        dropOffHomeTxt = rideDrop.findViewById(R.id.fromActivityDropHomeText)
        dropOffHomeLocation = rideDrop.findViewById(R.id.fromActivityDropHomeLocation)
        dropOffHomeLocation.text = rideData.pickupAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        dropOffHomeTime = rideDrop.findViewById(R.id.fromActivityDropHomeTime)

        rideTypeInclude.addView(rideDrop)

        when (thisRideType) {
            RideType.DROP_PICK -> { }
            RideType.DROP -> onlyPick.visibility = View.GONE
            RideType.PICK -> onlyDrop.visibility = View.GONE
            else -> showToast(this, "Unknown ride type")
        }

        val imageToPut = if (thisUserIsTracker) rideData.userImage else rideData.rideTakers[0].userImage
        val personImg = findViewById<ImageView>(R.id.rideTakerImg)
        imageToPut?.let { endpoint ->
            lifecycleScope.launch {
                ImageUtilities.imageFullPath(endpoint)?.let { base64Data ->
                    try {
                        val decodedString = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap: Bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        Glide.with(this@Activity_RideTracking_Giver).load(bitmap).into(personImg)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        personImg.setImageResource(R.drawable.user)
                    }
                } ?: run { personImg.setImageResource(R.drawable.user) }
            }
        }

        notifyButton = findViewById(R.id.notifyButton)
//        Glide.with(this@Activity_Giver_Tracking).load(rideData.rideTakerImg).into(findViewById(R.id.rideTakerImg))
        userName = findViewById(R.id.rideTakerName)
        "${rideData.rideTakers[0].userFirstName} ${rideData.rideTakers[0].userLastName}".also { userName.text = it }

        userName.setOnClickListener {
            getRideTrackingAndUpdateUI()
        }

        val act = findViewById<TextView>(R.id.activityType)
        if (rideData.dropoffAddress.addressLine1?.isNotBlank() == true) {
            act.text = rideData.dropoffAddress.addressLine1
            act.visibility = View.VISIBLE
        }
        else act.visibility = View.GONE

        findViewById<TextView>(R.id.activityTiming).text =
        formatDateTime(rideData.pickupTime, "hh:mm a") + " - " +formatDateTime(rideData.dropoffTime, "hh:mm a") + "(${rideData.rideTakers[0].preferredPickupTime} min)"

        messageText = findViewById(R.id.messageText)
        callPerson = findViewById(R.id.iconCallPerson)
        messagePerson = findViewById(R.id.iconMesPerson)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener {
            disconnectWebSocket()
            this.onBackPressed()
        }
        notifyButton.setOnClickListener { notifyParent() }
        messageText.setOnFocusChangeListener { v, hasFocus -> v.elevation = if (hasFocus) 2f else 0f }
        callPerson.setOnClickListener { rideData.rideTakers[0].mobileNumber?.let { startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$it") }) } ?: showToast(this, "Phone number is missing!") }
        messagePerson.setOnClickListener {
            val theMessage = messageText.text.trim().toString()
            rideData.rideTakers[0].mobileNumber?.let {
                val messageIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$it") // Use "smsto:" for SMS
                    putExtra("sms_body", theMessage) // Pre-fill the message body
                }
                startActivity(messageIntent)
            } ?: showToast(this, "Phone number is missing!")
        }
    }

    private fun checkAndAddLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        else
            getCurrentLocation()
    }

    // Web socket
    private var currentStatus = RideStatus.SCHEDULED.name
    private var nextStatus = WebSocketEvents.RIDE_START.name
    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()

        val request = Request.Builder().apply {
            SharedPreferencesManager.getValidatedAccessToken()?.let { addHeader("Authorization", "Bearer $it") }
        }.url("$wsUrl?role=${RiderType.GIVER.name}&rideId=${rideData.id}&giverId=${SharedPreferencesManager.getDecodedAccessToken()!!.userId}").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                checkAndAddLocationPermission()
                startPingPong() }
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.equals("PONG", true)) {
                    logger.info { "Received pong" }
                }
                else if (text.equals("PING", true)) {
                    logger.info { "Received ping" }
                }
                else runOnUiThread {
                    logger.info { "Data from WS: $text" }
                    val wsData = Gson().fromJson(text, JsonObject::class.java)
                    when (val socketEventType = wsData.getAsJsonPrimitive("socketEventType")?.asString) {
                        WebSocketEvents.ERROR.name -> {
                            when (val errorCode = wsData.getAsJsonPrimitive("errorCode")?.asInt) {
                                7007 -> {
//                                    wsData.getAsJsonPrimitive("errorDescription")?.asString!! >>> Ride location does not exists.
//                                    showToast(this@Activity_RideTracking_Giver, getString(R.string.ride_not_started_yet))
                                    nextStatus = WebSocketEvents.RIDE_START.name
                                }
                                7010 -> showToast(this@Activity_RideTracking_Giver, wsData.getAsJsonPrimitive("errorDescription")?.asString!!)
                                else -> logger.info { "Some other code: $errorCode" }
                            }
                        }
                        WebSocketEvents.STATUS.name -> {
                            currentStatus = wsData.getAsJsonPrimitive("rideStatus")?.asString!!
                            nextStatus = wsData.getAsJsonPrimitive("nextStatus")?.asString!!

//                            showToast(this@Activity_RideTracking_Giver, "$currentStatus >>> $nextStatus")
//                            logger.info { "$currentStatus >>> $nextStatus" }
//                            logger.info { rideTrack }

                            updateRideStatusUI(currentStatus)
                            when (nextStatus) {
                                RideStatus.SCHEDULED.name -> showToast(this@Activity_RideTracking_Giver, "Start ride, and notify ride taker.")
                                else -> {}
                            }
                        }
                        else -> showToast(this@Activity_RideTracking_Giver, "Unknown status $socketEventType")
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logger.info { "WebSocket closed at: ${System.currentTimeMillis()}, code: $code, reason: $reason" }
                stopPingPong()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                logger.error { "WebSocket failure at: ${System.currentTimeMillis()}, error: ${t.message}" }
                t.printStackTrace() // Log the full stack trace
                stopPingPong()
                CoroutineScope(Dispatchers.IO).launch {
                    delay(4000)
                    launch(Dispatchers.Main) {
                        connectWebSocket()
                    }
                }
            }
        })
    }
    private fun sendDataInWS(data: String) {
        logger.info { "Data to WebSocket: $data" }
        webSocket?.let {
            try { it.send(data) }
            catch (e: Exception) { logger.error { "Error sending data: ${e.message}" } }
        } ?: connectWebSocket()
    }
    private fun sendNextStage(nextStage: WebSocketEvents) {
        val data = WsEvent(
            socketEventType = nextStage,
            rideId = rideData.id,
            takerId = rideData.rideTakers[0].userId,
            latitude = latLong[0],
            longitude = latLong[1]
        )
        sendDataInWS(SharedPreferencesManager.gson.toJson(data))
        if (nextStage != WebSocketEvents.LOCATION_UPDATE && nextStage != WebSocketEvents.RETURNED_HOME)
            CoroutineScope(Dispatchers.Main).launch {
                delay(400)
                getRideTrackingAndUpdateUI()
            }
    }

    private fun disconnectWebSocket() {
        stopPingPong()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    // Web socket Ping-Pong
    private fun startPingPong() {
        pingPongJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) { // Keep sending pings while the job is active
                try {
                    sendDataInWS("PING")
                    delay(35000)
                } catch (e: Exception) {
                    logger.error("Error sending ping: ${e.message}")
                    break // Exit the loop if there's an error
                }
            }
        }
    }
    private fun stopPingPong() {
        pingPongJob?.cancel()
        pingPongJob = null
    }

    private fun wsGetRideStatus() {
        val data = WsEventGeneral(
            socketEventType = WebSocketEvents.STATUS,
            rideId = rideData.id,
            takerId = null)
        sendDataInWS(SharedPreferencesManager.gson.toJson(data))
    }
    private fun callWsGetRideStatusAfterDelay(time: Long = 1000) {
        CoroutineScope(Dispatchers.Main).launch {
            delay(time)
            wsGetRideStatus()
        }
    }

    private fun getCurrentLocation() {
        locationRequest = LocationRequest.create().apply {
            interval = 30000
            fastestInterval = 3000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    latLong = listOf(location.latitude, location.longitude)
                    sendNextStage(WebSocketEvents.LOCATION_UPDATE)
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private var rideTrack: RideTrack? = null
    private fun getRideTrackingAndUpdateUI() {
        lifecycleScope.launch {
            try {
                loader.showLoader(this@Activity_RideTracking_Giver)
                val response = NetworkManager.apiService.getRideDetailsByID(rideData.id)
                if (response.isSuccessful) {
                    loader.hideLoader(this@Activity_RideTracking_Giver)
                    response.body()?.let {
                        rideTrack = it
                        updateRideStatusUI(it.status.name)
                        logger.info { it.rideTakers[0].statusHistory }
                        callWsGetRideStatusAfterDelay(200)
                    } ?:
                    showToast(this@Activity_RideTracking_Giver, "Fetching error!")
                }
                else if (response.code() == 400) {
                    loader.hideLoader(this@Activity_RideTracking_Giver)
                    showToast(this@Activity_RideTracking_Giver, getString(R.string.ride_not_started_yet))
                }
                else {
                    loader.hideLoader(this@Activity_RideTracking_Giver)
                    showToast(this@Activity_RideTracking_Giver, "${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
                }
            }
            catch (e: Exception) {
                loader.hideLoader(this@Activity_RideTracking_Giver)
                showToast(this@Activity_RideTracking_Giver, e.message.toString())
            }
        }
    }
    private fun updateRideStatusUI(rideStatus: String) {
        logger.info { "Updating ride status time on UI" }
        when (rideStatus) {  // Next step view
            RideStatus.STARTED.name -> {
                if (thisRideType == RideType.PICK) {
                    stageReturnedAtActivity()
                }
                else {
                    stageStarted()
                }
            }
            RideStatus.RIDER_ARRIVED.name -> {
                stageStarted()
                stageArrivedAtHome()
            }
            RideStatus.PICKED_UP.name -> {
                stageStarted()
                stageArrivedAtHome()
                stagePickedFromHome()
            }
            RideStatus.ACTIVITY_ONGOING.name -> {
                if (thisRideType == RideType.PICK) {
                    stageReturnedAtActivity()
                }
                else {
                    stageStarted()
                    stageArrivedAtHome()
                    stagePickedFromHome()
                    stageDroppedAtActivity()
                }
            } // Completed for ride type pick
            RideStatus.RETURNED_ACTIVITY.name -> {
                if (thisRideType != RideType.PICK) {
                    stageStarted()
                    stageArrivedAtHome()
                    stagePickedFromHome()
                    stageDroppedAtActivity()
                }
                stageReachedActivity()
                stageReturnedAtActivity()
            }
            RideStatus.PICKED_UP_FROM_ACTIVITY.name -> {
                stageStarted()
                stageArrivedAtHome()
                stagePickedFromHome()
                stageDroppedAtActivity()
                stageReturnedAtActivity()
                stagePickedFromActivity()
            }
            RideStatus.RETURNED_HOME.name -> {
                stageStarted()
                stagePickedFromHome()
                stageArrivedAtHome()
                stageDroppedAtActivity()
                stageReturnedAtActivity()
                stagePickedFromActivity()
                stageReachedHome()
            }
            RideStatus.COMPLETED.name, WebSocketEvents.RIDE_END.name -> {
                stageStarted()
                stageArrivedAtHome()
                stagePickedFromHome()
                stageDroppedAtActivity()
                stageReturnedAtActivity()
                stagePickedFromActivity()
                stageDroppedAtHome()
                stageCompleted()
            }
            else -> logger.error { "Unknown $currentStatus" }
        }
    }
    private fun notifyParent() {
        if(latLong.isNotEmpty()) {
            when (nextStatus) {
                WebSocketEvents.RIDE_START.name -> {
                    val data = RideStartEvent(
                        rideId = rideData.id,
                        socketEventType = WebSocketEvents.RIDE_START,
                        startLatitude = latLong[0],
                        startLongitude = latLong[1],
                        endLatitude = rideData.dropoffAddress.location.x,
                        endLongitude = rideData.dropoffAddress.location.y
                    )
                    sendDataInWS(SharedPreferencesManager.gson.toJson(data))
//                    if (thisRideType == RideType.PICK) {
//                        stageReturnedAtActivity()
//                    } else {
//                        stageStarted()
//                    }
                }
                // arrived: first time notification, picked: send notification again
                RideStatus.RIDER_ARRIVED.name, RideStatus.PICKED_UP.name -> {
                    sendNextStage(WebSocketEvents.RIDER_ARRIVED)
                    showToast(this, getString(R.string.parent_notified_ride_has_arrived))
                }
                RideStatus.ARRIVED_AT_ACTIVITY.name -> {
                    sendNextStage(WebSocketEvents.ARRIVED_AT_ACTIVITY)
                    stageDroppedAtActivity()
                }
                RideStatus.RETURNED_ACTIVITY.name -> {
                    sendNextStage(WebSocketEvents.RETURNED_ACTIVITY)
                    stageReturnedAtActivity()
                }
                RideStatus.PICKED_UP_FROM_ACTIVITY.name -> {
                    sendNextStage(WebSocketEvents.PICKED_UP_FROM_ACTIVITY)
                    stagePickedFromActivity()
                }
                RideStatus.RETURNED_HOME.name, RideStatus.COMPLETED.name -> {
                    sendNextStage(WebSocketEvents.RETURNED_HOME)
                    stageReachedHome()
                }
                else -> { showToast(this, "Unknown stage: $nextStatus") }
            }
        }
        else showToast(this, "Getting location!")
    }

    private fun stageStarted() {
        startedImg.setImageResource(R.drawable.location_reached)
        startText.text = getString(R.string.started_txt)
        notifyButton.text = getString(R.string.arrived_at_location)

        val time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.STARTED.name) ?: Instant.now().toString()
        startRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageArrivedAtHome() {
        notifyButton.text = getString(R.string.arrived_at_location)
    }
    private fun stagePickedFromHome() {
        pickedImg.setImageResource(R.drawable.location_reached)
        pickUpText.text = getString(R.string.picked_up)
        notifyButton.text = getString(R.string.dropped_text)

        val time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.PICKED_UP.name) ?: Instant.now().toString()
//        if (at.isNotBlank()) time = at
        pickupRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageDroppedAtActivity(at: String = "") {
        droppedImg.setImageResource(R.drawable.location_reached)
        dropOffText.text = getString(R.string.dropped_text)
        notifyButton.text = getString(R.string.heading_to_pick_from_activity)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.ARRIVED_AT_ACTIVITY.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        dropOffTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageReachedActivity() {
        rePickUpText.text = getString(R.string.reached)
    }
    private fun stageReturnedAtActivity(at: String = "") {
        reStartedImg.setImageResource(R.drawable.location_reached)
        reStartText.text = getString(R.string.started_txt)
        notifyButton.text = getString(R.string.picked_up_from_activity_txt)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.RETURNED_ACTIVITY.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        reStartRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stagePickedFromActivity(at: String = "") {
        rePickedImg.setImageResource(R.drawable.location_reached)
        rePickUpText.text = getString(R.string.picked_up)
        notifyButton.text = getString(R.string.reached_your_location)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.PICKED_UP_FROM_ACTIVITY.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        rePickupRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageReachedHome() {
        notifyButton.text = getString(R.string.notify_reached_home)
    }
    private fun stageDroppedAtHome(at: String = "") {
        dropOffHomeImg.setImageResource(R.drawable.location_reached)
        dropOffHomeTxt.text = getString(R.string.dropped_text)
        notifyButton.text = getString(R.string.dropped_at_home)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.RETURNED_HOME.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        dropOffHomeTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageCompleted() {
        if (isCompletionDialogShown) return
        isCompletionDialogShown = true
        
        showToast(this, getString(R.string.ride_completed_txt))
        disconnectWebSocket()

        val startAt = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.STARTED.name) ?: Instant.now().toString()
        val endedAt = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.RETURNED_HOME.name) ?: Instant.now().toString()
        val text2 = "${formatDateTime(startAt, "hh:mm")} -- ${formatDateTime(endedAt, "hh:mm")}"
        showBottomSheetDialog(this,
            getString(R.string.ride_completed_txt), text2,
            getString(R.string.ok_Txt), getString(R.string.give_feedback),
            { this.onBackPressed() },
            {
                val data = FeedBackPreReq(
                    rideId = rideData.id,
                    fromUserId = rideData.userId,
                    fromUserName = rideData.userFirstName + " " + rideData.userLastName,
                    forUserId = rideData.rideTakers[0].userId,
                    forUserName = rideData.rideTakers[0].userFirstName + " " + rideData.rideTakers[0].userLastName,
                    riderType =  RiderType.GIVER
                )
                SharedPreferencesManager.saveObject(feedBackRequired, data)
                val intent = Intent(this, Activity_RideFeedback::class.java)
                intent.putExtra("ride_date", rideData.date)
                startActivity(intent)
            },
            onDismissListener = {
                this.onBackPressed()
            }
        )
    }
}
