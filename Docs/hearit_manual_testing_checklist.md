# HearIt — Master Manual Testing Checklist (Phases 1 to 5)

This document tracks all features implemented across Phases 1 through 5. You can test them one by one at your own pace. When you test an item, tell me if it is **OK** or if you want a **change / fix**, and I will update the tracker and make the code adjustments immediately.

---

## 📋 Testing Progress Summary

| Phase | Category | Total Features | Verified | Ready for Testing | Pending (Real Device) |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Phase 1** | Core Resilience & Playback Controls | 8 | 1 | 0 | 7 |
| **Phase 2** | Local Intelligence (Smart NLP) | 7 | 0 | 1 (2.1 NLP Screen) | 6 |
| **Phase 3** | Audio Polish & Speech Controls | 6 | 0 | 0 | 6 |
| **Phase 4** | Devices, Rules & Safety | 6 | 2 | 0 | 4 |
| **Phase 5** | Wear OS Companion App | 3 | 0 | 2 (5.1 & 5.2) | 1 |
| **New Items** | Custom Spoken Name & System Apps Filter | 2 | 0 | 2 (6.1 & 6.2) | 0 |
| **Total** | | **32** | **3** | **5** | **24** |

---

## 📦 Phase 1: Core Service Resilience & Playback Controls

### [x] Item 1.1: Multi-Action Persistent Notification — ✅ VERIFIED OK (User Tested)
- **Location**: Android Notification Shade (pull down status bar when HearIt is running).
- **Status**: Tested and confirmed working! Action buttons adapt between idle and speaking states.

### [ ] Item 1.2: Shake or Flip Phone to Silence Current Speech — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Settings Tab $\to$ **Gestures & Playback Controls** (Toggle: *Shake / Flip to Silence & Repeat*).

### [ ] Item 1.3: Shake within 10s to Repeat Last Announcement — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Physical gesture.

### [ ] Item 1.4: Physical Volume Key Silence — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Hardware Volume Keys (Settings Tab $\to$ Toggle: *Physical Volume Key Silence*).

### [ ] Item 1.5: Auto-Dismiss Notification from Android Status Bar — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Settings Tab $\to$ **Gestures & Playback Controls** (Toggle: *Auto-Dismiss Spoken Alerts*).

### [ ] Item 1.6: Music Paused Hold (Wait-to-Speak) — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Settings Tab $\to$ **Gestures & Playback Controls** (Toggle: *Music Paused Hold (60s)*).

### [ ] Item 1.7: Bluetooth Earphones Auto-Start / Auto-Stop — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: Settings Tab $\to$ **Gestures & Playback Controls** (Toggle: *Bluetooth Auto-Start / Stop*).

### [ ] Item 1.8: History Detail Modal Scroll Fix — ⏳ PENDING
- **Status**: User will test on real device with release APK.
- **Location**: History Tab $\to$ Tap any logged notification item.

---

## 📦 Phase 2: Local Intelligence (Smart NLP Cleaning Engine)

### [ ] Item 2.1: Smart Content Cleaning & NLP Settings Screen — 🚀 READY FOR TESTING
- **User Request**: *"Needed a screen to configure that"*.
- **Implementation**: Added **Smart Content Cleaning & NLP Card** in **Settings Tab** with granular switches:
  1. *Clean & Summarize URLs* (Speaks *"Link from domain.com"* instead of lengthy http URLs).
  2. *Currency & Unit Normalization* (Converts `\$50` to *"50 dollars"*, `₹100` to *"100 rupees"*).
  3. *Strip Markdown & Markup* (Removes `*bold*`, HTML tags, and backticks).
  4. *Collapse Repeated Emojis* (Collapses `😂😂😂😂` to *"2 laughing faces"*).
  5. *Filter Tapback Reactions* (Simplifies reactions like *"Liked an image"*).
  6. *Clean Bot & SMS Sender Headers* (Normalizes alphanumeric shortcodes e.g. `VK-HDFCBK`).
- **Location**: Phone App $\to$ Home Screen **Active Intelligence & Controls** Card (tap card or "Configure") OR Settings Tab $\to$ Rules &amp; Activity $\to$ **Intelligence &amp; Controls** (Dedicated full screen with future Premium paywall readiness).

### [ ] Item 2.2: Currency, Amounts & Units Spoken Normalization — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 2.3: Markdown & Bullet Formatting Stripper — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 2.4: Emoji Spam Collapsing — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 2.5: Tap-Back & Reaction Filter — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 2.6: Automated & Bot Sender Identification — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 2.7: Contextual Time-Stamp for Delayed Messages — ⏳ PENDING
- **Status**: User will test on real device with release APK.

---

## 📦 Phase 3: Audio Polish & Smart Speech Controls

### [ ] Item 3.1: Soft Pre-Speech Earcon Chime — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 3.2: Anti-Collision Audio Delay — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 3.3: Adaptive Speed for Long Messages — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 3.4: Mono Audio Fold-Down — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 3.5: Earphone Catch-up Summary on Connect — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 3.6: Smooth Music Ducking Return Fade — ⏳ PENDING
- **Status**: User will test on real device with release APK.

