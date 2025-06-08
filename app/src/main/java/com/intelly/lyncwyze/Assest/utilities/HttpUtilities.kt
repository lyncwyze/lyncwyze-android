package com.intelly.lyncwyze.Assest.utilities

import com.intelly.lyncwyze.Assest.networkWork.ErrorResponse
import com.google.gson.Gson
import mu.KotlinLogging
import retrofit2.Response

object HttpUtilities {
    private val logger = KotlinLogging.logger {}

    fun parseError(response: Response<*>): ErrorResponse? {
        return try {
            val errorBody = response.errorBody()?.string()

            val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
            logger.info { error }

            return error
        } catch (e: Exception) {
            null // Return null if parsing fails
        }
    }
}