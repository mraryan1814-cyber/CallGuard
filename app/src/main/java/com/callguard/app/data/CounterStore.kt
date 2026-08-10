package com.callguard.app.data

import android.content.Context

/**
 * Tracks per-number call counter + last call timestamp.
 * Uses plain SharedPreferences (synchronous) because CallScreeningService.onScreenCall()
 * must make its block/allow decision quickly.
 */
data class NumberState(val count: Int, val lastCallTimeMillis: Long)

class CounterStore(context: Context) {

    private val prefs = context.getSharedPreferences("callguard_counters", Context.MODE_PRIVATE)

    fun getState(number: String): NumberState {
        val raw = prefs.getString(number, null) ?: return NumberState(0, 0L)
        val parts = raw.split(":")
        val count = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val time = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        return NumberState(count, time)
    }

    fun saveState(number: String, state: NumberState) {
        prefs.edit().putString(number, "${state.count}:${state.lastCallTimeMillis}").apply()
    }

    fun resetNumber(number: String) {
        saveState(number, NumberState(0, 0L))
    }

    fun resetAll(numbers: List<String>) {
        val editor = prefs.edit()
        for (n in numbers) editor.putString(n, "0:0")
        editor.apply()
    }

    fun clearNumber(number: String) {
        prefs.edit().remove(number).apply()
    }
}
