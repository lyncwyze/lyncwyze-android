package com.intelly.lyncwyze.Assest.modals

data class DataCountResp(
    var activity: Int,
    var child: Int,
    var emergencyContact: Int,
    var vehicle: Int,
    val ongoingRides: Int,
    val givenRides: Int,
    val takenRides: Int,
    val upcomingRides: Int,
    var profile: Int
)

