package com.callguard.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import com.callguard.app.data.AppDatabase
import com.callguard.app.data.CallOutcome
import com.callguard.app.data.DebugStats
import com.callguard.app.data.HistoryEntity
import com.callguard.app.logic.CallGuardEngine
import com.callguard.app.logic.Decision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallScreeningServiceImpl : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val number = try {
            callDetails.handle?.schemeSpecificPart
        } catch (_: Exception) {
            null
        }
        DebugStats.recordScreeningInvoked(applicationContext, number)

        try {
            if (number == null) {
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
                    .setSkipCallLog(!decision.silent)
                    .setSkipNotification(decision.silent)
                    .build()
            }

            respondToCall(callDetails, response)

            if (historyEntry.outcome != CallOutcome.IGNORED) {
                serviceScope.launch {
                    try {
                        AppDatabase.getInstance(applicationContext).historyDao().insert(historyEntry)
                    } catch (e: Exception) {
                        DebugStats.recordError(applicationContext, "history insert failed: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            DebugStats.recordError(applicationContext, "onScreenCall crashed: ${e.message}")
            try {
                respondToCall(callDetails, CallResponse.Builder().build())
            } catch (_: Exception) {
            }
            try {
                serviceScope.launch {
                    AppDatabase.getInstance(applicationContext).historyDao().insert(
                        HistoryEntity(
                            phoneNumber = number ?: "(unknown)",
                            timestampMillis = System.currentTimeMillis(),
                            outcome = CallOutcome.IGNORED,
                            counterAtEvent = -1
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }
    }
}
