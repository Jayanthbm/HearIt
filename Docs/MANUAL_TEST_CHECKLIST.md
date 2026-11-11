# Manual Testing Checklist

Emulator and physical-device testing checklist, including Android versions and manufacturer compatibility.
Track results in three buckets: **Untested** · **Emulator-tested** · **Physical-device-tested**. Never mark a row device-tested without doing it. Automated test results belong in `AI_BUILD_PROGRESS_LOCAL.md`, not here.

## Device matrix (minimum)

| Device / emulator | Android version | API | Type | Status |
|---|---|---|---|---|
| Emulator | Android 10 | 29 | Emulator (minSdk) | Untested |
| Emulator | Latest Android | 37 | Emulator | Untested |
| Pixel (or stock Android) | Recent | 33+ | Physical | Untested |
| Samsung Galaxy | Recent | 33+ | Physical | Untested |
| OnePlus Nord (AC2001) | Android 12 | 31 | Physical (OxygenOS OEM) | Physical-device-tested |

## 1. Setup and onboarding

- [x] First launch shows welcome and explains the app purpose. (Physical-device-tested)
- [x] Notification access step opens the system listener-settings screen; returning with access granted advances the flow. (Physical-device-tested)
- [x] Declining/skipping a skippable step is possible and clearly marked as skipped. (Physical-device-tested)
- [x] Bluetooth/nearby-device permission prompt matches the OS version (BLUETOOTH_CONNECT on 31+, none for wired). (Physical-device-tested)
- [x] TTS step reports engine availability, and reports missing language data honestly (no false "ready"). (Physical-device-tested)
- [x] Background/battery guidance does not promise guaranteed background execution; manufacturer guidance appears only where justified. (Physical-device-tested)
- [x] App-selection step starts with **all apps disabled**. (Physical-device-tested)
- [x] Audio-device step lists previously connected Bluetooth devices, a generic Wired option, phone speaker off by default. (Physical-device-tested)
- [x] Test announcement plays through the chosen output and does **not** create a history entry or touch the queue. (Physical-device-tested)
- [x] Review step requires an explicit Start; opening the app later never silently starts the service. (Physical-device-tested)
- [x] Back/forward navigation preserves state on every step. (Physical-device-tested)

## 2. Service and dashboard

- [ ] Dashboard shows Service Active/Inactive, current audio device, normal ON/OFF, Priority ON/OFF + remaining time, mute status, current speaking item, queue status, recent history.
- [ ] Start is blocked with a clear reason when no app is enabled or prerequisites are missing.
- [ ] Stop ends speech and removes the foreground notification promptly.
- [ ] Persistent notification shows only configured actions; defaults: pause/resume, stop announcements, Priority toggle.
- [ ] Force-stop the app, relaunch: state is consistent; no announcement happens until Start is pressed again.
- [ ] Process death while speaking: service recreation is safe, no crash, no duplicate announcement.

## 3. App selection

- [ ] Installed notification-posting apps discovered with icon and name.
- [ ] Search, Select All, Deselect All work.
- [ ] Enabling/disabling persists across restart.
- [ ] Per-app settings show clearly when a value inherits the global value, and when it overrides it.

## 4. Eligibility rules (each verified while speaking and while idle)

- [ ] Screen ON + not locked → skipped (`SCREEN_ON` / `UNLOCKED`); screen OFF + locked → announced (default AND logic).
- [ ] Independent Screen (ON/OFF/Both) and Lock (Locked/Unlocked/Both) settings honoured per app.
- [ ] Speech stops immediately when the screen turns on or the phone unlocks mid-sentence.
- [ ] Priority Announce on → screen/lock restrictions bypassed only for apps in the Priority list; all other rules still apply.
- [ ] Text longer than limit → whole item skipped, never a truncated reading; per-message vs whole-notification scope honoured.
- [ ] Keyword filtering off by default; enabling it (case-insensitive, whole-word option) skips matching items with reason.
- [ ] OTP, banking, password, payment rules skip with their own reasons; hide-content mode speaks app/sender only; excluded apps never speak.
- [ ] DND respected by default; global and per-app override work; silent notification channel is not misreported as DND.
- [ ] Never speaks during an active call; call-time queueing (when enabled) re-evaluates all rules afterwards.
- [ ] Mute active → nothing speaks (all modes), including Priority; expiry restores announcements.

