# HearIt — AI Build Master Prompt

## 1. Role and source of truth

You are an engineering agent working on **HearIt**, a native Android notification-reader application.

Read this document before making changes. Also read:
- `AI_BUILD_WORKFLOW_LOCAL.md` or `AI_BUILD_WORKFLOW_CLOUD.md`, depending on your role.
- `AI_BUILD_PHASES.md`
- Your assigned progress file.
- `AI_BUILD_ARCHITECTURE.md`
- `GLOBAL_APP_STANDARDS.md`
- `TECHNICAL_DECISIONS.md`
- `MANUAL_TEST_CHECKLIST.md`
- `CHANGELOG.md`

If a document is still a placeholder, create or complete it from the requirements in this master document and record the work in the relevant progress file. Do not silently assume a missing document contains decisions.

If documents conflict, do not invent a resolution. Prefer explicit, newer user-approved decisions; record the conflict and ask the user when it changes product behaviour or architecture.

## 2. Product identity

- Product: **HearIt**
- Android application ID / namespace: `com.jayanth.hearit`
- Existing Android Studio project: `/Users/jayanthbharadwajm/AndroidStudioProjects/HearIt`
- Language: Kotlin
- Minimum Android: Android 10 / API 29
- Native Android application; phones only, portrait-first, adaptive to phone screen sizes.
- Initial UI language: English, with architecture ready for localization.
- Theme: Material 3 Expressive; Light, Dark and System, default System.
- Initial release: `1.0.0`, direct signed APK distribution.
- No cloud backend, account, sync, analytics, crash-reporting SDK, ads, subscriptions, IAP or monetization SDK in v1.
- Keep future Play Store compatibility in mind without making Play Store distribution a v1 requirement.

## 3. Product purpose and scope

HearIt reads notifications aloud through supported Bluetooth or wired earphones/headphones. It is not limited to any manufacturer.

Use Android `NotificationListenerService` as the primary notification source and native Android TextToSpeech. Do not use translation. Pass notification text to the installed TTS engine; actual spoken language depends on installed engine voice/language support.

**Phase 1 is notification reading only.** Do not implement voice commands, speech recognition, voice replies, dismissing notifications by voice, or executing notification actions.

All notification content and application data stay locally on the device.

## 4. Setup and service lifecycle

First launch should guide the user through:
1. Welcome and explanation.
2. Notification access.
3. Bluetooth / nearby-device permissions where required by Android version.
4. TTS availability and language/voice check.
5. Background execution and battery guidance.
6. App selection (all apps disabled initially).
7. Audio-device selection.
8. Test announcement.
9. Review and explicit Start.

Explain why each permission is required. Optional steps may be skipped where safe. Do not claim that Android or an OEM will guarantee background execution. Provide manufacturer-specific guidance only when justified.

Returning users open Dashboard. Opening the app must not silently start the service. The user explicitly starts/stops HearIt. Start is available only after at least one app is enabled and setup prerequisites are satisfied. Warn when permissions are missing or revoked.

Run independently of the main UI after explicit start. Use a foreground service where required, a persistent notification with controls, and reboot recovery subject to Android restrictions. WorkManager is for deferrable work, not real-time notification speech. Handle process death and service recreation safely.

## 5. App selection

- Discover installed apps capable of posting notifications.
- Display app icon and name.
- Search, Select All and Deselect All.
- All apps disabled by default; user enables each desired app.
- Per-app settings with inheritance from global defaults.
- App settings must clearly show when a value inherits the global value.

## 6. Eligibility rules

Evaluate notification eligibility through a deterministic, testable rules pipeline. Keep collection/history separate from announcement eligibility.

### Screen and lock
- Global default: announce only when screen is OFF **and** phone is LOCKED (AND logic).
- Screen condition independently supports ON, OFF or Both.
- Lock condition independently supports Locked, Unlocked or Both.
- Global audio default: **honor device silence — the phone must not be spoken to when it is on Silent** (`RINGER_MODE_SILENT`); `VIBRATE` alone keeps the standard notification sound and is *not* silenced (ADR-017). Honors global and per-app overrides; Priority Announce never overrides silence.
- Per-app overrides independently inherit or override each condition, including silence.
- If conditions stop matching during speech, stop speech immediately.
- Priority Announce may override only screen/lock restrictions for apps explicitly selected in its separate app list.

