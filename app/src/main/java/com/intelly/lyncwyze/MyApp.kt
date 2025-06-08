package com.intelly.lyncwyze

import android.app.Application
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import com.intelly.lyncwyze.Assest.utilities.SharedPreferencesManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkManager.init(this)
        SharedPreferencesManager.init(this)
    }
}