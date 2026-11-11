# HearIt — Google Play Store Release & Listing Guide (`v1.0.0`)

This document contains verified, copy-paste-ready Google Play Console store listing copy, release metadata, Data Safety answers, asset specifications, and upload checklists for both the **Phone** and **Wear OS** tracks of **HearIt v1.0.0**.

---

## 1. Release Build Coordinates & Signing Parity

Modeled after `ParkPin`, both modules share one semantic version (`appVersionName` in [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml)) and derive distinct Play Store `versionCode` numbers (`major * 10000 + minor * 100 + patch * 10 + offset`):

| Track | Module | Package (`applicationId`) | `versionName` | `versionCode` | Bundle Artifact |
|---|---|---|---|---|---|
| **Android Phone** | `:app` | `com.jayanth.hearit` | `1.0.0` | `10001` *(odd: `base + 1`)* | `app/build/outputs/bundle/release/app-release.aab` |
| **Wear OS Watch** | `:wear` | `com.jayanth.hearit` | `1.0.0` | `10002` *(even: `base + 2`)* | `wear/build/outputs/bundle/release/wear-release.aab` |

- **Future Bumps**: Changing `appVersionName` to `"1.0.1"` automatically yields `10011` (Phone) and `10012` (Wear OS).
- **UI Version Display**: Phone Settings/Diagnostics and Watch footer display `"HearIt v1.0.0"` cleanly without internal build numbers.
- **Release Signing**: Both `app/build.gradle.kts` and `wear/build.gradle.kts` load the same upload keystore via `keystore.properties` / `local.properties` so Play Store Wear OS Data Layer verification matches cleanly.

---

## 2. Play Console Store Listing Copy

### App Title (Max 30 characters)

- **Primary (27 chars)**:
```text
HearIt: Notification Reader
```
- **Alternate (30 chars)**:
```text
HearIt: Voice Alerts & Wear OS
```

### Short Description (Max 80 characters)

- **Primary (75 chars)**:
```text
Hands-free notification reader with Maps priority, NLP & Wear OS companion.
```
- **Alternate (76 chars)**:
```text
Reads messages hands-free, pauses for Maps turns & syncs to Wear OS watches.
```

### Full Description (Max 4000 characters)

```text
Stay connected and keep your eyes on the road with HearIt—a 100% on-device hands-free notification voice reader built for Bluetooth earphones, car audio, wired headsets, and Wear OS smartwatches.

Unlike basic TTS tools, HearIt understands context. It cleans messy links, emojis, OTPs, and bank SMS formatting before speaking, yields immediately whenever Google Maps speaks turn-by-turn directions, and lets you mirror your top apps straight to your wrist.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
KEY FEATURES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🗺️ GOOGLE MAPS & NAVIGATION PRIORITY
Never talk over a crucial turn again. HearIt detects active navigation audio guidance and navigation alerts mid-sentence, pauses regular notifications instantly, and smoothly re-announces deferred messages after your turn instruction finishes.

⚡ 3-TIER ADAPTIVE SPEED & VOICE POLISH
Short messages stay natural while long messages speed up automatically across your chosen word-count brackets (<20w base rate, 20–30w, 30–40w, and >40w). Includes soft pre-speech earcon chimes, anti-collision audio delay, dual-mono fold-down for single earbuds, hardware voice boost, and optional instantaneous ambient noise loudness boost.

🧠 SMART ON-DEVICE NLP & CATEGORY CLEANING
Make notifications sound human before Text-to-Speech begins:
• Pronunciation & Acronym Dictionary (e.g., ETA → E T A, custom words & test button)
• Smart URL Summarizer with custom & predefined domain rules
• Currency & Unit Normalizer (speaks ₹, $, km/h, GB naturally)
• Markdown Stripper, Emoji Collapsing & Emoji Reaction Filter (Condense or Silence)
• OTP & Phone Number digit-by-digit chunking & Sender Contact Name Sanitizer
• Category Intelligence: Bank SMS condensation, Carrier/Promo SMS spam filter, Email sign-off & disclaimer cleanup, plus optional Tier-2 ML Kit Entity Extraction & Android AICore support.

⌚ WEAR OS COMPANION APP
Pair HearIt with your Wear OS watch for an AMOLED-black glanceable feed:
• Live green/offline connection dot showing phone status
• Forward up to 4 selected apps independently via Google Play Services Data Layer
• Real synced app icons filter bar (ALL + your selected apps) and optional watch haptic pulse

🎧 ZERO-TOUCH BLUETOOTH & SMART CATCH-UP
Auto-start announcements when your allowed Bluetooth earphones connect and auto-stop on disconnect. Customize per-device allowlists, run 1-tap Test Sound checks per device, hear "What Did I Miss?" summaries when reconnecting, and enable periodic spoken Time, Phone Battery, Earphones Battery, and Summary check-ins.

🛡️ POCKET DETECTION, GESTURES & SAFEGUARDS
• Proximity Pocket Detection: Speaks when in your pocket and stays quiet while holding your phone (unless Priority Announce is active)
• Critical Keyword Breakthrough: Urgent keywords break through muted apps with a distinctive chime
• Physical Controls: Flip face-down, press physical Volume keys, or Shake to silence; Shake shortly after speech or tap notification/Home shortcut to Repeat
• Low Battery & Battery Saver Auto-Mute safeguard + optional Biometric & Secure Screen Lock

📊 PER-APP TUNING, HISTORY & LOCAL BACKUP
Set custom TTS voices, speech rate, pitch, Priority Announce membership, and per-app auto-dismiss rules—individually or in multi-select bulk actions. Search spoken/skipped history by date range, export CSV/TXT logs, or save local JSON settings backups anytime.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
100% PRIVATE & ON-DEVICE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
HearIt does not upload notifications, collect analytics, or require an account. Every rule, cleaning pipeline, and history database stays strictly on your personal devices.
```