### Audio output and devices
- Users can select multiple allowed outputs.
- Bluetooth device list includes previously connected devices and devices that can be added while connected.
- Persist device identity/name and selection across restarts.
- Include a generic Wired option.
- Optional phone speaker output; disabled by default.
- Support global and per-app output preferences, including multiple outputs per app.
- Announce only while the active output is allowed. If the active output changes to an unselected output, stop speech.
- If an earphone disconnects while speaking, stop immediately. Do not pause/resume and do not fall back to speaker by default.
- If a selected output is unavailable, apply configurable Skip or Queue behaviour.
- Do not promise arbitrary routing to a specific Bluetooth device if Android/OEM APIs do not support it. Use supported APIs, test on physical devices and explain limitations.

### Text length
- Global maximum text length and per-app overrides.
- Limit scope: per message or whole notification.
- Default recommendation: per message for messaging apps.
- If a limit is exceeded, skip the applicable message/notification entirely; never read a truncated partial message.

### DND and calls
- Respect Android DND by default; global and per-app override.
- Never speak while a phone call is active.
- Do not queue during calls by default; per-app setting may enable call-time queueing.
- A queued item after a call must be re-evaluated against all current eligibility rules. Queueing must not bypass screen/lock, device, privacy, mute or other rules.
    Distinguished and honored: DND is a preference; Silent/ Vibrate-only is a physical-constraints check (ADR-017) that never gets overridden; notification channel policies stay visible and distinct.

### Categories and keywords
- Use notification channel/category/type metadata where Android exposes it.
- Provide custom keyword filtering globally and per app.
- Keyword filtering disabled by default.
- Support multiple case-insensitive keywords and configurable whole-word/phrase matching.
- Android does not expose standardized semantic categories for every app; provide sensible fallback and do not claim perfect categorization.

### Privacy
Global and per-app controls:
- Skip OTP/verification-code content.
- Skip banking/financial notifications.
- Skip password-related notifications.
- Skip payment/transaction alerts.
- Hide content and announce app/sender only.
- Exclude an app entirely.
- Custom sensitive keywords/rules.
Detection is imperfect; combine available metadata and user rules. Never promise perfect semantic detection.

## 7. Announcement content and TTS

- Fully customizable announcement format.
- Default format: app name + sender (if available) + message.
- Example: “WhatsApp. Rahul says, Hey, are you coming today?”
- Configurable app name, sender, message, custom introduction, pauses between sections and speaking speed.
- Handle missing sender or message gracefully.
- Native Android TTS only; no translation.
- Configure speed, pitch and voice where the installed engine supports them.
- Handle unavailable language data gracefully.
- Example: Kannada text can be spoken in Kannada only if an installed TTS voice supports it.

## 8. Grouping, scheduling and queue

- Group notifications from the same app within a configurable time window.
- Announce app name and message count, followed by available sender/message content.
- Configurable delay before announcement and maximum messages per announcement.
- Avoid duplicate announcements, including notification updates.
- Different apps use a shared pending queue.
- Configurable minimum gap between announcements.
- Configurable app priority (Critical, High, Normal, Low).
- Default: do not interrupt an item already being spoken.
- Higher-priority pending items move ahead of lower-priority pending items immediately; equal priority is FIFO.
- Per-app interruption setting may be provided, but its exact semantics must be explicit and tested. Do not interrupt current speech by default.
- Queue controls: global maximum size and per-app maximum/retain-latest-N.
- Configurable notification expiry age.
- Overflow policies: discard oldest, discard newest, or keep highest priority.
- Suggested defaults, subject to confirmation in product settings: global 50, per-app 10, expiry 30 minutes, discard oldest.
- When an item is discarded/expired, retain its history record and final status/reason.

## 9. Audio focus and interruption

- Global default: duck other audio.
- Global and per-app modes: Duck, Pause, No intervention.
- Per-app inherits global by default.
- Restore prior audio behaviour after speech or interruption where Android APIs permit.
- Handle audio focus loss, call state, device disconnect and changing eligibility safely.

## 10. Priority Announce mode

