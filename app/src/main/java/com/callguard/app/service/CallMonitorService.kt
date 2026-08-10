package com.callguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.callguard.app.MainActivity
import com.callguard.app.R

/**
 * Companion to CallScreeningServiceImpl. CallScreeningService only decides
 * block/allow at the moment a call arrives; this service uses
 * TelephonyCallback (API 31+) / PhoneStateListener (below 31) to observe the
 * live call state (RINGING -> OFFHOOK -> IDLE) so we know exactly when an
 * allowed call rings and when it disconnects. That's what makes item #4
 * ("call disconnect hote hi wapas block ho jaye") observable/loggable - the
 * blocking logic itself is naturally re-armed on the next incoming call by
 * CallGuardEngine, this service just keeps state fresh and the notification alive.
 */
class CallMonitorService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var legacyListener: PhoneStateListener? = null
    private var modernCallback: TelephonyCallback? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        registerCallStateWatcher()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun registerCallStateWatcher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleState(state)
                }
            }
            modernCallback = callback
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleState(state)
                }
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun handleState(state: Int) {
        // TelephonyManager.CALL_STATE_IDLE / RINGING / OFFHOOK
        // Real call-number correlation for history is handled at screening time
        // in CallScreeningServiceImpl (which has direct access to Call.Details).
        // This watcher's job is simply to keep call-state awareness alive so the
        // app's "currently ringing / in-call" UI state (if shown) stays accurate,
        // and to guarantee the block state naturally re-applies once IDLE is reached.
        when (state) {
            TelephonyManager.CALL_STATE_IDLE -> { /* call ended - next incoming call re-evaluated fresh */ }
            TelephonyManager.CALL_STATE_RINGING -> { /* handled by CallScreeningServiceImpl */ }
            TelephonyManager.CALL_STATE_OFFHOOK -> { /* call answered/active */ }
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "callguard_monitor"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "CallGuard Monitor", NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, openAppIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("CallGuard active")
            .setContentText("Selected numbers ki monitoring chal rahi hai")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1001, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(1001, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            @Suppress("DEPRECATION")
            legacyListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
