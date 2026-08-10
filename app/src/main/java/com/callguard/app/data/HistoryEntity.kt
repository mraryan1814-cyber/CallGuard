package com.callguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallOutcome {
    BLOCKED,        // call rejected outright
    SILENCED,       // call silenced (rang with no sound), still counted as blocked
    ALLOWED,        // Nth call - allowed to ring
    WHITELISTED,    // VIP number, always allowed
    IGNORED         // number not monitored, app took no action
}

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val timestampMillis: Long,
    val outcome: CallOutcome,
    val counterAtEvent: Int
)
