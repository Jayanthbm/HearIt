# HearIt — Feature Implementation Backlog

This document tracks all features identified from `hearit_features_gap_analysis_and_roadmap.md` and our screen-by-screen audit that are **NOT yet implemented** or need improvements.

Once the review is completed, these items will be batched and implemented systematically per user instruction.

---

## 📋 Status Overview
- Total Identified Backlog Items: 11
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
  - Add a **Pronunciation & Acronym Dictionary** card directly inside the **Active Intelligence** screen (`IntelligenceControlsScreen`), keeping it grouped with NLP controls (and ready for future Premium gating):
    - Enable/disable switch + list of configured word/acronym $\to$ spoken replacement rules.
    - Add / edit / delete rule dialog + quick "Test Pronunciation" button via TTS engine.
  - Add unit tests in `SmartContentCleanerTest` and `SettingsCodecTest`.
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt`
  - `core/tts/src/test/kotlin/com/jayanth/hearit/core/tts/SmartContentCleanerTest.kt`
  - `core/common/src/main/kotlin/com/jayanth/hearit/core/common/model/...` / `core/datastore/.../SettingsCodec.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsViewModel.kt`

### 4. Roadmap Item 4.1: Priority Announce Configurable Expiry Durations — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - `AnnouncementService` hardcodes a fixed 24-hour expiry (`PRIORITY_ANNOUNCE_EXPIRY_MILLIS = 24 * 60 * 60 * 1000L`) when `ACTION_PRIORITY_ANNOUNCE_ON` or `ACTION_PRIORITY_ANNOUNCE_TOGGLE` turns Priority Announce on.
  - `DashboardScreen` only has a plain on/off switch for Priority Announce and shows static status text (`"Bypassing lock rules"`), with no duration picker and no remaining time indicator (despite unused strings `priority_announce_no_expiry` / `priority_announce_remaining` already present in `strings.xml`).
- **What Needs to Be Implemented**:
  - Update `AnnouncementService` to accept an optional duration extra (e.g., `EXTRA_DURATION_MILLIS` / `EXTRA_DURATION_MINUTES`) for `ACTION_PRIORITY_ANNOUNCE_ON`, supporting **30 minutes**, **1 hour**, **2 hours**, **4 hours**, or **Until turned off** (`<= 0L` / no timer).
  - Track Priority Announce expiry timestamp in `ServiceStatusMonitor` / `DashboardUiState` so the Home screen Priority Announce card can display the remaining time (or `"Until turned off"`).
  - In `DashboardScreen`, show a duration selector dialog (matching the existing `showMuteDurationDialog` pattern for Temporary Mute) when enabling Priority Announce (or tapping/long-pressing the Priority Announce card), while keeping the switch / QS tile / notification action functional cleanly.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementService.kt`
  - `app/src/main/java/com/jayanth/hearit/dashboard/ServiceStatusMonitor.kt` (or wherever `ServiceStatusMonitor` lives)
  - `app/src/main/java/com/jayanth/hearit/dashboard/DashboardUiState.kt` / `DashboardViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/dashboard/DashboardScreen.kt`
  - `app/src/main/res/values/strings.xml`

### 5. Roadmap Item 4.2: Priority Announce Quick-Access List — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - **Engine & Persistence Already Built**: `HearItSettings.priorityAnnounceApps` (`core/datastore`), `SettingsCodec`, `EligibilityRules` (`settings.priorityAnnounceListed`), and `SettingsSource.setPriorityApp()` all exist.
  - **Missing UI & Editor**: Neither `ApplicationsUiState`, `AppSettingsUiState`, `AppSettingsEditor`, `ApplicationsViewModel`, nor `ApplicationsScreen` exposes `priorityAnnounceApps` at all (neither a quick-access list/tab nor a per-app toggle — only queue `AppPriority` Low/Normal/High/Critical is exposed in per-app details).
  - `ApplicationsListScreen` currently only filters by **All / Enabled / Disabled** (plus System Apps toggle).
