package com.intelly.lyncwyze.Assest.networkWork

import com.intelly.lyncwyze.Assest.modals.Address2
import com.intelly.lyncwyze.Assest.modals.BackgroundVerificationRequestBody
import com.intelly.lyncwyze.Assest.modals.ChildActivityDC
import com.intelly.lyncwyze.Assest.modals.CreateProfileRequestBodyFromEmail
import com.intelly.lyncwyze.Assest.modals.CreateProfileRequestBodyFromMobile
import com.intelly.lyncwyze.Assest.modals.DataCountResp
import com.intelly.lyncwyze.Assest.modals.EachRide
import com.intelly.lyncwyze.Assest.modals.EmergencyContact
import com.intelly.lyncwyze.Assest.modals.GeoCode
import com.intelly.lyncwyze.Assest.modals.Location
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.modals.PaginatedResponse
import com.intelly.lyncwyze.Assest.modals.Providers
import com.intelly.lyncwyze.Assest.modals.RideTrack
import com.intelly.lyncwyze.Assest.modals.SaveChild
import com.intelly.lyncwyze.Assest.modals.SurveyReport
import com.intelly.lyncwyze.Assest.modals.UserProfile
import com.intelly.lyncwyze.Assest.modals.Vehicle
import com.intelly.lyncwyze.enteringUser.notifyMe.NotifyMeRequest
import com.intelly.lyncwyze.enteringUser.notifyMe.NotifyMeResponse
import kotlinx.serialization.descriptors.StructureKind
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Objects

val whiteListEndPoints: List<String> = listOf(
    "user/getProviders",
    "user/retryOtp",
//    "user/retryEmailOtp"
)

interface ApiService {
    // Main activity
    @GET("user/getProviders")
    suspend fun getProviders(@Query("pageSize") pageSize: Int, @Query("offSet") offSet: Int): Response<PaginatedResponse<Providers>>

    // If provider not found
    @POST("match/addNotifyUser")
    suspend fun saveNotifyMe(@Body request: NotifyMeRequest): Response<NotifyMeResponse>

    // Register
    @GET("user/generateOtp/{mobileNumber}")
    suspend fun generateOtpForNumber(@Path("mobileNumber") mobileNumber: String): Response<Boolean>
    @GET("user/verifyOtpAndCreateUser/{otp}/{mobileNumber}")
    suspend fun verifyNumberOtpAndCreateUser(
        @Path("otp") otp: String,
        @Path("mobileNumber") mobileNumber: String,
        @Header("token") token: String,
        @Header("type") type: String = "mobile",    // Default value for type
        @Header("platform") platform: String,       // Pass platform (android/iOS)
        @Header("version") version: String          // Pass version of the OS
    ): Response<LoginResponse>

    // Register
    @GET("user/generateEmailOtp/{email}")
    suspend fun generateOtpFromEmail(@Path("email") email: String): Response<Boolean>
    @GET("user/verifyEmailOtpAndCreateUser/{otp}/{email}")
    suspend fun verifyEmailOtpAndCreateUser(
        @Path("otp") otp: String,
        @Path("email") email: String,
        @Header("token") token: String,
        @Header("type") type: String = "mobile",    // Default value for type
        @Header("platform") platform: String,       // Pass platform (android/iOS)
        @Header("version") version: String          // Pass version of the OS
    ): Response<LoginResponse>

    @GET("user/createMobileNumberUser/{mobileNumber}")
    suspend fun createUserUsingNumber(
        @Path("mobileNumber") mobileNumber: String,
        @Header("token") token: String,
        @Header("type") type: String = "mobile",    // Default value for type
        @Header("platform") platform: String,       // Pass platform (android/iOS)
        @Header("version") version: String          // Pass version of the OS
    ): Response<LoginResponse>

    @GET("user/createEmailUser/{email}")
    suspend fun createUserUsingEmail(
        @Path("email") email: String,
        @Header("token") token: String,
        @Header("type") type: String = "mobile",    // Default value for type
        @Header("platform") platform: String,       // Pass platform (android/iOS)
        @Header("version") version: String          // Pass version of the OS
    ): Response<LoginResponse>

