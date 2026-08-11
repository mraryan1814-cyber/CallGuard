package com.callguard.app.logic

import android.content.Context
import com.callguard.app.data.CallOutcome
import com.callguard.app.data.CounterStore
import com.callguard.app.data.HistoryEntity
import com.callguard.app.data.NumberState
import com.callguard.app.data.Prefs
import kotlinx.coroutines.runBlocking

sealed class Decision {
    data class Block(val silent: Boolean) : Decision()
    data object Allow : Decision()
}

class CallGuardEngine(context: Context) {

    private val prefs = Prefs(context)
    private val counterStore = CounterStore(context)

    fun evaluate(rawNumber: String): Pair<Decision, HistoryEntity> {
        val number = Prefs.normalize(rawNumber)
        val now = System.currentTimeMillis()

        return runBlocking {
            val whitelisted = prefs.getWhitelistNumbersOnce().contains(number)
            if (whitelisted) {
                return@runBlocking Decision.Allow to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = CallOutcome.WHITELISTED, counterAtEvent = 0
                )
            }

            val masterEnabled = prefs.getMasterEnabledOnce()
            val monitored = prefs.getMonitoredNumbersOnce().contains(number)

            if (!masterEnabled || !monitored) {
                return@runBlocking Decision.Allow to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = CallOutcome.IGNORED, counterAtEvent = 0
                )
            }

            val blockCount = prefs.getBlockCountOnce()
            val gapMinutes = prefs.getTimeGapMinutesOnce()
            val gapMillis = gapMinutes * 60_000L
            val isSilent = prefs.getSilentModeOnce()

            val state = counterStore.getState(number)

            val windowStillOpen = state.lastCallTimeMillis != 0L &&
                (now - state.lastCallTimeMillis) <= gapMillis

            return@runBlocking if (windowStillOpen) {
                val newCount = state.count + 1
                counterStore.saveState(number, NumberState(newCount, now))
                val outcome = if (isSilent) CallOutcome.SILENCED else CallOutcome.BLOCKED
                Decision.Block(silent = isSilent) to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = outcome, counterAtEvent = newCount
                )
            } else {
                val previousWindowMatchedExactly =
                    state.lastCallTimeMillis != 0L && state.count == blockCount

                if (previousWindowMatchedExactly) {
                    counterStore.saveState(number, NumberState(0, now))
                    Decision.Allow to HistoryEntity(
                        phoneNumber = number, timestampMillis = now,
                        outcome = CallOutcome.ALLOWED, counterAtEvent = state.count
                    )
                } else {
                    counterStore.saveState(number, NumberState(1, now))
                    val outcome = if (isSilent) CallOutcome.SILENCED else CallOutcome.BLOCKED
                    Decision.Block(silent = isSilent) to HistoryEntity(
                        phoneNumber = number, timestampMillis = now,
                        outcome = outcome, counterAtEvent = 1
                    )
                }
            }
        }
    }
}
