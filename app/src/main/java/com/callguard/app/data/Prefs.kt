package com.callguard.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "callguard_prefs")

/**
 * Central store for all user-configurable settings.
 * Numbers are stored delimiter-separated ("|") to avoid pulling in a JSON library.
 */
class Prefs(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SET = booleanPreferencesKey("pin_set")
        val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
        val SILENT_MODE = booleanPreferencesKey("silent_mode")
        val BLOCK_COUNT = intPreferencesKey("block_count")       // e.g. 4 -> blocks 1..4, allows 5th
        val TIME_GAP_MINUTES = intPreferencesKey("time_gap_minutes") // e.g. 2
        val MONITORED_NUMBERS = stringPreferencesKey("monitored_numbers")
        val WHITELIST_NUMBERS = stringPreferencesKey("whitelist_numbers")
        val LAST_RESET_DAY = stringPreferencesKey("last_reset_day") // yyyy-MM-dd
    }

    companion object {
        const val MASTER_CODE = "bettaua" // alphabet-only master recovery code
        private const val DELIM = "|"

        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun normalize(number: String): String {
            // Keep digits and leading + only, so formatting differences don't break matching
            val sb = StringBuilder()
            for ((i, c) in number.withIndex()) {
                if (c.isDigit()) sb.append(c)
                if (c == '+' && i == 0) sb.append(c)
            }
            return sb.toString()
        }
    }

    // ---------- PIN ----------
    val isPinSet: Flow<Boolean> = context.dataStore.data.map { it[Keys.PIN_SET] ?: false }

    suspend fun setPin(pin: String) {
        context.dataStore.edit {
            it[Keys.PIN_HASH] = sha256(pin)
            it[Keys.PIN_SET] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = context.dataStore.data.first()[Keys.PIN_HASH] ?: return false
        return stored == sha256(pin)
    }

    suspend fun verifyMasterCode(code: String): Boolean {
        return code.trim().equals(MASTER_CODE, ignoreCase = true)
    }

    suspend fun resetPinAfterMasterCode() {
        context.dataStore.edit { it[Keys.PIN_SET] = false }
    }

    // ---------- Master switch ----------
    val masterEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MASTER_ENABLED] ?: true }

    suspend fun setMasterEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MASTER_ENABLED] = enabled }
    }

    suspend fun getMasterEnabledOnce(): Boolean = masterEnabled.first()

    // ---------- Silent mode ----------
    val silentMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.SILENT_MODE] ?: false }

    suspend fun setSilentMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SILENT_MODE] = enabled }
    }

    suspend fun getSilentModeOnce(): Boolean = silentMode.first()

    // ---------- Block count threshold (3, 4, 5, 7 ...) ----------
    val blockCount: Flow<Int> = context.dataStore.data.map { it[Keys.BLOCK_COUNT] ?: 4 }

    suspend fun setBlockCount(count: Int) {
        context.dataStore.edit { it[Keys.BLOCK_COUNT] = count }
    }

    suspend fun getBlockCountOnce(): Int = blockCount.first()

    // ---------- Time gap in minutes (1, 2, 5 ...) ----------
    val timeGapMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.TIME_GAP_MINUTES] ?: 2 }

    suspend fun setTimeGapMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.TIME_GAP_MINUTES] = minutes }
    }

    suspend fun getTimeGapMinutesOnce(): Int = timeGapMinutes.first()

    // ---------- Monitored numbers (2-4 target numbers) ----------
    val monitoredNumbers: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.MONITORED_NUMBERS]?.split(DELIM)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun getMonitoredNumbersOnce(): List<String> = monitoredNumbers.first()

    suspend fun addMonitoredNumber(number: String) {
        val norm = normalize(number)
        if (norm.isBlank()) return
        context.dataStore.edit {
            val current = it[Keys.MONITORED_NUMBERS]?.split(DELIM)?.filter { n -> n.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            if (!current.contains(norm)) current.add(norm)
            it[Keys.MONITORED_NUMBERS] = current.joinToString(DELIM)
        }
    }

    suspend fun removeMonitoredNumber(number: String) {
        val norm = normalize(number)
        context.dataStore.edit {
            val current = it[Keys.MONITORED_NUMBERS]?.split(DELIM)?.filter { n -> n.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            current.remove(norm)
            it[Keys.MONITORED_NUMBERS] = current.joinToString(DELIM)
        }
    }

    suspend fun setAllMonitoredNumbers(numbers: List<String>) {
        context.dataStore.edit {
            it[Keys.MONITORED_NUMBERS] = numbers.map { n -> normalize(n) }.filter { n -> n.isNotBlank() }
                .joinToString(DELIM)
        }
    }

    // ---------- Whitelist (VIP) numbers - always ring, never blocked ----------
    val whitelistNumbers: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.WHITELIST_NUMBERS]?.split(DELIM)?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun getWhitelistNumbersOnce(): List<String> = whitelistNumbers.first()

    suspend fun addWhitelistNumber(number: String) {
        val norm = normalize(number)
        if (norm.isBlank()) return
        context.dataStore.edit {
            val current = it[Keys.WHITELIST_NUMBERS]?.split(DELIM)?.filter { n -> n.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            if (!current.contains(norm)) current.add(norm)
            it[Keys.WHITELIST_NUMBERS] = current.joinToString(DELIM)
        }
    }

    suspend fun removeWhitelistNumber(number: String) {
        val norm = normalize(number)
        context.dataStore.edit {
            val current = it[Keys.WHITELIST_NUMBERS]?.split(DELIM)?.filter { n -> n.isNotBlank() }?.toMutableList()
                ?: mutableListOf()
            current.remove(norm)
            it[Keys.WHITELIST_NUMBERS] = current.joinToString(DELIM)
        }
    }

    // ---------- Daily auto-reset bookkeeping ----------
    suspend fun getLastResetDay(): String? = context.dataStore.data.first()[Keys.LAST_RESET_DAY]

    suspend fun setLastResetDay(day: String) {
        context.dataStore.edit { it[Keys.LAST_RESET_DAY] = day }
    }
}
