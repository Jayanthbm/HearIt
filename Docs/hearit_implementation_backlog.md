# HearIt — Feature Implementation Backlog

This document tracks all features identified from `hearit_features_gap_analysis_and_roadmap.md` and our screen-by-screen audit that are **NOT yet implemented** or need improvements.

Once the review is completed, these items will be batched and implemented systematically per user instruction.

---

## 📋 Status Overview
- Total Identified Backlog Items: 38
- In Discussion / Under Review
- **Mandatory Requirement (All Features)**: Every capability—including automatic intelligence rules and playback behaviors—MUST have a dedicated UI **Enable / Disable switch** and **configuration controls** (thresholds, patterns, presets, or timing options) persisted across restarts.

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
  - **UI Enable/Disable & Configuration in Active Intelligence $\to$ Gestures & Playback Controls**: Add persisted *"Bluetooth Headset Button Controls"* switch + configurable single/double/triple-press action selector.
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

### 12. Roadmap Item 9.2 A.3: OTP & Phone Number Digit-by-Digit Speech Formatting — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - `SmartContentCleaner` (`core/tts`) already summarizes URLs and collapses repeated emoji spam, **but** does not format phone numbers (`+91 9876543210`, `555-123-4567`) or standalone multi-digit codes/PINs (`482910`) digit-by-digit — TTS currently speaks them as giant cardinal integers (hundreds of thousands / billions).
- **What Needs to Be Implemented**:
  - Add digit-by-digit formatting inside `SmartContentCleaner` (carefully avoiding currency amounts like `$50`, `₹1,499`, dates, or times like `10:30`):
    - Phone numbers (e.g., `+?\d[\d\s-]{8,}`) and standalone 4–8 digit OTP/verification codes/tracking IDs $\to$ space-separated digits with brief comma chunking (e.g., `"4 8 2, 9 1 0"`).
  - **UI Enable/Disable & Configuration in Active Intelligence $\to$ Smart Content Cleaning & NLP**: Add persisted switch (*"Speak Codes & Phone Numbers Digit-by-Digit"*) + configuration toggles/options (OTP/verification codes vs Phone numbers, pause grouping style).
  - Add unit tests in `SmartContentCleanerTest`.
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt`
  - `core/tts/src/test/kotlin/com/jayanth/hearit/core/tts/SmartContentCleanerTest.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt`, `GeneralSettingsViewModel.kt`, `GeneralSettingsScreen.kt` (if toggle added)

### 13. Roadmap Item 9.2 A.1: Smart Link & URL Summarization — Configurable Predefined & Custom Rules UI (Inside Active Intelligence) — ⚠️ ENHANCEMENT / PARTIALLY IMPLEMENTED
- **Priority / Effort**: High Value / Low-Medium Effort
- **Current State**:
  - `SmartContentCleaner.summarizeUrls()` (`core/tts`) currently uses a **hardcoded** `when` block for a few domains (`instagram.com/reel`, `instagram.com/p/`, `youtube.com`/`youtu.be`, `maps.google.com`, `facebook.com`, `twitter.com`/`x.com`, `linkedin.com`) plus generic `"link to <domain>"`.
  - **Active Intelligence** (`IntelligenceControlsScreen` $\to$ `SmartContentCleaningCard`) only has an on/off toggle (`isUrlSummarizerEnabled`) — there is **no UI** to load/toggle predefined URL rules or add custom URL-pattern-to-spoken-phrase mappings.
- **Planned Implementation**:
  - **Data Model & Persistence**:
    - Define `UrlSummaryRule(pattern: String, spokenPhrase: String, isPredefined: Boolean, enabled: Boolean)` stored in `SessionStore` / `SettingsRepository` and synced into `SmartContentCleaner` on startup and update.
    - Provide a **Predefined Preset Pack** (e.g., YouTube video, Instagram Reel, Instagram Post, Google Maps location, Facebook post, X post, LinkedIn post, GitHub link, Zoom meeting, Google Meet, Spotify track, Reddit post).
  - **Engine (`SmartContentCleaner.kt`)**:
    - Match dynamic rules sorted by specificity (longest pattern / custom overrides before shorter/broader domains), using `Matcher.quoteReplacement()` safely, and fall back to `"link to <domain>"` for unmatched URLs.
  - **UI Inside Active Intelligence (`IntelligenceControlsScreen` / `SmartContentCleaningCard`)**:
    - Under *"Clean & Summarize URLs"* (shown when enabled), add a clean **URL Rules** manager section/sheet featuring:
      - **"Load Predefined Rules"** one-tap action (restores/adds default domain presets without erasing custom rules).
      - **"+ Add Custom URL Rule"** form (URL / domain pattern e.g. `notion.so` or `zoom.us/j` $\to$ spoken phrase e.g. `"Notion page"` / `"Zoom meeting"`).
      - Toggle / edit / delete controls for each predefined and custom rule.
  - **Unit Tests**: Verify precedence, custom rules, preset loading, and fallback behavior in `SmartContentCleanerTest`.
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt`
  - `core/tts/src/test/kotlin/com/jayanth/hearit/core/tts/SmartContentCleanerTest.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt` (or `core/datastore`)
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt` & `GeneralSettingsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt` (or shared card in `GeneralSettingsScreen.kt`)

### 14. Roadmap Item 9.2 B: Category Intelligence (Messaging, SMS/RCS, Email) & Configuration UI — ⚠️ PARTIALLY IMPLEMENTED (No UI)
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - **WhatsApp & Messaging**: `SmartContentCleaner.normalizeMediaTags()` handles Photo, Voice message, Location, GIF, Sticker, bare Document; `AnnouncementTemplate.build()` formats `"$sender in $groupTitle says,"` only when notification metadata provides `groupTitle`; `cleanMessageText()` suppresses `"Checking for new messages..."` and `"WhatsApp Web is currently active..."` but misses `"Backup in progress..."` and doesn't skip the notification silently (if `APP_NAME` is emitted, it may still speak `"WhatsApp."`); missing `"🎥 Video"` $\to$ `"Sent a video"`, `"📄 Document: file.pdf"` $\to$ `"Sent a PDF document"`, `"Contact: Name"` $\to$ `"Shared a contact card"`, and colon-based group chat parsing (`"Family Group: Rahul: Hey"`).
  - **Messages (SMS & RCS)**: **Not implemented** — no bank/transactional alert summarizer (`"Acct XX1234 debited INR 500 at Merchant..."` $\to$ `"500 rupees debited at Merchant"`), no `INR`/`USD` prefix currency handling, and no carrier promo/spam suppression (`"Recharge now"`, `"Dial *121#"`).
  - **Email (Gmail, Outlook, Mail)**: **Bugged/Incomplete** — `cleanEmailContent()` currently **deletes** `Re:`/`Fwd:` instead of replacing with `"Reply:"`/`"Forward:"`; `EMAIL_SIGNOFF` handles some sign-offs (`Regards`, `Sincerely`, `Cheers`, `Sent from my...`) but misses `"Kind regards"`, `"Thanks & Regards"`, and **Legal Disclaimers** (`"Disclaimer: This email..."`, `"CONFIDENTIALITY NOTICE:..."`).
  - **UI**: **Zero configuration UI exists** in **Active Intelligence** (`SmartContentCleaningCard` currently only has 6 global NLP toggles).
- **Planned Implementation**:
  - **1. Engine Upgrades (`SmartContentCleaner`, `AnnouncementTemplate`, `AnnouncementPipeline`)**:
    - *Messaging Toggle (`isMediaAndMessagingIntelligenceEnabled`)*: Add Video, Document-with-extension (`PDF`, `Word`, `Spreadsheet`, generic document), and Contact card patterns; parse inline group patterns (`"Group: Sender"` / `"Group: Sender: Msg"`); expand sync noise filter (`"Backup in progress"`, `"Restoring..."`, `"Connecting..."`) and ensure noise-suppressed messages skip announcing the notification completely in `AnnouncementPipeline` instead of speaking only the app name.
    - *SMS & Bank Toggle (`isSmsAndBankCleanerEnabled` / `isCarrierSpamFilterEnabled`)*: Parse `INR`/`Rs.`/`$`/`₹` debited/credited/UPI SMS patterns into crisp spoken summaries (`"500 rupees debited at Merchant"`), and filter/skip configurable carrier promo/spam SMS patterns (`Recharge now`, `Dial *`, `validity expires` + custom spam keywords if desired).
    - *Email Toggle (`isEmailCleanupEnabled`)*: Replace `Re:` $\to$ `"Reply:"` and `Fwd:` / `Fw:` $\to$ `"Forward:"`; strip `"Kind regards"`, `"Thanks & Regards"`, `"Thanks and regards"`, `"Warm regards"`, and common legal/confidentiality disclaimer footers.
  - **2. Configuration UI Inside Active Intelligence (`IntelligenceControlsScreen` + `GeneralSettingsViewModel` + `SessionStore`)**:
    - Add a dedicated **"Category Intelligence"** card (or expandable subsections inside `IntelligenceControlsScreen`) with individual switches and details for:
      - **Messaging & Media Polish** (Media attachments, group chat format, sync noise suppression)
      - **Bank & Transactional SMS Cleaner** (Condenses debit/credit/UPI alerts into concise speech)
      - **Carrier & Promo SMS Filter** (Auto-skips telecom/recharge ads, with optional View/Edit promo phrases)
      - **Email Subject & Disclaimer Cleanup** (`Re:`/`Fwd:` replacement + sign-off & legal disclaimer stripping)
  - **Unit Tests**: Expand `SmartContentCleanerTest` and `AnnouncementTemplateTest` covering Video/PDF/Contact, Group formats, Bank SMS, Carrier Promo, Email `Reply:`/`Forward:` and Legal Disclaimers.
- **Files Likely Touched**:
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt`
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/AnnouncementTemplate.kt`
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementPipeline.kt` & `SessionStore.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt` & `GeneralSettingsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt`
  - `core/tts/src/test/kotlin/com/jayanth/hearit/core/tts/SmartContentCleanerTest.kt`

