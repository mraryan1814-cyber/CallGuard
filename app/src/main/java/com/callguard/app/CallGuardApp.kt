package com.callguard.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.callguard.app.service.CallMonitorService
import com.callguard.app.util.AlarmScheduler

class CallGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmScheduler.scheduleMidnightReset(this)

        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val intent = Intent(this, CallMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (_: Exception) {
            }
        }
    }
}
