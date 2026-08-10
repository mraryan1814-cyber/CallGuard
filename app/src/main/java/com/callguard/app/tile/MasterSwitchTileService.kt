package com.callguard.app.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.callguard.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MasterSwitchTileService : TileService() {

    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        CoroutineScope(Dispatchers.IO).launch {
            val current = prefs.getMasterEnabledOnce()
            prefs.setMasterEnabled(!current)
            refreshTile()
        }
    }

    private fun refreshTile() {
        val enabled = runBlocking { prefs.getMasterEnabledOnce() }
        qsTile?.apply {
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (enabled) "CallGuard: ON" else "CallGuard: OFF"
            updateTile()
        }
    }
}
