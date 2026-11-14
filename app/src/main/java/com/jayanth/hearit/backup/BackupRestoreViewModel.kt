package com.jayanth.hearit.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jayanth.hearit.core.datastore.SettingsRepository
import com.jayanth.hearit.service.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * ViewModel managing Backup & Restore operations (master prompt §15, phase P19).
 *
 * Provides safe export and import using Storage Access Framework (SAF) Uris.
 * Guarantees that only settings are exported/imported; notification content and
 * history are strictly excluded.
 */
@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreUiState())
    val state: StateFlow<BackupRestoreUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect {
                refreshExportJson()
            }
        }
    }

    private suspend fun getFullBackupJson(): String {
        val baseJson = settingsRepository.exportSettings()
        return try {
            val json = JSONObject(baseJson)
            val rules = sessionStore.exportActiveRulesJson()
            json.put("activeRules", rules)
            json.toString(2)
        } catch (_: Exception) {
            baseJson
        }
    }

    private suspend fun refreshExportJson() {
        val json = getFullBackupJson()
        _state.update { it.copy(currentSettingsJson = json) }
    }

    fun onExportToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            try {
                val json = getFullBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream for selected file")
                }
                _state.update {
                    it.copy(
                        currentSettingsJson = json,
                        lastExportTimeMillis = System.currentTimeMillis(),
                        statusMessage = "Backup successfully exported to file.",
                        isError = false,
                        isExporting = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        statusMessage = "Export failed: ${e.message ?: "Unknown error"}",
                        isError = true,
                        isExporting = false,
                    )
                }
            }
        }
    }

    fun onImportFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            try {
                val content = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Unable to open input stream for selected file")
                }
                importJsonContent(content)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        statusMessage = "Import failed: ${e.message ?: "Could not read file"}",
                        isError = true,
                        isImporting = false,
                    )
                }
            }
        }
    }

    fun onApplyRawJson(json: String) {
        viewModelScope.launch {
            _state.update { it.copy(isImporting = true) }
            importJsonContent(json)
        }
    }

    private suspend fun importJsonContent(content: String) {
        if (content.isBlank()) {
            _state.update {
                it.copy(
                    statusMessage = "Selected file is empty.",
                    isError = true,
                    isImporting = false,
                )
            }
            return
        }

        try {
            val parsed = JSONObject(content)
            val version = parsed.optInt("version", 0)
            if (version < 1 || !parsed.has("settings")) {
                _state.update {
                    it.copy(
                        statusMessage = "Invalid backup format: missing required version or settings structure.",
                        isError = true,
                        isImporting = false,
                    )
                }
                return
            }

            if (parsed.has("activeRules")) {
                val rulesJson = parsed.optJSONObject("activeRules")
                if (rulesJson != null) {
                    sessionStore.importActiveRulesJson(rulesJson)
                }
            }

            settingsRepository.importSettings(content)
            refreshExportJson()
            _state.update {
                it.copy(
                    statusMessage = "Settings successfully restored from backup!",
                    isError = false,
                    isImporting = false,
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    statusMessage = "Failed to parse backup JSON: ${e.message}",
                    isError = true,
                    isImporting = false,
                )
            }
        }
    }

    fun onClearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }
}
