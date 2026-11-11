# HearIt v1.0.0 — Hands-Free Notification Reader & Wear OS Companion

HearIt turns your phone into an intelligent, hands-free notification voice reader designed for driving, cycling, workouts, and work sessions. Featuring deep system intelligence, smart audio focus, customizable app rules, and a lightweight Wear OS companion.

---

## 🚀 What's New in v1.0.0

### ⌚ Wear OS Companion App
- **Standalone & Paired Wear OS App**: Wrist companion with low-power tile-like glanceable interface.
- **Connection Indicator**: Header badge shows real-time phone link status (green connected dot vs. grey offline dot).
- **Empty State UX**: Clean guidance card displayed when no apps are forwarding notifications.
- **App Filter & Multi-App Quick Switching**: Quick filter pills for "ALL" plus up to 4 user-selected watch apps configured directly from the phone.
- **Independent Notification Store**: Receive forwarded notifications over the Wear OS Data Layer without needing to pull out your phone.

### 🚗 Maps Navigation Priority & Intelligent Yield
- **Zero Collision Navigation**: Detects turn-by-turn spoken audio from Google Maps and other navigation services.
- **Instant Ducking / Pausing**: HearIt automatically pauses pending notification speech when navigation directions announce, and seamlessly resumes afterwards.

### ⚙️ Completely Revamped Settings Architecture
- **Unified Navigation Hub**: Replaced fragmented settings menus with four intuitive categories:
  - **Playback**: Audio routing, ducking, focus management, and custom announcement templates.
  - **Intelligence**: ML Kit smart summarization, spam cleaning, keyword filtering, and URL rules.
  - **Devices**: Headset autodetection, vehicle Bluetooth triggers, and Wear OS forwarding controls.
  - **Preferences**: App-specific speech rates, quiet hours, shake-to-silence, and custom dictionary.
- **Adaptive Speech Rate**: 3-tiered speed options (>20 words, 30–40 words, >40 words) for long notifications.
- **Redesigned Bottom-Sheet Modals**: Segmented button headers, consistent padding, and modern Material 3 styling.

### 📱 Release Assets Included
- **`HearIt-phone-v1.0.0.apk`** (Version Code: `10001`): Signed release APK for Android 10+ (API 29+) phones.
- **`HearIt-wear-v1.0.0.apk`** (Version Code: `10002`): Signed release APK for Wear OS 3+ (API 30+) smartwatches.
