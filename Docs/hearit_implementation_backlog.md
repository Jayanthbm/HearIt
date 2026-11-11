# HearIt — Feature Implementation Backlog

This document tracks all features identified from `hearit_features_gap_analysis_and_roadmap.md` and our screen-by-screen audit that are **NOT yet implemented** or need improvements.

Once the review is completed, these items will be batched and implemented systematically per user instruction.

---

## 📋 Status Overview
- Total Identified Backlog Items: 3
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

### 2. Roadmap Item 3.2: Global DND & Phone Call Handling Settings — ❌ UI NOT IMPLEMENTED (Engine Ready)
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - **Engine & Persistence Already Built**: `EligibilitySettings` (`core/common`), `SettingsCodec` (`core/datastore`), and `EligibilityRules` (`core/rules`) already support global `respectDnd: Boolean = true` and `queueDuringCall: Boolean = false`, and per-app inheritance (`AppEligibilityOverride`).
  - **Missing UI**: `GlobalSettingsUiState`, `GlobalSettingsViewModel`, and `GlobalSettingsScreen` do **not** expose any controls for `respectDnd` or `queueDuringCall` (they are only editable per-app in `ApplicationsScreen`).
- **What Needs to Be Implemented**:
  - Add `respectDnd: Boolean = true` and `queueDuringCall: Boolean = false` to `GlobalSettingsUiState` (and update `hasOverrides` + `from()`).
  - Add `onRespectDndChanged(Boolean)` and `onQueueDuringCallChanged(Boolean)` in `GlobalSettingsViewModel`.
  - Add a **"Do Not Disturb & Phone Calls"** card (or section inside the Device Conditions card) in `GlobalSettingsScreen`:
    - Toggle: **"Silence during Do Not Disturb"** (`respectDnd`).
    - Call Handling Selector / Toggle: **"During active phone calls: Discard vs Queue until call ends"** (`queueDuringCall`).
  - Update `GlobalSettingsUiStateTest` (if any) to verify mapping and `hasOverrides`.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/applications/GlobalSettingsUiState.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/GlobalSettingsScreen.kt`
  - `app/src/main/res/values/strings.xml` (optional string resources)

### 3. Roadmap Item 3.3: Text Pronunciation & Acronym Dictionary — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - `SmartContentCleaner` (`core/tts`) already does hardcoded normalization for currency symbols (`$`, `₹`, `€`, `£`) and basic units (`mins` $\to$ `minutes`, `hrs`, `secs`, `km/h`, `mph`, `GB`/`MB`/`KB`), plus per-app spoken aliases (`setAppAlias` / `resolveAppName`).
  - There is **no** user-configurable **Pronunciation & Acronym Dictionary** (custom word/acronym replacements like `"OTP"` $\to$ `"O T P"`, `"Dr."` $\to$ `"Doctor"`, `"ETA"` $\to$ `"E T A"`), no persistence for custom replacements, and no dictionary management UI.
- **What Needs to Be Implemented**:
  - Add user-configurable pronunciation dictionary storage (e.g., `Map<String, String>` / list of `PronunciationRule(match, spokenAs, matchCase, wholeWord)` in `HearItSettings` + `SettingsCodec` or `SessionStore`).
  - Apply dictionary replacements inside `SmartContentCleaner.cleanMessageText()` (and optionally `cleanSender()`), using whole-word / safe regex matching before text reaches the Android TTS engine, with optional starter presets (e.g., `OTP`, `ETA`, `ASAP`, `FYI`).
  - Add a **Pronunciation Dictionary** management UI card/screen accessible from **Voice & Speech** (or **Active Intelligence**):
    - List of configured word/acronym $\to$ spoken replacement rules.
    - Add / edit / delete rule dialog + quick "Test Pronunciation" button via TTS engine.
  - Add unit tests in `SmartContentCleanerTest` and `SettingsCodecTest`.
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt`
  - `core/tts/src/test/kotlin/com/jayanth/hearit/core/tts/SmartContentCleanerTest.kt`
  - `core/common/src/main/kotlin/com/jayanth/hearit/core/common/model/...` / `core/datastore/.../SettingsCodec.kt`
  - `app/src/main/java/com/jayanth/hearit/voicespeech/VoiceSpeechScreen.kt` (or `VoiceSpeechViewModel.kt`)

---

## ✅ Already Verified / Implemented Features in this Audit

1. **Item 1.1 Phone Reboot Auto-Recovery (`BOOT_COMPLETED`)**: Fully implemented with `BootReceiver` and `RECEIVE_BOOT_COMPLETED`.
2. **Item 1.2 Direct Battery Optimization Exemption**: Fully implemented with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (1-tap OS system prompt) in Setup and Diagnostics.
3. **Item 2.1 Multi-Action Persistent Notification**: Fully implemented in `AnnouncementNotification.kt` (Dynamic actions: Pause/Skip/Stop when speaking, Priority Toggle/Mute/Stop when idle).
