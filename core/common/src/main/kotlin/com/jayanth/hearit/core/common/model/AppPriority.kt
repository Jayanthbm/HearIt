package com.jayanth.hearit.core.common.model

/**
 * Configurable per-app announcement priority (master prompt §8): Critical > High > Normal > Low.
 *
 * [rank] makes ordering explicit for the queue (ADR-019): a higher rank is announced
 * before a lower rank among pending items, and equal ranks are FIFO by arrival (ADR-005).
 * Ranks are contiguous and increasing so comparisons never depend on declaration order.
 */
enum class AppPriority(val rank: Int) {

    /** Announced before every other tier. */
    CRITICAL(3),

    /** Announced before Normal and Low. */
    HIGH(2),

    /** Default tier for every app. */
    NORMAL(1),

    /** Announced after every other tier. */
    LOW(0),
}
