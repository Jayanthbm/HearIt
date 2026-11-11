package com.jayanth.hearit.core.setup

/**
 * Flat string-map codec for [SetupState], stored through `:core:datastore`'s
 * [com.jayanth.hearit.core.datastore.PreferenceStore] seam (P11).
 *
 * Pure Kotlin so the persistence contract is JVM-tested: the DataStore file is only a map
 * of strings, and every field here has a documented default when missing.
 *
 * Rules (mirroring [com.jayanth.hearit.core.datastore.SettingsCodec]):
 *  - every non-collection field is always encoded;
 *  - sets use the ASCII unit separator and are sorted for stable output;
 *  - decode is tolerant: unknown keys and enum names are ignored, malformed booleans fall
 *    back to the documented default, so a newer or corrupted store can never crash launch.
 */
object SetupStateCodec {

    /** Schema marker; bump with a migration when a key changes meaning. */
    const val SCHEMA_VERSION: Int = 1

    private const val LIST_SEPARATOR = "\u001F"

    private const val KEY_VERSION = "setup.version"
    private const val KEY_COMPLETED = "setup.completed"
    private const val KEY_STEP = "setup.step"
    private const val KEY_VISITED = "setup.visited"
    private const val KEY_SKIPPED = "setup.skipped"
    private const val KEY_BATTERY_ACK = "setup.batteryAcknowledged"

    /** Encodes [state]; always writes every key. */
    fun encode(state: SetupState): Map<String, String> = mapOf(
        KEY_VERSION to SCHEMA_VERSION.toString(),
        KEY_COMPLETED to state.completed.toString(),
        KEY_STEP to state.currentStep.name,
        KEY_VISITED to encodeSteps(state.visitedSteps),
        KEY_SKIPPED to encodeSteps(state.skippedSteps),
        KEY_BATTERY_ACK to state.batteryGuidanceAcknowledged.toString(),
    )

    /** Decodes a stored map; missing or invalid values restore [SetupState]'s defaults. */
    fun decode(values: Map<String, String>): SetupState = SetupState(
        completed = values[KEY_COMPLETED]?.toBooleanStrictOrNull() ?: false,
        currentStep = values[KEY_STEP]?.toSetupStep() ?: SetupStep.WELCOME,
        visitedSteps = decodeSteps(values[KEY_VISITED]),
        skippedSteps = decodeSteps(values[KEY_SKIPPED]),
        batteryGuidanceAcknowledged = values[KEY_BATTERY_ACK]?.toBooleanStrictOrNull() ?: false,
    )

    private fun encodeSteps(steps: Set<SetupStep>): String =
        steps.map { it.name }.sorted().joinToString(LIST_SEPARATOR)

    private fun decodeSteps(raw: String?): Set<SetupStep> =
        raw?.takeIf { it.isNotEmpty() }
            ?.split(LIST_SEPARATOR)
            ?.mapNotNull { it.toSetupStep() }
            ?.toSet()
            ?: emptySet()

    private fun String.toSetupStep(): SetupStep? =
        SetupStep.ordered.firstOrNull { it.name == this }
}
