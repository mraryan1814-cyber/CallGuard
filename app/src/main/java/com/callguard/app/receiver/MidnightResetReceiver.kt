package com.callguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callguard.app.data.CounterStore
import com.callguard.app.data.Prefs
import com.callguard.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fired by AlarmManager every day at 00:00. Resets every monitored number's
 * call counter back to zero, then reschedules itself for the next midnight.
 */
class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val prefs = Prefs(context)
        val counterStore = CounterStore(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val numbers = prefs.getMonitoredNumbersOnce()
                counterStore.resetAll(numbers)
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                prefs.setLastResetDay(today)
            } finally {
                AlarmScheduler.scheduleMidnightReset(context)
                pendingResult.finish()
            }
        }
    }
}
