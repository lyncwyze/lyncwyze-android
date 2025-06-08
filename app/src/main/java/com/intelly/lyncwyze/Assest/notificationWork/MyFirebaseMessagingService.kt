package com.intelly.lyncwyze.Assest.notificationWork

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import mu.KotlinLogging

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val logger = KotlinLogging.logger {}
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.info { "Notification data: ${remoteMessage.data}" }

        val title = remoteMessage.data["title"].toString()
        val message = remoteMessage.data["body"] ?: ""
        when (remoteMessage.data["actionType"]) {
            "ride_scheduling" -> NotificationReceiver().withScheduleOptions(context = baseContext, title, message, remoteMessage.data["dayOfWeek"], remoteMessage.data["activityId"])
            "rider_arrived" -> NotificationReceiver().showNotificationOpenOnGoing(context = baseContext, title, message)
            "returned_home" -> NotificationReceiver().showNotificationOpenOnGoing(context = baseContext, title, message)
            else -> NotificationReceiver().showSimpleNotification(context = baseContext, title, message)
        }
    }
}