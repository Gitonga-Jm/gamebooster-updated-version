package com.gameboost.pro.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.gameboost.pro.MainActivity

class QuickSettingsTile : TileService() {
    override fun onTileAdded() { updateTile() }
    
    override fun onStartListening() { updateTile() }
    
    override fun onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = "BOOST_QUICK"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(intent)
        }
        updateTile()
    }
    
    private fun updateTile() {
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            label = "GameBoost"
            updateTile()
        }
    }
}
