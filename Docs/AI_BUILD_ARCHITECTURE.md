# HearIt Technical Architecture

System architecture, modules, dependencies, data flows, services and technical design.

Source of truth for *what* the product must do is `AI_BUILD_MASTER_PROMPT.md`. This document defines *how* the code is structured. Decisions and their rationale live in `TECHNICAL_DECISIONS.md`.

---

## 1. High-level overview

```
                 ┌──────────────────────────────────────────────┐
                 │                    :app                      │
                 │  MainActivity (single activity, Compose)     │
                 │  HearItApplication (Hilt)                    │
                 │  Foreground announcement service            │
                 └───────┬───────────────┬───────────────┬───────┘
                         │               │               │
        ┌────────────────▼──┐   ┌────────▼───────┐  ┌────▼─────────────┐
        │ :core:notifications│   │ :core:audio    │  │ :core:tts        │
        │ NotificationListener│   │ Audio focus,   │  │ Android TTS      │
        │ extraction, dedupe │   │ device routing │  │ wrapper          │
        └────────┬───────────┘   └────────┬───────┘  └────┬─────────────┘
                 │                        │               │
                 └───────────┬────────────┴───────────────┘
                             ▼
              ┌──────────────────────────────────┐
              │ :core:rules  (pure Kotlin/JVM)   │  deterministic eligibility
              │ :core:queue  (pure Kotlin/JVM)   │  grouping, ordering, expiry
              │ :core:common (pure Kotlin/JVM)   │  shared domain models
              └──────────────────────────────────┘
                             ▲
        ┌────────────────────┴──────────────────────────────┐
        │ :core:datastore (preferences)  :core:database     │
        │                              (Room history)       │
        └───────────────────────────────────────────────────┘

  :feature:dashboard | :feature:applications | :feature:devices |
  :feature:settings  | :feature:history        → Compose UI, ViewModels
  :core:designsystem → Material 3 Expressive theme, reusable components
```

**Dependency rule:** dependencies point downward only. UI features depend on domain/data modules; domain modules never depend on UI, Android services or data frameworks. `:core:rules` and `:core:queue` are pure Kotlin/JVM so they are unit-testable without an Android device.

## 2. Module layout

| Module | Type | Responsibility |
|---|---|---|
| `:app` | Android application | Application class, Hilt root, MainActivity, foreground announcement service, navigation host, boot receiver |
| `:core:common` | Kotlin/JVM library | Shared domain models, `Clock`, small utilities, logging contract |
| `:core:rules` | Kotlin/JVM library | Deterministic announcement-eligibility pipeline |
| `:core:queue` | Kotlin/JVM library | Grouping, scheduling, priority ordering, overflow, expiry, dedup |
| `:core:notifications` | Android library | `NotificationListenerService`, extraction into domain model, notification identity/update detection |
| `:core:audio` | Android library | Audio focus, active output-device observation, device selection models |
| `:core:tts` | Kotlin/JVM library (Android wrapper joins with the speech service) | Announcement-template builder (pure logic) and the `TextToSpeechEngine` contract: availability, voices, speed/pitch, utterance lifecycle; the Android `TextToSpeech` wrapper satisfies this contract when P08 lands (ADR-018) |
| `:core:datastore` | Android library | Preferences DataStore, typed settings, global/per-app inheritance |
| `:core:database` | Android library | Room history storage, retention, migrations |
| `:core:history` | Android library | History recording/filtering/search business logic over Room |
| `:core:designsystem` | Android library | Material 3 Expressive theme, typography, shared components |
| `:feature:dashboard` | Android library | Dashboard UI and ViewModel |
| `:feature:applications` | Android library | App discovery, selection, per-app settings |
| `:feature:devices` | Android library | Audio output selection |
| `:feature:settings` | Android library | Global Rules, Voice & Speech, Settings, Backup & Restore, Diagnostics |
| `:feature:history` | Android library | History list UI |

Modules are introduced **when their first code lands**, not as empty skeletons (see ADR-002). Not every module exists at every point in the build; `AI_BUILD_PHASES.md` records when each appears.

## 3. Primary data flow

1. **Collection** — `NotificationListenerService` receives a notification. It extracts a `NotificationSnapshot` (stable key, package, app name, sender, message text, channel, category, post time, update flag) and forwards it regardless of eligibility. History collection is independent of announcement eligibility.
2. **Resolution** — settings from DataStore are merged: global defaults → per-app overrides → explicit "inherit" markers → Priority Announce app list → temporary-mute state. The result is an immutable `ResolvedSettings` for that notification's app.
3. **Eligibility** — `EligibilityRules.evaluate(snapshot, resolvedSettings, runtimeState)` returns `Announce` or `Skip(reason)` / `Queue(reason)`. Precedence is fixed and documented in ADR-006.
4. **Queueing** — eligible items enter the shared pending queue: grouping window, delay, min gap, priority ordering, size limits, expiry, overflow, dedup of already-announced updates, and call-blocked hold/release for full re-evaluation (ADR-019).
5. **Speaking** — the foreground service dequeues an item, builds the announcement string from the template, requests audio focus per policy, and speaks through the TTS engine.
6. **Continuous re-evaluation** — while speaking, screen/lock, active output, mute, DND, call and privacy state changes stop speech immediately (except when the user is using a control like Pause).
7. **Recording** — history entry written with final status (`Announced`, `Skipped(reason)`, `Queued`, `Pending`, `Interrupted`) and reason.

