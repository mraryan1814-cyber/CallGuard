package com.callguard.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.callguard.app.data.CounterStore
import com.callguard.app.data.Prefs
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    prefs: Prefs,
    onOpenNumbers: () -> Unit,
    onOpenWhitelist: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val masterEnabled by prefs.masterEnabled.collectAsState(initial = true)
    val monitored by prefs.monitoredNumbers.collectAsState(initial = emptyList())
    val blockCount by prefs.blockCount.collectAsState(initial = 4)
    val counterStore = remember { CounterStore(context) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("CallGuard") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Master Switch", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (masterEnabled) "Blocking chalu hai" else "Blocking band hai (sab calls normal ring hongi)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = masterEnabled,
                        onCheckedChange = { scope.launch { prefs.setMasterEnabled(it) } }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Monitored Numbers", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            if (monitored.isEmpty()) {
                Text("Abhi koi number add nahi kiya gaya. 'Numbers' se add karein.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(monitored) { number ->
                        val state = counterStore.getState(number)
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(number, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Counter: ${state.count} / $blockCount",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavIconButton(Icons.Filled.List, "Numbers", onOpenNumbers)
                NavIconButton(Icons.Filled.Shield, "Whitelist", onOpenWhitelist)
                NavIconButton(Icons.Filled.Settings, "Settings", onOpenSettings)
                NavIconButton(Icons.Filled.History, "History", onOpenHistory)
            }
        }
    }
}

@Composable
private fun NavIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