- **What Needs to Be Implemented**:
  - Expose `priorityAnnounceApps: Set<String>` in `ApplicationsUiState` and `priorityAnnounceListed: Boolean` in `AppSettingsUiState` + `onTogglePriorityAnnounceApp(packageName: String)` in `ApplicationsViewModel` / `AppSettingsEditor`.
  - Add a **"Priority"** filter tab/chip (e.g., **All / Enabled / Disabled / Priority** or dedicated Priority Announce quick-access view) in `ApplicationsScreen` showing all apps in `priorityAnnounceApps` with **1-tap Star/Toggle add/remove** right from the list (plus an option inside Per-App Details and/or a link from the Home Priority Announce card).
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsModels.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/AppSettingsEditor.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsScreen.kt`

### 6. Roadmap Item 5.2: Per-App Voice / Pitch / Speed Overrides — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Low Value / Medium Effort
- **Current State**:
  - `SpeechSettings` (`speechRate`, `pitch`, `volumeLevel`, `voiceId`) is purely global (`HearItSettings.speech`), and `AnnouncementService` always passes `settingsSource.speech()` to `engine.speak()`.
  - `AppOverride` in `HearItSettings.kt` currently only supports `eligibility`, `queue`, `history`, `priority`, `priorityAnnounceOverride`, and `spokenName` (no speech/voice override fields).
- **What Needs to Be Implemented**:
  - Add optional per-app `AppSpeechOverride(speechRate: Float? = null, pitch: Float? = null, voiceId: String? = null)` (and optionally format override if desired) to `AppOverride` (`core/datastore`), `SettingsCodec`, and `SettingsSource.speechFor(packageName)`.
  - Update `AnnouncementService` (and pipeline/template resolution if template override is included) to resolve per-app `SpeechSettings` instead of fixed global `settingsSource.speech()`.
  - Add a **Voice & Speed Override** card inside Per-App Details (`ApplicationsScreen` when app is enabled, with "Inherit global" reset option matching `Inherited<T>` pattern).
  - Add codec + resolution unit tests (`SettingsCodecTest`, `HearItSettingsTest`, `AppSettingsEditorTest`).
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/TextToSpeechEngine.kt` (or `core/common`)
  - `core/datastore/src/main/kotlin/com/jayanth/hearit/core/datastore/HearItSettings.kt` & `SettingsCodec.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SettingsSource.kt` & `AnnouncementService.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsModels.kt`, `AppSettingsEditor.kt`, `ApplicationsViewModel.kt`, `ApplicationsScreen.kt`

### 7. Roadmap Item 6.2: Per-App Batch Actions & Multi-Select Mode — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - `ApplicationsListScreen` only has an *"Enable all visible" / "Disable all visible"* toggle (`ApplicationsViewModel.onSelectAllVisible()` / `onDeselectAllVisible()`), and note that `onSelectAllVisible()` currently selects from `visibleApps` rather than `displayedApps` (so it doesn't filter out hidden system apps or Selected/Unselected tabs).
  - There is **no** multi-select checkbox mode and **no** bulk action bar for applying settings to a custom subset of apps.
- **What Needs to Be Implemented**:
  - Add multi-select mode in `ApplicationsListScreen` (enter via long-press on an app card or a *"Select"* icon/chip), showing checkboxes on each app row and selection count.
  - Add a contextual bottom/top **Batch Action Bar** when 1+ apps are selected:
    - **Enable** / **Disable** selected apps.
    - **Add to Priority Announce** / **Remove from Priority Announce** (integrating with Item 4.2).
    - **Set Speech Priority** (Critical / High / Normal / Low) or bulk reset to global defaults.
  - Also fix `onSelectAllVisible` / `onDeselectAllVisible` (or pass `showSystemApps` / `filterType` into ViewModel) so *"Enable all visible"* respects the active System Apps / tab filters.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsModels.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/AppSettingsEditor.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsScreen.kt`

### 8. Roadmap Item 7.2: Manual "Clear Queue" Action — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Very Low Effort
- **Current State**:
  - `AnnouncementQueue` (`core/queue`), `AnnouncementPipeline`, `AnnouncementService`, `AnnouncementNotification`, and `DashboardScreen` have no method or action to clear/flush pending queued items on demand.
  - Queued notifications must either finish speaking or wait for `expiryMillis`.
- **What Needs to Be Implemented**:
  - Add `clearPending(): List<QueueEvent>` (and optionally record cleared items as `QueueEvent.OverflowDiscarded` or skipped in history) in `AnnouncementQueue` + `AnnouncementPipeline.clearQueue()`.
  - Add `ACTION_CLEAR_QUEUE` in `AnnouncementService` (stopping/skipping or letting current finish while flushing pending + held queue and updating `serviceStatus.updateSpeech(pendingCount = 0)` + notification).
  - Add a **"Clear Queue"** pill/button on the Home Hero Card (`DashboardScreen`) when `state.pendingCount > 0` (and optionally in `AnnouncementNotification` when queue > 0 or via action).
  - Add unit tests in `AnnouncementQueue` + `AnnouncementPipelineTest`.
- **Files Likely Touched**:
  - `core/queue/src/main/kotlin/com/jayanth/hearit/core/queue/AnnouncementQueue.kt`
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementPipeline.kt` & `AnnouncementService.kt`
  - `app/src/main/java/com/jayanth/hearit/dashboard/DashboardViewModel.kt` & `DashboardScreen.kt`

### 9. Roadmap Item 8.1: History Date Range Filtering — ❌ UI NOT IMPLEMENTED (Database Ready)
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - **Database Layer Already Built**: `HistoryQuery` (`core/database/.../HistoryPolicy.kt`) and `HistoryRepository` already support `dateFrom: Long?` and `dateTo: Long?`.
  - **Missing UI & Mapper**: `HistoryFilter` (`HistoryModels.kt`), `HistoryQueryMapper.toQuery()`, `HistoryViewModel`, and `HistoryScreen` do not include date range filtering or chips.
