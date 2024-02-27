package com.baxramov.eloonnet

import android.app.Application
import com.onesignal.OneSignal

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
        OneSignal.initWithContext(this)
        OneSignal.setAppId(APP_ID)
    }

    companion object {
        private const val APP_ID = "807db3c5-7fe6-4b2e-9ad7-0259721c767f"
    }
}