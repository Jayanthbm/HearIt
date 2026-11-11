package com.jayanth.hearit.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Persistence seam for [SettingsCodec]'s flat map.
 *
 * DataStore is the only production implementation (master prompt §16: "DataStore for
 * preferences"), but the seam keeps the repository logic testable with an in-memory fake
 * and keeps `:core:datastore`'s settings behavior independent of coroutine timing.
 */
interface PreferenceStore {

    /** Current values, re-emitted whenever the store changes. */
    val values: Flow<Map<String, String>>

    /** One-shot read (used by `update` to compute the next state atomically enough). */
    suspend fun read(): Map<String, String>

    /** Atomically replaces the whole settings map. */
    suspend fun replaceAll(values: Map<String, String>)
}

/**
 * [PreferenceStore] over a Preferences [DataStore]. Only string entries are read back;
 * everything the codec writes is a string, and unknown non-string entries are ignored
 * rather than crashing a future migration.
 */
class DataStorePreferenceStore(
    private val dataStore: DataStore<Preferences>,
) : PreferenceStore {

    override val values: Flow<Map<String, String>> = dataStore.data.map(::toFlatMap)

    override suspend fun read(): Map<String, String> = values.first()

    override suspend fun replaceAll(values: Map<String, String>) {
        dataStore.edit { preferences ->
            preferences.clear()
            values.forEach { (key, value) ->
                preferences[stringPreferencesKey(key)] = value
            }
        }
    }

    private fun toFlatMap(preferences: Preferences): Map<String, String> =
        preferences.asMap().entries.mapNotNull { (key, value) ->
            (value as? String)?.let { key.name to it }
        }.toMap()
}