- **What Needs to Be Implemented**:
  - Add `DateRangePreset` (`ALL`, `TODAY`, `YESTERDAY`, `LAST_7_DAYS`, `CUSTOM`) + `dateFrom: Long?`, `dateTo: Long?` to `HistoryFilter` (`HistoryUiState.isFiltered` updated accordingly).
  - Forward `dateFrom` / `dateTo` in `HistoryQueryMapper.toQuery(...)`.
  - Add `onDateRangeChanged(...)` in `HistoryViewModel`.
  - Add horizontal Date Range filter chips (**All / Today / Yesterday / Last 7 Days / Custom Range** with Material 3 `DateRangePickerDialog`) in `HistoryScreen`.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/history/HistoryModels.kt`
  - `app/src/main/java/com/jayanth/hearit/history/HistoryQueryMapper.kt`
  - `app/src/main/java/com/jayanth/hearit/history/HistoryViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/history/HistoryScreen.kt`

### 10. Roadmap Item 8.2: History Export (CSV / TXT via SAF) — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Low Value / Low Effort
- **Current State**:
  - History entries reside strictly in local Room storage; no export button, serializer, or SAF document launcher exists in `HistoryScreen` or `HistoryViewModel`.
- **What Needs to Be Implemented**:
  - Add pure CSV / TXT formatter for `HistoryEntry` / `HistoryItem` list (properly escaping CSV quotes/commas and respecting `Privacy` / `METADATA_ONLY` redacted messages).
  - Query matching filtered history (or all history) without the 50-row page cap via `HistoryRepository` (`limit = Int.MAX_VALUE` / larger limit).
  - Use Android Storage Access Framework (`ActivityResultContracts.CreateDocument("text/csv")` or `"text/plain"`) from `HistoryScreen` to save the file cleanly via `ContentResolver.openOutputStream(uri)`.
  - Add an **"Export"** icon/button in the `HistoryScreen` header/toolbar alongside Clear All (with format choice CSV vs TXT).
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/history/HistoryViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/history/HistoryScreen.kt`
  - Pure formatter + unit test under `app/src/test/java/com/jayanth/hearit/history/`

### 11. Roadmap Item 9.1: Notification History Detail Modal — Visible Scrollbar Indicator — ⚠️ PARTIALLY IMPLEMENTED
- **Priority / Effort**: Polish / Very Low Effort
- **Current State**:
  - `HistoryDetailDialog` in `HistoryScreen.kt` **already implements** bounded height (`heightIn(max = 380.dp)`) and `verticalScroll(rememberScrollState())` inside the body `Column` (`weight(1f, fill = false)`), keeping the header (`appName`, `relativeTime`, `HistoryStatusPill`) and action buttons (`Listen`, `Filter this`, `Delete`, `Close`) fixed while long text scrolls.
  - **What Is Missing**: A visible vertical **scrollbar indicator** when text overflows (Compose `verticalScroll` does not draw a scrollbar track/thumb automatically).
- **What Needs to Be Implemented**:
  - Add a clean custom vertical scrollbar indicator (`drawWithContent` thumb tied to `ScrollState.value` and `ScrollState.maxValue`, visible whenever `maxValue > 0`) on the scrollable content area inside `HistoryDetailDialog` (and optionally tune `heightIn(max = 420.dp)` if needed).
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/history/HistoryScreen.kt`

---

## ✅ Already Verified / Implemented Features in this Audit

1. **Item 1.1 Phone Reboot Auto-Recovery (`BOOT_COMPLETED`)**: Fully implemented with `BootReceiver` and `RECEIVE_BOOT_COMPLETED`.
2. **Item 1.2 Direct Battery Optimization Exemption**: Fully implemented with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (1-tap OS system prompt) in Setup and Diagnostics.
3. **Item 2.1 Multi-Action Persistent Notification**: Fully implemented in `AnnouncementNotification.kt` (Dynamic actions: Pause/Skip/Stop when speaking, Priority Toggle/Mute/Stop when idle).
4. **Item 5.1 System TTS Settings Direct Shortcut**: Fully implemented in `VoiceSpeechScreen.kt` (`SystemTtsSettingsCard` + Voice Picker shortcut) and `DiagnosticsScreen.kt` firing `Intent("com.android.settings.TTS_SETTINGS")`.
5. **Item 6.1 System Apps vs User Apps Filter**: Implemented via `AppCatalog.kt` (`FLAG_SYSTEM` detection) and `FilterChip` (*"System Apps Hidden" / "System Apps Shown"*) in `ApplicationsScreen.kt`. *(Follow-up fix tracked in Item 6.2: ensure "Enable all visible" respects `showSystemApps` & filter tab).*
6. **Item 7.1 Priority Ordering in Queue**: Fully implemented end-to-end — Per-app **Speech Priority** picker (`Low` / `Normal` / `High` / `Critical`) in `ApplicationsScreen.kt`, passed via `AnnouncementService` (`settingsSource.priorityFor`) to `AnnouncementQueue.pollReady()`, which orders by highest `priority.rank` then FIFO without interrupting active speech.
