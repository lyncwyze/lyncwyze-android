package com.intelly.lyncwyze.Assest.modals

enum class WeekDays(val displayName: String) {
    MONDAY("MONDAY"),
    TUESDAY("TUESDAY"),
    WEDNESDAY("WEDNESDAY"),
    THURSDAY("THURSDAY"),
    FRIDAY("FRIDAY"),
    SATURDAY("SATURDAY"),
    SUNDAY("SUNDAY");

    companion object {
        fun fromString(day: String): WeekDays? {
            return values().find { it.displayName.equals(day, ignoreCase = true) }
        }
    }
}

enum class RideType(val displayName: String) {
    DROP("DROP"),
    PICK("PICK"),
    DROP_PICK("DROP_PICK");

    companion object {
        fun fromString(value: String): RideType? {
            return entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }
}
enum class RiderType(val displayName: String) {
    GIVER("GIVER"),
    TAKER("TAKER");

    companion object {
        fun fromString(value: String): RiderType? {
            return entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }
}

// Stages
enum class RideStatus(val displayName: String) {
    SCHEDULED("SCHEDULED"),         // or no data from match/get/{rideId} -- next: RIDE_START (STARTED)
    STARTED("STARTED"),             // next: Giver-RIDER_ARRIVED - notification to taker (PICKED_UP)

    RIDER_ARRIVED("RIDER_ARRIVED"), // next: PICKED_UP (confirmed by taker)
    PICKED_UP("PICKED_UP"),         // taker via notification banner (next: ARRIVED_AT_ACTIVITY by giver)
    ARRIVED_AT_ACTIVITY("ARRIVED_AT_ACTIVITY"),         // Giver - next: RETURNED_ACTIVITY

    ACTIVITY_ONGOING("ACTIVITY_ONGOING"),               // handled by server

    RETURNED_ACTIVITY("RETURNED_ACTIVITY"),             // Giver
    PICKED_UP_FROM_ACTIVITY("PICKED_UP_FROM_ACTIVITY"), // Giver
    RETURNED_HOME("RETURNED_HOME"), // Giver

    COMPLETED("COMPLETED"),         // Taker and in some case handled by server - next: go to feedback

    CANCELED("CANCELED"),           // Not in scope
    ONGOING("ONGOING");             // All between start and end

    companion object {
        fun fromString(value: String): RideStatus? {
            return entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }
}
// Updating via WS
enum class WebSocketEvents(val displayName: String) {
    STATUS("STATUS"),                   // Send to get current and next status in ride
    NEXT_STATUS("NEXT_STATUS"),         // Only to receive from server

    RIDE_START("RIDE_START"),           // Giver then LOCATION_UPDATE)
    LOCATION_UPDATE("LOCATION_UPDATE"), // Giver interval - next: RIDER_ARRIVED
    RIDER_ARRIVED("RIDER_ARRIVED"),     // Giver (then PICKED_UP)  - next: taker confirm with PICKED_UP
    PICKED_UP("PICKED_UP"),             // Taker confirm - next: ARRIVED_AT_ACTIVITY (WS)
    ARRIVED_AT_ACTIVITY("ARRIVED_AT_ACTIVITY"), // Giver at drop location

    ACTIVITY_ONGOING("ACTIVITY_ONGOING"),       // Giver waiting
    RETURNED_ACTIVITY("RETURNED_ACTIVITY"),     // Giver re-pick up location reached
    PICKED_UP_FROM_ACTIVITY("PICKED_UP_FROM_ACTIVITY"), // Picked up
    RETURNED_HOME("RETURNED_HOME"),     // Giver (then taker do RIDE_END)
    RIDE_END("RIDE_END"),               // Taker confirms
    ERROR("ERROR"),
    ;


    companion object {
        fun fromString(value: String): WebSocketEvents? {
            return WebSocketEvents.entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }
}

enum class Gender(val displayName: String) {
    SELECT_GENDER("SELECT_GENDER"),
    MALE("MALE"),
    FEMALE("FEMALE"),
    OTHER("OTHER");

    companion object {
        fun fromString(value: String): Gender? {
            return Gender.entries.find { it.displayName.equals(value, ignoreCase = true) }
        }
    }


}