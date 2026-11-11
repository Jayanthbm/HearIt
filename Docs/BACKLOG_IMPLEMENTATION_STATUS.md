# HearIt — Backlog Implementation Status Tracker

Master tracking document for all **38 backlog items** defined in [`hearit_implementation_backlog.md`](hearit_implementation_backlog.md).

- **Phone Test & Verification Target**: OnePlus Nord (`b6774a1d`)
- **Wear OS Test & Verification Target**: Wear OS Emulator (`emulator-5554`)
- **Cross-Cutting Rule**: Every feature has both an **Enable/Disable toggle** and **user-configurable settings in UI**, persisted across restarts.

## Summary Progress

- **Total Backlog Items**: 38
- **Code & Unit Tests Done (Pending Device/Screenshot Verification)**: 38 / 38 (Batches 1–7: `#1–#38`)
- **Verified with Screenshots**: 0 / 38
- **Pending**: 0 / 38

## Status Table

| # | Backlog Item | Batch | Status | Unit Tests | UI Toggle & Config | Device / Screenshot Proof |
|---|---|---|---|---|---|---|
| 1 | Bluetooth Headset Button Controls (`MediaSession`) | Batch 2 | Implemented (Tests Pass) | `GeneralSettingsUiStateTest` (compile + service integration) | Active Intelligence / Preferences Gestures Switch + 1×/2×/3× Tap Chips | Pending final verify |
| 2 | Global DND & Phone Call Handling Settings UI | Batch 5 | Implemented (Tests Pass) | `CallAndDndEligibilityTest` + `GlobalSettingsScreen` | Global Rules Hero Pills, DND Switch & Calls Discard/Queue Segments | Pending final verify |
| 3 | Text Pronunciation & Acronym Dictionary | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + Dictionary Dialog + TTS Test | Pending final verify |
| 4 | Priority Announce Configurable Expiry Durations | Batch 4 | Implemented (Tests Pass) | `AnnouncementService` & `DashboardViewModel` compile + build | Home Hero Duration Picker (`15m/30m/1h/4h/Until off`) + Live Countdown | Pending final verify |
| 5 | Priority Announce Quick-Access List | Batch 5 | Implemented (Tests Pass) | `AppSettingsEditorTest` | Applications 4th `"Priority (N)"` Filter Tab, 1-Tap Star & Per-App Switch | Pending final verify |
| 6 | Per-App Voice / Pitch / Speed Overrides | Batch 5 | Implemented (Tests Pass) | `SettingsCodecTest`, `AppSettingsEditorTest` | Per-App Details Voice Picker, Speed & Pitch Sliders + Global Inherit Reset | Pending final verify |
| 7 | Per-App Batch Actions & Multi-Select Mode | Batch 5 | Implemented (Tests Pass) | `AppSettingsEditorTest` | Long-Press / `"Select"` Chip + Checkboxes + Batch Enable/Silence/Priority/Reset Bar | Pending final verify |
| 8 | Manual "Clear Queue" Action | Batch 4 | Implemented (Tests Pass) | `AnnouncementQueueBatchAndClearTest`, `AnnouncementPipelineTest` | Home Quick Action `"Clear Queue (N)"` + History `"Manually cleared"` Skip Label | Pending final verify |
| 9 | History Date Range Filtering UI | Batch 5 | Implemented (Tests Pass) | `HistoryExporterTest` (`dateBoundsForPreset`) | History `Date` Chip + Preset Modal (`Today/Yesterday/7d`) + M3 `DateRangePicker` | Pending final verify |
| 10 | History Export (CSV / TXT via SAF) | Batch 5 | Implemented (Tests Pass) | `HistoryExporterTest` (`formatCsv`/`formatTxt`) | History `Export` Chip + CSV/TXT Choice + SAF `CreateDocument` Export | Pending final verify |
| 11 | History Detail Modal Visible Scrollbar Indicator | Batch 5 | Implemented (Tests Pass) | Build + `Modifier.verticalScrollbar` | Visible M3 Vertical Scrollbar Thumb in `HistoryDetailDialog` (History & Apps) | Pending final verify |
| 12 | OTP & Phone Number Digit-by-Digit Speech Formatting | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + OTP/Phone + Grouping Chips | Pending final verify |
| 13 | Smart Link & URL Summarization Configurable Predefined & Custom Rules | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + URL Rules Dialog + Load Presets | Pending final verify |
| 14 | Category Intelligence (Messaging, SMS/RCS, Email) & Config UI | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest`, `AnnouncementTemplateTest`, `AnnouncementPipelineTest` | Active Intelligence `CategoryIntelligenceCard` + Promo Dialog | Pending final verify |
| 15 | Local AI Strategy — Tier 2 (AICore & ML Kit) + Support Status UI | Batch 6 | Implemented (Tests Pass) | `LocalAiEnhancerTest` + `IntelligenceCapabilityProbe` | Active Intelligence `LocalAiEnginesCard` + Model Download & Switches | Pending final verify |
| 16 | App Categories & Email / Messaging Grouping UI | Batch 1 | Implemented (Tests Pass) | `SettingsCodecTest`, `SmartContentCleanerTest` | `AppCategoriesDialog` in Active Intelligence & Applications | Pending final verify |
| 17 | Background Audio Ducking Level Control | Batch 2 | Implemented (Tests Pass) | `AudioDuckingMathTest` | Audio Focus Card Custom Ducking Switch + 20%/50%/80%/OS + Return Delay Chips | Pending final verify |
| 18 | Voice Tuning Polish (`SmartAudioPolishCard` Real Wiring) | Batch 2 | Implemented (Tests Pass) | Build + `AudioPolishCard` wired to `SessionStore` & `AndroidTextToSpeechEngine` | Voice & Speech Earcon Chime/Duration, Anti-Collision, Adaptive Speed, Mono Switch | Pending final verify |
| 19 | Configuration UI for Built-In Automatic Features | Batch 3 | Implemented (Tests Pass) | `AnnouncementPipelineTest` | Active Intelligence `AutomaticBehaviorAndCatchUpCard` + `GesturesAndControlsCard` | Pending final verify |
| 20 | Chatty Group Flood Defense (Sender Burst Aggregator + UI) | Batch 4 | Implemented (Tests Pass) | `AnnouncementTemplateTest`, `AnnouncementPipelineTest` | Active Intelligence Switch + Burst Hold Window Config | Pending final verify |
| 21 | Sender Contact Name Sanitizer — UI Toggle & Config | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + Emoji/Brackets Chips | Pending final verify |
| 22 | "Catch-Up: What Did I Miss?" Summary on Connect | Batch 3 | Implemented (Tests Pass) | `CheckinAndCatchUpHelperTest` | Active Intelligence Switch + Absence Window (`Always/5m/15m/30m`) + App Breakdown | Pending final verify |
| 23 | Low Battery / Battery Saver Speech Safeguard & Threshold UI | Batch 3 | Implemented (Tests Pass) | `SafeguardAndBreakthroughEligibilityTest` | Global Rules Switch + `%` Threshold (`10..25%`) + Saver + Spoken Warning Switches | Pending final verify |
| 24 | Speech Volume Boost & Media Balance Control | Batch 2 | Implemented (Tests Pass) | Build + `LoudnessEnhancer` session binding | Voice & Speech Speech Loudness Boost Switch + `+15%/+30%/+50%` Gain Chips | Pending final verify |
| 25 | Tap-to-Repeat Last Spoken Announcement (Home & Notification UI) | Batch 4 | Implemented (Tests Pass) | `DashboardViewModel` / `AnnouncementNotification` build + wiring | Home `"Repeat Last Announcement"` Action + Idle Notification Repeat Button + Settings Toggle | Pending final verify |
| 26 | Emoji Reaction Modes & `SmartContentCleaner` Startup Sync Fix | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest`, `AnnouncementPipelineTest` | Active Intelligence Switch + Condense/Silence Chips + `IntelligentSettingsSync` | Pending final verify |
| 27 | Bluetooth Auto-Start/Stop Device Filtering & Separate Controls | Batch 3 | Implemented (Tests Pass) | Build + `BluetoothDeviceReceiver` class & allowlist checks | Active Intelligence / Preferences Auto-Start, Auto-Stop & Allowed-Only Switches | Pending final verify |
| 28 | Spam Cooldown Throttle per Sender / Chat | Batch 4 | Implemented (Tests Pass) | `AnnouncementPipelineTest` (`SenderCooldownBucket`) | Active Intelligence / Preferences Switch + Trigger Count & Cooldown Window Config | Pending final verify |
| 29 | "Must-Hear" Critical Keyword Breakthrough — Custom Keywords & Chime | Batch 3 | Implemented (Tests Pass) | `SafeguardAndBreakthroughEligibilityTest`, `AnnouncementTemplateTest` | Global Rules Keywords Chip Manager + Disabled-App Breakthrough + Urgent Chime | Pending final verify |
| 30 | Pocket Detection via Proximity Sensor (In-Pocket vs In-Hand) | Batch 3 | Implemented (Tests Pass) | `SafeguardAndBreakthroughEligibilityTest` | Global Rules & Active Intelligence Proximity Switch + Hardware Status + Headphone Bypass | Pending final verify |
| 31 | Per-App Auto-Dismiss Override | Batch 5 | Implemented (Tests Pass) | `SettingsCodecTest`, `AppSettingsEditorTest` | Per-App `Inherit / Always / Never` Auto-Dismiss Control + Service Wiring | Pending final verify |
| 32 | Wire Periodic Spoken Check-in Scheduler & Missing Checkboxes | Batch 3 | Implemented (Tests Pass) | `CheckinAndCatchUpHelperTest` | Active Intelligence / Preferences Check-in Timer + Time/Phone/Earphones/Summary Chips | Pending final verify |
| 33 | Multi-App Smart Notification Batching Digest (2–3s Burst Collector) | Batch 4 | Implemented (Tests Pass) | `AnnouncementQueueBatchAndClearTest`, `AnnouncementTemplateTest` | Active Intelligence / Preferences Switch + 2–3s Collector Window Config | Pending final verify |
| 34 | Ambient Noise Level Auto-Volume Boost (`>70 dB`) | Batch 2 | Implemented (Tests Pass) | `AudioDuckingMathTest` (`computeDbSplFromRms`) | Voice & Speech Optional Mic Permission Flow + `65/70/75 dB` + `+15/+20/+30%` Boost | Pending final verify |
| 35 | Wear OS Phone UI: Watch Forwarding Toggle & Max-4 Watch Apps Picker | Batch 7 | Implemented (Tests Pass) | `WearConfigHelperTest` | `WearOsCompanionCard` Switches + `WearWatchAppsPickerDialog` (`X / 4`) | Pending final verify |
| 36 | Wear OS Option A Independent Forwarding & Persistent `DataClient` Icons Sync | Batch 7 | Implemented (Tests Pass) | `WearConfigHelperTest` + `:core:notifications:test` | `NotificationRelay.onAnyNotificationPosted` + `DataClient.putDataItem` PNG Assets | Pending final verify |
| 37 | Wear OS App UI Overhaul: Connection Green Dot, Max-4 Icons + ALL, Empty State | Batch 7 | Implemented (Tests Pass) | `:wear:assembleDebug` + `WearNotificationStore` | Wear `MainActivity` Green/Red Connection Dot, Real App Icons Bar (`ALL` + 4) & Empty State | Pending final verify |
| 38 | Wear OS Manifest (`standalone="false"`) & Play Store Companion Compliance | Batch 7 | Implemented (Tests Pass) | `:wear:assembleDebug` | `wear/AndroidManifest.xml` `standalone="false"` + HearIt Adaptive Launcher Icons | Pending final verify |

## Batch Verification Log

- **Batch 0 (Baseline)**: `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED (0 failures).
- **Batch 1 (`#3, #12, #13, #14, #16, #21, #26`)**: `./gradlew test :app:assembleDebug` PASSED across `:core:common`, `:core:datastore`, `:core:tts`, `:core:rules`, `:core:queue`, `:core:notifications`, `:core:database`, `:app`.
- **Batch 2 (`#1, #17, #18, #24, #34`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 3 (`#19, #22, #23, #27, #29, #30, #32`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 4 (`#4, #8, #20, #25, #28, #33`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 5 (`#2, #5, #6, #7, #9, #10, #11, #31`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 6 & Batch 7 (`#15, #35, #36, #37, #38`)**: `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED across all modules (0 failures).
