package com.jayanth.hearit.history

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.applications.AppSettingsEditor
import com.jayanth.hearit.core.common.model.AudioFocusPolicy
import com.jayanth.hearit.core.common.model.HistoryStatus
import com.jayanth.hearit.core.common.model.HistoryStorageMode
import com.jayanth.hearit.core.common.model.HistoryUpdateHandling
import com.jayanth.hearit.core.common.model.KeywordMatchMode
import com.jayanth.hearit.core.common.model.NotificationSnapshot
import com.jayanth.hearit.core.common.model.SkipReason
import com.jayanth.hearit.core.database.HistoryQuery
import com.jayanth.hearit.core.database.HistoryRepository
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.core.rules.EligibilityDecision
import com.jayanth.hearit.core.rules.EligibilityRules
import com.jayanth.hearit.core.tts.AnnouncementFormat
import com.jayanth.hearit.core.tts.AnnouncementTemplate
import com.jayanth.hearit.core.tts.BuiltAnnouncement
import com.jayanth.hearit.core.tts.SmartContentCleaner
import com.jayanth.hearit.core.tts.TextToSpeechEngine
import com.jayanth.hearit.core.tts.UtteranceListener
import com.jayanth.hearit.service.RuntimeStateReader
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Drives the History feature (master prompt §12, phase P18).
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val engine: TextToSpeechEngine,
    private val sessionStore: SessionStore,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private var loadJob: Job? = null
    private val rules = EligibilityRules()
    private val audioManager: AudioManager? = context.getSystemService(AudioManager::class.java)
    private val stateReader: RuntimeStateReader? = audioManager?.let { RuntimeStateReader(context, it, sessionStore) }
    private var focusRequest: AudioFocusRequest? = null

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = HistorySettingsState(
                            storageMode = settings.history.storageMode,
                            updateHandling = settings.history.updateHandling,
                            retentionDays = settings.history.retentionDays,
                        ),
                    )
                }
            }
        }
        viewModelScope.launch {
            historyRepository.observeCount().collect {
                load(reset = true)
            }
        }
    }

    fun onRefresh() = load(reset = true)

    fun onQueryChanged(query: String) = updateFilter { it.copy(query = query) }

    fun onStatusChanged(status: HistoryStatus?) = updateFilter { it.copy(status = status) }

    fun onSortChanged(sort: HistorySortOrder) = updateFilter { it.copy(sort = sort) }

    fun onDatePresetSelected(preset: HistoryDatePreset) {
        val (fromMillis, toMillis) = HistoryExporter.dateBoundsForPreset(preset)
        updateFilter {
            it.copy(
                datePreset = preset,
                dateFromMillis = fromMillis,
                dateToMillis = toMillis,
            )
        }
    }

    fun onCustomDateRangeSelected(startUtcMillis: Long, endUtcMillis: Long) {
        val (fromMillis, toMillis) = HistoryExporter.normalizePickerRange(startUtcMillis, endUtcMillis)
        updateFilter {
            it.copy(
                datePreset = HistoryDatePreset.CUSTOM,
                dateFromMillis = fromMillis,
                dateToMillis = toMillis,
            )
        }
    }

    fun exportToUri(contentResolver: ContentResolver, uri: Uri, asCsv: Boolean) {
        val currentFilter = _state.value.filter
        viewModelScope.launch {
            val entries = safely {
                val all = mutableListOf<com.jayanth.hearit.core.database.HistoryEntry>()
                var offset = 0
                while (true) {
                    val page = historyRepository.page(
                        HistoryQueryMapper.toQuery(currentFilter, offset)
                            .copy(limit = HistoryQuery.MAX_PAGE_SIZE),
                    )
                    all += page
                    if (page.size < HistoryQuery.MAX_PAGE_SIZE) break
                    offset += page.size
                }
                all.toList()
            } ?: return@launch

            val content = withContext(Dispatchers.Default) {
                if (asCsv) HistoryExporter.formatCsv(entries) else HistoryExporter.formatTxt(entries)
            }
            val wrote = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    } ?: error("Cannot open output stream")
                }.isSuccess
            }
            if (wrote) {
                _userMessage.emit("Exported ${entries.size} history ${if (entries.size == 1) "entry" else "entries"}")
            } else {
                _userMessage.emit("Could not write export file")
            }
        }
    }

    private fun updateFilter(transform: (HistoryFilter) -> HistoryFilter) {
        _state.update { it.copy(filter = transform(it.filter)) }
        load(reset = true)
    }

    private fun load(reset: Boolean) {
        if (reset) loadJob?.cancel()
        if (!reset && (state.value.loading || state.value.endReached)) return
        loadJob = viewModelScope.launch {
            val offset = if (reset) 0 else state.value.items.size
            _state.update { it.copy(loading = true, unavailable = false, endReached = false) }
            try {
                val query = HistoryQueryMapper.toQuery(state.value.filter, offset)
                val page = historyRepository.page(query)
                val newItems = HistoryQueryMapper.toItems(page)
                _state.update { current ->
                    val merged = if (reset) newItems else current.items + newItems
                    current.copy(
                        items = merged,
                        loaded = true,
                        loading = false,
                        endReached = page.size < HistoryQueryMapper.PAGE_SIZE,
                        unavailable = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loaded = true, loading = false, unavailable = true) }
            }
        }
    }

    fun onLoadMore() = load(reset = false)

    fun onDeleteEntry(id: Long) {
        viewModelScope.launch {
            if (safely { historyRepository.delete(id) } != null) {
                load(reset = true)
                _userMessage.emit("Notification deleted")
            }
        }
    }

    fun onDeleteMultiple(ids: Set<Long>) {
        viewModelScope.launch {
            ids.forEach { id -> safely { historyRepository.delete(id) } }
            load(reset = true)
            _userMessage.emit("${ids.size} notifications deleted")
        }
    }

    fun onClearAll() {
        viewModelScope.launch {
            if (safely { historyRepository.deleteAll() } != null) {
                load(reset = true)
                _userMessage.emit("History cleared")
            }
        }
    }

    fun onStorageModeChanged(storageMode: HistoryStorageMode) =
        updateSettings { it.copy(storageMode = storageMode) }

    fun onUpdateHandlingChanged(updateHandling: HistoryUpdateHandling) =
        updateSettings { it.copy(updateHandling = updateHandling) }

    fun onRetentionChanged(retentionDays: Int?) =
        updateSettings { it.copy(retentionDays = retentionDays) }

    fun onPlayEntry(item: HistoryItem) {
        if (_state.value.playingItemId == item.id) {
            stopPlayback()
            return
        }
        viewModelScope.launch { playEvaluated(item, bypassedReasons = emptySet()) }
    }

    fun onConfirmPlayBypass() {
        val pending = _state.value.pendingPlayBlocked ?: return
        _state.update { it.copy(pendingPlayBlocked = null) }
        val updatedBypass = pending.bypassedReasons + pending.reason
        viewModelScope.launch { playEvaluated(pending.item, bypassedReasons = updatedBypass) }
    }

    fun onDismissPlayBlocked() {
        _state.update { it.copy(pendingPlayBlocked = null) }
    }

    // Backward-compatible no-op names if referenced elsewhere; UI will use new callbacks.
    fun onConfirmPlayDisabledApp() = onConfirmPlayBypass()
    fun onDismissPlayDisabledApp() = onDismissPlayBlocked()

    private suspend fun playEvaluated(item: HistoryItem, bypassedReasons: Set<SkipReason>) {
        stopPlayback()
        val settings = settingsRepository.settings.first()

        val snapshot = NotificationSnapshot(
            key = item.key.ifBlank { "${item.packageName}|history|${item.id}" },
            packageName = item.packageName,
            appName = item.appName.ifBlank { item.packageName },
            sender = item.sender?.takeIf { it.isNotBlank() },
            messageText = item.messageText.orEmpty(),
            channelId = item.channelId,
            category = item.category,
            postTimeMillis = if (item.postTimeMillis > 0L) item.postTimeMillis else item.recordedAtMillis,
            isUpdate = item.isUpdate,
        )

        // Keep cleaner/intelligence state fully aligned with current settings for replay.
        syncCleanerForReplay(settings, sessionStore)

        var resolved = settings.resolvedFor(item.packageName)
        var runtimeState = stateReader?.read(
            priorityAnnounceActive = false,
            isProximityCovered = true,
            lowBatteryThresholdPercent = resolved.eligibility.lowBatteryThresholdPercent,
            pauseOnPowerSaver = resolved.eligibility.lowBatteryPauseOnSaverEnabled,
        ) ?: com.jayanth.hearit.core.common.model.RuntimeState(
            screenOn = true,
            isLocked = false,
            temporaryMuteActive = false,
            priorityAnnounceActive = false,
            isCallActive = false,
            isDndActive = false,
            activeOutput = null,
            isBatteryLowOrSaver = false,
            isProximityCovered = true,
        )

        val adjusted = applyPlaybackBypasses(resolved, runtimeState, bypassedReasons)
        resolved = adjusted.first
        runtimeState = adjusted.second

        val decision = rules.evaluate(snapshot, resolved, runtimeState)
        when (decision) {
            is EligibilityDecision.Skip -> {
                _state.update { it.copy(pendingPlayBlocked = PendingPlayBlocked(item, decision.reason, bypassedReasons)) }
                return
            }
            is EligibilityDecision.Queue -> {
                // Manual history replay: allow bypassing queue-only blocks (e.g. call active) the same way.
                _state.update { it.copy(pendingPlayBlocked = PendingPlayBlocked(item, decision.reason, bypassedReasons)) }
                return
            }
            is EligibilityDecision.Announce -> Unit
        }

        if (SkipReason.SUPPRESSED_INTELLIGENCE !in bypassedReasons &&
            (SmartContentCleaner.shouldSuppressEmptyContent(snapshot.sender, snapshot.messageText) ||
                SmartContentCleaner.shouldSuppress(snapshot.messageText, snapshot.packageName))
        ) {
            _state.update { it.copy(pendingPlayBlocked = PendingPlayBlocked(item, SkipReason.SUPPRESSED_INTELLIGENCE, bypassedReasons)) }
            return
        }

        val format: AnnouncementFormat = settings.format
        val speech = settings.speechFor(item.packageName)
        val isCritical = EligibilityRules.isCriticalBreakthrough(snapshot, resolved.eligibility)
        val built = AnnouncementTemplate.build(
            snapshots = listOf(snapshot),
            format = format,
            isCritical = isCritical,
        )
        if (built.isEmpty) {
            _userMessage.emit("Nothing to play for this notification")
            return
        }

        requestAudioFocusForPlayback(resolved.eligibility.audioFocus)
        _state.update { it.copy(playingItemId = item.id) }
        val accepted = engine.speak(
            announcement = built,
            settings = speech,
            listener = object : UtteranceListener {
                override fun onComplete(announcement: BuiltAnnouncement) {
                    abandonAudioFocusPlayback()
                    _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
                }

                override fun onError(announcement: BuiltAnnouncement, error: com.jayanth.hearit.core.tts.TtsError) {
                    abandonAudioFocusPlayback()
                    _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
                }
            },
        )
        if (!accepted) {
            abandonAudioFocusPlayback()
            _state.update { if (it.playingItemId == item.id) it.copy(playingItemId = null) else it }
            _userMessage.emit("Couldn't play this notification")
        }
    }


    private fun requestAudioFocusForPlayback(policy: AudioFocusPolicy) {
        val am = audioManager ?: return
        val gain = when (policy) {
            AudioFocusPolicy.DUCK -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            AudioFocusPolicy.PAUSE -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            AudioFocusPolicy.NONE -> return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { runCatching { am.abandonAudioFocusRequest(it) } }
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val req = AudioFocusRequest.Builder(gain)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { /* best-effort for history playback */ }
                    .build()
                focusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, gain)
            }
        } catch (_: Exception) {}
    }

    private fun abandonAudioFocusPlayback() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { runCatching { am.abandonAudioFocusRequest(it) } }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (_: Exception) {}
    }

    fun onStopPlaying() {
        stopPlayback()
    }

    private fun stopPlayback() {
        engine.stop()
        abandonAudioFocusPlayback()
        _state.update { it.copy(playingItemId = null) }
    }

    fun onAppFilterChanged(packageName: String?, appName: String?) {
        updateFilter { it.copy(selectedPackageName = packageName, selectedAppName = appName) }
    }

    fun onAddFilter(keyword: String, matchMode: KeywordMatchMode, isSender: Boolean = false, targetAppPackage: String?) {
        viewModelScope.launch {
            safely {
                val formatted = AppSettingsEditor.formatKeyword(keyword, matchMode, isSender)
                val parsedNew = AppSettingsEditor.parseFilterItem(formatted)
                val settings = settingsRepository.current()

                val existingKeywords = if (targetAppPackage == null) {
                    settings.eligibility.keywordFilter.keywords
                } else {
                    (settings.appOverrides[targetAppPackage]?.eligibility?.keywordFilter ?: settings.eligibility.keywordFilter).keywords
                }

                val alreadyExists = existingKeywords.any { existing ->
                    val parsedExisting = AppSettingsEditor.parseFilterItem(existing)
                    parsedExisting.type.equals(parsedNew.type, ignoreCase = true) &&
                        parsedExisting.text.equals(parsedNew.text, ignoreCase = true)
                }

                if (alreadyExists) {
                    _userMessage.emit("Filter '${parsedNew.text}' already exists")
                    return@safely
                }

                settingsRepository.update { current ->
                    if (targetAppPackage == null) {
                        val existing = current.eligibility.keywordFilter
                        val newKeywords = if (formatted in existing.keywords) existing.keywords else existing.keywords + formatted
                        current.copy(
                            eligibility = current.eligibility.copy(
                                keywordFilter = existing.copy(
                                    enabled = true,
                                    keywords = newKeywords,
                                    mode = matchMode,
                                )
                            )
                        )
                    } else {
                        AppSettingsEditor.addKeyword(current, targetAppPackage, keyword, matchMode, isSender)
                    }
                }
                _userMessage.emit(
                    if (targetAppPackage == null) "Added global ${parsedNew.type.lowercase()} filter for '${parsedNew.text}'"
                    else "Added ${parsedNew.type.lowercase()} filter for '${parsedNew.text}'"
                )
            }
        }
    }

    private fun updateSettings(transform: (com.jayanth.hearit.core.common.model.HistorySettings) -> com.jayanth.hearit.core.common.model.HistorySettings) {
        viewModelScope.launch {
            safely { settingsRepository.update { current -> current.copy(history = transform(current.history)) } }
        }
    }

    private suspend fun <T> safely(block: suspend () -> T): T? = try {
        block()
    } catch (_: CancellationException) {
        throw CancellationException()
    } catch (_: Exception) {
        _userMessage.emit("Something went wrong")
        null
    }

    private fun skipReasonText(reason: SkipReason): String = when (reason) {
        SkipReason.APP_DISABLED -> "app is disabled"
        SkipReason.MUTED -> "temporarily muted"
        SkipReason.SCREEN_ON -> "screen is on"
        SkipReason.SCREEN_OFF -> "screen is off"
        SkipReason.UNLOCKED -> "device is unlocked"
        SkipReason.LOCKED -> "device is locked"
        SkipReason.TOO_LONG -> "message is too long"
        SkipReason.KEYWORD_FILTERED -> "filtered by keyword"
        SkipReason.PRIVACY_EXCLUDED_APP -> "excluded by privacy settings"
        SkipReason.PRIVACY_OTP -> "OTP/code content is hidden"
        SkipReason.PRIVACY_PASSWORD -> "password content is hidden"
        SkipReason.PRIVACY_PAYMENT -> "payment content is hidden"
        SkipReason.PRIVACY_BANKING -> "banking content is hidden"
        SkipReason.PRIVACY_SENSITIVE_KEYWORD -> "sensitive keyword matched"
        SkipReason.CALL_ACTIVE -> "a call is in progress"
        SkipReason.DND -> "Do Not Disturb is on"
        SkipReason.OUTPUT_NOT_ALLOWED -> "audio output not allowed"
        SkipReason.EXPIRED -> "expired"
        SkipReason.OVERFLOW -> "queue overflow"
        SkipReason.DUPLICATE_UPDATE -> "duplicate update"
        SkipReason.SUPPRESSED_INTELLIGENCE -> "filtered by active intelligence"
        SkipReason.LOW_BATTERY -> "low battery or battery saver"
        SkipReason.PROXIMITY_UNCOVERED -> "pocket/proximity check blocked playback"
        SkipReason.SPAM_COOLDOWN -> "sender spam cooldown"
        SkipReason.MANUALLY_CLEARED -> "manually cleared"
        SkipReason.SERVICE_NOT_RUNNING -> "service was not running"
    }

    override fun onCleared() {
        stopPlayback()
        super.onCleared()
    }
}