## 4. Services and components

- **`NotificationListenerService`** — primary notification source. Declared in the manifest, enabled only after the user grants notification access. Contains no speech or rules logic.
- **Foreground announcement service** (`foregroundServiceType="mediaPlayback"`) — runs after explicit Start; hosts the queue/TTS loop and the persistent notification with configurable actions. Started/stopped only by explicit user action; recreation after process death re-reads persisted state and never auto-announces before the user has started the service in that install.
- **Quick Settings tile** — `TileService` for Priority Announce, reflecting ON/OFF state.
- **Boot receiver** — restores an already-running session only within Android's boot/restart restrictions and only if the service was explicitly started before reboot; never silently re-announces.
- **WorkManager** — reserved for deferrable maintenance only (e.g. history retention cleanup if not done in-process). Never used for real-time notification speech.

## 5. Threading and concurrency

- Notification callbacks arrive on the listener's main thread; extraction is cheap and immediately handed to a dedicated `Dispatchers.Default`/service-scoped coroutine for rules + queue work.
- The foreground service owns a single `CoroutineScope` (SupervisorJob + service dispatcher); everything is cancelled in `onDestroy`.
- Queue mutation is confined to one actor/scope (single-writer) so ordering is deterministic; no `synchronized` blocks on the main thread.
- Room and DataStore do their own dispatching; no blocking I/O on the main thread. Suspend functions for all repository access.
- TTS callbacks (`onDone`, `onError`) move back into the service scope; utterance IDs carry the queue item identity so stale callbacks cannot affect the current item.

## 6. State and UI architecture

- MVVM + UDF: ViewModels expose `StateFlow<UiState>`; UI emits events (`sealed interface`); no business logic in composables.
- Single activity, Navigation Compose, bottom navigation + secondary routes.
- Service state is observed from the UI as a hot flow (in-app process); cross-process state that cannot be shared simply is read on demand and refreshed on lifecycle events. No silent service start from UI.
- Business rules live in `:core:rules`/`:core:queue` and are UI-independent and unit-testable.

## 7. Dependency injection

- Hilt with constructor injection; modules per feature area. Services are `@AndroidEntryPoint`/`@HiltAndroidInst`-injected where applicable (`@AndroidEntryPoint` for activities/fragments; services use `@AndroidEntryPoint` too).
- Scopes: `SingletonComponent` for repositories, DataStore, Room, TTS engine holder; `ServiceComponent`-scoped objects only if the service lifecycle requires it.
- No service locator, no global singletons holding state.

## 8. Storage

- **Room** — history only. Versioned schema; every schema change ships a `Migration`; destructive migration is not allowed for user data.
- **DataStore (Preferences)** — all settings, global and per-app, plus device selection, setup completion flag, service-started preference.
- Nothing leaves the device. Backups are settings-only, user-initiated, file-picker based.

## 9. Privacy and security in code

- No notification content, OTPs, sender names or message text in logs, crash output or any remote system (there are none in v1).
- Logs are local, structured by category (Notification, Rules, Queue, Audio, TTS, Service), level-configurable, 7-day retention.
- No secrets or signing material in Git; release signing lives outside the repository.
- Notification content is stored per the configured storage mode (Full content default / Metadata only / Disabled).

## 10. Compatibility envelope

- minSdk 29 (Android 10), targetSdk/compileSdk 37.
- Version-specific behaviour isolated behind small adapters: Bluetooth permissions (`BLUETOOTH_CONNECT` on 31+), notification-listener permission UI, foreground-service type requirements (34+), `POST_NOTIFICATIONS` (33+), partial-screen-share/call-state nuances.
- OEM risk (Samsung and others) isolated in `:core:audio` and the service layer; guidance strings added only when justified. No claim that background execution is guaranteed.

## 11. Testing architecture

- Pure JVM unit tests for `:core:rules`, `:core:queue`, template building, settings inheritance logic.
- Robolectric/Android instrumentation for Room, DataStore and listener extraction.
- Compose UI tests for navigation and key flows.
- Physical device required for notification access, Bluetooth/wired routing, TTS, calls, DND, reboot, background behaviour — tracked in `MANUAL_TEST_CHECKLIST.md`.
