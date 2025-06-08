package com.intelly.lyncwyze.Assest.modals

val feedBackRequired = "feedBackRequired"


data class FeedBackPreReq(
    val rideId: String,
    val fromUserId: String,
    val fromUserName: String,
    val forUserId: String,
    val forUserName: String,
    val riderType: RiderType
)

data class SurveyReport(
    val id: String?,
    val rideId: String,
    val reviewerId: String,     // Who is rating
    val revieweeId: String,     // Whom is being rated
    val reviewerRole: RiderType,
    var favorite: Boolean,
    var ratings: MutableMap<String, Int>,
    val comments: String,
)