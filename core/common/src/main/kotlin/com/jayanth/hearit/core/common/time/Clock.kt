package com.jayanth.hearit.core.common.time

/**
 * Injectable time source so every time-dependent rule (queue expiry, grouping windows,
 * announcement delay and minimum gap — phase P05) is deterministic and JVM-unit-testable.
 *
 * Nothing outside the platform layer creates one: the Android service backs this with a
 * monotonic source such as `SystemClock.elapsedRealtime()` (immune to wall-clock jumps),
 * while tests inject a fake. Domain code must never read the system clock directly.
 */
fun interface Clock {

    /** Current time in milliseconds on this clock's timeline. */
    fun nowMillis(): Long
}
