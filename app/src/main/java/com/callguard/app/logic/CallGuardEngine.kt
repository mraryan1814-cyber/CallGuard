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
            val whitelistNumbers = prefs.getWhitelistNumbersOnce()
            val whitelisted = whitelistNumbers.any { Prefs.numbersMatch(number, it) }
            if (whitelisted) {
                return@runBlocking Decision.Allow to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = CallOutcome.WHITELISTED, counterAtEvent = 0
                )
            }

            val masterEnabled = prefs.getMasterEnabledOnce()
            val monitoredNumbers = prefs.getMonitoredNumbersOnce()
            val matchedStoredNumber = monitoredNumbers.firstOrNull { Prefs.numbersMatch(number, it) }
            val monitored = matchedStoredNumber != null

            if (!masterEnabled || !monitored) {
                return@runBlocking Decision.Allow to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = CallOutcome.IGNORED, counterAtEvent = 0
                )
            }

            val storageKey = matchedStoredNumber!!

            val blockCount = prefs.getBlockCountOnce()
            val gapMinutes = prefs.getTimeGapMinutesOnce()
            val gapMillis = gapMinutes * 60_000L
            val isSilent = prefs.getSilentModeOnce()

            val state = counterStore.getState(storageKey)

            val windowStillOpen = state.lastCallTimeMillis != 0L &&
                (now - state.lastCallTimeMillis) <= gapMillis

            return@runBlocking if (windowStillOpen) {
                val newCount = state.count + 1
                counterStore.saveState(storageKey, NumberState(newCount, now))
                val outcome = if (isSilent) CallOutcome.SILENCED else CallOutcome.BLOCKED
                Decision.Block(silent = isSilent) to HistoryEntity(
                    phoneNumber = storageKey, timestampMillis = now,
                    outcome = outcome, counterAtEvent = newCount
                )
            } else {
                val previousWindowMatchedExactly =
                    state.lastCallTimeMillis != 0L && state.count == blockCount

                if (previousWindowMatchedExactly) {
                    counterStore.saveState(storageKey, NumberState(0, now))
                    Decision.Allow to HistoryEntity(
                        phoneNumber = storageKey, timestampMillis = now,
                        outcome = CallOutcome.ALLOWED, counterAtEvent = state.count
                    )
                } else {
                    counterStore.saveState(storageKey, NumberState(1, now))
                    val outcome = if (isSilent) CallOutcome.SILENCED else CallOutcome.BLOCKED
                    Decision.Block(silent = isSilent) to HistoryEntity(
                        phoneNumber = storageKey, timestampMillis = now,
                        outcome = outcome, counterAtEvent = 1
                    )
                }
            }
        }
    }
}
