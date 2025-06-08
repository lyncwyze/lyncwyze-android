package com.intelly.lyncwyze.Assest.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intelly.lyncwyze.Assest.modals.DecodedUserDetails
import com.intelly.lyncwyze.Assest.modals.LoginResponse
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import mu.KotlinLogging

object SharedPreferencesManager {
    private val logger = KotlinLogging.logger {}

    lateinit var sharedPreferences: SharedPreferences
    val gson = Gson()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_TOKEN_FILE, Context.MODE_PRIVATE)
    }

    fun saveAuthDetails(body: LoginResponse) {
        val currMs = System.currentTimeMillis()
        // Mark token expired if only 4 seconds remained in expiry (to fetch new token)
        val expOnMs: Long = currMs + ((body.expires_in - 4) * 1000);
        sharedPreferences.edit() {
            saveObject(LoggedInDataKey, body)
            putString(LOGGED_IN_TOKEN, body.access_token)
            putLong(TOKEN_EXPIRATION_TIME, expOnMs)
            putString(REFRESH_TOKEN, body.refresh_token)
        }
    }
    fun deleteAuthDetails() {
        sharedPreferences.edit {
            remove(LoggedInDataKey)
            remove(LOGGED_IN_TOKEN)
            remove(TOKEN_EXPIRATION_TIME)
            remove(REFRESH_TOKEN)
        }
        logger.info { "Authentication details have been deleted. checking: ${getAccessToken()}" }
    }

    private fun getAccessToken(): String? { return getString(LOGGED_IN_TOKEN) }
    fun getValidatedAccessToken(): String? {
        val currTimeMs = System.currentTimeMillis()
        val expirationTimeMs = sharedPreferences.getLong(TOKEN_EXPIRATION_TIME, 0)
        logger.info { "times: Current: ${currTimeMs} expire on: $expirationTimeMs, ${currTimeMs > expirationTimeMs}" }
        return if (currTimeMs > expirationTimeMs) { // Expired token
//            getNewTokenWithRefreshToken()
            null
        } else {
            getAccessToken()
        }
    }
    fun getDecodedAccessToken(): DecodedUserDetails? {
        val token = getValidatedAccessToken()
        return token?.let { accessToken ->
            try {
                val jwt = JWT(accessToken)
                return DecodedUserDetails(
                    user_name = jwt.getClaim("user_name").asString(),
                    fullName = jwt.getClaim("fullName").asString(),
                    profileComplete = jwt.getClaim("profileComplete").asBoolean(),
                    clientId = jwt.getClaim("clientId").asString(),
                    emailId = jwt.getClaim("emailId").asString(),
                    userId = jwt.getClaim("userId").asString()!!,
                    authorities = jwt.getClaim("authorities").asList(String::class.java),
                    client_id = jwt.getClaim("client_id").asString(),
                    changePassword = jwt.getClaim("changePassword").asBoolean(),
                    scope = jwt.getClaim("scope").asList(String::class.java),
                    name = jwt.getClaim("name").asString(),
                    exp = jwt.getClaim("exp").asInt(),
                    acceptTermsAndPrivacyPolicy = jwt.getClaim("acceptTermsAndPrivacyPolicy").asBoolean(),
                    jti = jwt.getClaim("jti").asString(),
                    profileStatus = jwt.getClaim("profileStatus").asString(),
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN, null)
    }
    // Refresh token synchronously
    fun getNewTokenWithRefreshToken(): String? {
        val refreshToken: String? = getRefreshToken()
        if(!refreshToken.isNullOrBlank()) {
            return try {
                val response = NetworkManager.apiService.refreshToken(refreshToken).execute()
                return if (response.isSuccessful) {
                    response.body()?.let {
                        saveAuthDetails(it)
                        return it.access_token
                    }
                } else {
                    logger.error { "Failed to refresh token: ${response.code()} - ${response.errorBody()?.string()}" }
                    null
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during refresh token request" }
                null
            }
        }
        return null
    }

    // Save-get String data
    private fun saveString(key: String, value: String) {
        sharedPreferences.edit() {
            putString(key, value)
        }
    }
    fun getString(key: String): String? {
        return sharedPreferences.getString(key, null)   // Else null
    }

    // Save-Get Int data
    fun saveInt(key: String, value: Int) {
        sharedPreferences.edit() {
            putInt(key, value)
        }
    }
    fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, 0) // Return 0 if not found
    }

    // Save-Get Boolean data
    fun saveBoolean(key: String, value: Boolean) {
        sharedPreferences.edit() {
            putBoolean(key, value)
        }
    }
    fun getBoolean(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false) // Return false if not found
    }

    // Save-Get Custom Object
    fun <T> saveObject(key: String, obj: T) {
        val jsonString = gson.toJson(obj)
        saveString(key, jsonString)
    }
    inline fun <reified T> getObject(key: String): T? {
        val jsonString = getString(key) ?: return null
        return gson.fromJson(jsonString, object : TypeToken<T>() {}.type)
    }

    // Function to clear all SharedPreferences data
    fun clearAll() {
        sharedPreferences.edit() {
            clear()     // Clear all data from SharedPreferences
        } // Apply changes
    }
}