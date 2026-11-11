# HearIt — Backlog Implementation Status Tracker

Master tracking document for all **38 backlog items** defined in [`hearit_implementation_backlog.md`](hearit_implementation_backlog.md).

- **Phone Test & Verification Target**: OnePlus Nord (`b6774a1d`)
- **Wear OS Test & Verification Target**: Wear OS Emulator (`emulator-5554`)
- **Cross-Cutting Rule**: Every feature has both an **Enable/Disable toggle** and **user-configurable settings in UI**, persisted across restarts.

## Summary Progress

- **Total Backlog Items**: 38
- **Code & Unit Tests Done (Pending Device/Screenshot Verification)**: 7 / 38 (Batch 1: `#3, #12, #13, #14, #16, #21, #26`)
- **Verified with Screenshots**: 0 / 38
- **Pending**: 31 / 38

## Status Table

| # | Backlog Item | Batch | Status | Unit Tests | UI Toggle & Config | Device / Screenshot Proof |
|---|---|---|---|---|---|---|
| 1 | Bluetooth Headset Button Controls (`MediaSession`) | Batch 2 | Pending | — | — | — |
| 2 | Global DND & Phone Call Handling Settings UI | Batch 5 | Pending | — | — | — |
| 3 | Text Pronunciation & Acronym Dictionary | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + Dictionary Dialog + TTS Test | Pending final verify |
| 4 | Priority Announce Configurable Expiry Durations | Batch 4 | Pending | — | — | — |
| 5 | Priority Announce Quick-Access List | Batch 5 | Pending | — | — | — |
| 6 | Per-App Voice / Pitch / Speed Overrides | Batch 5 | Pending | — | — | — |
| 7 | Per-App Batch Actions & Multi-Select Mode | Batch 5 | Pending | — | — | — |
| 8 | Manual "Clear Queue" Action | Batch 4 | Pending | — | — | — |
| 9 | History Date Range Filtering UI | Batch 5 | Pending | — | — | — |
| 10 | History Export (CSV / TXT via SAF) | Batch 5 | Pending | — | — | — |
| 11 | History Detail Modal Visible Scrollbar Indicator | Batch 5 | Pending | — | — | — |
| 12 | OTP & Phone Number Digit-by-Digit Speech Formatting | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + OTP/Phone + Grouping Chips | Pending final verify |
| 13 | Smart Link & URL Summarization Configurable Predefined & Custom Rules | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + URL Rules Dialog + Load Presets | Pending final verify |
| 14 | Category Intelligence (Messaging, SMS/RCS, Email) & Config UI | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest`, `AnnouncementTemplateTest`, `AnnouncementPipelineTest` | Active Intelligence `CategoryIntelligenceCard` + Promo Dialog | Pending final verify |
| 15 | Local AI Strategy — Tier 2 (AICore & ML Kit) + Support Status UI | Batch 6 | Pending | — | — | — |
| 16 | App Categories & Email / Messaging Grouping UI | Batch 1 | Implemented (Tests Pass) | `SettingsCodecTest`, `SmartContentCleanerTest` | `AppCategoriesDialog` in Active Intelligence & Applications | Pending final verify |
| 17 | Background Audio Ducking Level Control | Batch 2 | Pending | — | — | — |
| 18 | Voice Tuning Polish (`SmartAudioPolishCard` Real Wiring) | Batch 2 | Pending | — | — | — |
| 19 | Configuration UI for Built-In Automatic Features | Batch 3 | Pending | — | — | — |
| 20 | Chatty Group Flood Defense (Sender Burst Aggregator + UI) | Batch 4 | Pending | — | — | — |
| 21 | Sender Contact Name Sanitizer — UI Toggle & Config | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest` | Active Intelligence Switch + Emoji/Brackets Chips | Pending final verify |
| 22 | "Catch-Up: What Did I Miss?" Summary on Connect | Batch 3 | Pending | — | — | — |
| 23 | Low Battery / Battery Saver Speech Safeguard & Threshold UI | Batch 3 | Pending | — | — | — |
| 24 | Speech Volume Boost & Media Balance Control | Batch 2 | Pending | — | — | — |
| 25 | Tap-to-Repeat Last Spoken Announcement (Home & Notification UI) | Batch 4 | Pending | — | — | — |
| 26 | Emoji Reaction Modes & `SmartContentCleaner` Startup Sync Fix | Batch 1 | Implemented (Tests Pass) | `SmartContentCleanerTest`, `AnnouncementPipelineTest` | Active Intelligence Switch + Condense/Silence Chips + `IntelligentSettingsSync` | Pending final verify |
| 27 | Bluetooth Auto-Start/Stop Device Filtering & Separate Controls | Batch 3 | Pending | — | — | — |
| 28 | Spam Cooldown Throttle per Sender / Chat | Batch 4 | Pending | — | — | — |
| 29 | "Must-Hear" Critical Keyword Breakthrough — Custom Keywords & Chime | Batch 3 | Pending | — | — | — |
| 30 | Pocket Detection via Proximity Sensor (In-Pocket vs In-Hand) | Batch 3 | Pending | — | — | — |
| 31 | Per-App Auto-Dismiss Override | Batch 5 | Pending | — | — | — |
| 32 | Wire Periodic Spoken Check-in Scheduler & Missing Checkboxes | Batch 3 | Pending | — | — | — |
| 33 | Multi-App Smart Notification Batching Digest (2–3s Burst Collector) | Batch 4 | Pending | — | — | — |
| 34 | Ambient Noise Level Auto-Volume Boost (`>70 dB`) | Batch 2 | Pending | — | — | — |
| 35 | Wear OS Phone UI: Watch Forwarding Toggle & Max-4 Watch Apps Picker | Batch 7 | Pending | — | — | — |
| 36 | Wear OS Option A Independent Forwarding & Persistent `DataClient` Icons Sync | Batch 7 | Pending | — | — | — |
| 37 | Wear OS App UI Overhaul: Connection Green Dot, Max-4 Icons + ALL, Empty State | Batch 7 | Pending | — | — | — |
| 38 | Wear OS Manifest (`standalone="false"`) & Play Store Companion Compliance | Batch 7 | Pending | — | — | — |

## Batch Verification Log

- **Batch 0 (Baseline)**: `./gradlew test :app:assembleDebug :wear:assembleDebug` PASSED (0 failures).
- **Batch 1 (`#3, #12, #13, #14, #16, #21, #26`)**: `./gradlew test :app:assembleDebug` PASSED across `:core:common`, `:core:datastore`, `:core:tts`, `:core:rules`, `:core:queue`, `:core:notifications`, `:core:database`, `:app`.
