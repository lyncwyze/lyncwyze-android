package com.intelly.lyncwyze.enteredUser.rideTracking

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intelly.lyncwyze.Assest.modals.EachRide
import com.intelly.lyncwyze.Assest.modals.FeedBackPreReq
import com.intelly.lyncwyze.Assest.modals.RideStatus
import com.intelly.lyncwyze.Assest.modals.RideTrack
import com.intelly.lyncwyze.Assest.modals.RideType
import com.intelly.lyncwyze.Assest.modals.RiderType
import com.intelly.lyncwyze.Assest.modals.WebSocketEvents
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
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import android.app.Dialog

class Activity_RideTracking_Taker : AppCompatActivity() {
    private val logger = KotlinLogging.logger { }

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

    private var bottomSheetDialog: Dialog? = null
    private var isCompletionDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ride_tracking_taker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        activityCheck()
        setupUI()
        setUpFunctionality()
    }
    override fun onResume() {
        super.onResume()
        connectWebSocket()
        getRideTrackingAndUpdateUI()
    }
    override fun onPause() {
        super.onPause()
        disconnectWebSocket()
    }

    private var thisUserIsTracker = false
    private var thisRideType: RideType? = RideType.DROP_PICK
    private fun activityCheck() {
        SharedPreferencesManager.getObject<EachRide>(RideData)?.let {
            rideData = it
            thisUserIsTracker = it.rideTakers.map { me -> me.userId }.contains(SharedPreferencesManager.getDecodedAccessToken()!!.userId)
            thisRideType = rideData.rideTakers[0].role
        }
        ?: { this.onBackPressed(); showToast(this, "Ride data not found!") }
    }
    private fun setupUI() {
        backButton = findViewById(R.id.rideTrackRiderBackButton)

        rideTypeInclude = findViewById(R.id.rideTypeInclude)
        rideTypeInclude.removeAllViews()

        // Ride stages
        val rideDrop = layoutInflater.inflate(R.layout.layout_ride, rideTypeInclude, false)

        // layout drop
        onlyDrop = rideDrop.findViewById(R.id.onlyDrop)

        // Start
        startedImg = rideDrop.findViewById(R.id.toActivityStartImg)
        startText = rideDrop.findViewById(R.id.toActivityStartText)
        startRideTime = rideDrop.findViewById(R.id.toActivityStartTime)

        // Picked
        pickedImg = rideDrop.findViewById(R.id.toActivityPickImg)
        pickUpText = rideDrop.findViewById(R.id.toActivityPickText)
        pickUpLocationName = rideDrop.findViewById(R.id.toActivityPickLocation)
        pickUpLocationName.text = rideData.pickupAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        pickupRideTime = rideDrop.findViewById(R.id.toActivityPickTime)
//        pickupRideTime.text = formatDateTime(rideData.pickupTime, "hh:mm a")

        // Dropped
        droppedImg = rideDrop.findViewById(R.id.toActivityDropImg)
        dropOffText = rideDrop.findViewById(R.id.toActivityDropText)
        dropOffLocation = rideDrop.findViewById(R.id.toActivityDropLocation)
        dropOffLocation.text = rideData.dropoffAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        dropOffTime = rideDrop.findViewById(R.id.toActivityDropTime)
//        dropOffTime.text = formatDateTime(rideData.dropoffTime, "hh:mm a")


        //
        onlyPick = rideDrop.findViewById(R.id.onlyPick)
        reStartedImg = rideDrop.findViewById(R.id.fromActivityStartImg)
        reStartText = rideDrop.findViewById(R.id.fromActivityStartText)
        reStartRideTime = rideDrop.findViewById(R.id.fromActivityStartTime)

        // Picked from activity
        rePickedImg = rideDrop.findViewById(R.id.fromActivityPickImg)
        rePickUpText = rideDrop.findViewById(R.id.fromActivityPickText)
        rePickUpLocationName = rideDrop.findViewById(R.id.fromActivityPickLocation)
        rePickUpLocationName.text = rideData.dropoffAddress.addressLine1 ?: getString(R.string.__time_placeholder)
        rePickupRideTime = rideDrop.findViewById(R.id.fromActivityPickTime)

        // Drop home
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
                        Glide.with(this@Activity_RideTracking_Taker).load(bitmap).into(personImg)
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        personImg.setImageResource(R.drawable.user)
                    }
                } ?: run { personImg.setImageResource(R.drawable.user) }
            }
        }

        userName = findViewById(R.id.rideTakerName)
        "${rideData.userFirstName} ${rideData.userLastName}".also { userName.text = it }
        findViewById<TextView>(R.id.activityVehicleLicNo).text = rideData.vehicle?.licensePlate

        messageText = findViewById(R.id.messageText)
        callPerson = findViewById(R.id.iconCallPerson)
        messagePerson = findViewById(R.id.iconMesPerson)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        callPerson.setOnClickListener {
            rideData.mobileNumber?.let {
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$it") })
            } ?: showToast(this, "Phone number is missing!")
        }
        messagePerson.setOnClickListener {
            val theMessage = messageText.text.trim().toString()
            rideData.mobileNumber?.let {
                val messageIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$it") // Use "smsto:" for SMS
                    putExtra("sms_body", theMessage) // Pre-fill the message body
                }
                startActivity(messageIntent)
            } ?: showToast(this, "Phone number is missing!")

        }
    }

    private fun showRiderArrivedNotification(title: String, message: String, actionTitle: String, okButtonAction: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rider_arrived, null)

        val titleTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
        titleTextView.text = title
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)
        messageTextView.text = message
        val okButton = dialogView.findViewById<Button>(R.id.dialogButton)
        okButton.text = actionTitle

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()
        okButton.setOnClickListener {
            dialog.dismiss()
            okButtonAction.invoke()
        }
        dialog.setOnCancelListener {  }

        dialog.show()
    }

    // Web socket
    private var currentStatus = RideStatus.SCHEDULED.name
    private var nextStatus = WebSocketEvents.RIDE_START.name
    private var webSocket: WebSocket? = null
    private fun connectWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        val request = Request.Builder().apply {
            SharedPreferencesManager.getValidatedAccessToken()?.let { addHeader("Authorization", "Bearer $it") }
        }.url("$wsUrl?role=${RiderType.TAKER.name}&rideId=${rideData.id}&takerId=${SharedPreferencesManager.getDecodedAccessToken()!!.userId}").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                startPingPong()
            }
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
                    when (val socketEvent = wsData.getAsJsonPrimitive("socketEventType")?.asString) {
                        WebSocketEvents.LOCATION_UPDATE.name -> { }
                        WebSocketEvents.STATUS.name -> {
                            currentStatus = wsData.getAsJsonPrimitive("rideStatus")?.asString!!
                            nextStatus = wsData.getAsJsonPrimitive("nextStatus")?.asString!!

//                            showToast(this@Activity_RideTracking_Taker, "$currentStatus >>> $nextStatus")
//                            logger.info { "$currentStatus >>> $nextStatus" }
//                            logger.info { rideTrack }

                            updateRideStatusUI(currentStatus)
                            when (nextStatus) {
                                RideStatus.SCHEDULED.name -> showToast(this@Activity_RideTracking_Taker, "Start ride, and notify ride taker.")
                                else -> {}
                            }
                        }
                        WebSocketEvents.ERROR.name -> {
                            when (val errorCode = wsData.getAsJsonPrimitive("errorCode")?.asInt) {
                                7007 -> nextStatus = WebSocketEvents.RIDE_START.name
                                7010 -> showToast(this@Activity_RideTracking_Taker, getString(R.string.ride_not_started_yet))
                                else -> logger.error { "Some other error code: $errorCode" }
                            }
                        }
                        else -> showToast(this@Activity_RideTracking_Taker, "Unknown status: $socketEvent")
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                logger.info { "WebSocket closed at: ${System.currentTimeMillis()}, code: $code, reason: $reason" }
                stopPingPong()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
//                logger.error { "WebSocket failure at: ${System.currentTimeMillis()}, error: ${t.message}" }
//                logger.error("WebSocket error: ${t.message}")
                stopPingPong()
                CoroutineScope(Dispatchers.IO).launch {
                    delay(4000)
                    launch(Dispatchers.Main) { connectWebSocket() }
                }
                t.printStackTrace()
            }
        })
    }
    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    private var pingPongJob: Job? = null
    private fun startPingPong() {
        pingPongJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    withContext(Dispatchers.Main) { sendDataInWS("PING") }
                    delay(35000)
                }
            } catch (e: CancellationException) {
                logger.info("Ping-pong coroutine cancelled.")
            } catch (e: Exception) {
                logger.error("Error sending ping: ${e.message}")
            }
        }
    }
    private fun stopPingPong() {
        pingPongJob?.cancel()
        pingPongJob = null
    }
    private fun callWsGetRideStatusAfterDelay() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            wsGetRideStatus()
        }
    }
    private fun wsGetRideStatus() {
        val data = WsEventGeneral(
            socketEventType = WebSocketEvents.STATUS,
            rideId = rideData.id,
            takerId = null)
        sendDataInWS(SharedPreferencesManager.gson.toJson(data))
    }
    private fun sendDataInWS(data: String) {
        logger.info { "Data to WebSocket: $data" }
        if (webSocket != null) {
//            logger.info { "WebSocket state: webSocket is alive" }
            try {
                webSocket?.send(data)
//                logger.info { "Data sent successfully" }
            } catch (e: Exception) {
                logger.error { "Error sending data: ${e.message}" }
            }
        } else {
//            logger.warn { "WebSocket is null, cannot send data" }
            connectWebSocket()
        }
    }

    private var rideTrack: RideTrack? = null
    private fun getRideTrackingAndUpdateUI() {
        lifecycleScope.launch {
            try {
                val response = NetworkManager.apiService.getRideDetailsByID(rideId = rideData.id)
                if (response.isSuccessful) {
                    response.body()?.let {
                        rideTrack = it
                        updateRideStatusUI(it.status.name)
                        logger.info { it.rideTakers[0].statusHistory }
                    } ?: showToast(this@Activity_RideTracking_Taker, "Fetching error!")
                }
                else if (response.code() == 400)
                    showToast(this@Activity_RideTracking_Taker, getString(R.string.ride_not_started_yet))
                else
                    showToast(this@Activity_RideTracking_Taker, "${response.code()}, ${HttpUtilities.parseError(response)?.errorInformation?.errorDescription}")
            }
            catch (e: Exception) {
                logger.error { e.message }
                showToast(this@Activity_RideTracking_Taker, e.message.toString())
            }
        }
    }
    private fun updateRideStatusUI(rideStatus: String) {
        logger.info { "Updating ride status time on UI" }
        when (rideStatus) {
            RideStatus.STARTED.name -> {
                if (thisRideType == RideType.PICK)
                    stageReturnedAtActivity()
                else
                    stageStarted()
            }
            RideStatus.RIDER_ARRIVED.name -> {
                stageStarted()
                stageArrivedAtHome()
                showRiderArrivedNotification(
                    title = getString(R.string.rider_arrived),
                    message = getString(R.string.the_rider_has_arrived_at_the_pickup_location),
                    actionTitle = getString(R.string.confirm_pick_up),
                    okButtonAction = {
                        val data = WsEventGeneral(
                            rideId = rideData.id,
                            socketEventType = WebSocketEvents.PICKED_UP,
                            takerId = SharedPreferencesManager.getDecodedAccessToken()!!.userId
                        )
                        sendDataInWS(SharedPreferencesManager.gson.toJson(data))
                        stagePickedFromHome()
                        getRideTrackingAndUpdateUI()
                    }
                )
            }
            RideStatus.PICKED_UP.name -> {
                stageStarted()
                stageArrivedAtHome()
                stagePickedFromHome()
            }
            RideStatus.ACTIVITY_ONGOING.name -> {
                if (thisRideType == RideType.PICK) {
                    stageReturnedAtActivity()
                } else {
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
                stagePickedFromHome()
                stageArrivedAtHome()
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
                showRiderArrivedNotification(
                    title = getString(R.string.rider_arrived_home),
                    message = getString(R.string.the_rider_has_arrived_at_the_drop_location),
                    actionTitle = getString(R.string.confirm_drop),
                    okButtonAction = {
                        val data = WsEventGeneral(
                            rideId = rideData.id,
                            socketEventType = WebSocketEvents.RIDE_END,
                            takerId = SharedPreferencesManager.getDecodedAccessToken()!!.userId
                        )
                        sendDataInWS(SharedPreferencesManager.gson.toJson(data))
                        stageDroppedAtHome()
                        stageCompleted()
                        getRideTrackingAndUpdateUI()
                    }
                )
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

    private fun stageStarted(at: String = "") {
        startedImg.setImageResource(R.drawable.location_reached)
        startText.text = getString(R.string.started_txt)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.STARTED.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        startRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageArrivedAtHome() { }
    private fun stagePickedFromHome(at: String = "") {
        pickedImg.setImageResource(R.drawable.location_reached)
        pickUpText.text = getString(R.string.picked_up)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.PICKED_UP.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        pickupRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageDroppedAtActivity(at: String = "") {
        droppedImg.setImageResource(R.drawable.location_reached)
        dropOffText.text = getString(R.string.dropped_text)

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

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.RETURNED_ACTIVITY.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        reStartRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stagePickedFromActivity(at: String = "") {
        rePickedImg.setImageResource(R.drawable.location_reached)
        rePickUpText.text = getString(R.string.picked_up)
        rePickUpLocationName.text = rideData.dropoffAddress.addressLine1 ?: getString(R.string.__time_placeholder)

        var time = rideTrack?.rideTakers?.get(0)?.statusHistory?.get(RideStatus.PICKED_UP_FROM_ACTIVITY.name) ?: Instant.now().toString()
        if (at.isNotBlank()) time = at
        rePickupRideTime.text = formatDateTime(time, "hh:mm a")
    }
    private fun stageDroppedAtHome(at: String = "") {
        dropOffHomeImg.setImageResource(R.drawable.location_reached)
        dropOffHomeTxt.text = getString(R.string.dropped_text)

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
        bottomSheetDialog = showBottomSheetDialog(this,
            getString(R.string.ride_completed_txt), text2,
            getString(R.string.ok_Txt), getString(R.string.give_feedback),
            { this.onBackPressed() },
            {
                val data = FeedBackPreReq(
                    rideId = rideData.id,
                    forUserId = rideData.userId,
                    forUserName = rideData.userFirstName + " " + rideData.userLastName,
                    fromUserId = rideData.rideTakers[0].userId,
                    fromUserName = rideData.rideTakers[0].userFirstName + " " + rideData.rideTakers[0].userLastName,
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

    override fun onDestroy() {
        super.onDestroy()
        bottomSheetDialog?.dismiss()
        bottomSheetDialog = null
    }
}
