package com.jayanth.hearit.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.jayanth.hearit.R
import com.jayanth.hearit.dashboard.ServiceStatusMonitor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Quick Settings tile for Priority Announce (P19b, master prompt §10).
 *
 * The tile is **only a remote control**: it reflects the in-process
 * [ServiceStatusMonitor] and forwards a toggle to [AnnouncementService], which owns the
 * mode, its 24-hour expiry and its persistence. Nothing is announced, started or stored
 * here, and the tile is a no-op while announcements are stopped — Priority Announce only
 * changes which apps may bypass screen/lock, so it has no meaning when nothing runs
 * (master prompt §4: no silent service start).
 *
 * API note: [Tile] replaced `QsTile` in API 33, and the compile SDK no longer contains
 * `QsTile`, so the on/off state is only reflected on API 33+. On API 29–32 the tile keeps
 * its manifest label and still toggles the mode when tapped; only the subtitle/state
 * reflection is unavailable there.
 */
@AndroidEntryPoint
class PriorityAnnounceTileService : TileService() {

    @Inject
    lateinit var serviceStatus: ServiceStatusMonitor

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        // Forward only while the service is alive: starting it from a tile would be a
        // silent start, which master prompt §4 forbids.
        if (serviceStatus.state.value.isRunning) {
            startService(
                Intent(this, AnnouncementService::class.java)
                    .setAction(AnnouncementService.ACTION_PRIORITY_ANNOUNCE_TOGGLE),
            )
        }
        updateTile()
    }

    /** Reflects the current mode; safe to call on every listen/click. */
    private fun updateTile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val tile: Tile = qsTile ?: return
        val active = serviceStatus.state.value.isPriorityAnnounceActive

        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.priority_announce)
        tile.subtitle = getString(
            if (active) R.string.priority_announce_on else R.string.priority_announce_off,
        )
        tile.contentDescription = getString(
            if (active) {
                R.string.priority_announce_active_description
            } else {
                R.string.priority_announce_inactive_description
            },
        )
        tile.updateTile()
    }
}