### 15. Roadmap Item 9.2 C: Local AI Strategy — Tier 2 (Android AICore & ML Kit Entity Extraction) + Device Support Status UI — ❌ TIER 2 NOT IMPLEMENTED (Tier 1 Active)
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - **Tier 1 (Instant Regex & Pattern NLP)**: ✅ **Implemented** in `SmartContentCleaner` (`core/tts`, 0 MB added, runs on all Android devices).
  - **Tier 2 (Android AICore / ML Kit Entity Extraction)**: ❌ **Not implemented** — no AICore or ML Kit libraries exist in `gradle/libs.versions.toml` or `app/build.gradle.kts`, and there is no device capability probing or status UI anywhere.
  - **Tier 3 (Bundled Local LLMs)**: Intentionally **Skipped** (keeps APK lightweight & battery-friendly).
- **Planned Implementation**:
  - **1. Device AI Capability Probe (`IntelligenceCapabilityProbe.kt`)**:
    - Detect hardware/OS support on the device cleanly:
      - **Android AICore (System Gemini Nano)**: Check Android version (`>= Android 14 / API 34`), installed/enabled `com.google.android.aicore` system package, and AICore generative availability status (`Supported` vs `Unsupported on this device`).
      - **ML Kit Entity Extraction**: Add lightweight `com.google.mlkit:entity-extraction` dependency (model downloaded on-demand via Google Play Services, negligible APK size), check model availability (`Ready` vs `Download Required` vs `Play Services Unavailable`), and support detecting entities (tracking numbers, IBAN/account numbers, flight codes, phone numbers, addresses, dates/times) asynchronously / with fast timeout fallback to Tier 1.
  - **2. Hybrid Tier 1 + Tier 2 Enhancement Pipeline**:
    - Use Tier 1 (`SmartContentCleaner`) instantaneously on every message, and when Tier 2 is supported + enabled, enhance structured entity pronunciation (tracking IDs, phone numbers, dates, or AICore smart long-email summarization) without blocking or slowing down standard short alerts.
  - **3. Hardware Support & Control UI Inside Active Intelligence (`IntelligenceControlsScreen`)**:
    - Add an **"On-Device AI Engines"** status & control card inside **Active Intelligence**:
      - **Tier 1 · Instant Pattern NLP**: Green `"Active · On-Device"` badge.
      - **Tier 2A · ML Kit Entity Extraction**: Live status badge (`"Supported · Ready"` / `"Model Not Downloaded"` / `"Not Supported"`) + Enable toggle / Download Model action when supported.
      - **Tier 2B · Android AICore**: Live status badge (`"Supported (Hardware AI)"` vs `"Not Supported on this Device"`) + Enable toggle (disabled/greyed with explanation when unsupported on the phone).
- **Files Likely Touched**:
  - `gradle/libs.versions.toml` & `app/build.gradle.kts`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceCapabilityProbe.kt` (new)
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt` & `GeneralSettingsViewModel.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt` & `AnnouncementService.kt`

### 16. Roadmap Item 9.4: App Categories & Email / Messaging Grouping UI — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium-High Value / Medium Effort
- **Current State**:
  - No `AppCategory` grouping (Email vs Messaging vs SMS/Financial) exists in `AppCatalog`, `HearItSettings`, `ApplicationsScreen`, or `SettingsHub`.
  - `SmartContentCleaner` currently treats every app identically regardless of whether it is Gmail/Outlook vs WhatsApp vs SMS.
