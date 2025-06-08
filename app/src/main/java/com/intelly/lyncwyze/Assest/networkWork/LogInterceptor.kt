package com.intelly.lyncwyze.Assest.networkWork

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.intelly.lyncwyze.Activity_No_Internet_Connection
import com.intelly.lyncwyze.enteringUser.signIn.Activity_LoginWithEmail
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import okhttp3.Interceptor
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogInterceptor(private val appContext: Context) : Interceptor {
    private val logger = KotlinLogging.logger {}

    override fun intercept(chain: Interceptor.Chain): Response {
        // Network not available
        if (!isNetworkAvailable())
            navigateToNoInternet()

        val originalRequest = chain.request()
        logger.info { originalRequest.url }
        originalRequest.headers.forEach { header ->
            logger.info { header }
        }
        if (originalRequest.body != null) {
            logger.info { originalRequest.body }
        }

        // Resp check
        val response = chain.proceed(originalRequest)
        if (response.code == 401)     // Token expired or invalid, navigate to login
            runBlocking { navigateToLogin() }
        return response
    }
    private suspend fun navigateToLogin() = suspendCoroutine { continuation ->
        appContext.startActivity(Intent(appContext, Activity_LoginWithEmail::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        )
        continuation.resume(Unit)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    private fun navigateToNoInternet() {
        appContext.startActivity(Intent(appContext, Activity_No_Internet_Connection::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
