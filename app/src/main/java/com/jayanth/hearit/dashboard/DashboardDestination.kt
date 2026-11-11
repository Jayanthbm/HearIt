package com.jayanth.hearit.dashboard

import androidx.annotation.StringRes
import com.jayanth.hearit.R

/**
 * Every destination the navigation shell can show (master prompt §13, phase P12).
 *
 * Two groups: the four bottom-navigation tabs and the five secondary entries that the
 * dashboard lists under "Settings and tools". [id] is the stable string persisted with
 * `rememberSaveable`, so a process recreation restores the same screen; names are never
 * renamed without accepting that a restored id falls back to [HOME]
 * ([fromId] is deliberately tolerant instead of throwing).
 *
 * @property labelRes Visible title; the bottom bar and the header both read it.
 * @property isBottomBarDestination True for the four tabs, false for the secondary screens.
 */
enum class DashboardDestination(
    @StringRes val labelRes: Int,
    val isBottomBarDestination: Boolean,
) {

    /** Dashboard (master prompt §13 sections). */
    HOME(R.string.nav_home, true),

    /** Applications feature (P13). */
    APPS(R.string.nav_apps, true),

    /** Settings hub (Morphe 3-tab architecture). */
    SETTINGS(R.string.nav_settings, true),

    /** Devices feature (P14). */
    DEVICES(R.string.nav_devices, false),

    /** History feature (P18). */
    HISTORY(R.string.nav_history, false),

    /** Global rules settings (P15). */
    GLOBAL_RULES(R.string.nav_global_rules, false),

    /** Voice and speech settings (P15). */
    VOICE_SPEECH(R.string.nav_voice_speech, false),

    /** App preferences: Queue, Grouping, Audio Focus (P15). */
    PREFERENCES(R.string.nav_preferences, false),

    /** Backup and restore (P19). */
    BACKUP_RESTORE(R.string.nav_backup_restore, false),

    /** Diagnostics (P19). */
    DIAGNOSTICS(R.string.nav_diagnostics, false),

    /** Active Intelligence and Controls (Phase 2 & Phase 1). */
    ACTIVE_INTELLIGENCE(R.string.nav_active_intelligence, false),
    ;

    /** Stable identifier persisted across process recreation. */
    val id: String get() = name

    companion object {

        /** Tabs, in master-prompt order. */
        val bottomBar: List<DashboardDestination> = entries.filter { it.isBottomBarDestination }

        /** Secondary entries, in master-prompt order. */
        val secondary: List<DashboardDestination> = entries.filterNot { it.isBottomBarDestination }

        /** Resolves a persisted [id]; unknown or null ids fall back to [HOME]. */
        fun fromId(id: String?): DashboardDestination =
            entries.firstOrNull { it.id == id } ?: HOME
    }
}
