package com.callguard.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import com.callguard.app.data.AppDatabase
import com.callguard.app.logic.CallGuardEngine
import com.callguard.app.logic.Decision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * System hands every incoming call to this service (once CallGuard is set as the
 * default Call Screening app via RoleManager). We must respond with a CallResponse
 * quickly - reject to block, or do nothing to let it ring normally.
 */
class CallScreeningServiceImpl : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            // Unknown number -> can't match against our list, let it ring
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val engine = CallGuardEngine(applicationContext)
        val (decision, historyEntry) = engine.evaluate(number)

        val response = when (decision) {
            is Decision.Allow -> CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build()

            is Decision.Block -> CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                // silent block: don't show as a missed-call notification banner-shock,
                // hard block: reject + skip call log entirely
                .setSkipCallLog(!decision.silent)
                .setSkipNotification(decision.silent)
                .build()
        }

        respondToCall(callDetails, response)

        // Persist to history log (Room) off the critical path
        serviceScope.launch {
            AppDatabase.getInstance(applicationContext).historyDao().insert(historyEntry)
        }
    }
}
