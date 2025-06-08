package com.intelly.lyncwyze.Assest.networkWork

data class ErrorResponse(
    val errorInformation: ErrorInformation,
    val timestamp: String
)

data class ErrorInformation(
    val errorCode: String,
    val errorDescription: String
)
