package com.sasayaki.service

import android.Manifest
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.sasayaki.MainActivity
import com.sasayaki.R

class BubbleTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        when {
            BubbleService.isRunning -> BubbleService.stop(this)
            !canRunBubble() -> openApp(startBubble = false)
            else -> try {
                BubbleService.start(this)
            } catch (e: IllegalStateException) {
                // Android may refuse a foreground start from a tile; the app can start it
                // from the foreground instead.
                Log.w(TAG, "Could not start the bubble from the tile", e)
                openApp(startBubble = true)
            }
        }
        refresh()
    }

    private fun canRunBubble(): Boolean =
        Settings.canDrawOverlays(this) &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun openApp(startBubble: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .apply { if (startBubble) action = MainActivity.ACTION_START_BUBBLE }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val running = BubbleService.isRunning
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (running) R.string.tile_on else R.string.tile_off)
        }
        tile.updateTile()
    }

    companion object {
        private const val TAG = "BubbleTileService"

        /** Asks the system to refresh the tile after the bubble starts or stops elsewhere. */
        fun requestRefresh(context: Context) {
            requestListeningState(context, ComponentName(context, BubbleTileService::class.java))
        }
    }
}