- Separate app list from normal enabled-app list; normal settings remain unchanged.
- Manual ON/OFF, configurable automatic expiry duration, visible remaining time.
- Dashboard toggle, Android Quick Settings tile, and persistent-notification action enabled by default (user can disable that action).
- No home-screen widget.
- Quick Settings tile reflects ON/OFF state.
- Overrides screen ON/OFF and lock/unlock restrictions only.
- Does NOT override allowed audio output, max text length, keywords, categories, privacy, active-call suppression, temporary mute or queue rules.
- Duplicate prevention across normal and Priority mode. An item announced in either mode must not be repeated by the other mode. Notification updates should not repeat unless configured.
- Use notification identity/update metadata for deduplication.

## 11. Reading controls and temporary mute

Provide Pause, Resume, Stop current, Skip current, Repeat current and Temporary mute.
Temporary mute choices: 15 minutes, 30 minutes, 1 hour, or until manually re-enabled. Mute is a global override.
Controls available in app and persistent notification. Bluetooth headset media-button support is optional and must be implemented only if reliably supported.
Persistent notification actions are configurable; defaults include pause/resume, stop announcements and Priority Announce toggle.

## 12. History

History collection is independent of whether an item is eligible for speech. Record selected-app notifications even if screen/lock, device, DND, call, mute or another rule prevents announcement.

Statuses include:
- Announced
- Skipped (with specific reason)
- Queued
- Pending
- Interrupted

Record actionable reasons such as screen on, unlocked, no allowed output, too long, keyword, DND, call, privacy, muted, expired or overflow.

History features:
- Filter by app and status.
- Search notification content, sender, app, date range and status.
- Sort newest/oldest.
- Lazy loading/pagination.
- Clear individual entries and clear all.
- Local-only storage.
- Global/per-app storage modes: Full content (default), Metadata only, Disable history; per-app can inherit global.
- Configurable retention: 1, 3, 7, 10, 15, 30, 60, 90 days or Never.
- Configurable notification update handling: update existing history entry or save every update as a new entry.
- Distinguish notification updates from genuinely new messages; dedupe speech separately from history collection.
- History is sensitive; retention, clearing and privacy settings must work reliably.

## 13. Dashboard and navigation

Bottom navigation:
- Home (Dashboard)
- Apps
- Devices
- History

Secondary navigation/settings access:
- Global Rules
- Voice & Speech
- Settings
- Backup & Restore
- Diagnostics

Dashboard sections:
- Service Active/Inactive.
- Current connected/active audio device.
- Normal announcements ON/OFF.
- Priority Announce ON/OFF and remaining duration.
- Temporary mute status.
- Current speaking item and queue status.
- Recent announcement history.
- Quick access to app/device settings.
- Test Announcement, Applications, Audio Devices and Diagnostics actions.

## 14. Test Announcement

Provide a test flow where the user chooses an audio output, enters sample text and taps Test Announcement. Play using selected TTS voice/speed/pitch. Test announcements must not enter notification history or affect the notification queue.

## 15. Backup, security and privacy

- Manual, user-confirmed settings-only backup and restore.
- Do not include notification history or content.
- Use Android file picker; no cloud upload.
- Validate backup format/version and handle invalid files safely.
- Optional biometric/device-credential authentication for settings/history, disabled by default.
- Background service continues when UI is locked.
- Optional secure-screen mode to hide sensitive screens in recents and use `FLAG_SECURE` where supported.
- No notification content sent off-device.
- No secrets, credentials or signing keys in Git.

## 16. Technical architecture

Use native Kotlin, Jetpack Compose, Material 3, Navigation Compose, MVVM, StateFlow and unidirectional data flow. Single Activity. Repository pattern. Keep business rules independent of UI and make them testable. Use Hilt for dependency injection; constructor injection and Hilt modules; avoid service locator.

Proposed feature-oriented modules (adjust only with documented technical justification; avoid needless fragmentation):
- `:app`
- `:core:common`
- `:core:database`
- `:core:datastore`
- `:core:designsystem`
- `:core:notifications`
- `:core:audio`
- `:core:tts`
- `:core:rules`
- `:core:queue`
- `:core:history`
- `:feature:dashboard`
- `:feature:applications`
- `:feature:devices`
- `:feature:settings`
- `:feature:history`

Use Room for structured history and DataStore for preferences. Coroutines with structured concurrency. No blocking main thread. Separate notification listening from speech playback responsibilities. `NotificationListenerService` is the primary source. Use a foreground service for ongoing playback where required. Boot receiver recovery must respect Android restrictions. WorkManager is only for deferrable work. Accessibility Service is not a primary approach; investigate only if a specific requirement cannot be met with supported APIs, and then assess policy/privacy before proposing it.

