# HearIt — Master Manual Testing Checklist & Device Verification Matrix

This is the **single, unified manual testing checklist** for HearIt across both **Phone (`:app`)** and **Wear OS (`:wear`)**.
It consolidates core architecture, playback controls, smart NLP intelligence, audio polish, hardware gestures, safety safeguards, and Wear OS companion synchronization.

---

## 📱 Testing Device Matrix

| Device / Emulator | OS Version | API Level | Type | Role | Status |
|---|---|---|---|---|---|
| **OnePlus Nord (AC2001)** | Android 12 | API 31 | Physical | Primary Testbed (OxygenOS OEM) | 🟡 In Progress |
| **Wear OS Watch / Emulator** | Wear OS 3+ / 4+ | API 30+ | Physical / Emu | Wear Companion & DataLayer | 🟡 Ready for Testing |
| **Pixel / Stock Android** | Android 13+ | API 33+ | Physical / Emu | Clean AOSP baseline | ⚪ Pending |
| **Samsung Galaxy** | Android 13+ | API 33+ | Physical | OneUI background & audio quirks | ⚪ Pending |
| **Android 10 (minSdk)** | Android 10 | API 29 | Emulator | Minimum supported SDK verification | ⚪ Pending |
| **Android 17 / Canary** | Android 17 | API 37 | Emulator | Forward compatibility & edge APIs | ⚪ Pending |

### Testing Status Legend
- ✅ **VERIFIED OK**: Tested and confirmed working on device.
- 🟡 **READY FOR TESTING**: Implemented, verified in build/unit tests, awaiting physical device run.
- ⏳ **PENDING**: Needs physical testing under specific conditions (e.g. driving, active Bluetooth call).
- ❌ **NEEDS FIX**: Bug identified during testing (record notes below).

---

