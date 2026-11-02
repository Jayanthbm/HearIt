package com.jayanth.hearit.core.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import org.json.JSONObject

/**
 * Typed access to the user's settings (P09).
 *
 * Consumers observe [settings] or call [current]; every write goes through [update] with a
 * pure transform, so no caller ever manipulates raw keys. Per-app inheritance is resolved
 * by [HearItSettings]'s typed accessors (`resolvedFor`, `queueFor`, `historyFor`,
 * `priorityFor`), never by hand-merging in UI or pipeline code.
 */
interface SettingsRepository {

    /** Live settings; emits the defaults on an empty store. */
    val settings: Flow<HearItSettings>

    /** One-shot read of the current settings. */
    suspend fun current(): HearItSettings

    /** Applies [transform] to the current value and persists the result. */
    suspend fun update(transform: (HearItSettings) -> HearItSettings)

    /** Exports the current settings as JSON for backup (P19). */
    suspend fun exportSettings(): String

    /** Imports settings from JSON, replacing the current settings (P19). */
    suspend fun importSettings(json: String)
}

/**
 * [SettingsRepository] over a [PreferenceStore] and [SettingsCodec].
 *
 * [update] is a single-writer read-modify-write: the settings UI owns writes, so no
 * compare-and-set is needed yet. If concurrent writers ever appear, add it behind
 * [PreferenceStore] (ADR-021) rather than changing the codec.
 */
class DefaultSettingsRepository(
    private val store: PreferenceStore,
) : SettingsRepository {

    override val settings: Flow<HearItSettings> = store.values.map(SettingsCodec::decode)

    override suspend fun current(): HearItSettings = SettingsCodec.decode(store.read())

    override suspend fun update(transform: (HearItSettings) -> HearItSettings) {
        val next = transform(SettingsCodec.decode(store.read()))
        store.replaceAll(SettingsCodec.encode(next))
    }

    override suspend fun exportSettings(): String {
        val settings = SettingsCodec.decode(store.read())
        return JSONObject().apply {
            put("version", 1)
            put("settings", SettingsCodec.encode(settings))
        }.toString()
    }

    override suspend fun importSettings(json: String) {
        try {
            val parsed = JSONObject(json)
            val version = parsed.optInt("version", 0)
            if (version >= 1) {
                val settings = SettingsCodec.decode(parsed.getJSONObject("settings"))
                store.replaceAll(SettingsCodec.encode(settings))
            }
        } catch (e: Exception) {
            // Invalid JSON - silently fail; UI will surface the error
        }
    }
}

/**
 * Process-wide DataStore file for settings. The delegate guarantees **one instance per
 * file per process**, which DataStore requires; consumers must go through
 * [HearItSettingsDataStore.repository] rather than creating their own store.
 */
private val Context.hearItSettingsDataStore by preferencesDataStore(name = "hearit_settings")

/** Entry point for the real (persisted) repository. */
object HearItSettingsDataStore {

    /** Builds the repository backed by the single process-wide DataStore file. */
    fun repository(context: Context): SettingsRepository = DefaultSettingsRepository(
        DataStorePreferenceStore(context.applicationContext.hearItSettingsDataStore),
    )
}
