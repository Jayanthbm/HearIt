package com.jayanth.hearit.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*;
import android.os.BatteryManager
import android.service.quicksettings.TileService;
import android.widget.RemoteViews;
import com.jayanth.hearit.R;

@DrawableRes
class PriorityAnnounceTileService : TileService() {

    private lateinit var handler: android.os.Handler
    private var isRunning: Boolean = false

    override fun onStartListening() {
        super.onStartListening()
        handler = android.os.Handler(Looper.getMainLooper())
        updateTileState()
    }

    override fun onStopListening() {
        handler.removeCallbacksAndMessages(null)
        super.onStopListening()
    }

    private fun updateTileState() {
        val intent = Intent(this, javaClass).apply {
            action = AnnouncementService.ACTION_PRIORITY_ANNOUNCE_TOGGLE
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check current state from service status
        val state = stateReader.read()
        val isActive = state.priorityAnnounce.isActive

        isRunning = isActive

        val views = RemoteViews(javaClass.packageName, R.layout.qs_tile_layout)
        views.setTextViewText(R.id.tile_title, stringResource(R.string.priority_announce))
        views.setTextViewText(R.id.tile_status, if (isActive) {
            stringResource(R.string.priority_announce_on)
        } else {
            stringResource(R.string.priority_announce_off)
        })

        // Set click pending intent
        views.setOnClickPendingIntent(R.id.tile_container, pendingIntent)

        // Set state description for accessibility
        val desc = if (isActive) {
            stringResource(R.string.priority_announce_active_description)
        } else {
            stringResource(R.string.priority_announce_inactive_description)
        }
        views.contentDescription = desc

        setQsTile(
            QsTile.Builder(this)
                .setRemoteViews(views)
                .build()
        )
    }

    private fun restartListening() {
        handler.postDelayed({ updateTileState() }, 1000)
    }
}
