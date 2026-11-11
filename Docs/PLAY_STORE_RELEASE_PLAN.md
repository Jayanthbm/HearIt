# Google Play Store Release & Wear OS Quality Guidelines Plan: HearIt

> **App Name**: HearIt - Smart Notification Reader & Wear OS Companion  
> **Package Name (Phone)**: `com.jayanth.hearit`  
> **Package Name (Wear OS)**: `com.jayanth.hearit.wear`  
> **Target Android Versions**: Phone (minSdk 29, targetSdk 34+), Wear OS (minSdk 30, targetSdk 34+)  
> **Reference Architecture**: Modeled after `com.jayanth.parkpin`  

---

## 1. Overview & Google Play Target Architecture

HearIt delivers hands-free notification announcements over Bluetooth earphones, wired headsets, and phone speakers, accompanied by a dedicated Wear OS companion app for glanceable notification curation.

To achieve successful Google Play Store approval for both Phone and Wear OS tracks, the app must satisfy:
1. **Google Play Core App Quality Guidelines**
2. **Wear OS App Quality Guidelines (Wear OS Review Checklist)**
3. **Google Play Sensitive Permissions & User Data Policy** (specifically regarding `BIND_NOTIFICATION_LISTENER_SERVICE`)

---

## 2. Wear OS Play Store Guidelines & Readiness Checklist

Wear OS apps undergo a dedicated manual review track in Google Play. Failing any of the following criteria causes immediate rejection of the Wear OS artifact:

### Mandatory Wear OS Quality Standards

| Guideline | Requirement | HearIt Status & Action Plan |
| :--- | :--- | :--- |
| **W-1: App Launch & Performance** | Must start within 5s with cold start; responsive UI. | ✅ HearIt uses pure Jetpack Compose for Wear OS with minimal memory footprint and instant cold start. |
| **W-2: Round & Square Display Adaptation** | Must render properly on both round and square watch faces without text truncation or button clipping. | ✅ HearIt uses `Scaffold`, `TimeText`, `PositionIndicator`, and `ScalingLazyColumn` with auto-centering to ensure circular clipping never obscures content. |
| **W-3: Dark / Black Theme Requirement** | AMOLED power optimization: Wear OS apps must use a pure black background (`#000000`) across all screens. | ✅ All screens in `:wear` use `Modifier.background(Color.Black)`. |
| **W-4: Font Size & Legibility** | Minimum recommended body font size of 12sp, title font size 14-16sp. Content must be glanceable. | ✅ All cards use 12sp body, 13sp titles, and 10sp metadata times. |
| **W-5: Touch Target Size** | All interactive elements must meet the minimum 48x48dp touch target (or standard Wear OS CompactChip/Button). | ✅ Top category buttons are 32dp with 8dp surrounding padding (48dp touch hit target). Cards span full width. |
| **W-6: Standalone vs Companion Metadata** | Wear OS manifest must explicitly declare whether the app can function independently or requires a phone companion. | ✅ In `wear/src/main/AndroidManifest.xml`: `<meta-data android:name="com.google.android.wearable.standalone" android:value="false" />` (companion app). |
| **W-7: Rotary Input (Crown / Bezel)** | Recommended: Users should be able to scroll lists using the watch's physical rotating crown. | 🛠️ Add `Modifier.rotaryHandler` or `rememberRotaryScrollableState` attached to `ScalingLazyColumn`. |
| **W-8: Ongoing Activity / Notification** | If an announcement or service is running, an ongoing indicator / notification should be presented on the watch. | 🛠️ Support Wear OS Ongoing Activity API when audio is actively being announced on phone. |
| **W-9: Tiles & Complications** | Optional but strongly favored for Play Store featuring: A glanceable Wear OS Tile showing recent announcement count or quick Mute toggle. | 🛠️ Plan a Wear OS Tile: "HearIt Quick Mute / Status". |

---

## 3. Phone App Quality & Sensitive Permission Guidelines

### A. Notification Listener Service Disclosure (`BIND_NOTIFICATION_LISTENER_SERVICE`)
Google Play has strict policies regarding apps that access notification streams.
- **Mandatory In-App Prominent Disclosure**: Before prompting the user to grant Notification Access in Android Settings, HearIt must show a clear, prominent disclosure modal stating:
  > *"HearIt uses Notification Access solely on your device to read incoming notifications aloud through your connected earphones and sync alerts to your Wear OS watch. Your notification content is never uploaded to external servers, sold, or shared."*
- **No Remote Transmission**: HearIt processes all text-to-speech strictly on-device using Android's native TTS engine. This must be affirmed in the Google Play Data Safety form.

