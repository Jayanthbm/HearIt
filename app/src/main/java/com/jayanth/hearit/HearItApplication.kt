package com.jayanth.hearit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class and Hilt dependency-injection root (architecture §3, ADR-001).
 *
 * Every Hilt-backed component in the app is rooted here (activities via
 * `@AndroidEntryPoint`; services the same when they arrive in P07/P08). Constructor
 * injection and Hilt modules only — no service locator, no global mutable state
 * (global standards).
 *
 * Deliberately empty beyond [HiltAndroidApp]: initialization that depends on settings or
 * stored state belongs in the components that own it, not in a startup god-object.
 * Not locally verified — written under the cloud-agent restriction (no build/test run).
 */
@HiltAndroidApp
class HearItApplication : Application()