    @GET("user/retryOtp/{mobileNumber}")
    suspend fun retryPhoneOtp(@Query("mobileNumber") mobileNumber: String): Response<Boolean>
    @GET("user/retryEmailOtp/{email}")
    suspend fun retryEmailOtp(@Path("email") email: String): Response<Boolean>

    // Login
    @POST("auth/authenticate")
    suspend fun loginUser(
        @Header("Authorization") Authorization: String, // Basic email:pass / number:pass (in Base64 only _:_)
        @Query("authType") authType: String,        // app
        @Header("token") token: String,             // Get to send FCM token
        @Header("platform") platform: String,       // Pass platform (android/iOS)
        @Header("version") version: String,         // Pass version of the OS
    ): Response<LoginResponse>

    // logout
    @POST("auth/invalidate")
//    suspend fun invalidate(): Response<Boolean>
    suspend fun invalidate(@Header("token") token: String, @Body body: HashMap<String, String>): Response<Boolean>

    @POST("auth/getAccessToken") // Replace with your actual refresh token endpoint
    fun refreshToken(
        @Query("refresh_token") refresh_token: String
    ): Call<LoginResponse>

    // NOTE: As user logins/registers successfully we send the user to the dashboard

    // As dashboard loads
    @GET("user/getCount")
    suspend fun dataCount(): Response<DataCountResp>

    //
    @GET("match/checkServiceAvailability")
    suspend fun checkServiceAvailability(@Query("placeId") placeId: String): Response<Boolean>


    @GET("match/getSuggestions")
    suspend fun getLocations(@Query("address") address: String, @Query("sessionToken") sessionToken: String? = null): Response<List<Location>>

    @GET("match/getGeoCode")
    suspend fun getGeoCode(@Query("address") address: String) : Response<List<GeoCode>>

    @POST("user/acceptTermsAndPrivacyPolicy")
    suspend fun acceptTermsAndPrivacyPolicy(@Query("acceptTermsAndPrivacyPolicy") acceptTermsAndPrivacyPolicy: Boolean): Response<Boolean>





    @POST("user/createProfile")
    suspend fun createProfileFromEmail(@Body body: CreateProfileRequestBodyFromEmail): Response<UserProfile>

    @POST("user/createProfile")
    suspend fun createProfileFromMobile(@Body body: CreateProfileRequestBodyFromMobile): Response<UserProfile>

    @POST("user/backgroundVerification")
    suspend fun backgroundVerification(@Body body: BackgroundVerificationRequestBody): Response<UserProfile>

    @POST("user/addAddress")
    suspend fun addAddress(@Body body: Address2): Response<Address2>

    @Multipart
    @POST("user/addProfileImage")
    suspend fun addProfileImage(@Part file: MultipartBody.Part): Response<ResponseBody>
    @DELETE("user/deleteProfileImage")
    suspend fun deleteProfileImage(@Query("userId") userId: String): Response<ResponseBody>
    @Multipart
    @POST("/user/addChild")
    suspend fun addChild(@Part("child") child: RequestBody, @Part image: MultipartBody.Part?): Response<SaveChild>
    @Multipart
    @POST("/user/updateChild")
    suspend fun updateChild(@Part("child") child: RequestBody, @Part image: MultipartBody.Part?): Response<SaveChild>
    @GET("/user/getChildById")
    suspend fun getChildById(@Query("childId") childId: String): Response<SaveChild>
    @GET("/user/getChildren")
    suspend fun getChildren(
        @Query("pageSize") pageSize: Int,
        @Query("offSet") offSet: Int,
        @Query("sortOrder") sortOrder: String,
    ): Response<PaginatedResponse<SaveChild>>
    @DELETE("/user/deleteChild")
    suspend fun deleteChild(@Query("childId") childId: String): Response<Boolean>