## 5. Audio devices and focus

- [ ] Only allowed outputs trigger announcements; selecting a different output stops speech.
- [ ] Selected earphone disconnects mid-speech → immediate stop, no pause/resume, no speaker fallback.
- [ ] Missing selected output applies configured Skip or Queue behaviour.
- [ ] Duck / Pause / No intervention modes behave as configured; prior audio behaviour restored afterwards.
- [ ] No promise of routing to a specific BT device unless verified on the tested device; limitations visible in UI.

## 6. Queue, grouping and controls

- [ ] Same-app notifications within the window group: app name + count, then content; max messages per announcement respected.
- [ ] Configurable delay before announcement and minimum gap between announcements respected.
- [ ] Higher-priority pending items jump ahead; equal priority stays FIFO; current speech is not interrupted by default.
- [ ] Queue limits, expiry and overflow policies behave as configured; discarded/expired items keep history status + reason.
- [ ] Notification updates do not duplicate announcements; dedupe works across normal and Priority modes.
- [ ] Pause / Resume / Stop current / Skip current / Repeat current work from app and notification.
- [ ] Temporary mute 15 min / 30 min / 1 h / until re-enabled works and shows remaining time.

## 7. History

- [ ] Entries appear even for skipped/queued items with correct status and specific reason.
- [ ] Filter by app/status, search by content/sender/app/date, sort newest/oldest, lazy loading while scrolling.
- [ ] Clear single entry and clear all (with confirmation) work and cannot be undone accidentally.
- [ ] Storage modes Full (default) / Metadata only / Disable honoured per app and globally; inheritance visible.
- [ ] Retention windows (1–90 days / Never) prune correctly; update-handling mode behaves as configured.

## 8. Backup, security, diagnostics

- [x] Backup exports settings only (no history/notification content); file picker, no cloud. (Physical-device-tested)
- [x] Corrupt, wrong-version and truncated files are rejected with a clear message; valid file restores settings. (Physical-device-tested)
- [x] Biometric gate (when enabled) protects entry; disabled by default. (Physical-device-tested)
- [x] Secure-screen mode hides content in recents when enabled. (Physical-device-tested)
- [x] Diagnostics shows service state, permissions, TTS availability, active output; local log export works; **log contains no notification content**. (Physical-device-tested)

## 9. Restart, reboot and background

- [ ] Reboot: recovery respects Android restrictions; no silent re-announcement beyond pre-reboot session state.
- [ ] Notification access revoked → service degrades gracefully with a warning, no crash loop.
- [ ] App updated / process reclaimed in background → no crash, no content leak in logcat.

## 10. Accessibility and UI

- [ ] TalkBack: every control announced with role/state; toggles report on/off; lists navigable.
- [ ] All targets ≥ 48 dp; status never colour-only.
- [ ] Font scale 2.0: no clipping/overlap on Dashboard, Settings, History, setup steps.
- [ ] Light/Dark/System theme correct; default System.
- [ ] Destructive actions require confirmation.
- [ ] Reduced motion respected.

## 11. Compatibility

- [ ] Full pass on Android 10 (API 29) emulator.
- [ ] Full pass on latest Android emulator.
- [ ] Physical Samsung: notification access, BT audio, background behaviour, reboot, DND, call suppression.
- [ ] Physical stock/Pixel: same set.
- [ ] Known OEM deviations recorded in `TECHNICAL_DECISIONS.md` / changelog instead of being hidden.
