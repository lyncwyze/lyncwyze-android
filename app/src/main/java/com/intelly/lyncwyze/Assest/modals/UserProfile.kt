package com.intelly.lyncwyze.Assest.modals

data class Providers (
    val id: String,
    val type: String,
    val subType: String,
    val name: String,
    val address: Address2
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val refresh_token: String,
    val expires_in: Int,

    val clientId: String,
    val name: String,
    val fullName: String,
    val emailId: String,

    var profileStatus: String?,  //  PROFILE/BACKGROUND/PHOTO/POLICY
    var acceptTermsAndPrivacyPolicy: Boolean,
    var profileComplete: Boolean,
    val changePassword: Boolean,

    val userId: String,
    val jti: String
)
enum class ProfileStatus(val value: String) {
    PROFILE("PROFILE"),
    BACKGROUND("BACKGROUND"),
    PHOTO("PHOTO"),
    POLICY("POLICY")
}

data class UserProfile(
    val id: String,
    var firstName: String,
    val middleName: String?,
    var lastName: String,
    var email: String,
    var mobileNumber: String,

    var image: String?,

    val dateOfBirth: String,
    val gender: String,
    val active: Boolean,
    val locked: Boolean,
    val forcePasswordChange: Boolean,
    val imei: String?,
    val pwdExpiryDate: String,
    val lastSuccessfulLogin: String?,
    val lockedTill: Long,
    val failedLoginAttempt: Int,
    val expiryNotifyCount: Int,
    val forgetPwdToken: String?,
    val activatePwdToken: String?,
    val status: String,
    val oldPassword: String?,
    val password: String?,
    val confirmPassword: String?,
    val addresses: List<Address>,
    val ssnLast4: String?,
    val community: String?,
    val consentForBackgroundCheck: Boolean,
    val backgroundCheck: BackgroundCheck,
    val membership: Membership,
    val referralCode: String?,
    val child: List<Child>,
    val vehicles: List<Vehicle>,
    val rideRole: String,
    val pointsBalance: Int,
    val comment: String?,
    val createdBy: String,
    val createdDate: String,
    val modifiedBy: String?,
    val modifiedDate: String
)
data class DecodedUserDetails(
    var user_name: String? = null,
    var fullName: String? = null,
    var profileComplete: Boolean? = null,
    var clientId: String? = null,
    var emailId: String? = null,
    var userId: String,
    var authorities: List<String>? = null,
    var client_id: String? = null,
    var changePassword: Boolean? = null,
    var scope: List<String>? = null,
    var name: String? = null,
    var exp: Int? = null,
    var acceptTermsAndPrivacyPolicy: Boolean? = null,
    var jti: String? = null,
    val profileStatus: String?
)

data class CreateProfileRequestBodyFromEmail(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val confirmPassword: String,
//    val ssnLast4: String
)

data class CreateProfileRequestBodyFromMobile(
    val firstName: String,
    val lastName: String,
    var mobileNumber: String,
    val password: String,
    val confirmPassword: String,
//    val ssnLast4: String
)


data class Address(
    val user: String,
    val userId: String,
    var addressLine1: String?,
    var addressLine2: String?,
    var landMark: String?,
    var pincode: Int,
    var state: String,
    var city: String,
    var location: Location
) {
    companion object {
        fun default(): Address {
            return Address(
                user = "",
                userId = "",
                addressLine1 = "",
                addressLine2 = null, // Nullable
                landMark = null, // Nullable
                pincode = 0, // Assuming 0 is a valid default for pincode
                state = "",
                city = "",
                location = Location.default() // Call the default method of Location
            )
        }
    }
}
data class Location(
    var description: String,
    var placeId: String,
    var sessionToken: String,
    var x: Double,
    var y: Double,
    var coordinates: List<Double>,
    var type: String
) {
    companion object {
        fun default(): Location {
            return Location(
                description = "",
                placeId = "",
                sessionToken = "",
                x = 0.0,
                y = 0.0,
                coordinates = listOf(0.0, 0.0),
                type = "Point"
            )
        }
    }
}
data class BackgroundCheck(
    val ssn: String,
    val licenseNumber: String?,
    val licenseState: String?,
    val licenseExpiredDate: String,
    var status :String,
    var initiatedDate :String,
    var completedDate :String
)
data class Membership(
    var status :String,
    var startDate :String,
    var expiryDate :String,
    var renewalCost :Int,
    var trial :Boolean
)
data class Child(
    var id :String?,
    var firstName :String,
    var lastName :String,
    var image :String?,
    var dateOfBirth :String,
    var gender :String,
    var preferences : Preferences,
    var emergencyContacts :List<EmergencyContact>,
    var activities :List<Activity>
)
data class Preferences(
    var rideInFront :Boolean,
    var boosterSeatRequired :Boolean
)