---

## 📦 Phase 4: Devices, Rules & Safety Protections

### [ ] Item 4.1: Periodic Spoken Check-in (Time & Battery %) — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [x] Item 4.2: Global Keyword Filtering UI — ✅ VERIFIED OK (User Tested)
- **Status**: Tested and confirmed working! Global keywords can be added with Word/Exact/Phrase matching modes.

### [x] Item 4.3: Per-Device Sound Output Test — ✅ VERIFIED OK (User Tested)
- **Status**: Tested and confirmed working! 1-tap speaker test audio button on connected Bluetooth device cards.

### [ ] Item 4.4: Low Battery Auto-Mute (<15%) — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 4.5: Critical Emergency Breakthrough — ⏳ PENDING
- **Status**: User will test on real device with release APK.

### [ ] Item 4.6: Android System TTS Settings Shortcut — ⏳ PENDING
- **Status**: User will test on real device with release APK.

---

## 📦 Phase 5: Wear OS Companion App (`:wear`)

### [ ] Item 5.1: Wear OS Companion Status & Sync on Phone — 🚀 READY FOR TESTING
- **User Request**: *"Wear os app has to be compaanion app, also need the status of wear os app in phone. Also option to sync, when is last connected, connected or not etc"*.
- **Implementation**:
  1. Dedicated **Wear OS Companion Card** added to **Devices Screen** and **Settings Screen**.
  2. Displays real-time watch connection status:
     - 🟢 **Connected: [Watch Name]** (or ⚪ Disconnected).
     - **Last Synced**: Relative timestamp (e.g. *"Just now"*, *"2m ago"* or *"Never"*).
     - **Phone Apps Enabled**: Displays count of enabled apps currently configured on phone.
  3. **"Sync Enabled Apps to Watch"** action button: One tap pushes all enabled app rules to the watch via Google Play Wearable message API and updates sync timestamp.
  4. Background auto-sync: Watch automatically requests and receives fresh sync whenever launched.
- **Location**: Phone App $\to$ Devices Screen / Settings Tab $\to$ **Wear OS Companion**.

### [ ] Item 5.2: Watch App Shows Only Enabled Apps & Icon Filters — 🚀 READY FOR TESTING
- **User Request**: *"In watch need to show only apps enabled on phone. If nothing is enabled need to show as no apps enabled, enable some apps. Also above filter need to have icons rather than app text"*.
- **Implementation**:
  1. **Only Enabled Apps**: The watch filters notifications to only show apps that are enabled on the phone. Notifications from disabled apps are never added or shown on watch.
  2. **Empty State**: If no apps are enabled on phone, watch shows:
     - Warning Icon + **"No apps enabled"**
     - Subtext: *"Enable some apps in HearIt on your phone to hear and view them."*
  3. **Icon Filter Buttons**: The top category bar replaced bare text with round 32dp icon buttons:
     - 🔔 **All** (`Icons.Rounded.Notifications`)
     - 💬 **Chat / WhatsApp** (`Icons.Rounded.Chat`)
     - ✉️ **SMS** (`Icons.Rounded.Email`)
     - 🏛️ **Finance / Banking** (`Icons.Rounded.AccountBalance`)
- **Location**: Watch App (`:wear` on Wear OS emulator / watch).

### [ ] Item 5.3: Bluetooth DataLayer Sync — ⏳ PENDING
- **Status**: User will test on real device.

---

## 🆕 New Functional Additions Requested

### [ ] Item 6.1: Custom App Spoken Name / Alias in Per-App Screen (Idea 33) — 🚀 READY FOR TESTING
- **User Request**: *"custom app Name in announcement, for ex instead of Microsoft Teams -> Teams this has to be settings in in Per app screen"*.
- **Implementation**:
  - In **App Details Screen** (tap any app from the list $\to$ Rules & Filters tab):
  - Added **Custom Spoken App Name** card with input field, placeholder (*"e.g. Teams, WhatsApp, Gmail"*), and Save / Clear buttons.
  - Whenever an announcement for that app is generated, `SmartContentCleaner` speaks the custom spoken nickname instead of the official package label!
- **Location**: Phone App $\to$ Applications Tab $\to$ Tap any enabled app $\to$ Rules & Filters $\to$ **Custom Spoken App Name** (only shown when app announcements are enabled).

### [ ] Item 6.2: Hide / Show System Apps Filter in Applications List — 🚀 READY FOR TESTING
- **User Request**: *"In App List filters for hide or show system apps"*.
- **Implementation**:
  - Filter chip added to top of Applications screen: **"System Apps: Shown"** / **"System Apps: Hidden"**.
  - Toggling it instantly hides/shows pre-installed system apps (`FLAG_SYSTEM`), keeping the app list clean and focused on user-installed apps.
- **Location**: Phone App $\to$ Applications Tab $\to$ Filter Chip bar.

---

> [!TIP]
> Tell me which item you are testing (e.g. *"Tested Item 2.1, it is OK"* or *"Item 5.1: change X"*), and I will update this document and adjust code immediately!
