package com.ironmind.sleepingdragon

import android.app.Application
import android.util.Log
import com.ironmind.sleepingdragon.core.AppConstants

class SleepingDragonApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Sleeping Dragon ${AppConstants.VERSION_NAME} — foundation ready")
    }

    companion object {
        private const val TAG = "SleepingDragon"
    }
}