data class Activity(
    var id :String,
    var type :String,
    var subType :String?,
    var address : Address, // Reusing the Address data class
    var schedulePerDay :Map<String, ScheduleTime>, // Using Map for dynamic properties
    var preferences : ActivityPreferences
)
data class ScheduleTime(
    var startTime : TimeDetails, // Using TimeDetails for start time
    var endTime : TimeDetails // Using TimeDetails for end time
)
data class TimeDetails(
    var hour :Int,
    var minute :Int,
    var second :Int,
    var nano :Int
)
data class ActivityPreferences(
    var preferredPickupTime :String?,
    var pickupRole :String?,
    var dropoffRole :String?
)
data class Vehicle(
    val id: String?,
    var make: String,
    var model: String,
    var bodyStyle: String,
    var bodyColor: String,
    var licensePlate: String,
    var alias: String,
    var year: Int,
    var seatingCapacity: Int,
    var primary: Boolean
) {
    companion object {
        fun default(): Vehicle {
            return Vehicle(
                id = null,
                make = "",
                model = "",
                bodyStyle = "",
                bodyColor = "",
                licensePlate = "",
                alias = "",
                year = 0,
                seatingCapacity = 0,
                primary = false
            )
        }
    }
}




// BackgroundVerificationRequestBody
data class BackgroundVerificationRequestBody(
    val ssn: String,
    val licenseNumber: String,
    val licenseState: String,
    val licenseExpiredDate: String,
    val address: Address2
)
data class Address2(
    val userId: String?,
    val addressLine1: String,
    val addressLine2: String?,
    val landMark: String?,
    val pincode: Int,
    val state: String,
    val city: String,
    var location: Location2
)
data class Location2(
    var coordinates: List<Double>,
    var type: String
)


// Child
data class SaveChildRequest(
    val id: String?,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val mobileNumber: String,
    val rideInFront: Boolean,
    val boosterSeatRequired: Boolean,
)
data class SaveChild(
    var id: String?,
    var firstName: String,
    var lastName: String,
    var dateOfBirth: String,
    var gender: Gender,
    var mobileNumber: String,
    var rideInFront: Boolean,
    var boosterSeatRequired: Boolean,
    var image: String?
)
data class EmergencyContact(
    val id: String?,
    var name :String?,
    val firstName: String,
    val lastName: String,
    var mobileNumber: String,
    val email: String?
)

// Activity
data class ChildActivityDC(
    var id: String?,
    var childId: String,
    var type: String,
    var subType: String,
    var address: Address,
    var image: String?,
    var schedulePerDay: MutableMap<String, DailySchedule>
) {
    companion object {
        fun default(): ChildActivityDC {
            return ChildActivityDC(
                id = "",
                childId = "",
                type = "",
                subType = "",
                address = Address.default(),
                image = null,
                schedulePerDay = mutableMapOf()
            )
        }
    }
}
data class DailySchedule(
    var startTime: String,
    var endTime: String,
    var preferredPickupTime: Int,
    var pickupRole: String,
    var dropoffRole: String
) {
    companion object {
        fun default(): DailySchedule {
            return DailySchedule(
                startTime = "",
                endTime = "",
                preferredPickupTime = 0, // Assuming 0 is a valid default for pickup time
                pickupRole = "",
                dropoffRole = ""
            )
        }
    }
}


// Ride Record
data class EachRide(
    val id: String,
    val activityId: String,
    val activityType: String,
    val activitySubType: String,
    val userId: String,
    val userFirstName: String?,
    val userLastName: String?,
    val userImage: String?,
    val childId: String,
    val childFirstName: String?,
    val childLastName: String?,
    val childImage: String?,
    val mobileNumber: String?,
    val dayOfWeek: WeekDays,
    val date: String,
    val dateTime: String,
    val pickupTime: String,
    val dropoffTime: String,
    val noOfCompletedRides: Int,
    val rating: Double,
    val status: String,

    val rideTakers: List<RideTaker>,
    val pickupAddress: Address,
    val dropoffAddress: Address,
    val vehicle: Vehicle?
)
data class RideTaker(
    val activityId: String,
    val activityType: String,
    val activitySubType: String,
    val userId: String,
    val userFirstName: String?,
    val userLastName: String?,
    val userImage: String?,
    val childId: String,
    val childFirstName: String?,
    val childLastName: String?,
    val role: RideType,
    val mobileNumber: String?,
    val noOfCompletedRides: Int,
    val rating: Int,
    val preferredPickupTime: Int,
    val pickupDistance: Int,    // start to pick up
    val distance: Int,          // pick to drop
    val pointsSpent: Int?,
    val favorite: Boolean,
    val address: Address,
    val statusHistory: MutableMap<String, String>
)


// Web socket messages
data class WsEventGeneral(
    val socketEventType: WebSocketEvents,
    val rideId: String,
    val takerId: String?
)
// Data class for LOCATION_PICKUP event
data class WsEvent(
    val socketEventType: WebSocketEvents,
    val takerId: String?,
    val rideId: String,
    val latitude: Double?,
    val longitude: Double?
)
data class RideStartEvent(
    val socketEventType: WebSocketEvents,
    val rideId: String,
    val startLatitude: Double,
    val startLongitude: Double,
    val endLatitude: Double,
    val endLongitude: Double
)
// Ride details
data class RouteLocation(
    val takerId: String,
    val location: Location,
    val dateTime: String
)

data class RideTrack(
    val rideId: String,
    val startLocation: Location,
    val endLocation: Location,
    val dateTime: String,
//    val routeLocations: List<RouteLocation> = emptyList(),
//    val pickupLocations: List<RouteLocation> = emptyList(),
    val status: RideStatus,
    val rideTakers: List<RideTaker>
)