package com.intelly.lyncwyze.Assest.networkWork

import android.app.Application
import android.content.Context
import com.intelly.lyncwyze.Assest.utilities.BASE_URL
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkManager {
    private var retrofit: Retrofit? = null
    val apiService: ApiService
        get() {
            if (retrofit == null) {
                throw IllegalStateException("NetworkManager.init(application) must be called before using apiService.")
            }
            return retrofit!!.create(ApiService::class.java)
        }

    private lateinit var appContext: Context

    fun init(application: Application) {
        appContext = application.applicationContext
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .authenticator(TokenAuthenticator())
            .addInterceptor(LogInterceptor(appContext))
            .build()
    }
}