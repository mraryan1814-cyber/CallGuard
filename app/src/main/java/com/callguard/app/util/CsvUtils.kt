package com.callguard.app.util

import android.content.Context
import android.net.Uri

/**
 * Simple CSV/TXT export & import for the monitored + whitelist number lists.
 * Format (one row per line):
 *   type,number
 * where type is MONITORED or WHITELIST. Kept plain-text so it also opens
 * fine as a .txt file.
 */
object CsvUtils {

    fun buildCsv(monitored: List<String>, whitelist: List<String>): String {
        val sb = StringBuilder()
        sb.append("type,number\n")
        monitored.forEach { sb.append("MONITORED,$it\n") }
        whitelist.forEach { sb.append("WHITELIST,$it\n") }
        return sb.toString()
    }

    fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray())
        }
    }

    data class ParsedLists(val monitored: List<String>, val whitelist: List<String>)

    fun readFromUri(context: Context, uri: Uri): ParsedLists {
        val monitored = mutableListOf<String>()
        val whitelist = mutableListOf<String>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.useLines { lines ->
            lines.forEachIndexed { index, rawLine ->
                if (index == 0 && rawLine.startsWith("type", ignoreCase = true)) return@forEachIndexed
                val line = rawLine.trim()
                if (line.isBlank()) return@forEachIndexed
                val parts = line.split(",")
                if (parts.size < 2) return@forEachIndexed
                val type = parts[0].trim().uppercase()
                val number = parts[1].trim()
                if (number.isBlank()) return@forEachIndexed
                when (type) {
                    "MONITORED" -> monitored.add(number)
                    "WHITELIST" -> whitelist.add(number)
                }
            }
        }
        return ParsedLists(monitored, whitelist)
    }
}
