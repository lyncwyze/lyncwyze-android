package com.intelly.lyncwyze.Assest.notificationWork

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_DAY
import com.intelly.lyncwyze.Assest.utilities.CHILD_ACTIVITY_ID
import com.intelly.lyncwyze.Assest.utilities.showToast
import com.intelly.lyncwyze.R
import com.intelly.lyncwyze.enteredUser.Dashboard
import com.intelly.lyncwyze.enteredUser.scheduleRide.Activity_ConfirmActivity
import com.intelly.lyncwyze.enteredUser.records.Activity_OnGoingRide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

class NotificationReceiver: BroadcastReceiver() {
    private val logger = KotlinLogging.logger { }

    companion object {
        const val ACTION_OPEN_ACTIVITY = "ACTION_OPEN_ACTIVITY"
        const val ACTION_RUN_FUNCTION = "ACTION_RUN_FUNCTION"
        const val ACTION_DISMISS_NOTIFICATION = "ACTION_DISMISS_NOTIFICATION"
        const val NOTIFICATION_ID_KEY = "NOTIFICATION_ID_KEY"
    }
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OPEN_ACTIVITY -> handleOpenActivity(context, intent)
            ACTION_RUN_FUNCTION -> callApiIgnoreActivity(context, intent)
            ACTION_DISMISS_NOTIFICATION -> dismissNotification(context, intent)
            else -> { logger.info{"Unknown action: ${intent.action}"} }
        }
    }

    private fun handleOpenActivity(context: Context, intent: Intent) {
        val intent2 = Intent(context, Activity_ConfirmActivity::class.java).apply {
            putExtra(CHILD_ACTIVITY_ID, intent.getStringExtra(CHILD_ACTIVITY_ID))
            putExtra(CHILD_ACTIVITY_DAY, intent.getStringExtra(CHILD_ACTIVITY_DAY))
        }
        intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK// or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent2)
        dismissNotification(context, intent)
    }
    private fun callApiIgnoreActivity(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activityId = intent.getStringExtra(CHILD_ACTIVITY_ID)
                val dayOfWeek = intent.getStringExtra(CHILD_ACTIVITY_DAY)
                if (activityId != null && dayOfWeek != null) {
                    NetworkManager.apiService.ignoreSchedule(activityId = activityId, dayOfWeek = dayOfWeek)
                    launch(Dispatchers.Main) { dismissNotification(context, intent) }
                } else {
                    logger.error { "Activity ID or Day of Week is null" }
                    launch(Dispatchers.Main) {
                        showToast(context, "Activity ID or Day of Week is missing.")
                    }
                }
            } catch (e: Exception) {
                logger.error { e.message }
                launch(Dispatchers.Main) {
                    showToast(context, e.message.toString())
                }
            }
        }
    }
    private fun dismissNotification(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, -1)
        if (notificationId != -1)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notificationId)
    }

    fun withScheduleOptions(context: Context, title: String, message: String, dayOfWeek: String?, activityId: String?) {
        val scheduleOptionsNotID = System.currentTimeMillis().toInt()
        val channelId = "your_channel_id"

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(NotificationChannel(channelId, "Your Channel Name", NotificationManager.IMPORTANCE_DEFAULT))

        // Intent for the "Open Activity" button
        val openActivityIntent = Intent(context, Activity_ConfirmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CHILD_ACTIVITY_ID, activityId)
            putExtra(CHILD_ACTIVITY_DAY, dayOfWeek)
        }
        val scheduleNow = PendingIntent.getActivity(
            context,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val runFunctionIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_RUN_FUNCTION
            putExtra(NOTIFICATION_ID_KEY, scheduleOptionsNotID)
            putExtra(CHILD_ACTIVITY_ID, activityId)
            putExtra(CHILD_ACTIVITY_DAY, dayOfWeek)
        }
        val notThisOne = PendingIntent.getBroadcast(context, 1, runFunctionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val removeNotificationIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra(NOTIFICATION_ID_KEY, scheduleOptionsNotID)
        }
        val maybeLater = PendingIntent.getBroadcast(context, 2, removeNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(com.hbb20.R.drawable.flag_norfolk_island)
        builder.setContentTitle(title)
        builder.setContentText(message)
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)

        logger.info { "activityId: [$activityId], dayOfWeek: [$dayOfWeek]" } // Added brackets to make sure to show empty strings

        if (!activityId.isNullOrBlank() && !dayOfWeek.isNullOrBlank()) {
            builder.addAction(R.drawable.plus, context.getString(R.string.schedule_now_text), scheduleNow)      // Open activity Confirm activity schedule
            builder.addAction(R.drawable.car_icon, context.getString(R.string.maybe_later_text), maybeLater)    // Call API "match/ignoreSchedule" and dismiss notification
        }

        builder.addAction(R.drawable.car_icon, context.getString(R.string.not_this_one_text), notThisOne)
        builder.setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            notify(scheduleOptionsNotID, builder.build())
        }
    }

    // Open Activity - OnGoingRides and clears the notification
    fun showNotificationOpenOnGoing(context: Context, title: String, message: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val channelId = "simple_notification_channel_a"

        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Simple Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val openActivityIntent = Intent(context, Activity_OnGoingRide::class.java)
        val openActivityPendingIntent = PendingIntent.getActivity(context, notificationId, openActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Add app icon
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(openActivityPendingIntent)
            if (message.isNotBlank())
                builder.setContentText(message)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            notify(notificationId, builder.build())
        }
    }

    // Open the app on the dashboard and clear the notification
    fun showSimpleNotification(context: Context, title: String, message: String) {
        val notificationId = System.currentTimeMillis().toInt()
        val channelId = "simple_notification_channel"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Simple Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        // Intent to open the Dashboard activity
        val dashboardIntent = Intent(context, Dashboard::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val dashboardPendingIntent = PendingIntent.getActivity(context, notificationId, dashboardIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(dashboardPendingIntent)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                return
            notify(notificationId, builder.build())
        }
    }
}