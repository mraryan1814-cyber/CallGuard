package com.callguard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callguard.app.data.AppDatabase
import com.callguard.app.data.CallOutcome
import com.callguard.app.data.HistoryEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val historyFlow = remember { db.historyDao().getAll() }
    val history by historyFlow.collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { db.historyDao().clearAll() }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear all")
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Abhi tak koi history nahi hai")
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
                items(history) { entry ->
                    HistoryRow(entry, dateFormat)
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntity, dateFormat: SimpleDateFormat) {
    val (label, color) = when (entry.outcome) {
        CallOutcome.BLOCKED -> "BLOCKED" to MaterialTheme.colorScheme.error
        CallOutcome.SILENCED -> "SILENCED" to MaterialTheme.colorScheme.error
        CallOutcome.ALLOWED -> "ALLOWED (RING)" to MaterialTheme.colorScheme.primary
        CallOutcome.WHITELISTED -> "WHITELISTED" to MaterialTheme.colorScheme.primary
        CallOutcome.IGNORED -> "IGNORED" to MaterialTheme.colorScheme.outline
    }

    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(entry.phoneNumber, style = MaterialTheme.typography.bodyLarge)
                Text(label, color = color, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "${dateFormat.format(Date(entry.timestampMillis))} · counter=${entry.counterAtEvent}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
