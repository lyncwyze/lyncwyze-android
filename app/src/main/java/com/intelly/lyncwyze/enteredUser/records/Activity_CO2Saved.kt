package com.intelly.lyncwyze.enteredUser.records

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.intelly.lyncwyze.Assest.utilities.notification
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.children.Activity_ChildrenList
import com.intelly.lyncwyze.enteredUser.emergencyContacts.Activity_EmergencyContact_List
import com.intelly.lyncwyze.enteredUser.vehicles.Activity_VehicleList
import mu.KotlinLogging


class Activity_CO2Saved : AppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var backButton: ImageView
    private lateinit var showNotification: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_co2_saved)
        setupUI()
        setUpFunctionality()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        backButton = findViewById(R.id.co2SavedBackBtn)
        showNotification = findViewById(R.id.showNotification)
    }
    private fun setUpFunctionality() {
        backButton.setOnClickListener { this.onBackPressed() }
        showNotification.setOnClickListener {
            createCustomNotification("James 4pm", "Gym Class- Tomorrow", mapOf("abs" to "dat"))
        }
    }

    private fun createCustomNotification(title: String, messageBody: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notification, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        // Intent for the Button 1
        val action1Intent = Intent(this, Activity_VehicleList::class.java)
            .apply {
                action = "btn1"
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("data", data.toString())   // Pass any necessary data to the activity
            }
        val action1PendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, action1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        // Intent for the Button 2
        val action2Intent = Intent(this, Activity_ChildrenList::class.java)
            .apply {
                action = "btn2"
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("data", data.toString())   // Pass any necessary data to the activity
            }
        val action2PendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, action2Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        // Intent for the Button 3
        val action3Intent = Intent(this, Activity_EmergencyContact_List::class.java)
            .apply {
                action = "btn3"
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("data", data.toString())   // Pass any necessary data to the activity
            }
        val action3PendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, action3Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)


        // Build the notification
        val builder = NotificationCompat.Builder(this, notification)
            .setSmallIcon(R.drawable.plus) // Replace with your notification icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(R.drawable.car_icon, "Schedule Now", action1PendingIntent) //Add action buttons
            .addAction(R.drawable.user, "Maybe Later", action2PendingIntent) //Add action buttons
            .addAction(R.drawable.child_img, "Not this one", action3PendingIntent)
            .setAutoCancel(true)

        // Show the notification
        notificationManager.notify(1, builder.build())
    }
}