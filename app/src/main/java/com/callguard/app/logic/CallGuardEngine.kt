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

/**
 * Pure decision logic, kept separate from Android call-screening APIs so it's easy to test.
 *
 * Rules:
 *  - Number not monitored & not whitelisted -> Allow, no bookkeeping (IGNORED)
 *  - Whitelisted -> always Allow (WHITELISTED)
 *  - Master switch OFF -> always Allow (all blocking disabled)
 *  - Monitored number:
 *      - if gap since last call > timeGapMinutes -> counter resets to 0 first
 *      - counter++
 *      - if counter < blockCount  -> Block (silent or hard, per setting)
 *      - if counter >= blockCount -> Allow, then counter resets to 0 for next cycle
 */
class CallGuardEngine(context: Context) {

    private val prefs = Prefs(context)
    private val counterStore = CounterStore(context)

    /**
     * Evaluate an incoming call. Returns the decision plus a HistoryEntity to persist.
     * Runs blocking-on-IO reads of DataStore settings since CallScreeningService needs
     * a fast, synchronous-style answer.
     */
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

            var state = counterStore.getState(number)

            // Reset counter if gap since last call exceeds the configured threshold
            if (state.lastCallTimeMillis > 0 && (now - state.lastCallTimeMillis) > gapMillis) {
                state = NumberState(0, state.lastCallTimeMillis)
            }

            val newCount = state.count + 1

            return@runBlocking if (newCount >= blockCount) {
                // This is the Nth call -> allow it to ring, then reset cycle
                counterStore.saveState(number, NumberState(0, now))
                Decision.Allow to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = CallOutcome.ALLOWED, counterAtEvent = newCount
                )
            } else {
                counterStore.saveState(number, NumberState(newCount, now))
                val outcome = if (isSilent) CallOutcome.SILENCED else CallOutcome.BLOCKED
                Decision.Block(silent = isSilent) to HistoryEntity(
                    phoneNumber = number, timestampMillis = now,
                    outcome = outcome, counterAtEvent = newCount
                )
            }
        }
    }
}