## 17. Quality, diagnostics and accessibility

- Small focused classes/functions; SOLID where useful.
- Supported Android APIs; isolate version/OEM differences.
- Minimal justified dependencies.
- Proper lifecycle, cancellation, error handling and logging.
- No swallowed exceptions or silent failures.
- No hardcoded user-facing strings.
- KDoc for public APIs/complex logic.
- Detekt or equivalent static analysis where practical.
- Structured local logs: Notification, Rules, Queue, Audio, TTS, Service.
- Never log notification content, OTPs or sensitive text.
- Configurable log levels; default retention 7 days.
- Diagnostics screen: service state, permissions, TTS availability, active output device; local log export.
- No remote analytics/crash SDK initially.
- Every UI phase must consider TalkBack, Compose semantics, meaningful content descriptions, large fonts, contrast, minimum 48dp targets, non-colour-only status, reduced motion and confirmation for destructive actions.

## 18. Testing and compatibility

Automated tests should cover:
- Eligibility/filtering and precedence.
- Screen/lock and Priority Announce.
- Queue ordering, priority, overflow and expiry.
- Grouping, text limits and deduplication.
- Privacy and per-app inheritance.
- Room/DataStore.
- Important UI and integrations.

Use emulator for UI, navigation, settings and general logic. Physical device is mandatory for real notification access, Bluetooth/wired routing, TTS, calls, DND, reboot and background behaviour. Test Android 10 and a recent Android version; prioritize Samsung and stock Android/Pixel. Track emulator-tested, physical-device-tested and untested separately. Never claim physical verification without doing it.

## 19. Build and release

- Start version `1.0.0`.
- Debug and Release variants.
- Secure release signing; signing material excluded from Git.
- R8/ProGuard configuration and release-build verification.
- Changelog and release checklist.
- Initial distribution is a signed APK installed manually.
- No v1 update checker or GitHub Releases integration.
- Show installed version in About.

## 20. Git strategy

- `main`: stable, locally verified implementation.
- `development`: shared work branch for both agents.
- Agents work sequentially; user ensures they are never run in parallel.
- Before work, fetch and inspect latest Git state; never assume local/cloud progress is current.
- Cloud commits to `development` and marks work not locally verified.
- Local pulls latest `development`, builds/tests, fixes issues and commits.
- Local phase completion updates both progress files with actual verification.
- Cloud updates only cloud progress.
- One commit per completed small phase.
- Use Conventional Commits, e.g. `feat(notification): implement notification listener`.
- Inspect `git status` before committing.
- Never commit secrets, signing keys, local machine paths, generated build artifacts.
- Merge `development` into `main` only after planned work is locally verified and user-approved.

## 21. Required documentation

Maintain these files under `Docs/`:
- `AI_BUILD_MASTER_PROMPT.md`
- `AI_BUILD_WORKFLOW_LOCAL.md`
- `AI_BUILD_WORKFLOW_CLOUD.md`
- `AI_AGENT_START_CONTINUE_PROMPT.md`
- `AI_BUILD_PHASES.md`
- `AI_BUILD_PROGRESS_LOCAL.md`
- `AI_BUILD_PROGRESS_CLOUD.md`
- `AI_BUILD_ARCHITECTURE.md`
- `GLOBAL_APP_STANDARDS.md`
- `TECHNICAL_DECISIONS.md`
- `MANUAL_TEST_CHECKLIST.md`
- `CHANGELOG.md`

Keep requirements, architecture, phases, decisions, progress and changelog synchronized. Do not leave placeholder-only documents once implementation begins.

## 22. Definition of done for every small phase

A phase is complete only when:
1. Scope and acceptance criteria are met.
2. Code and relevant docs are updated.
3. Tests appropriate to the phase are written.
4. Local agent has actually built and executed relevant tests, or cloud agent explicitly marks them Not locally verified.
5. Known limitations and follow-up work are documented.
6. Git commit exists and is recorded.
7. Progress files are updated by the correct agent.
8. User approval is obtained for local phase completion before the local agent proceeds to the next phase.

Do not implement the entire application in one pass. Work in small, reviewable phases.
