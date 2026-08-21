package com.callguard.app.data

import android.content.Context

object DebugStats {

    private const val PREFS_NAME = "callguard_debug_stats"
    private const val KEY_SCREEN_INVOKED = "screen_invoked_count"
    private const val KEY_LAST_ERROR = "last_error"
    private const val KEY_LAST_NUMBER = "last_number_seen"

    fun recordScreeningInvoked(context: Context, number: String?) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = prefs.getInt(KEY_SCREEN_INVOKED, 0)
            prefs.edit()
                .putInt(KEY_SCREEN_INVOKED, current + 1)
                .putString(KEY_LAST_NUMBER, number ?: "(unknown)")
                .apply()
        } catch (_: Exception) {
        }
    }

    fun recordError(context: Context, message: String) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_ERROR, message)
                .apply()
        } catch (_: Exception) {
        }
    }

    fun getScreeningInvokedCount(context: Context): Int {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SCREEN_INVOKED, 0)
        } catch (_: Exception) {
            0
        }
    }

    fun getLastNumberSeen(context: Context): String? {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_NUMBER, null)
        } catch (_: Exception) {
            null
        }
    }

    fun getLastError(context: Context): String? {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_ERROR, null)
        } catch (_: Exception) {
            null
        }
    }
}
