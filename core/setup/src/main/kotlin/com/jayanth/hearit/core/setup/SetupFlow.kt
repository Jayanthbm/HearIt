package com.jayanth.hearit.core.setup

/**
 * The complete setup navigation and gating contract (master prompt §4, P11 acceptance:
 * "no silent service start; Start enabled only when prerequisites are met; back navigation
 * preserves state").
 *
 * Pure and stateless: the caller (the setup ViewModel) owns the persisted [SetupState] and
 * the current [SetupPrerequisites]; this object only decides what may happen next. The
 * setup UI must never decide navigation or availability itself.
 *
 * Rules:
 *  - Steps are walked through [visibleSteps], which hides [SetupStep.BLUETOOTH_PERMISSION]
 *    on Android versions that never ask for the permission.
 *  - A step may be advanced when it has no blockers **or** it is
 *    [SetupStep.isSkippable]; skipping is recorded by the caller so the review step can
 *    disclose it.
 *  - [startBlockers] is the authority for enabling Start. It ignores skippable-step
 *    deficits (for example the Bluetooth permission), because none of them prevents
 *    announcements from working.
 */
object SetupFlow {

    /** All documented steps, including ones hidden at this API level. */
    val orderedSteps: List<SetupStep>
        get() = SetupStep.ordered

    /** Steps actually shown on this device, in order. */
    fun visibleSteps(prerequisites: SetupPrerequisites): List<SetupStep> =
        SetupStep.ordered.filter { step ->
            step != SetupStep.BLUETOOTH_PERMISSION || prerequisites.bluetoothStepRequired
        }

    /**
     * Unmet prerequisites for [step], in a stable order.
     *
     * Empty for purely informational steps and for steps this device hides.
     */
    fun blockersFor(step: SetupStep, prerequisites: SetupPrerequisites): List<SetupBlocker> =
        when (step) {
            SetupStep.WELCOME -> emptyList()
            SetupStep.NOTIFICATION_ACCESS -> if (prerequisites.notificationAccessGranted) {
                emptyList()
            } else {
                listOf(SetupBlocker.NotificationAccessMissing)
            }
            SetupStep.BLUETOOTH_PERMISSION -> if (prerequisites.bluetoothPermissionMissing) {
                listOf(SetupBlocker.BluetoothPermissionMissing)
            } else {
                emptyList()
            }
            SetupStep.TEXT_TO_SPEECH -> ttsBlockers(prerequisites)
            SetupStep.BATTERY_GUIDANCE -> emptyList()
            SetupStep.APP_SELECTION -> if (prerequisites.hasEnabledApps) {
                emptyList()
            } else {
                listOf(SetupBlocker.NoEnabledApps)
            }
            SetupStep.AUDIO_DEVICES -> if (prerequisites.hasAllowedOutput) {
                emptyList()
            } else {
                listOf(SetupBlocker.NoAllowedOutput)
            }
            SetupStep.TEST_ANNOUNCEMENT -> emptyList()
            SetupStep.REVIEW -> startBlockers(prerequisites)
        }

    /**
     * True when the user may leave [step] forward: either its prerequisites are met, or
     * the step is skippable (the UI then labels the action "Skip").
     */
    fun canAdvanceFrom(step: SetupStep, prerequisites: SetupPrerequisites): Boolean =
        step.isSkippable || blockersFor(step, prerequisites).isEmpty()

    /** True when [step] can be passed only by skipping it right now. */
    fun canOnlySkip(step: SetupStep, prerequisites: SetupPrerequisites): Boolean =
        !canAdvanceFromNormally(step, prerequisites) && step.isSkippable

    /** The next visible step, or null on the last one. */
    fun nextStep(step: SetupStep, prerequisites: SetupPrerequisites): SetupStep? {
        val steps = visibleSteps(prerequisites)
        val index = steps.indexOf(step)
        return if (index < 0 || index >= steps.lastIndex) null else steps[index + 1]
    }

    /** The previous visible step, or null on the first one. */
    fun previousStep(step: SetupStep, prerequisites: SetupPrerequisites): SetupStep? {
        val steps = visibleSteps(prerequisites)
        val index = steps.indexOf(step)
        return if (index <= 0) null else steps[index - 1]
    }

    /** True when [step] is the final step before Start. */
    fun isFinalStep(step: SetupStep): Boolean = step == SetupStep.REVIEW

    /**
     * Everything that must hold before Start is offered (master prompt §4). Skippable
     * deficits are deliberately absent: the Bluetooth permission, battery guidance, output
     * selection beyond one category and the test announcement never gate Start.
     */
    fun startBlockers(prerequisites: SetupPrerequisites): List<SetupBlocker> = buildList {
        if (!prerequisites.notificationAccessGranted) add(SetupBlocker.NotificationAccessMissing)
        addAll(ttsBlockers(prerequisites))
        if (!prerequisites.hasEnabledApps) add(SetupBlocker.NoEnabledApps)
        if (!prerequisites.hasAllowedOutput) add(SetupBlocker.NoAllowedOutput)
    }

    /** True only when Start may be offered — the single source of truth for the Start button. */
    fun canStart(prerequisites: SetupPrerequisites): Boolean = startBlockers(prerequisites).isEmpty()

    /**
     * The step to actually show for [requested]: if it is hidden at this API level (for
     * example a persisted `BLUETOOTH_PERMISSION` on Android 10), the nearest earlier visible
     * step is used so restoring state can never land on an invisible screen.
     */
    fun resolveVisibleStep(requested: SetupStep, prerequisites: SetupPrerequisites): SetupStep {
        val steps = visibleSteps(prerequisites)
        if (requested in steps) return requested
        return steps.lastOrNull { it.order < requested.order } ?: steps.first()
    }

    /** True when the step's own prerequisites are met (ignoring its skippability). */
    private fun canAdvanceFromNormally(step: SetupStep, prerequisites: SetupPrerequisites): Boolean =
        blockersFor(step, prerequisites).isEmpty()

    private fun ttsBlockers(prerequisites: SetupPrerequisites): List<SetupBlocker> = when {
        !prerequisites.ttsEngineAvailable -> listOf(SetupBlocker.TtsEngineUnavailable)
        !prerequisites.ttsLanguageAvailable -> listOf(SetupBlocker.TtsLanguageMissing)
        else -> emptyList()
    }
}
