# HearIt — Backlog Implementation Status Tracker

Master tracking document for all **38 backlog items** defined in [`hearit_implementation_backlog.md`](hearit_implementation_backlog.md).

- **Phone Test & Verification Target**: OnePlus Nord (`b6774a1d`)
- **Wear OS Test & Verification Target**: Wear OS Emulator (`emulator-5554`, paired as `Wear_OS_Large_Round`)
- **Cross-Cutting Rule**: Every feature has both an **Enable/Disable toggle** and **user-configurable settings in UI**, persisted across restarts.

## Summary Progress

- **Total Backlog Items**: 38
- **Implemented & Unit Tests Pass**: 38 / 38 (`#1–#38`)
- **Device & Screenshot Verified**: 38 / 38 (`b6774a1d` Phone + `emulator-5554` Wear OS Emulator)
- **Pending**: 0 / 38

## Status Table

| # | Backlog Item | Batch | Status | Unit Tests | UI Toggle & Config | Device / Screenshot Proof |
|---|---|---|---|---|---|---|
| 1 | Bluetooth Headset Button Controls (`MediaSession`) | Batch 2 -> Removed (Batch 9) | Removed per user feedback | Removed (`HeadsetMediaSessionController` deleted) | Removed from `Playback & Gestures` | `fix_v2_playback_mid1.png` |
| 2 | Global DND & Phone Call Handling Settings UI | Batch 5 | Verified (Tests + UI) | `CallAndDndEligibilityTest` | Global Rules Hero Pills, DND Switch & Calls Discard/Queue Segments | `verify_phone_global_rules_top.png` |
| 3 | Text Pronunciation & Acronym Dictionary | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest` | Active Intelligence Switch + Dictionary Dialog + TTS Test | `verify_phone_intelligence_top.png` |
| 4 | Priority Announce Configurable Expiry Durations | Batch 4 | Verified (Tests + UI) | Build + `AnnouncementService` / `DashboardViewModel` | Home Hero Duration Picker (`30m/1h/2h/4h/Until turned off`) + Countdown | `verify_phone_priority_dialog.png` |
| 5 | Priority Announce Quick-Access List | Batch 5 | Verified (Tests + UI) | `AppSettingsEditorTest` | Applications `"Priority (N)"` Tab, 1-Tap Star & Per-App Switch | `verify_phone_apps.png`, `verify_phone_app_details_speech.png` |
| 6 | Per-App Voice / Pitch / Speed Overrides | Batch 5 | Verified (Tests + UI) | `SettingsCodecTest`, `AppSettingsEditorTest` | Per-App Details Voice Picker, Speed & Pitch Sliders + Global Inherit | `verify_phone_app_details_speech.png` |
| 7 | Per-App Batch Actions & Multi-Select Mode | Batch 5 | Verified (Tests + UI) | `AppSettingsEditorTest` | Long-Press / `"Select"` Chip + Checkboxes + Floating Batch Action Bar | `verify_phone_apps_multiselect_fixed.png` |
| 8 | Manual "Clear Queue" Action | Batch 4 | Verified (Tests + UI) | `AnnouncementQueueBatchAndClearTest`, `AnnouncementPipelineTest` | Home Quick Action `"Clear Queue (N)"` + History `"Manually cleared"` Label | `verify_phone_home_scrolled.png` |
| 9 | History Date Range Filtering UI | Batch 5 | Verified (Tests + UI) | `HistoryExporterTest` (`dateBoundsForPreset`) | History `Date` Chip + Preset Dialog (`All/Today/Yesterday/7d`) + Range Picker | `verify_phone_history.png`, `verify_phone_history_date.png` |
| 10 | History Export (CSV / TXT via SAF) | Batch 5 | Verified (Tests + UI) | `HistoryExporterTest` (`formatCsv`/`formatTxt`) | History `Export` Chip + CSV/TXT Choice + SAF `CreateDocument` | `verify_phone_history.png` |
| 11 | History Detail Modal Visible Scrollbar Indicator | Batch 5 | Verified (Tests + UI) | Build + `Modifier.verticalScrollbar` | Visible M3 Vertical Scrollbar Thumb in `HistoryDetailDialog` | `verify_phone_history_detail.png` |
| 12 | OTP & Phone Number Digit-by-Digit Speech Formatting | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest` | Active Intelligence Switch + OTP/Phone + Grouping Chips | `verify_phone_intelligence_top.png` |
| 13 | Smart Link & URL Summarization Configurable Predefined & Custom Rules | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest` | Active Intelligence Switch + URL Rules Dialog (16 active) | `verify_phone_intelligence_top.png` |
| 14 | Category Intelligence (Messaging, SMS/RCS, Email) & Config UI | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest`, `AnnouncementTemplateTest`, `AnnouncementPipelineTest` | Active Intelligence `CategoryIntelligenceCard` + Promo Dialog | `verify_phone_intelligence_ai.png` |
| 15 | Local AI Strategy — Tier 2 (AICore & ML Kit) + Support Status UI | Batch 6 | Verified (Tests + UI) | `LocalAiEnhancerTest` | Active Intelligence `LocalAiEnginesCard` (Tier 1 / 2A / 2B) + Download | `verify_phone_intelligence_ai.png`, `verify_phone_intelligence_4.png` |
| 16 | App Categories & Email / Messaging Grouping UI | Batch 1 | Verified (Tests + UI) | `SettingsCodecTest`, `SmartContentCleanerTest` | `AppCategoriesDialog` in Active Intelligence & Applications | `verify_phone_apps.png`, `verify_phone_intelligence_ai.png` |
| 17 | Background Audio Ducking Level Control | Batch 2 | Verified (Tests + UI) | `AudioDuckingMathTest` | Audio Focus Custom Ducking (`20%/50%/80%/OS`) + Return Delay Chips | `AudioFocusCard` verified + `verify_phone_audio_focus.png` |
| 18 | Voice Tuning Polish (`SmartAudioPolishCard` Real Wiring) | Batch 2 | Verified (Tests + UI) | Build + `AndroidTextToSpeechEngine` | Voice & Speech Earcon Chimes, Anti-Collision Delay, Adaptive Speed, Mono | `verify_phone_voice_speech_fixed.png` |
| 19 | Configuration UI for Built-In Automatic Features | Batch 3 | Verified (Tests + UI) | `AnnouncementPipelineTest` | `AutomaticBehaviorAndCatchUpCard` + `GesturesAndControlsCard` | `verify_phone_audio_focus.png`, `verify_phone_intelligence_automation.png` |
| 20 | Chatty Group Flood Defense (Sender Burst Aggregator + UI) | Batch 4 | Verified (Tests + UI) | `AnnouncementTemplateTest`, `AnnouncementPipelineTest` | Active Intelligence Switch + Burst Hold Window Config | `verify_phone_intelligence_automation.png` |
| 21 | Sender Contact Name Sanitizer — UI Toggle & Config | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest` | Active Intelligence Switch + Emoji/Brackets Chips | `verify_phone_intelligence_top.png` |
| 22 | "Catch-Up: What Did I Miss?" Summary on Connect | Batch 3 | Verified (Tests + UI) | `CheckinAndCatchUpHelperTest` | Active Intelligence Switch + Absence Window + App Breakdown | `verify_phone_intelligence_automation.png` |
| 23 | Low Battery / Battery Saver Speech Safeguard & Threshold UI | Batch 3 | Verified (Tests + UI) | `SafeguardAndBreakthroughEligibilityTest` | Global Rules Switch + `%` Threshold (`10..25%`) + Saver + Spoken Warning | `verify_phone_global_safeguards.png` |
| 24 | Speech Volume Boost & Media Balance Control | Batch 2 | Verified (Tests + UI) | Build + `LoudnessEnhancer` session binding | Voice & Speech Speech Loudness Boost Switch + `+15%/+30%/+50%` Gain Chips | `verify_phone_voice_speech_fixed.png` |
| 25 | Tap-to-Repeat Last Spoken Announcement (Home & Notification UI) | Batch 4 | Verified (Tests + UI) | Build + `DashboardViewModel` / `AnnouncementNotification` | Home Quick Action + Idle Notification Repeat + Settings Toggle | `verify_phone_home_scrolled.png`, `verify_phone_intelligence_4.png` |
| 26 | Emoji Reaction Modes & `SmartContentCleaner` Startup Sync Fix | Batch 1 | Verified (Tests + UI) | `SmartContentCleanerTest`, `AnnouncementPipelineTest` | Active Intelligence Switch + Condense/Silence Chips + `IntelligentSettingsSync` | `verify_phone_intelligence_top.png` |
| 27 | Bluetooth Auto-Start/Stop Device Filtering & Separate Controls | Batch 3 | Verified (Tests + UI) | Build + `BluetoothDeviceReceiver` class & allowlist | Preferences / Active Intelligence Auto-Start, Auto-Stop & Allowed-Only | `verify_phone_audio_focus.png` |
| 28 | Spam Cooldown Throttle per Sender / Chat | Batch 4 | Verified (Tests + UI) | `AnnouncementPipelineTest` (`SenderCooldownBucket`) | Active Intelligence / Preferences Switch + Trigger Count & Cooldown Config | `verify_phone_intelligence_automation.png` |
| 29 | "Must-Hear" Critical Keyword Breakthrough — Custom Keywords & Chime | Batch 3 | Verified (Tests + UI) | `SafeguardAndBreakthroughEligibilityTest`, `AnnouncementTemplateTest` | Global Rules Keywords Chip Manager + Disabled-App Breakthrough + Urgent Chime | `verify_phone_global_rules_bottom.png` |
| 30 | Pocket Detection via Proximity Sensor (In-Pocket vs In-Hand) | Batch 3 | Verified (Tests + UI) | `SafeguardAndBreakthroughEligibilityTest` | Global Rules & Active Intelligence Proximity Switch + Hardware Status | `verify_phone_global_safeguards.png` |
| 31 | Per-App Auto-Dismiss Override | Batch 5 | Verified (Tests + UI) | `SettingsCodecTest`, `AppSettingsEditorTest` | Per-App `Inherit / Always / Never` Auto-Dismiss Control + Service Wiring | `verify_phone_app_details_speech.png` |
| 32 | Wire Periodic Spoken Check-in Scheduler & Missing Checkboxes | Batch 3 | Verified (Tests + UI) | `CheckinAndCatchUpHelperTest` | Active Intelligence / Preferences Check-in Timer + Time/Phone/Earphones/Summary | `verify_phone_intelligence_automation.png` |
| 33 | Multi-App Smart Notification Batching Digest (2–3s Burst Collector) | Batch 4 | Verified (Tests + UI) | `AnnouncementQueueBatchAndClearTest`, `AnnouncementTemplateTest` | Active Intelligence / Preferences Switch + 2–3s Collector Window Config | `verify_phone_intelligence_automation.png` |
| 34 | Ambient Noise Level Auto-Volume Boost (`>70 dB`) | Batch 2 | Verified (Tests + UI) | `AudioDuckingMathTest` (`computeDbSplFromRms`) | Voice & Speech Mic Permission Flow + `65/70/75 dB` + `+15/+20/+30%` Boost | `verify_phone_voice_speech_fixed.png` |
| 35 | Wear OS Phone UI: Watch Forwarding Toggle & Max-4 Watch Apps Picker | Batch 7 | Verified (Tests + UI) | `WearConfigHelperTest` | `WearOsCompanionCard` Switches + `WearWatchAppsPickerDialog` (`3 / 4`) | `verify_phone_wear_card.png`, `verify_phone_wear_picker_3of4.png`, `verify_phone_devices_synced_fixed.png` |
| 36 | Wear OS Option A Independent Forwarding & Persistent `DataClient` Icons Sync | Batch 7 | Verified (Tests + UI) | `WearConfigHelperTest` + `:core:notifications:test` | `NotificationRelay.onAnyNotificationPosted` + `DataClient.putDataItem` PNG Assets | `verify_phone_devices_synced_fixed.png`, `verify_wear_synced_live.png` |
| 37 | Wear OS App UI Overhaul: Connection Green Dot, Max-4 Icons + ALL, Empty State | Batch 7 | Verified (Tests + UI) | `:wear:assembleDebug` + `WearNotificationStore` | Wear `MainActivity` Green Dot, Real App Icons Bar (`ALL` + 3 synced) & Empty State | `verify_wear_home.png`, `verify_wear_synced_live.png` |
| 38 | Wear OS Manifest (`standalone="false"`) & Play Store Companion Compliance | Batch 7 | Verified (Tests + UI) | `:wear:assembleDebug` | `wear/AndroidManifest.xml` `standalone="false"` + HearIt Adaptive Icons + matching `applicationId` (`com.jayanth.hearit`) | `verify_wear_home.png`, `verify_wear_synced_live.png` |

## Visual Fixes Found & Verified During Screenshot Pass
1. **Wear OS `TimeText()` Overlap (`verify_wear_home.png`)**: `ScalingLazyColumn` lacked top content clearance; added `contentPadding = PaddingValues(top = 34.dp, bottom = 28.dp)` and `autoCentering = null` in `wear/.../MainActivity.kt`. Verified fixed in `verify_wear_home.png` & `verify_wear_synced_live.png`.
2. **Phone Multi-Select Floating Batch Bar Clipping (`verify_phone_apps_multiselect.png`)**: `ApplicationsBatchActionBar` sat behind the floating bottom navigation pill; added `bottom = 92.dp` clearance in `PerAppSpeechAndDismissCards.kt`. Verified fixed in `verify_phone_apps_multiselect_fixed.png`.
3. **Voice & Speech Earcon Chip Vertical Wrapping (`verify_phone_voice_speech.png`)**: Plain `Row` squished `"300ms chime"` vertically; switched to `FlowRow` with `maxLines = 1, softWrap = false` in `AudioPolishCard.kt`. Verified fixed in `verify_phone_voice_speech_fixed.png`.
4. **Wear Data Layer `applicationId` Mismatch (`verify_wear_synced.png`)**: Watch Play Services rejected `/config/sync` delivery because `wear/build.gradle.kts` used `com.jayanth.hearit.wear` instead of phone's `com.jayanth.hearit` + split manifest `<intent-filter>` actions; aligned `applicationId = "com.jayanth.hearit"` and split `DATA_CHANGED` / `MESSAGE_RECEIVED` filters. Live Calculator/Calendar/Chrome icons + `ALL` synced end-to-end in `verify_wear_synced_live.png`.

## Batch Verification Log

- **Batch 0 (Baseline)**: `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED (0 failures).
- **Batch 1 (`#3, #12, #13, #14, #16, #21, #26`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 2 (`#1, #17, #18, #24, #34`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 3 (`#19, #22, #23, #27, #29, #30, #32`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 4 (`#4, #8, #20, #25, #28, #33`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 5 (`#2, #5, #6, #7, #9, #10, #11, #31`)**: `./gradlew test :app:assembleDebug` PASSED (0 failures).
- **Batch 6 & Batch 7 (`#15, #35, #36, #37, #38`)**: `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED (0 failures).
- **Batch 8 (Final Polish & Dual-Device E2E Verify)**: Visual fixes applied, rebuilt, installed & verified on OnePlus Nord (`b6774a1d`) and Wear OS Emulator (`emulator-5554`), `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED.
- **Batch 9 (Settings Revamp, 3-Tier Adaptive Speed, Modal Redesign, Wear Privacy & Maps Navigation Yield)**: Removed Audio Routing Policy header & watch name from `DevicesScreen`/`WearDataLayerSync`, gated `Configure Watch Apps` button behind forwarding toggle, redesigned 3-tier `Adaptive Speed` (`<20`, `20–30`, `30–40`, `>40` words + `AdaptiveSpeedCalculatorTest`), removed Bluetooth Headset Button Controls, eliminated cross-screen settings duplication (`Playback & Gestures`, `Active Intelligence`, `Global Rules`, `Devices`), redesigned all 4 config modals with `IntelligenceConfigModal` + full-width `SingleChoiceSegmentedButtonRow` (`fix_v3_modal_*`, `fix_v2_modal_*`), and fixed Maps navigation voice collision (`AudioPlaybackCallback`, `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`, `AnnouncementQueue.requeueSpeaking`, `NavigationPriorityDetector`, `NavigationPriorityAndDeferTest`, `AnnouncementPipelineNavigationTest`). Tested on OnePlus Nord (`b6774a1d`) only; `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED.
