package com.callguard.app

import android.app.Application
import android.content.Intent
import android.os.Build
import com.callguard.app.service.CallMonitorService
import com.callguard.app.util.AlarmScheduler

class CallGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmScheduler.scheduleMidnightReset(this)
        try {
            val intent = Intent(this, CallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            // READ_PHONE_STATE permission may not be granted yet on first launch;
            // MainActivity will request it and can restart the service after.
        }
    }
}
