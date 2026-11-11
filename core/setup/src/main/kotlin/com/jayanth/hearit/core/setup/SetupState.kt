package com.jayanth.hearit.core.setup

/**
 * Persisted progress through the setup flow (architecture §8: DataStore holds the setup
 * completion flag; P11 adds the per-step progress that makes back navigation and process
 * death lossless).
 *
 * @property completed True only after the user pressed Start on the review step. A
 *   returning user therefore opens the dashboard, and a fresh install always sees setup
 *   (master prompt §4: "Returning users open Dashboard").
 * @property currentStep Step to restore on relaunch; [SetupFlow.resolveVisibleStep] guards
 *   against a step that this API level hides.
 * @property visitedSteps Steps the user has actually seen, so the review step can disclose
 *   what was skipped and a future "resume setup" can jump to the first unseen step.
 * @property skippedSteps Steps passed without meeting their prerequisite.
 * @property batteryGuidanceAcknowledged Set when the user confirmed the background guidance
 *   on the battery step; kept separately so later screens can tell "was shown" from "was
 *   acknowledged" without re-asking.
 */
data class SetupState(
    val completed: Boolean = false,
    val currentStep: SetupStep = SetupStep.WELCOME,
    val visitedSteps: Set<SetupStep> = emptySet(),
    val skippedSteps: Set<SetupStep> = emptySet(),
    val batteryGuidanceAcknowledged: Boolean = false,
) {

    /** Records that [step] is now the step on screen (and has been seen). */
    fun visiting(step: SetupStep): SetupState = copy(
        currentStep = step,
        visitedSteps = visitedSteps + step,
    )

    /** Records that [step] was passed without meeting its prerequisite. */
    fun skipping(step: SetupStep): SetupState = copy(
        skippedSteps = skippedSteps + step,
        visitedSteps = visitedSteps + step,
    )

    /** Marks the whole flow complete and moves the stored step to the review step. */
    fun completing(): SetupState = copy(
        completed = true,
        currentStep = SetupStep.REVIEW,
        visitedSteps = visitedSteps + SetupStep.REVIEW,
    )

    /** True when the user skipped [step] on this device. */
    fun wasSkipped(step: SetupStep): Boolean = step in skippedSteps
}
