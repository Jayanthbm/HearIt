package com.jayanth.hearit.service

import android.content.Context

/**
 * Minimal persisted session state for the announcement service.
 *
 * Placeholder until P09 lands `:core:datastore` (typed DataStore settings): it exists now
 * because two service behaviours need durable state immediately —
 *  - [isStarted]: the user explicitly started HearIt, so a process recreated by the system
 *    may resume, but a fresh install never announces on its own (architecture §4);
 *  - [isTemporaryMuteActive]: read by [RuntimeStateReader] as the mute override; the real
 *    mute control arrives with the reading controls (P16).
 *
 * Backed by `SharedPreferences` on purpose (synchronous, tiny, service-lifecycle friendly).
 * P09 replaces this with the settings repository; nothing else may read this file.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    /** True when the user has explicitly started announcements (persisted). */
    var isStarted: Boolean
        get() = prefs.getBoolean(KEY_STARTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_STARTED, value).apply()
        }

    /** True while temporary mute is active (P16 owns the control that changes it). */
    var isTemporaryMuteActive: Boolean
        get() = prefs.getBoolean(KEY_TEMPORARY_MUTE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TEMPORARY_MUTE, value).apply()
        }

    private companion object {
        const val FILE_NAME = "hearit_session"
        const val KEY_STARTED = "started"
        const val KEY_TEMPORARY_MUTE = "temporary_mute"
    }
}
