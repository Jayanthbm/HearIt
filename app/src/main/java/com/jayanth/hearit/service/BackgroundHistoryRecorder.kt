package com.jayanth.hearit.service

import com.jayanth.hearit.core.common.logger.HearItLogger
import com.jayanth.hearit.core.common.logger.LogCategory
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.HearItSettings
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.notifications.NotificationRelay
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Records notifications into history when the announcement service is not running.
 *
 * The listener forwards every posted snapshot through [NotificationRelay]; when no
 * [NotificationRelay.sink] is registered (announcement service inactive), this recorder
 * persists the snapshot so background notifications still appear in history.
 */
@Singleton
class BackgroundHistoryRecorder @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val latestSettings = MutableStateFlow(HearItSettings())

    fun start() {
        scope.launch {
            settingsRepository.settings.collect { latestSettings.value = it }
        }
    }

    fun onNotificationPosted(snapshot: NotificationSnapshot) {
        // If the announcement service is active, it owns recording for this snapshot.
        if (NotificationRelay.sink != null) return

        val settings = latestSettings.value
        scope.launch {
            try {
                historyRepository.record(
                    snapshot = snapshot,
                    settings = settings.historyFor(snapshot.packageName),
                    status = HistoryStatus.SKIPPED,
                    reason = SkipReason.SERVICE_NOT_RUNNING,
                    priority = settings.priorityFor(snapshot.packageName),
                )
            } catch (e: Exception) {
                HearItLogger.warn(
                    LogCategory.NOTIFICATION,
                    "Background history record failed for pkg=${snapshot.packageName}: ${e.message}",
                )
            }
        }
    }
}
