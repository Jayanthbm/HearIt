package com.jayanth.hearit.core.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.jayanth.hearit.core.setup.SetupState
import com.jayanth.hearit.core.setup.SetupStateCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistence for first-launch progress (P11; architecture §8: DataStore holds the setup
 * completion flag).
 *
 * Separate from [SettingsRepository] on purpose: [SetupState] is process/UX state, not a
 * user preference, and keeping it in its own DataStore file means neither flow invalidates
 * the other. The same [PreferenceStore] seam keeps it testable with an in-memory fake.
 */
interface SetupStateRepository {

    /** Live state; emits fresh-install defaults on an empty file. */
    val state: Flow<SetupState>

    /** One-shot read. */
    suspend fun current(): SetupState

    /** Applies [transform] to the current value and persists the result. */
    suspend fun update(transform: (SetupState) -> SetupState)
}

/**
 * [SetupStateRepository] over a [PreferenceStore] and [SetupStateCodec].
 *
 * Single-writer, like [DefaultSettingsRepository]: the setup flow is the only writer, so a
 * plain read-modify-write is sufficient (ADR-023).
 */
class DefaultSetupStateRepository(
    private val store: PreferenceStore,
) : SetupStateRepository {

    override val state: Flow<SetupState> = store.values.map(SetupStateCodec::decode)

    override suspend fun current(): SetupState = SetupStateCodec.decode(store.read())

    override suspend fun update(transform: (SetupState) -> SetupState) {
        val next = transform(SetupStateCodec.decode(store.read()))
        store.replaceAll(SetupStateCodec.encode(next))
    }
}

/**
 * Process-wide DataStore file for setup progress. The delegate guarantees one instance per
 * file per process; consumers must use [HearItSetupDataStore.repository].
 */
private val Context.hearItSetupDataStore by preferencesDataStore(name = "hearit_setup")

/** Entry point for the persisted setup-state repository. */
object HearItSetupDataStore {

    /** Builds the repository over the single process-wide DataStore file. */
    fun repository(context: Context): SetupStateRepository = DefaultSetupStateRepository(
        DataStorePreferenceStore(context.applicationContext.hearItSetupDataStore),
    )
}