### B. Foreground Service Types (Android 14+ / targetSdk 34)
- HearIt runs as `foregroundServiceType="mediaPlayback"`.
- Uses `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission declared in `AndroidManifest.xml`.
- When active, a persistent, dismissible notification is displayed in the shade with live state and interactive playback controls.

---

## 4. Play Store Listing Metadata & Copywriting

### App Title (Max 30 characters)
```text
HearIt: Earphone Voice Reader
```
*(Alternative: `HearIt: Voice Alerts & Wear OS`)*

### Short Description (Max 80 characters)
```text
Hands-free notification voice reader for Bluetooth earphones and Wear OS watch.
```

### Full Description (Max 4000 characters)
```text
HearIt keeps you connected and your eyes on the road! HearIt reads your incoming messages and notifications aloud through your Bluetooth earphones, car speakers, or headphones—intelligently cleaned and customized for hands-free listening.

Equipped with smart on-device NLP cleaning, HearIt skips tedious URLs, expands emojis into spoken phrases, normalizes currencies, and filters spam reactions so you only hear what truly matters.

Seamlessly integrated with a dedicated Wear OS companion app, you can review past alerts, filter notifications by category, and control announcements directly from your wrist.

🌟 KEY FEATURES:
• Smart Voice Announcements: Announces sender and message text hands-free over your preferred audio output (Bluetooth, Wired, or Phone Speaker).
• Wear OS Companion Watch App: Glanceable notification feed on your wrist, filtered by apps enabled on your phone with one-tap clear and quick categorization.
• Smart Content Cleaning Engine: Automatically collapses emoji spam (e.g. 5 laughing emojis become "2 laughing faces"), summarizes lengthy website URLs, and normalizes currency symbols.
• Custom Spoken App Nicknames: Personalize how apps are spoken (e.g. hear "Teams" instead of "Microsoft Teams").
• Shake or Flip to Silence: Instantly stop speech by gently shaking your phone or flipping it face down. Shake within 10 seconds to replay!
• Music Ducking & Hold: Smoothly ducks background music volume during announcements, and pauses speech while you are paused on a call.
• Per-App & Keyword Filtering: Whitelist or blacklist notifications with exact words, phrases, or specific priority rules.
• Periodic Spoken Check-ins: Hands-free spoken time and battery updates at your chosen interval.
• 100% Private & On-Device: Notification data is processed strictly on your phone using your local Text-to-Speech engine. Zero cloud tracking, zero data sold.

Drive, run, cycle, and work hands-free with HearIt!
```

---

## 5. Store Visual Asset Specifications & Screenshot Plan

### Store Asset Specifications

| Asset | Dimensions | Format | Requirement |
| :--- | :--- | :--- | :--- |
| **App Icon** | 512 x 512 px | 32-bit PNG, max 1MB | Mandatory (Phone & Watch) |
| **Feature Graphic** | 1024 x 500 px | PNG or JPEG, max 15MB | Mandatory (Play Store header) |
| **Phone Screenshots** | 1080 x 2400 px (9:16) | PNG/JPEG, min 4, max 8 | Mandatory for phone track |
| **Wear OS Screenshots**| 384 x 384 px or 454 x 454 px | PNG, min 2, round display | Mandatory for Wear OS track |

### Phone Screenshot Breakdown (6 Core Frames)
1. **Frame 1 - Home Dashboard**: *"Hands-Free Notification Voice Reader"* (Shows dynamic service status card, quick test speaker, connected Bluetooth device).
2. **Frame 2 - Applications & Filters**: *"Per-App Rules & Custom Spoken Names"* (Shows enabled app list, system app toggle, custom alias e.g. "Teams").
3. **Frame 3 - Smart Content Cleaning (NLP)**: *"Smart Audio Cleaning"* (Shows toggles for URL summarizer, currency converter, markdown stripper, emoji collapse).
4. **Frame 4 - Audio & Speech Tuning**: *"Natural Voice Tuning"* (Shows pitch/speed sliders, anti-collision delay segmented buttons, mono fold-down).
5. **Frame 5 - Devices & Wear OS Sync**: *"Seamless Bluetooth & Smartwatch Routing"* (Shows Wear OS companion status card, sync button, and allowed output types).
6. **Frame 6 - Gesture Controls & Safety**: *"Shake to Silence & Safety Breakthrough"* (Shows shake/flip gesture toggle, low battery auto-mute).

### Wear OS Screenshot Breakdown (4 Core Frames)
1. **Frame 1 - Watch Notification Feed**: Round screen showing recent WhatsApp announcement card with sender, message text, and timestamp.
2. **Frame 2 - Category Icon Filters**: Round screen showing top circular icon filter row (🔔 All, 💬 Chat, ✉️ SMS, 🏛️ Banking).
3. **Frame 3 - Empty / Synced State**: Round screen showing *"No apps enabled. Enable some apps in HearIt on phone."* or *"No notifications in Finance"*.
4. **Frame 4 - Quick Clear All**: Round screen showing *"Clear All"* action button at bottom of list.
