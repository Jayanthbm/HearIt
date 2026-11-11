# HearIt — Feature Implementation Backlog

This document tracks all features identified from `hearit_features_gap_analysis_and_roadmap.md` and our screen-by-screen audit that are **NOT yet implemented** or need improvements.

Once the review is completed, these items will be batched and implemented systematically per user instruction.

---

## 📋 Status Overview
- Total Identified Backlog Items: 1
- In Discussion / Under Review

---

## 🛠️ Backlog Items

### 1. Roadmap Item 2.2: Bluetooth Headset Button Controls (`MediaSession`) — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - `AnnouncementService` already handles `ACTION_PAUSE`, `ACTION_RESUME`, `ACTION_SKIP`, and `ACTION_REPEAT` intents internally.
  - Earphone disconnect stops speech via `AudioDeviceCallback` / `ACTION_AUDIO_BECOMING_NOISY`.
  - No `MediaSession` or media button key event handling is registered anywhere in `app/` or `core/`.
- **What Needs to Be Implemented**:
  - Register a lightweight native `android.media.session.MediaSession` (`minSdk = 29`) inside `AnnouncementService`.
  - **Lifecycle Scoping (Critical)**: Activate the `MediaSession` (`isActive = true`) **only while announcements are actively speaking or paused mid-utterance**, and release/deactivate it (`isActive = false`) when idle so Spotify / YouTube Music playback buttons aren't hijacked when HearIt isn't speaking.
  - **Key / Action Mapping in `MediaSession.Callback`**:
    - Single press (`KEYCODE_HEADSETHOOK` / `KEYCODE_MEDIA_PLAY_PAUSE` / `onPlay` / `onPause`): Toggle **Pause / Resume** (`ACTION_PAUSE` / `ACTION_RESUME`).
    - Double press (or `onSkipToNext` / `KEYCODE_MEDIA_NEXT`): **Skip** current notification (`ACTION_SKIP`).
    - Triple press / Long press / `onSkipToPrevious` (`KEYCODE_MEDIA_PREVIOUS`): **Repeat** last announcement (`ACTION_REPEAT`).
  - Optional toggle in **Active Intelligence $\to$ Gestures & Playback Controls** (*"Bluetooth Headset Button Controls"*) so users can enable/disable it.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementService.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsScreen.kt` (shared `GesturesAndControlsCard` used by `IntelligenceControlsScreen`)

---

## ✅ Already Verified / Implemented Features in this Audit

1. **Item 1.1 Phone Reboot Auto-Recovery (`BOOT_COMPLETED`)**: Fully implemented with `BootReceiver` and `RECEIVE_BOOT_COMPLETED`.
2. **Item 1.2 Direct Battery Optimization Exemption**: Fully implemented with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (1-tap OS system prompt) in Setup and Diagnostics.
3. **Item 2.1 Multi-Action Persistent Notification**: Fully implemented in `AnnouncementNotification.kt` (Dynamic actions: Pause/Skip/Stop when speaking, Priority Toggle/Mute/Stop when idle).
