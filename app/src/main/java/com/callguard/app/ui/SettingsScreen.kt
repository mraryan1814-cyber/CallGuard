package com.callguard.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callguard.app.data.Prefs
import com.callguard.app.util.CsvUtils
import kotlinx.coroutines.launch

private val COUNT_OPTIONS = listOf(3, 4, 5, 7)
private val GAP_OPTIONS = listOf(1, 2, 5)

@Composable
fun SettingsScreen(prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val blockCount by prefs.blockCount.collectAsState(initial = 4)
    val gapMinutes by prefs.timeGapMinutes.collectAsState(initial = 2)
    val silentMode by prefs.silentMode.collectAsState(initial = false)
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val monitored = prefs.getMonitoredNumbersOnce()
                val whitelist = prefs.getWhitelistNumbersOnce()
                val csv = CsvUtils.buildCsv(monitored, whitelist)
                CsvUtils.writeToUri(context, uri, csv)
                snackbarMessage = "Backup export ho gaya"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val parsed = CsvUtils.readFromUri(context, uri)
                parsed.monitored.forEach { prefs.addMonitoredNumber(it) }
                parsed.whitelist.forEach { prefs.addWhitelistNumber(it) }
                snackbarMessage = "Restore ho gaya (${parsed.monitored.size} monitored, ${parsed.whitelist.size} whitelist)"
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Block Count (kitni calls ke baad ring hogi)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                COUNT_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = blockCount == option,
                        onClick = { scope.launch { prefs.setBlockCount(option) } },
                        label = { Text("$option") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Time Gap (minutes) - isse zyada gap par counter reset", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                GAP_OPTIONS.forEach { option ->
                    FilterChip(
                        selected = gapMinutes == option,
                        onClick = { scope.launch { prefs.setTimeGapMinutes(option) } },
                        label = { Text("$option min") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Silent Block", style = MaterialTheme.typography.titleMedium)
                    Text("Call kaatne ki bajaye sirf silent kar de", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = silentMode,
                    onCheckedChange = { scope.launch { prefs.setSilentMode(it) } }
                )
            }

            Spacer(Modifier.height(32.dp))
            Text("Backup / Restore", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = { exportLauncher.launch("callguard_backup.csv") }) {
                    Text("Export CSV")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("text/*", "text/comma-separated-values")) }) {
                    Text("Import CSV")
                }
            }
        }
    }
}