---

## 3. Release Notes ("What's New in v1.0.0", Max 500 chars)

**Phone & Wear OS Release Notes (418 chars):**
```text
Initial v1.0.0 release of HearIt for Android & Wear OS!
• Hands-free voice announcements over Bluetooth, car audio, wired headsets & speaker
• Instant yield priority & auto re-announce for Google Maps turn guidance
• 3-tier Adaptive Speed, smart NLP text cleaning & category intelligence
• Wear OS companion app with live sync, real app icons & haptic alerts
• 100% on-device privacy with local backup & history export
```

---

## 4. Visual Assets & Screenshots Inventory

All assets are staged in [`Docs/playstore_assets/`](playstore_assets/) and mirrored in `/Users/jayanthbharadwajm/development/app-web/public/images/`:

| Asset Type | Dimensions | File Path |
|---|---|---|
| **App Icon** | `512 × 512 px` PNG | `Docs/playstore_assets/hearit_icon_512.png` |
| **Feature Graphic** | `1024 × 500 px` PNG | `Docs/playstore_assets/hearit_feature_graphic_1024x500.png` |
| **Phone 1 (Home)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_home.png` |
| **Phone 2 (Apps)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_apps.png` |
| **Phone 3 (Active Intelligence)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_intelligence.png` |
| **Phone 4 (Voice & 3-Tier Adaptive Speed)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_voice.png` |
| **Phone 5 (Playback & Maps Yield)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_playback.png` |
| **Phone 6 (Devices & Wear Sync)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_devices.png` |
| **Phone 7 (Global Safeguards & Pocket)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_rules.png` |
| **Phone 8 (Settings Hub v1.0.0)** | `1080 × 2400 px` PNG | `Docs/playstore_assets/screenshots/hearit_phone_settings_hub.png` |
| **Watch 1 (Connected & App Icons)** | `454 × 454 px` PNG | `Docs/playstore_assets/screenshots/hearit_watch_empty.png` |
| **Watch 2 (Per-App Icon Filter)** | `454 × 454 px` PNG | `Docs/playstore_assets/screenshots/hearit_watch_feed.png` |
| **Watch 3 (No Apps / Empty State)** | `454 × 454 px` PNG | `Docs/playstore_assets/screenshots/hearit_watch_version.png` |

---

## 5. Play Console Data Safety, Permissions & URLs Checklist

### Store URLs (`app-web` Cloudflare Pages)
- **Website / Marketing Overview**: `<domain>/hearit/`
- **Privacy Policy URL (Mandatory)**: `<domain>/hearit/privacy/`
- **Terms of Service URL**: `<domain>/hearit/terms/`
- **Support URL / Email**: `<domain>/hearit/support/` · `jayanthbharadwaj.m@gmail.com`

### Data Safety Form Answers (Exact Match to `AndroidManifest.xml`)
- **Does your app collect or share any of the required user data types?** → **No**
  - `:app` and `:wear` declare **zero network/internet data collection** (no `INTERNET` permission, no ads SDK, no third-party analytics SDK in `v1.0.0`).
  - All notification content stays strictly in-memory or in local sandboxed Room/DataStore storage; Wear mirroring uses local Play Services Data Layer between user-paired devices.
- **Account Creation / Deletion**: HearIt does not require account creation or login.
- **Notifications (`BIND_NOTIFICATION_LISTENER_SERVICE`)**: Declared strictly for user-initiated hands-free TTS announcements & Wear OS notification mirroring; prominently explained during onboarding.
- **Optional Microphone (`RECORD_AUDIO`)**: Requested **only** if the user manually enables *Ambient Noise Auto-Volume Boost*; runs an instantaneous (~120ms) RMS loudness measurement immediately before TTS and never records, saves, or uploads audio.
- **Package Visibility**: Uses scoped `<queries>` for launcher apps only (does not request restricted `QUERY_ALL_PACKAGES`).

### Pre-Submission Upload Checklist
1. **Opt in to Wear OS release track** in Google Play Console → *Advanced Settings → Form Factors → Wear OS*.
2. Upload **`app/build/outputs/bundle/release/app-release.aab`** (`versionCode = 10001`, `versionName = "1.0.0"`) under the Phone/Mobile track.
3. Upload **`wear/build/outputs/bundle/release/wear-release.aab`** (`versionCode = 10002`, `versionName = "1.0.0"`) under the **Wear OS** track.
4. Attach all Phone (`1080×2400`) and Wear OS screenshots, 512×512 Icon, and 1024×500 Feature Graphic.
5. Publish `app-web` (`cd /Users/jayanthbharadwajm/development/app-web && npx wrangler pages deploy public --project-name=app-web`) and verify live HTTP 200 on `/hearit/privacy/` before submitting Play Console review.
6. Verify foreground service (`foregroundServiceType="mediaPlayback"`) and Notification Listener video walkthrough if Play Review requests permission demonstration.
