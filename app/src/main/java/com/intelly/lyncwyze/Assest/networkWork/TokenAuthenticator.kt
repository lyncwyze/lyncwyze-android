package com.intelly.lyncwyze.Assest.networkWork

import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager
import mu.KotlinLogging
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.net.URL

class TokenAuthenticator() : Authenticator {
    private val logger = KotlinLogging.logger {}

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2)   // Prevent infinite loop
            return null

        synchronized(this) {        // Synchronized block to prevent multiple refreshes
            val requestUrl = response.request.url.toString()
            val url = URL(requestUrl)
            val path = url.path.removePrefix("/") // Get the path without the leading slash

            // Check if the path is in the whitelist
            logger.info { "whiteListEndPoints: ${whiteListEndPoints.contains(path)} $whiteListEndPoints" }

            if (whiteListEndPoints.contains(path))
                return response.request


            val validToken = SharedPreferencesManager.getValidatedAccessToken()
            return if (!validToken.isNullOrBlank()) {
                logger.info { "Bearer Token: $validToken" }
                response.request.newBuilder()
                    .header("Authorization", "Bearer $validToken")
                    .build()
            }
            else if (!SharedPreferencesManager.getRefreshToken().isNullOrBlank()) {
                val token = SharedPreferencesManager.getNewTokenWithRefreshToken()  // get new token with api call
                return if (token != null) {
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    response.request
                }
            } else {
                response.request
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var r = response.priorResponse
        while (r != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
