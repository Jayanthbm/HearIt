package com.jayanth.hearit.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jayanth.hearit.service.AnnouncementService

/**
 * The dashboard's only way to change service state (master prompt §4: the user explicitly
 * starts and stops Hear It).
 *
 * Kept as an interface so the ViewModel performs no framework calls and the intent
 * contract stays in one place; the P08 actions are reused unchanged.
 */
interface ServiceControl {

    /** Starts the announcement service (foreground). */
    fun startAnnouncements()

    /** Stops the announcement service and clears its persisted session flag. */
    fun stopAnnouncements()
}

/** [ServiceControl] over explicit intents; nothing here starts anything implicitly. */
class AndroidServiceControl(private val context: Context) : ServiceControl {

    override fun startAnnouncements() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_START),
        )
    }

    override fun stopAnnouncements() {
        // Sent as a command rather than via stopService(): the service clears
        // SessionStore.isStarted in onStartCommand, and a bare stopService() would skip that
        // and leave the install looking started after the process is gone.
        context.startService(
            Intent(context, AnnouncementService::class.java).setAction(AnnouncementService.ACTION_STOP),
        )
    }
}