- **Planned Implementation**:
  - **1. Category Model & Smart Defaults (`AppCategory`: `EMAIL`, `MESSAGING`, `SMS`)**:
    - Pre-seed recognized package defaults cleanly (e.g., **Email**: `com.google.android.gm` Gmail, `com.microsoft.office.outlook` Outlook, Yahoo Mail, Samsung Email, Spark; **Messaging**: WhatsApp, WhatsApp Business, Telegram, Signal, Slack, MS Teams, Discord; **SMS / Carrier**: Google Messages, Samsung Messages, OnePlus/stock SMS).
    - Persist user-customized package sets (`emailCategoryApps`, `messagingCategoryApps`, `smsCategoryApps` — or `appCategories: Map<String, AppCategory>` overrides) in `HearItSettings` / `SettingsCodec` (so unchecking/adding any installed app persists seamlessly).
  - **2. Pipeline & Category-Rule Scoping (Pairs directly with Backlog #14 / Item 9.2 B)**:
    - Pass `packageName` / `AppCategory` into `AnnouncementTemplate.build()` $\to$ `SmartContentCleaner` so **Email-specific rules** (`Re:` $\to$ `"Reply:"`, `Fwd:` $\to$ `"Forward:"`, legal disclaimer & sign-off truncation) apply specifically to apps in the **Email Apps Group**, while Messaging / SMS rules target their assigned groups cleanly.
  - **3. Dedicated Grouping UI (Accessible from Applications Tab & Active Intelligence / Category Rules)**:
    - Add an **"App Categories"** button/entry (in `ApplicationsScreen` top bar or **Active Intelligence $\to$ Category Intelligence**) opening a clean Group Manager with tabs/sections for **Email Apps**, **Messaging Apps**, and **SMS Apps**:
      - Shows installed apps matching defaults pre-checked, plus search/checkboxes so users can easily check or uncheck any installed app in each category.
- **Files Likely Touched**:
  - `core/common/src/main/kotlin/...` (`AppCategory` enum & defaults)
  - `core/datastore/src/main/kotlin/com/jayanth/hearit/core/datastore/HearItSettings.kt` & `SettingsCodec.kt`
  - `core/tts/src/main/kotlin/com/jayanth/hearit/core/tts/SmartContentCleaner.kt` & `AnnouncementTemplate.kt`
  - `app/src/main/java/com/jayanth/hearit/applications/ApplicationsViewModel.kt` & `ApplicationsScreen.kt` (plus link from `IntelligenceControlsScreen.kt`)

### 17. Roadmap Item 10 (Feature D): Background Audio Ducking Level Control — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - `AudioFocusCard` (`GeneralSettingsScreen.kt`) only offers the 3 OS focus policies: `DUCK` (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`), `PAUSE` (`AUDIOFOCUS_GAIN_TRANSIENT`), and `NONE`.
  - There is **no** Ducking Level Slider / selector (e.g., 20%, 50%, 80%, or Pause). When `DUCK` is chosen, the OS lets other media players duck to an unconfigurable default volume.
- **What Needs to Be Implemented**:
  - Add `duckingLevelPercent: Int` (or presets: 20%, 50%, 80% alongside Pause option) persisted in `SessionStore` / `SettingsRepository`.
  - Add a **Ducking Level** slider / preset selector inside `AudioFocusCard` (`GeneralSettingsScreen.kt` when `AudioFocusPolicy.DUCK` is selected).
  - In `AnnouncementService.requestAudioFocus()` / `abandonAudioFocus()`: when ducking level is custom (e.g., 20% / 50% / 80%), safely scale `AudioManager.STREAM_MUSIC` volume down proportionally while HearIt speaks via `USAGE_ASSISTANCE_ACCESSIBILITY` (tracking original volume index cleanly in memory/store and always restoring it on completion, interruption, `onDestroy`, and crash recovery — or note OS API limitations gracefully if `FLAG_FIXED_VOLUME` applies).
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt`, `GeneralSettingsViewModel.kt`, `GeneralSettingsScreen.kt`
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt` & `AnnouncementService.kt`

### 18. Roadmap Item 10 (Feature F) & `SmartAudioPolishCard` Wiring Fix — ⚠️ ENGINE ONLY / UI MOCKED
- **Priority / Effort**: High Value (Bug Fix) / Low Effort
- **Current State**:
  - **Engine (`AndroidTextToSpeechEngine.kt`)**: Already implements adaptive speed (`effectiveRate = (settings.speechRate * 1.25f).coerceAtMost(2.5f)` when `totalWords > 30`), earcon beep (`ToneGenerator`), and `300L` anti-collision delay — **all hardcoded**!
  - **Broken / Non-Persisted UI (`VoiceSpeechScreen.kt` $\to$ `SmartAudioPolishCard`)**: All 4 settings in `SmartAudioPolishCard` (`earconEnabled`, `antiCollisionMs` [0/300/500 ms], `adaptiveSpeedEnabled`, `monoFoldDownEnabled`) are **local `remember { mutableStateOf(...) }` state variables**! They do not persist across navigation/restarts and are never read by `AndroidTextToSpeechEngine`!
- **What Needs to Be Implemented**:
  - Move `isEarconEnabled`, `antiCollisionDelayMs`, `isAdaptiveSpeedEnabled`, `adaptiveSpeedWordThreshold` (e.g. `20` / `30` / `50` words), `adaptiveSpeedMultiplier` (e.g. `1.25×` / `1.5×`), and `isMonoFoldDownEnabled` into `SessionStore` (or `SpeechSettings` / `VoiceSpeechUiState`) and expose callbacks from `VoiceSpeechViewModel`.
  - Wire `AndroidTextToSpeechEngine.speak()` to actually read all of those settings dynamically (skip earcon if disabled, honor `0 ms` / `300 ms` / `500 ms` delay, apply configurable adaptive speed multiplier when above word threshold if enabled, and configure channel routing).
  - Remove hardcoded local `mutableStateOf` variables in `SmartAudioPolishCard` and display the configuration selectors cleanly when enabled.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt` & `AndroidTextToSpeechEngine.kt`
  - `app/src/main/java/com/jayanth/hearit/voicespeech/VoiceSpeechUiState.kt`, `VoiceSpeechViewModel.kt`, `VoiceSpeechScreen.kt`

### 19. Configuration UI for Built-In Automatic Features (Zero-Config $\to$ User-Configurable) — ❌ UI MISSING
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - Several features currently execute unconditionally or under a single coarse flag without granular enable/disable switches or timing/behavior configs:
    1. **App Name Deduplication** (`AnnouncementPipeline.kt`): Hardcoded to omit app name when consecutive alerts arrive from the same app within `45_000L` (45s) — no enable/disable switch or cooldown duration selector (`15s` / `30s` / `45s` / `60s`).
    2. **Delayed Message Timestamp Tag** (`AnnouncementPipeline.kt`): Hardcoded `"Sent X minutes ago"` after `120_000L` (2 mins) — no enable/disable switch.
    3. **Music Paused Hold Duration** (`AnnouncementService.kt`): `isMusicPauseHoldEnabled` toggle exists, but hold window is hardcoded to `60_000L` (60s) with no duration selector (`30s` / `60s` / `90s` / `120s`).
    4. **Shake & Flip Gestures** (`AnnouncementService.kt`): One single toggle (`isShakeToStopEnabled`) controls Face-Down Flip Stop, Shake to Stop, AND Shake-within-10s to Repeat together — no separate switches/sensitivity config.
    5. **Audio Focus Return Fade** (`AnnouncementService.kt`): Smooth 500ms delay before releasing focus is hardcoded.
- **What Needs to Be Implemented**:
  - Add persisted preferences in `SessionStore` and wire controls inside **Active Intelligence** (`IntelligenceControlsScreen` + `GeneralSettingsViewModel`) & **Audio Focus** so the user can toggle and configure every behavior:
    - **App Name Deduplication**: Enable/disable switch + window chips (`30s` / `45s` / `60s`).
    - **Delayed Time Context Tag (Idea 40)**: Enable/disable switch + age threshold (`1m` / `2m` / `5m`) + integrate phrasing seamlessly (`"Rahul sent 12 minutes ago:"` instead of `"Rahul says, (Sent 12 minutes ago)."`).
    - **Music Paused Hold**: Duration chips (`30s` / `60s` / `90s`) when enabled.
    - **Separate Gesture Controls (Idea 41)**: Individual toggles/chips for **Flip Face-Down to Stop**, **Shake to Stop**, and **Shake to Repeat** (with `5s` / `10s` / `15s` window).
    - **Smooth Audio Return Delay (Idea 46)**: Enable/disable switch + smooth release delay (`0ms` / `300ms` / `500ms`) in `AudioFocusCard`.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/service/SessionStore.kt`, `AnnouncementPipeline.kt`, `AnnouncementService.kt`
  - `app/src/main/java/com/jayanth/hearit/settings/GeneralSettingsUiState.kt`, `GeneralSettingsViewModel.kt`, `GeneralSettingsScreen.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt`

### 20. Roadmap Item 11 (Idea 2): Chatty Group Flood Defense (Sender Burst Aggregator + UI) — ⚠️ PARTIAL
- **Priority / Effort**: High Value / Low-Medium Effort
- **Current State**:
  - `AnnouncementQueue` groups same-**package** messages within `groupingWindowMillis` (`3000L`), and `AnnouncementTemplate.build()` announces `"N messages"` followed by repeating `"<Sender> says,"` for every single item even when all N messages came from the same sender in a burst.
- **What Needs to Be Implemented**:
  - Detect consecutive/grouped messages from the **same sender** within a configurable burst window (`3s` / `5s` / `10s`) inside `AnnouncementTemplate` / `AnnouncementPipeline` and merge them smoothly into one phrase: e.g., `"Rahul sent 4 quick messages: Hey, where are you, check Slack"` instead of repeating `"Rahul says"` 4 times.
  - **UI Enable/Disable & Config**: Add *"Burst Aggregator (Chatty Group Defense)"* switch + burst threshold/window selector inside **Preferences $\to$ Grouping & Batching** / **Active Intelligence**.

### 21. Roadmap Item 11 (Idea 3): Sender Contact Name Sanitizer — UI Toggle & Config — ⚠️ ENGINE ONLY
- **Priority / Effort**: Medium Value / Very Low Effort
- **Current State**:
  - `SmartContentCleaner.cleanSender()` already strips emojis (`"Rahul 🏏🔥"` $\to$ `"Rahul"`) and bracketed notes (`"Rahul (Work)"` $\to$ `"Rahul"`) unconditionally with **no UI toggle**.
- **What Needs to Be Implemented**:
  - **UI Enable/Disable & Config**: Add persisted *"Sanitize Contact & Sender Names"* switch (with sub-toggles: *Strip Emojis from Senders*, *Strip Bracketed Notes `(Work)`/`(Gym)`*) inside **Active Intelligence $\to$ Smart Content Cleaning & NLP**, wired to `SmartContentCleaner.isContactSanitizerEnabled` etc.

### 22. Roadmap Item 11 (Idea 4): "Catch-Up: What Did I Miss?" Summary on Connect — ⚠️ PARTIAL
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - `AnnouncementService.audioDeviceCallback` currently speaks `"HearIt connected. You have X pending notifications."` ONLY if `pipeline.pendingCount > 0`.
  - Missing: tracking how long headphones were disconnected (**absence duration** threshold e.g. `15+ mins`), querying missed/recent skipped-because-no-output notifications from Room history/session since disconnect, listing **app names** (`"You have 3 unread messages from WhatsApp and Gmail"`), and **no UI toggle or threshold selector**.
- **What Needs to Be Implemented**:
  - Track `lastHeadphoneDisconnectedAtMillis` in `SessionStore` / `AnnouncementService`.
  - When headphones connect after the configured absence window (`5m` / `15m` / `30m`), read pending + recently missed (`NO_ALLOWED_OUTPUT` / `SCREEN_ON`) items grouped by app name into `"Connected. You have 3 unread messages from WhatsApp and Gmail."`
  - **UI Enable/Disable & Config**: Add persisted *"Catch-Up Summary on Connect"* toggle + absence threshold chips (`Always` / `5m` / `15m` / `30m`) + *"Include App Breakdown"* toggle inside **Devices & Audio Routing** and/or **Active Intelligence**.

### 23. Roadmap Item 11 (Idea 5) & Item 14 (Idea 19): Low Battery / Battery Saver Speech Safeguard Warning & Threshold UI — ⚠️ PARTIAL
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - `RuntimeStateReader` (`capacity in 1..15 || isPowerSave`) and `EligibilityRules` (`SkipReason.MUTED`) pause speech below 15% battery **hardcoded**, with **no gentle spoken warning**, **no UI enable/disable switch**, and **no threshold selector** (and uses `SkipReason.MUTED` rather than distinct low-battery feedback).
- **What Needs to Be Implemented**:
  - Trigger a one-time gentle spoken notice when battery drops across the threshold while service is running: `"Low battery, pausing voice announcements."`
  - **UI Enable/Disable & Config**: Add *"Low Battery Speech Safeguard"* toggle + threshold selector (`10%` / `15%` / `20%` + *"Also pause on Battery Saver"* toggle + *"Speak Low Battery Warning"* toggle) in **Global Eligibility Rules** / **Active Intelligence**.

### 24. Roadmap Item 11 (Idea 6): Speech Volume Boost & Media Balance Control — ⚠️ PARTIAL (Standard 0–100% Volume Exists)
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - `VoiceSpeechScreen` has a persisted **Announcement Volume** slider (`0%–100%`, mapped to `KEY_PARAM_VOLUME`). `AudioAttributes` uses `USAGE_ASSISTANCE_ACCESSIBILITY` (uses system accessibility/media mixer, no active boost >100% or relative media balance control).
- **What Needs to Be Implemented**:
  - Add an **Audio Balance / Voice Boost** toggle & slider in **Voice & Speech** (paired with Feature D Ducking Level #17) so users can prioritize speech loudness relative to media volume (`LoudnessEnhancer` / audio gain boost when `>100%` boost is enabled + Enable/Disable switch).

### 25. Roadmap Item 11 (Idea 7): Tap-to-Repeat Last Spoken Announcement (Home & Notification UI) — ⚠️ PARTIAL (Service Action Only)
- **Priority / Effort**: High Value / Very Low Effort
- **Current State**:
  - `AnnouncementService` already implements `ACTION_REPEAT` (`lastAction?.let { speak(it) }`), shake-within-10s calls it, and `strings.xml` already has `notification_action_repeat` (`"Repeat"`), **but there is no Tap-to-Repeat button on the Home screen OR in the persistent notification**, and no toggle to configure it.
- **What Needs to Be Implemented**:
  - Add a **Tap-to-Repeat Last Announcement** quick action button on `DashboardScreen` (Home Quick Actions / Active card when a last announcement exists) + optional **"Repeat"** action in `AnnouncementNotification` when idle.
  - Track `hasLastAnnouncement: Boolean` cleanly in `ServiceStatusMonitor` / `DashboardUiState` (content-free, preserving privacy).
  - **UI Enable/Disable & Config**: Add toggle in **Active Intelligence** / Notification Controls to enable/disable the Repeat shortcut on Home & persistent notification.

### 26. Roadmap Item 12 (Idea 12 Enhancements) & `SmartContentCleaner` Startup Sync Bug — ⚠️ PARTIALLY IMPLEMENTED
- **Priority / Effort**: High Value / Low Effort
- **Current State**:
  - `SmartContentCleaner.TAPBACK_PATTERN` handles quoted reactions (e.g., `Rahul loved "Happy birthday!"` $\to$ `"Reacted to a message"`), with UI toggle `isReactionFilterEnabled` in **Active Intelligence**.
  - **Gap 1 — Missing Common Unquoted Patterns**: Regex only matches strings ending in quotes (`"..."` / `'...'`), missing `"Liked a photo"`, `"Rahul reacted ❤️ to your message"`, `"Laughed at your message"`, etc.
  - **Gap 2 — Condense vs Silence Config**: No mode selector between **Condense** (*"Reacted to a message"*) vs **Silence** (skip speaking reactions altogether).
  - **Gap 3 — Startup Hydration Bug (All `SmartContentCleaner` Flags & `appAliases`)**: `SessionStore` flags (`isUrlSummarizerEnabled`, `isReactionFilterEnabled`, etc.) and `HearItSettings.appOverrides[*].spokenName` are only synced when ViewModels are opened/toggled — **never** hydrated in `HearItApplication.onCreate()` or `AnnouncementService.onCreate()`. After a phone reboot or background restart, `SmartContentCleaner` reverts to default `true`/empty aliases!
- **What Needs to Be Implemented**:
  - Expand `TAPBACK_PATTERN` cleanly to handle both quoted reactions (`loved "..."`) AND unquoted social reactions (`Liked a photo`, `reacted <emoji> to a message`, `Laughed at an image`, etc.).
  - Add Reaction Filter Mode UI inside **Active Intelligence $\to$ Smart Content Cleaning & NLP**: **Condense** vs **Silence** (skip speaking completely in `AnnouncementPipeline`).
  - Hydrate ALL `SmartContentCleaner` flags (from `SessionStore`) and `appAliases` (from `SettingsSource` / `HearItSettings`) on service startup in `AnnouncementService.onCreate()` and on DataStore updates!

### 27. Roadmap Item 14 (Idea 15 Enhancements): Bluetooth Auto-Start/Stop Device Filtering & Separate Controls — ⚠️ PARTIAL
- **Priority / Effort**: High Value / Low Effort
- **Current State**:
  - `BluetoothDeviceReceiver` (`ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED`) already starts and stops `AnnouncementService` when `sessionStore.isBluetoothAutoStartEnabled` is true (with toggle in **Active Intelligence**).
  - **Gap 1 — Triggers on Non-Audio Bluetooth Devices**: Does not check whether the connected Bluetooth device is an audio/headset profile (`BluetoothClass` audio/A2DP/HFP) or in `allowedBluetoothIds` — so connecting a smartwatch, car OBD, or fitness band triggers HearIt!
  - **Gap 2 — Combined Toggle**: Auto-Start and Auto-Stop share one toggle with no option to configure separately or restrict only to designated allowed earphones.
- **What Needs to Be Implemented**:
  - Verify `BluetoothDevice` audio class / active `AudioManager` audio output (or check `AudioOutputSettings.allowedBluetoothIds` when non-empty / configured) before starting or stopping on ACL events.
  - **UI Enable/Disable & Config**: Split/expand in **Active Intelligence** / **Devices**: *Auto-Start on Earphones Connect* toggle, *Auto-Stop on Disconnect* toggle, and *"Only Designated / Allowed Earphones"* toggle.

### 28. Roadmap Item 14 (Idea 16): Spam Cooldown Throttle per Sender / Chat — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Low-Medium Effort
- **Current State**:
  - `AnnouncementPipeline` currently only deduplicates **identical** text (`isContentDuplicate`, 20s) — if one contact/chat rapid-fires 10–20 *different* messages within 1 minute, HearIt announces every message!
- **What Needs to Be Implemented**:
  - Track sliding-window per `(packageName, sender/conversation)` message counts in `AnnouncementPipeline`: once more than N announcements (default `3`) occur within the cooldown window (default `60s`), throttle remaining rapid messages and emit a brief periodic summary (e.g., `"Rahul sent 5 more messages"`) or cooldown skip (`SkipReason.DUPLICATE_UPDATE` / new reason).
  - **UI Enable/Disable & Config**: Add *"Spam Cooldown Throttle"* toggle + max messages threshold (`2` / `3` / `5`) & cooldown window (`30s` / `60s` / `120s`) in **Active Intelligence** / **Preferences**.
- **Files Likely Touched**:
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementPipeline.kt` & `SessionStore.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt` / `GeneralSettingsViewModel.kt`
  - Unit tests in `AnnouncementPipelineTest.kt`

### 29. Roadmap Item 15 (Idea 21): "Must-Hear" Critical Keyword Breakthrough — Custom Keywords, Alert Chime & Config UI — ⚠️ PARTIAL (Hardcoded Engine Only)
- **Priority / Effort**: High Value / Low-Medium Effort
- **Current State**:
  - `EligibilityRules.isEmergencyBreakthrough()` (`core/rules`) hardcodes 4 keywords (`"emergency"`, `"urgent"`, `"icu"`, `"hospital"`) to bypass Low Battery Auto-Mute, Temporary Mute, and DND.
  - **Missing**:
    1. **UI Enable/Disable & Keyword Manager**: No toggle to enable/disable Critical Keyword Breakthrough, and no UI to add/remove custom keywords (e.g., `"Server Down"`, `"PagerDuty"`, `"SOS"`) or load starter defaults.
    2. **Distinctive Chime / Alert Cue**: Critical breakthrough messages play the standard beep instead of a distinctive emergency double/triple alert chime (or `"Critical alert:"` prefix).
    3. **Muted-App / Channel Config**: Currently skips breakthrough check if `appEnabled == false`; add toggle option (*"Allow breakthrough even if app is disabled/muted"* vs *"Enabled apps only"*).
- **Files Likely Touched**:
  - `core/common/.../EligibilitySettings.kt` & `core/datastore/.../SettingsCodec.kt`
  - `core/rules/.../EligibilityRules.kt` & `core/rules/src/test/...`
  - `app/src/main/java/com/jayanth/hearit/applications/GlobalSettingsUiState.kt` & `GlobalSettingsScreen.kt` (and/or `IntelligenceControlsScreen.kt`)
  - `app/src/main/java/com/jayanth/hearit/service/AndroidTextToSpeechEngine.kt` & `AnnouncementService.kt`

### 30. Roadmap Item 16 (Idea 26): Pocket Detection via Proximity Sensor (In-Pocket vs In-Hand) — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - `RuntimeState` (`core/common`) has a dummy unused field `val isProximityCovered: Boolean = true`.
  - No `Sensor.TYPE_PROXIMITY` listener exists in `AnnouncementService`, `EligibilityRules` never checks `state.isProximityCovered`, and there is **no UI setting or configuration** anywhere.
- **What Needs to Be Implemented**:
  - **Sensor Listener (`AnnouncementService.kt`)**: Register `Sensor.TYPE_PROXIMITY` listener on `SensorManager` when enabled (cleanly unregistered in `onDestroy` and when disabled; treat `distance < minOf(sensor.maximumRange, 5f)` as covered/in-pocket, updating `isProximityCovered` in `RuntimeStateReader` / service state).
  - **Rule Evaluation (`EligibilityRules.kt` & `EligibilitySettings`)**:
    - Only enforce when Pocket Detection is enabled **and** Priority Announce is inactive (`!state.priorityAnnounceActive` so priority alerts always break through).
    - Configurable behavior mode: e.g., **"Only Announce When in Pocket (Sensor Covered)"** (`SkipReason` when uncovered/in hand) OR optional Queue-until-pocket / Immediate Skip, plus whether Headphones Bypass (`"Even if uncovered when headphones connected"` = Off by default per Idea 26 spec).
  - **UI Enable/Disable & Configuration**: Add *"Pocket Detection (Proximity Sensor)"* toggle + mode/rule configuration card inside **Active Intelligence** (`IntelligenceControlsScreen`) and/or **Global Eligibility Rules** (with live proximity sensor availability badge if a tablet/emulator lacks `TYPE_PROXIMITY`).
- **Files Likely Touched**:
  - `core/common/.../RuntimeState.kt`, `EligibilitySettings.kt`, `SkipReason.kt`
  - `core/datastore/.../SettingsCodec.kt` & `core/rules/.../EligibilityRules.kt`
  - `app/src/main/java/com/jayanth/hearit/service/AnnouncementService.kt` & `SessionStore.kt`
  - `app/src/main/java/com/jayanth/hearit/intelligence/IntelligenceControlsScreen.kt` / `GlobalSettingsScreen.kt`
  - Unit tests in `EligibilityRulesTest.kt`

### 31. Roadmap Item 18 (Idea 39 Enhancement): Per-App Auto-Dismiss Override — ⚠️ GLOBAL ONLY
- **Priority / Effort**: Medium Value / Low Effort
- **Current State**:
  - `AnnouncementService.finishSpeech()` only checks global `sessionStore.isAutoDismissEnabled` (toggle inside **Active Intelligence**). There is **no** per-app override (`AppOverride` / `AppSettingsUiState`) to keep or dismiss notifications specifically per app.
- **What Needs to Be Implemented**:
  - Add `autoDismiss: Boolean? = null` (inherits global default = ON) to `AppOverride` (`HearItSettings.kt`, `SettingsCodec.kt`) and `AppSettingsUiState` (`ApplicationsModels.kt`).
  - Resolve per-app in `AnnouncementService.finishSpeech()` before calling `dismissNotification(...)`.
  - Add an **"Auto-Dismiss After Speaking"** toggle with *"Inherit global"* option inside Per-App Details (`ApplicationsScreen.kt` when app is enabled).

### 32. Roadmap Item 18 (Idea 43 Critical Fix): Wire Periodic Spoken Check-in Scheduler & Missing Checkboxes — ⚠️ UI ONLY / NOT RUNNING
- **Priority / Effort**: High Value (Broken Feature) / Low-Medium Effort
- **Current State**:
  - `PeriodicCheckinCard` (`GeneralSettingsScreen.kt` & `IntelligenceControlsScreen.kt`) saves `isPeriodicCheckinEnabled`, `periodicCheckinIntervalMinutes` (`15m`/`30m`/`1h`/`2h`), `periodicCheckinAnnounceTime`, and `periodicCheckinAnnounceBattery` to `SessionStore`.
  - **Critical Gap 1 — Never Executes**: `AnnouncementService` **never reads** `isPeriodicCheckinEnabled` and has **zero** scheduler/runnable to trigger check-ins!
  - **Gap 2 — Missing Checkboxes**: Spec calls for selecting **Time**, **Phone Battery %**, **Earphones Battery %**, and **Unread / Summary Count**, whereas UI currently only has Time & Battery chips.
- **What Needs to Be Implemented**:
  - Add periodic check-in runnable/clock check inside `AnnouncementService` (honoring `periodicCheckinIntervalMinutes`, active audio output & call/DND/mute guards, never interrupting active notification speech).
  - Read Phone Battery % (`BatteryManager`), Earphones Battery % (reflecting `BluetoothDevice.getBatteryLevel` via active Bluetooth A2DP/HFP metadata when supported), Time (`SimpleDateFormat`), and Summary Count (`history` / session announced+pending count).
  - Add `periodicCheckinAnnounceEarphonesBattery` and `periodicCheckinAnnounceSummary` checkboxes/chips to `SessionStore`, `GeneralSettingsUiState`, `GeneralSettingsViewModel`, and `PeriodicCheckinCard`.

### 33. Roadmap Item 20 (Idea 53): Multi-App Smart Notification Batching Digest (2–3s Burst Collector) — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - `AnnouncementQueue` groups notifications strictly within **one package** (`it.snapshot.packageName == first.snapshot.packageName`).
  - `AnnouncementTemplate.build()` enforces `require(snapshots.all { it.packageName == packageName })`. Multi-app simultaneous bursts (e.g. WhatsApp + Messages within 2s) cannot be combined into a single `"You have 2 notifications: Rahul says Hey, and Mom says Call me back"` digest.
- **What Needs to Be Implemented**:
  - Add configurable **Multi-App Digest Mode** inside `AnnouncementQueue.pollReady()` + `AnnouncementTemplate.buildMultiAppDigest()` (collecting ready pending items across packages arriving within configurable window e.g. `2s` / `3s` / `5s` up to `maxMessagesPerAnnouncement`).
  - **UI Enable/Disable & Config**: Add *"Multi-App Burst Digest"* toggle + burst window (`2s` / `3s` / `5s`) selector inside **Preferences $\to$ Grouping & Batching** / **Active Intelligence**.

### 34. Roadmap Item 20 (Idea 54): Ambient Noise Level Auto-Volume Boost (`>70 dB`) — ❌ NOT IMPLEMENTED
- **Priority / Effort**: Medium Value / Medium Effort
- **Current State**:
  - No `RECORD_AUDIO` permission in `AndroidManifest.xml`, no `AudioRecord` / `MediaRecorder` amplitude sampling, and no UI toggles/thresholds anywhere.
- **What Needs to Be Implemented**:
  - Request runtime `Manifest.permission.RECORD_AUDIO` gracefully when user enables the feature (never required by default; clear privacy disclosure: *on-device instantaneous 150ms amplitude check only, nothing ever recorded or saved*).
  - Sample short RMS amplitude before speaking, compute approximate dB SPL, and when `dB >= configuredThreshold` (e.g. `65 dB` / `70 dB` / `75 dB`), boost TTS volume/gain cleanly (`volumeLevel` + boost gain).
  - **UI Enable/Disable & Config**: Add *"Ambient Noise Auto-Boost"* switch (prompts `RECORD_AUDIO` permission if needed) + noise threshold (`65 / 70 / 75 dB`) & boost amount (`+15% / +20% / +30%`) inside **Voice & Speech** / **Active Intelligence**.

---

## ⌚ Wear OS Companion Backlog (Confirmed Design)

### 35. Wear OS Phone-Side UI: Watch Forwarding Toggle & Max-4 Watch Apps Selector — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - `WearOsCompanionCard` (`DevicesScreen.kt` & `IntelligenceControlsScreen.kt`) only displays connection status, last sync time, enabled apps count, and *"Sync Enabled Apps to Watch"* — it mirrors all `enabledApps` with no separate watch toggle or max-4 selector.
- **Planned Implementation**:
  - Add persisted settings (`watchForwardingEnabled: Boolean = true`, `watchFilterApps: List<String>` max 4 ordered packages, `watchHapticsEnabled: Boolean = false`).
  - Inside `WearOsCompanionCard` (or a dedicated sheet/dialog opened from *"Configure Watch Apps"*):
    - **Master Switch**: Enable/Disable Watch Companion Notification Forwarding.
    - **App Picker (Max 4)**: Choose up to **4 apps** whose notifications go to the watch (searchable app list showing `"Selected: X / 4"` counter, auto-disabling extra choices at 4, and triggering instant sync on save).
    - Toggle for **Watch Haptic Pulse** when a mirrored alert arrives.

### 36. Wear OS Option A (Independent Forwarding) & Persistent `DataClient` Config Sync with Real App Icons — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Medium Effort
- **Confirmed Decision (Option A)**: Notifications for the selected 4 Watch Apps MUST forward to the Watch **even when the Phone TTS speech service (`AnnouncementService`) is stopped** (not dependent on earphones).
- **Current State**:
  - `AnnouncementService.onPosted()` calls `WearDataLayerSync.syncNotification(this, snapshot)` — so if HearIt service isn't started, notifications never reach the watch.
  - `/config/sync` currently uses transient `MessageClient`, which drops if the watch is unreachable when edited, and does not send app icon bitmaps.
- **Planned Implementation**:
  - Forward notifications directly via listener/application-level hook in `HearItNotificationListenerService` (when `watchForwardingEnabled == true` and `snapshot.packageName in watchFilterApps`, guarded cleanly against duplicates when `AnnouncementService` is also running).
  - Switch `/config/sync` to **`DataClient.putDataItem()`** (with timestamp & `DataClient` `Asset` / compressed `48×48` PNG icon bytes per selected package, ordered `1..4`, plus `watchForwardingEnabled` & `watchHapticsEnabled`) so config + icons automatically persist and sync upon watch reconnect.

### 37. Wear OS App UI Overhaul: Connection Green Dot, Real App Icon Filters (Max 4 + ALL, No Categories) & Empty State — ❌ NOT IMPLEMENTED
- **Priority / Effort**: High Value / Medium Effort
- **Current State**:
  - `wear/.../MainActivity.kt` has hardcoded `FilterCategory` (`All / Chat / SMS / Finance`), no phone connection dot indicator, and shows notifications filtered only against `enabledApps` (`phoneEnabledNotifications`).
- **Planned Implementation**:
  - **Connection Status Indicator**: Show a live status badge right in the top `HearIt` header (`● HearIt` with a **Green dot** when connected to paired phone, **Red/Grey dot** when disconnected + live `NodeClient` check on resume/refresh).
  - **Remove Categories Completely**: Delete `FilterCategory` (`CHAT`, `SMS`, `BANKING`).
  - **Dynamic Real App Icon Filter Bar**: Top filter row renders **All** icon + **up to 4 real app icon buttons** decoded from the synced phone app icon bitmaps (`ImageBitmap`, only rendering as many slots as the user configured, up to 4). Clicking an icon filters the list to that `packageName` cleanly.
  - **"No apps enabled" Empty State**: Show `"No apps enabled"` + helper text `"Enable up to 4 apps for Watch in HearIt on your phone"` whenever synced watch apps are empty (`watchApps.isEmpty()`), and handle `"Not synced / Phone offline"` clearly if no sync has ever arrived yet.
  - Subscribe to `/config/sync` on both `DataClient.OnDataChangedListener` (`WearNotificationListenerService.onDataChanged`) and `MessageClient`.

### 38. Wear OS Manifest & Play Store Companion Compliance — ⚠️ FIXES NEEDED
- **Priority / Effort**: High Value (Play Store Blocker) / Very Low Effort
- **Current State**:
  - `wear/src/main/AndroidManifest.xml` has `com.google.android.wearable.standalone = "true"` (wrong for a phone-dependent companion app!) and `android:icon="@android:drawable/sym_def_app_icon"`.
- **Planned Implementation**:
  - Set `<meta-data android:name="com.google.android.wearable.standalone" android:value="false" />`.
  - Replace system default icon with HearIt's adaptive/branded launcher icons in `wear/src/main/res/`.
  - Keep arriving Watch notification updates silent by default (respecting synced `watchHapticsEnabled` toggle if enabled) and verify round-screen padding (`TimeText` + `PositionIndicator` + rotary `RSB` scroll).

---

## ✅ Already Verified / Implemented Features in this Audit

1. **Item 1.1 Phone Reboot Auto-Recovery (`BOOT_COMPLETED`)**: Fully implemented with `BootReceiver` and `RECEIVE_BOOT_COMPLETED`.
2. **Item 1.2 Direct Battery Optimization Exemption**: Fully implemented with `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (1-tap OS system prompt) in Setup and Diagnostics.
3. **Item 2.1 Multi-Action Persistent Notification**: Fully implemented in `AnnouncementNotification.kt` (Dynamic actions: Pause/Skip/Stop when speaking, Priority Toggle/Mute/Stop when idle).
4. **Item 5.1 System TTS Settings Direct Shortcut**: Fully implemented in `VoiceSpeechScreen.kt` (`SystemTtsSettingsCard` + Voice Picker shortcut) and `DiagnosticsScreen.kt` firing `Intent("com.android.settings.TTS_SETTINGS")`.
5. **Item 6.1 System Apps vs User Apps Filter**: Implemented via `AppCatalog.kt` (`FLAG_SYSTEM` detection) and `FilterChip` (*"System Apps Hidden" / "System Apps Shown"*) in `ApplicationsScreen.kt`. *(Follow-up fix tracked in Item 6.2: ensure "Enable all visible" respects `showSystemApps` & filter tab).*
6. **Item 7.1 Priority Ordering in Queue**: Fully implemented end-to-end — Per-app **Speech Priority** picker (`Low` / `Normal` / `High` / `Critical`) in `ApplicationsScreen.kt`, passed via `AnnouncementService` (`settingsSource.priorityFor`) to `AnnouncementQueue.pollReady()`, which orders by highest `priority.rank` then FIFO without interrupting active speech.
7. **Item 9.2 A (Basic URL Summarization & Emoji Spam Reducer)**: Basic hardcoded `summarizeUrls` & `collapseEmojiSpam` implemented in `SmartContentCleaner.kt` with toggles in **Active Intelligence**. *(Upgrading URL rules to predefined/custom UI & adding Digit-by-Digit Number formatting are tracked in Backlog #12 & #13).*
8. **Item 9.3 Intelligent App Name Deduplication (Back-to-Back Same App)**: Fully implemented in `AnnouncementPipeline.tick()` — automatically detects consecutive announcements from the same `packageName` within 45 seconds (`45_000L`) and omits repeating the app name (`includeAppName = false`) with zero configuration. *(UI config tracked in Backlog #19).*
9. **Item 9.5 Pipeline Hardening (Listener Watchdog, MessagingStyle/InboxStyle Parser & 1-Tap Battery Exemption)**: Implemented across `HearItNotificationListenerService.kt` + `AnnouncementService.kt` (periodic `requestRebindIfDisconnected()` watchdog every ~22.5s and `onListenerDisconnected()` rebind), `StatusBarNotificationMapper.kt` (`EXTRA_MESSAGES` & `EXTRA_TEXT_LINES` fallback parsing), and `SetupProbes.kt` (1-tap `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).
10. **Item 10 Feature A (Shake or Flip Phone to Stop Speech)**: Fully implemented in `AnnouncementService.sensorEventListener` (`TYPE_ACCELEROMETER` shake > 15.5 m/s² or face-down `z < -8.5f`) calling `engine.stop()` without triggering temporary mute, with `SessionStore.isShakeToStopEnabled` toggle in **Active Intelligence**.
11. **Item 11 Idea 1 & Idea 6 Engines**: Soft pre-speech earcon chime (`AndroidTextToSpeechEngine.kt` ToneGenerator `120ms`) and `Announcement Volume` slider (`0–100%` in `VoiceSpeechScreen.kt`) exist; wiring earcon/delay UI controls to persistence & engine (#18) and adding true boost >100% / balance (#24) are in the backlog.
12. **Item 12 Idea 12 (Basic Quoted Tap-Back Reaction Filter)**: Implemented via `TAPBACK_PATTERN` in `SmartContentCleaner.kt` with toggle `isReactionFilterEnabled` in **Active Intelligence**. *(Unquoted patterns, Silence vs Condense mode, and `SessionStore` startup hydration fix are tracked in Backlog #26).*
13. **Item 14 Idea 15 & Idea 19 Core Behavior**: `BluetoothDeviceReceiver` (`ACTION_ACL_CONNECTED`/`DISCONNECTED`) auto-starts/stops service via `SessionStore.isBluetoothAutoStartEnabled`, and `RuntimeStateReader` + `EligibilityRules` mute below 15% battery or Battery Saver. *(Designated audio device filtering & separate UI configs tracked in Backlog #27 & #23).*
14. **Item 15 Idea 22 (Per-Device Sound Output Test)**: Implemented in `DevicesScreen.kt` — each connected Bluetooth accessory row has a 1-tap speaker `IconButton` playing a `ToneGenerator(STREAM_MUSIC, 70)` test beep. *(Note: plays on active media route without `ToneGenerator.release()` or device-specific TTS speech routing).*
15. **Item 16 Idea 27 (Markdown & Formatting Syntax Stripper)**: Implemented via `SmartContentCleaner.stripMarkdown()` (`SmartContentCleaner.kt` + `SmartContentCleanerTest.kt`) with toggle `isMarkdownStripperEnabled` in **Active Intelligence**. *(Startup hydration fix tracked in Backlog #26).*
16. **Item 16 Idea 29 (Physical Volume Key Silence)**: Implemented via `AnnouncementService.volumeReceiver` (`android.media.VOLUME_CHANGED_ACTION` calling `engine.stop()`) with toggle `isVolumeKeyToStopEnabled` in **Active Intelligence** $\to$ **Gestures & Playback Controls**.
17. **Item 18 Idea 41 (Shake Within 10s to Repeat)**: Implemented in `AnnouncementService.sensorEventListener` (`lastSpeechEndTimeMillis <= 10_000L`) replaying `lastAction` *(separate toggle & timer config tracked in Backlog #19)*.
18. **Item 19 Idea 48 (Bot & Automated Sender Cleaning)**: Implemented via `SmartContentCleaner.cleanSender()` (`BOT_SENDER_PATTERN`, `SHORTCODE_SENDER_PATTERN`) with `isBotSenderCleaningEnabled` toggle in **Active Intelligence** *(startup hydration tracked in Backlog #26)*.
19. **Item 20 Idea 52 (Currency & Unit Spoken Normalizer)**: Implemented via `SmartContentCleaner.normalizeUnitsAndCurrencies()` (`₹`, `$`, `€`, `£`, `km/h`, `mph`, `GB`/`MB`/`KB`, `mins`/`hrs`/`secs`) with `isCurrencyNormalizerEnabled` toggle in **Active Intelligence** *(startup hydration tracked in Backlog #26)*.