## 📑 Quick Navigation
1. [Setup, Onboarding & Permissions](#1-setup-onboarding--permissions)
2. [Dashboard, Service Lifecycle & Status Bar Controls](#2-dashboard-service-lifecycle--status-bar-controls)
3. [Applications & Per-App Customization](#3-applications--per-app-customization)
4. [Announcement Eligibility, Safety & Maps Yield](#4-announcement-eligibility-safety--maps-yield)
5. [Audio Routing, Bluetooth & Focus Management](#5-audio-routing-bluetooth--focus-management)
6. [Queue, Gestures & Physical Controls](#6-queue-gestures--physical-controls)
7. [Active Intelligence & Smart NLP Engine](#7-active-intelligence--smart-nlp-engine)
8. [Voice & Speech Modulation](#8-voice--speech-modulation)
9. [Notification History & Storage Controls](#9-notification-history--storage-controls)
10. [Security, Backup & System Diagnostics](#10-security-backup--system-diagnostics)
11. [Wear OS Companion App & DataLayer Sync](#11-wear-os-companion-app--datalayer-sync)
12. [Accessibility, Theming & Hardware Edge Cases](#12-accessibility-theming--hardware-edge-cases)

---

## 1. Setup, Onboarding & Permissions

- [x] **First Launch Onboarding**: Welcome screen clearly explains app capabilities without jargon. *(Verified)*
- [x] **Notification Access Step**: Tapping button opens Android Notification Listener settings; returning with permission advances step. *(Verified)*
- [x] **Bluetooth / Nearby Devices Permission**: Runtime prompt matches Android version (`BLUETOOTH_CONNECT` on API 31+, no prompt on API 29/30). *(Verified)*
- [x] **TTS Engine Verification**: Checks TTS availability and reports missing language voices honestly. *(Verified)*
- [x] **1-Tap Battery Exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: Direct system dialog prompts user (*"Let HearIt run in background?"*) without sending user to navigate complex OEM settings menus. *(Verified)*
- [x] **Initial App Selection**: Starts with **all apps disabled** by default so user has full control. *(Verified)*
- [x] **Audio Device Setup**: Lists connected and paired Bluetooth devices, wired headset, with phone speaker disabled by default. *(Verified)*
- [x] **Test Announcement**: Plays sample speech via selected output; does not log an entry in history or touch the queue. *(Verified)*
- [x] **Final Review Step**: Requires explicit "Start HearIt" tap; opening the app later never silently starts the service. *(Verified)*
- [x] **Back / Forward State Preservation**: Wizard state retained when navigating back and forth across steps. *(Verified)*

---

## 2. Dashboard, Service Lifecycle & Status Bar Controls

- [ ] **Dashboard Overview**: Displays Service Active/Inactive state, current audio device, Normal ON/OFF, Priority ON/OFF + remaining duration, mute status, current speaking item, queue count, and recent history.
- [ ] **Prerequisite Blocking**: Starting the service is blocked with a clear banner if no apps are enabled or required permissions are revoked.
- [ ] **Multi-Action Persistent Notification**:
  - Displays adaptive buttons between idle (*Pause, Stop, Priority*) and speaking (*Mute, Skip, Stop*).
  - Tapping notification card opens Dashboard.
- [ ] **Clean Service Stop**: Stopping the service immediately cancels current speech, clears pending queue, and removes the foreground notification.
- [ ] **Force-Stop & Process Death Resilience**:
  - Killing the app from Recents or Force Stop in Android Settings leaves state consistent.
  - Reopening the app does not auto-speak old queued messages.
  - Process death mid-speech recovers cleanly without crash or infinite repetition.

---

## 3. Applications & Per-App Customization

- [ ] **App Catalog Discovery**: Automatically scans and lists installed apps that post notifications with icons and labels.
- [ ] **Hide / Show System Apps Filter**: Filter chip at top of Applications screen toggles visibility of pre-installed system packages (`FLAG_SYSTEM`).
- [ ] **Search & Batch Actions**: Fast search bar, "Select All", and "Deselect All" work reliably.
- [ ] **Custom Spoken App Name (Alias)**:
  - Inside App Details $\to$ Rules & Filters: configure custom spoken alias (e.g. speak *"Teams"* instead of *"Microsoft Teams"*).
  - Test notification verifies TTS speaks the custom name.
- [ ] **Per-App Speech & Dismiss Overrides**:
  - Custom speech rate (0.5x to 2.0x) per app.
  - Per-app auto-dismiss notification toggle.
  - Visual badge clearly indicates whether setting is *Inherited* or *Overridden*.
- [ ] **Persistence**: Enabled/disabled toggles and per-app overrides persist across app restarts and reboots.

---

## 4. Announcement Eligibility, Safety & Maps Yield

- [ ] **Screen & Lock State Rules**:
  - Screen ON + unlocked $\to$ speech skipped by default (`SCREEN_ON` / `UNLOCKED`).
  - Screen OFF + locked $\to$ speech announced.
  - Mid-sentence interruption: speech stops immediately if screen turns on or phone unlocks while speaking.
- [ ] **Maps Navigation Priority & Yielding**:
  - While Google Maps navigation is speaking turn-by-turn directions, HearIt pauses pending announcements.
  - Announcement automatically resumes smoothly once navigation finishes.
- [ ] **Active Phone Call Suppression**: Never speaks during an active phone call; call-time queueing re-evaluates rules once call disconnects.
- [ ] **DND (Do Not Disturb) Handling**:
  - Respects Android system DND mode by default.
  - Per-app or priority breakthrough overrides work as configured.
- [ ] **Privacy & Sensitive Content Protection**:
  - OTPs, two-factor codes, banking alerts, passwords, and payment notifications are blocked or redacted according to privacy setting.
  - "Hide message content" speaks sender/app name only.
- [ ] **Global Keyword Filters**:
  - Exact, Word, and Phrase matching modes.
  - Notifications containing filtered keywords are skipped with reason recorded in History.
- [ ] **Text Length Limiting**:
  - When notification exceeds max length, either skips reading or caps gracefully per configuration without garbling text.

---

## 5. Audio Routing, Bluetooth & Focus Management

- [ ] **Audio Focus Intervention**:
  - **Duck Music**: Lowers background music volume during announcement, then smoothly fades back.
  - **Pause Music**: Pauses media playback, speaks, then resumes.
  - **No Intervention**: Speaks concurrently over media.
- [ ] **Bluetooth Connect / Disconnect Automation**:
  - Auto-start service when target Bluetooth headphones / car audio connects.
  - Immediate stop: if headphones disconnect mid-sentence, speech halts instantly (never falls back to phone speaker).
- [ ] **Per-Device Sound Output Test**:
  - Devices Screen $\to$ Tap speaker icon on any Bluetooth device card to play test sound to that specific output.
- [ ] **Mono Audio Fold-Down**: Speech plays cleanly on single-earpiece Bluetooth headsets without clipping left/right channels.
- [ ] **Earphone Catch-up Summary**: When connecting Bluetooth earphones after being disconnected, announces pending notification count.

---

## 6. Queue, Gestures & Physical Controls

- [ ] **Shake / Flip Phone to Silence**:
  - Shaking or flipping the phone while speech is playing silences the current announcement immediately.
- [ ] **Shake to Repeat**:
  - Shaking the phone within 10 seconds of an announcement repeats the last message.
- [ ] **Hardware Volume Key Silence**:
  - Pressing Volume Up or Down while speaking silences the current announcement without changing media volume.
- [ ] **Music Paused Hold (Wait-to-Speak)**:
  - If media is paused by user, waits up to 60s for music to resume before announcing, avoiding breaking user silence.
- [ ] **Auto-Dismiss Spoken Alerts**:
  - When enabled, spoken notifications are dismissed from the Android status bar shade automatically.
- [ ] **Priority Queue Elevation**:
  - Notifications from Priority Apps bypass standard queue and jump to head-of-line.
- [ ] **Notification Deduplication**:
  - Rapid repeated notifications (e.g. repeated WhatsApp pings) are deduped or grouped (*"WhatsApp: 3 messages from Sarah"*).

---

## 7. Active Intelligence & Smart NLP Engine

- [ ] **URL Domain Cleaning**:
  - Instead of reading long links (`https://maps.google.com/dir/?api=1&origin=...`), speaks *"Link from google.com"*.
- [ ] **Currency & Unit Spoken Normalization**:
  - `$50` spoken as *"50 dollars"*.
  - `₹500` spoken as *"500 rupees"*.
  - `10km` spoken as *"10 kilometers"*, `25kg` as *"25 kilograms"*.
- [ ] **Markdown & Formatting Stripper**:
  - Strips asterisks (`*bold*`), underscores (`_italic_`), bullet points, and HTML formatting.
- [ ] **Emoji Spam Collapsing**:
  - Collapses strings of identical emojis (e.g. `😂😂😂😂` $\to$ *"4 laughing faces"* or *"laughing face"*).
- [ ] **Tapback & Reaction Filtering**:
  - Filters out redundant tapback reactions (*"Liked 'Sounds good'"*).
- [ ] **Automated Shortcode / Bot Sender Identification**:
  - Normalizes alphanumeric shortcodes (e.g. `VK-HDFCBK` $\to$ *"HDFC Bank"*).
- [ ] **Delayed Message Contextual Timestamp**:
  - For queued messages delivered late, adds temporal context (*"Received 10 minutes ago"*).

---

## 8. Voice & Speech Modulation

- [ ] **Soft Pre-Speech Earcon Chime**:
  - Plays a gentle, pleasant tone immediately before speaking to cue user's attention.
- [ ] **Anti-Collision Audio Delay**:
  - Configurable 0.5s–2.0s buffer between notification arrival and speech onset to avoid clipping notification sounds.
- [ ] **3-Tier Adaptive Speed for Long Messages**:
  - **> 20 words**: Speaks at mild acceleration (e.g. 1.1x).
  - **30 to 40 words**: Speaks at moderate acceleration (e.g. 1.25x).
  - **> 40 words**: Speaks at brisk rate (e.g. 1.4x).
  - Short messages (<20 words) remain at natural baseline rate.
- [ ] **Custom Pronunciation Dictionary**:
  - User can add custom replacements (e.g. *"Nguyen"* $\to$ *"Win"*).
- [ ] **Android System TTS Settings Shortcut**:
  - Direct 1-tap shortcut opens OS Text-to-Speech Engine selector and voice pack downloads.

---

## 9. Notification History & Storage Controls

- [ ] **Comprehensive Log Verification**:
  - Every evaluated notification appears with timestamp, package icon, title, text snippet, and outcome (*Spoken*, *Skipped*, *Queued*, *Muted*).
- [ ] **Skipped Reasons**:
  - Tapping a skipped entry shows exact reason (e.g. *Screen ON*, *DND active*, *Keyword filtered*, *Sensitive content*).
- [ ] **Search & Filtering**:
  - Filter by app, date range, or status; real-time full-text search.
- [ ] **Retention Windows & Auto-Pruning**:
  - Retention options (1 day, 7 days, 30 days, 90 days, Forever) prune older entries correctly.
- [ ] **Single & Batch Deletion**:
  - Swipe to dismiss individual entry; "Clear All History" prompts for confirmation.
- [ ] **Data Export**:
  - History log export produces clean, uncorrupted CSV/JSON without leaking private passwords/OTPs.

---

## 10. Security, Backup & System Diagnostics

- [x] **Settings-Only Backup & Restore**: Exports settings only (no history or notification content); restores valid files and rejects corrupt ones. *(Verified)*
- [x] **Biometric App Lock**: Fingerprint/Face authentication required to open HearIt when enabled. *(Verified)*
- [x] **Secure Screen in Recents**: Task thumbnail hidden in Android App Switcher when enabled. *(Verified)*
- [x] **System Diagnostics Hub**: Displays service state, audio routing, listener connectivity, and battery optimization status. *(Verified)*
- [x] **Clean Log Export**: Diagnostic export contains system state and timing only; **zero notification content leaked**. *(Verified)*
- [ ] **Low Battery Auto-Mute**: When phone battery drops below 15%, auto-mutes speech to preserve power.
- [ ] **Emergency Breakthrough**: Configured VIP contacts or urgent keywords breakthrough even when muted or on low battery.

---

## 11. Wear OS Companion App & DataLayer Sync

- [ ] **Watch Connection Status on Phone**:
  - Phone Devices screen displays watch name, connection badge (🟢 Connected / ⚪ Disconnected), and last synced timestamp.
- [ ] **Wear OS Header Connection Indicator**:
  - Top of Wear OS app shows live status dot: 🟢 **Green** when connected to phone, ⚪ **Grey** when offline.
- [ ] **Empty State Guidance**:
  - When no apps are enabled on phone, watch displays empty state icon + *"No apps enabled"* with subtext to configure on phone.
- [ ] **Max-4 Watch Apps Notification Forwarding**:
  - Phone settings allow selecting up to 4 priority apps to forward to watch.
  - Forwarded notifications appear on watch in real time over Bluetooth DataLayer.
- [ ] **Wear OS Quick Filter Icons**:
  - Horizontal filter pills display **"ALL"** plus dedicated icons for the 4 enabled watch apps.
  - Tapping an icon filters the feed to that app immediately.
- [ ] **Wear OS App Version Display**:
  - Watch footer displays clean semantic version: `"HearIt v1.0.0"` (no internal build numbers).
- [ ] **Offline Resilience**:
  - If phone disconnects, watch retains cached notifications without crashing.

---

## 12. Accessibility, Theming & Hardware Edge Cases

- [ ] **TalkBack Screen Reader**: Every button, slider, and toggle has a meaningful accessibility label and state description.
- [ ] **Touch Targets**: All interactive elements satisfy minimum 48x48 dp touch targets.
- [ ] **2.0x Font Scaling**: UI layout adapts without text clipping or overlapping across Dashboard, Settings, and Modals.
- [ ] **Dark / Light / System Theme**: Dynamic Material You palette matches system wallpaper; dark theme achieves high contrast.
- [ ] **Destructive Action Confirmations**: Clearing history, resetting settings, or revoking rules always prompts for confirmation.

---

## 📝 Testing Session Log

Record device test results below as you test:

| Date | Tester / Device | Section / Item | Result | Notes / Observed Behavior | Action / Fix Required |
|---|---|---|---|---|---|
| *2026-11-11* | OnePlus Nord (Android 12) | 1. Setup & Onboarding | ✅ PASS | All 10 setup steps verified cleanly | None |
| *2026-11-11* | OnePlus Nord (Android 12) | 10. Security & Backup | ✅ PASS | Biometric lock, settings export/import verified | None |
| *2026-11-11* | OnePlus Nord (Android 12) | 5. Sound Output Test | ✅ PASS | 1-tap audio test on Bluetooth card verified | None |
| | | | | | |
| | | | | | |
| | | | | | |