    @GET("/user/getEmergencyContacts")
    suspend fun getEmergencyContacts(
        @Query("pageSize") pageSize: Int,
        @Query("offSet") offSet: Int,
        @Query("sortOrder") sortOrder: String
    ): Response<PaginatedResponse<EmergencyContact>>
    @GET("/user/getEmergencyContactById")
    suspend fun getEmergencyContactByID(@Query("emergencyContactId") emergencyContactId: String): Response<EmergencyContact>
    @POST("/user/updateEmergencyContact")
    suspend fun updateEmergencyContact( @Body body: EmergencyContact): Response<EmergencyContact>
    @DELETE("/user/deleteEmergencyContact")
    suspend fun deleteEmergencyContact(@Query("emergencyContactId") emergencyContactId: String): Response<Boolean>


    @POST("user/addEmergencyContact")
    suspend fun addEmergencyContact(@Body body: EmergencyContact): Response<EmergencyContact>

    @GET("/match/validDays")
    suspend fun getValidDays(): Response<List<String>>

    @GET("/user/getActivities")
    suspend fun getChildActivities(
        @Query("childId") childId: String,
        @Query("pageSize") pageSize: Int,
        @Query("offSet") offSet: Int,
        @Query("sortOrder") sortOrder: String,
    ): Response<PaginatedResponse<ChildActivityDC>>
    @POST("/user/addActivity")
    suspend fun addChildActivity(@Body body: ChildActivityDC): Response<ChildActivityDC>
    @GET("/user/getActivityById")
    suspend fun getActivityById(@Query("activityId") activityId: String): Response<ChildActivityDC>
    @DELETE("/user/deleteActivity")
    suspend fun deleteActivity(@Query("activityId") activityId: String): Response<Boolean>


    // Ride Scheduling
    @GET("/match/getProbability")
    suspend fun getProbabilityData(@Query("activityId") activityId: String, @Query("dayOfWeek") dayOfWeek: String): Response<String>
    @GET("/match/rideSchedule")
    suspend fun addRideScheduleInQueue(
        @Query("activityId") activityId: String,
        @Query("dayOfWeek") dayOfWeek: String,
        @Query("role") role: String): Response<String>

    @GET("/match/getRides")
    suspend fun getUpcomingRides(
        @Query("pageSize") pageSize: Int,
        @Query("status") status: String,
        @Query("role") role: String?
    ): Response<PaginatedResponse<EachRide>>

    @GET("/match/get/{rideId}")
    suspend fun getRideDetailsByID(@Path("rideId") rideId: String): Response<RideTrack>

    // Vehicles related
    @GET("/user/getVehicles")
    suspend fun getVehicles(): Response<PaginatedResponse<Vehicle>>
    @GET("/user/getVehicleById")
    suspend fun getVehicleById(@Query("vehicleId") vehicleId: String): Response<Vehicle>
    @POST("/user/updateVehicle")
    suspend fun updateVehicle(@Body body: Vehicle): Response<Vehicle>
    @DELETE("/user/deleteVehicle")
    suspend fun deleteVehicle(@Query("vehicleId") vehicleId: String): Response<Boolean>

    @POST("/user/addVehicle")
    suspend fun addVehicle(@Body body: Vehicle): Response<Vehicle>

    @GET("user/loadImage")
    suspend fun loadImage(@Query("path") path: String): Response<ResponseBody>

    @POST("match/getReview")
    suspend fun getReview(@Body data: SurveyReport): Response<SurveyReport>
    @POST("match/submitReview")
    suspend fun submitSurvey(@Body data: SurveyReport): Response<SurveyReport>

    @GET("match/ignoreSchedule")
    suspend fun ignoreSchedule(@Query("activityId") activityId: String, @Query("dayOfWeek") dayOfWeek: String): Response<Any>

    @GET("user/getUserById")
    suspend fun getUserById(@Query("id") id: String): Response<UserProfile>
    @PUT("user/updateUser")
    suspend fun updateUser(@Query("username") username: String, @Body body: UserProfile): Response<UserProfile>
}