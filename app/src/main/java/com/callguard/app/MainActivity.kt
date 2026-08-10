package com.callguard.app

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.callguard.app.data.Prefs
import com.callguard.app.service.CallMonitorService
import com.callguard.app.ui.CallGuardNavHost
import com.callguard.app.ui.theme.CallGuardTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(applicationContext)

        setContent {
            CallGuardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var permissionsReady by remember { mutableStateOf(false) }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) {
                        permissionsReady = true
                        restartMonitorService()
                    }

                    val roleLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartActivityForResult()
                    ) { /* role grant result - user may accept or deny */ }

                    LaunchedEffect(Unit) {
                        val permissionsNeeded = mutableListOf(android.Manifest.permission.READ_PHONE_STATE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        permissionLauncher.launch(permissionsNeeded.toTypedArray())

                        // Request to become the default Call Screening app - required
                        // for CallScreeningServiceImpl to actually receive calls.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = getSystemService(RoleManager::class.java)
                            if (roleManager != null &&
                                roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                                !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                            ) {
                                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                roleLauncher.launch(intent)
                            }
                        }
                    }

                    CallGuardNavHost(prefs = prefs)
                }
            }
        }
    }

    private fun restartMonitorService() {
        try {
            val intent = Intent(this, CallMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {
        }
    }
}
