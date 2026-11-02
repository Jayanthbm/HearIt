# HearIt Development Phases

Detailed implementation milestones, smaller phases, dependencies, deliverables and acceptance criteria.

- Phase owner **Cloud** means: implemented and reviewed by the cloud agent, marked *Not locally verified* until the local agent builds and tests it.
- Phase owner **Local** means: verified by the local agent on emulator/device.
- Every phase has exactly one Conventional Commit on `development`.
- A phase is only "done" when its deliverables exist, tests are written where meaningful, docs are updated, and the progress file records the commit.

Status legend: `Not started` · `In progress (cloud, not locally verified)` · `Locally verified`.

---

## P00 — Documentation foundation

- **Owner:** Cloud · **Depends on:** nothing
- **Scope:** Replace every placeholder document under `Docs/` with content derived from `AI_BUILD_MASTER_PROMPT.md`: phase plan, architecture, global standards, technical decisions (ADRs), manual test checklist, changelog.
- **Deliverables:** complete `AI_BUILD_PHASES.md`, `AI_BUILD_ARCHITECTURE.md`, `GLOBAL_APP_STANDARDS.md`, `TECHNICAL_DECISIONS.md`, `MANUAL_TEST_CHECKLIST.md`, `CHANGELOG.md`.
- **Tests:** none (documentation only).
- **Acceptance:** no placeholder-only document remains; every requirement in the master prompt is represented in the plan or in an ADR; conflicts are recorded instead of silently resolved.

## P01 — Core domain: module foundation and eligibility rules (part 1)

- **Owner:** Cloud · **Depends on:** P00
- **Scope:** Introduce the first business-logic modules as pure Kotlin/JVM libraries: `:core:common` (shared domain models, clock abstraction) and `:core:rules` (deterministic announcement-eligibility pipeline). Implement the pipeline skeleton plus screen/lock, temporary mute and Priority Announce precedence rules.
- **Deliverables:**
  - `settings.gradle.kts` includes `:core:common`, `:core:rules`.
  - Version catalog entries for Kotlin JVM modules; root build applies `org.jetbrains.kotlin.jvm` with `apply false`.
  - Domain models: notification snapshot, resolved per-app settings, device/runtime state, decision + skip reasons.
  - `EligibilityRules.evaluate(...)` returning a decision with a specific reason.
  - JVM unit tests for precedence, inheritance and Priority Announce override.
- **Acceptance:** business logic has no Android/UI imports; every decision path returns a specific reason; tests cover each rule family added.

## P02 — Eligibility rules (part 2): text length, keywords and privacy

- **Owner:** Cloud · **Depends on:** P01
- **Scope:** Add text-length limits (per message / whole notification, skip-never-truncate), keyword filtering (case-insensitive, whole-word/phrase), and privacy rules (OTP, banking, password, payment, sensitive keywords, hide-content-and-announce-sender, app exclusion).
- **Acceptance:** each rule has pass/fail/skip tests; truncation is impossible by construction; privacy rules cannot be overridden by Priority Announce.

## P03 — Eligibility rules (part 3): audio output, DND and calls

- **Owner:** Cloud · **Depends on:** P01
- **Scope:** Allowed-output matching (multiple outputs, wired generic, phone speaker, per-app sets), DND respect with global/per-app override, active-call suppression with optional per-app call-time queueing, and re-evaluation requirement for queued items.
- **Acceptance:** DND vs silent-channel distinction is modelled; no rule other than screen/lock can be overridden by Priority Announce; queued items are re-evaluated through the full pipeline.

## P04 — Announcement format and TTS contract

- **Owner:** Cloud · **Depends on:** P01
- **Scope:** Announcement-template builder (app + sender + message, configurable sections, intro, pauses, speed/pitch/voice) as pure logic, plus the `:core:tts` interface boundary (`TextToSpeechEngine` contract) that the Android implementation will satisfy.
- **Acceptance:** missing sender/message handled gracefully; template output unit-tested; no Android TTS calls in pure logic.

## P05 — Queue, grouping and scheduling

- **Owner:** Cloud · **Depends on:** P01, P04
- **Scope:** `:core:queue`: grouping window per app, delay before announcement, max messages per announcement, min gap between announcements, priority ordering (Critical/High/Normal/Low, higher jumps ahead, equal FIFO), never interrupting current speech, global/per-app size limits, expiry, overflow policies (discard oldest / newest / keep highest priority), dedup of notification updates.
- **Acceptance:** ordering, overflow and expiry unit-tested; discarded/expired items keep a history status and reason.

## P06 — Build foundation: Hilt, Application and manifest baseline

- **Owner:** Cloud · **Depends on:** P01
- **Scope:** Hilt + KSP wiring, `HearItApplication` registered in the manifest, `@AndroidEntryPoint MainActivity`, baseline manifest declarations (notification-listener service slot, foreground-service types, `POST_NOTIFICATIONS`, `BLUETOOTH_CONNECT` where required).
- **Acceptance:** manifest `exported` flags and foreground-service types reviewed for API 29–37; no permission requested before it is explained by setup flow.

## P07 — Notification listener foundation

- **Owner:** Cloud · **Depends on:** P01, P06
- **Scope:** `NotificationListenerService` implementation: binding lifecycle, notification extraction into `NotificationSnapshot` (app, sender, text, channel, category, key, update detection), permission revocation handling, no content in logs.
- **Acceptance:** extraction handles missing title/text/extras; updates distinguished from new posts; listener code contains no speech logic.

## P08 — Foreground announcement service and speech pipeline

- **Owner:** Cloud · **Depends on:** P05, P07, P04
- **Scope:** Foreground playback service wiring listener → rules → queue → TTS; persistent notification; process death and recreation; audio focus (duck/pause/no intervention, per-app inheritance); stop speech when eligibility stops matching; audio-device disconnect handling.
- **Acceptance:** main thread never blocks; speech stops immediately on screen/lock, output or mute changes; no notification content in logs.

## P09 — Preferences foundation (`:core:datastore`)

- **Owner:** Cloud · **Depends on:** P01
- **Scope:** DataStore-backed global defaults, per-app overrides with explicit inheritance, typed settings accessors, defaults matching the master prompt (all apps disabled, keyword filtering off, speaker off, history full content, etc.).
- **Acceptance:** inheritance is observable (app can show "inherits global"); defaults unit/instrumentation-tested.

## P10 — History storage (`:core:database`)

- **Owner:** Cloud · **Depends on:** P01, P07
- **Scope:** Room schema for history entries (status, reason, app, sender, content per storage mode), retention cleanup, update-vs-new handling, pagination-friendly indices, schema versioning/migration policy.
- **Acceptance:** history recorded regardless of eligibility; storage modes and retention honoured; migrations have tests.

## P11 — Setup and onboarding flow

- **Owner:** Cloud · **Depends on:** P06, P09, P04
- **Scope:** 9-step first-launch flow (welcome, notification access, Bluetooth permissions by API level, TTS check, background/battery guidance, app selection, audio-device selection, test announcement, review + explicit Start) with skip rules and per-step explanations.
- **Acceptance:** no silent service start; Start enabled only when prerequisites are met; back navigation preserves state.

## P12 — Dashboard and navigation shell

- **Owner:** Cloud · **Depends on:** P11, P09
- **Scope:** Bottom navigation (Home, Apps, Devices, History), secondary entries (Global Rules, Voice & Speech, Settings, Backup & Restore, Diagnostics), dashboard sections per master prompt §13, service status observation.
- **Acceptance:** navigation state survives process recreation; all targets ≥48dp; TalkBack semantics verified statically.

## P13 — Applications feature

- **Owner:** Cloud · **Depends on:** P09, P12
- **Scope:** Installed notification-capable app discovery, icon/name list, search, select all/deselect all, per-app settings screens with visible inheritance indicators.
- **Acceptance:** all apps disabled by default; inheritance shown explicitly; large lists load lazily.

## P14 — Devices feature

- **Owner:** Cloud · **Depends on:** P09, P12
- **Scope:** Bluetooth/wired/speaker output selection (multi-select), previously connected device list, persistence across restarts, availability and skip/queue behaviour for missing outputs.
- **Acceptance:** no promise of routing beyond supported Android APIs; limitations documented in UI copy.

## P15 — Global Rules, Voice & Speech settings

- **Owner:** Cloud · **Depends on:** P09, P04, P12
- **Scope:** Settings UIs for screen/lock, audio, text length, DND/calls, categories/keywords, privacy, announcement format, speed/pitch/voice, grouping/queue, audio focus.
- **Acceptance:** every rule family is configurable; defaults match the master prompt; no hardcoded user-facing strings.

## P16 — Reading controls and temporary mute

- **Owner:** Cloud · **Depends on:** P08, P12
- **Scope:** Pause, Resume, Stop current, Skip current, Repeat current, Temporary mute (15 min / 30 min / 1 h / until re-enabled), in-app and persistent-notification controls, configurable notification actions.
- **Acceptance:** mute is a global override affecting the pipeline; controls reachable by TalkBack.

## P17 — Priority Announce mode

- **Owner:** Cloud · **Depends on:** P01, P08, P12
- **Scope:** Separate app list, manual ON/OFF with automatic expiry and visible remaining time, Dashboard toggle, Quick Settings tile, persistent-notification action (disable-able), dedup across normal and Priority modes.
- **Acceptance:** overrides only screen/lock; duplicate prevention verified by tests.

## P18 — History feature UI

- **Owner:** Cloud · **Depends on:** P10, P12
- **Scope:** Filter (app, status), search (content, sender, app, date range, status), sort, lazy loading, clear single/clear all with confirmation, storage-mode and retention settings.
- **Acceptance:** clear operations confirm before destroying; pagination avoids full-table loads.

## P19 — Backup & Restore, Diagnostics, logging

- **Owner:** Cloud · **Depends on:** P09, P08, P12
- **Scope:** Settings-only export/import via Android file picker with version validation; diagnostics screen (service state, permissions, TTS availability, active output); structured local logs with levels and 7-day retention; optional biometric lock and secure-screen mode.
- **Acceptance:** no history/notification content in backups; invalid backup files handled safely; no notification content in logs.

## P20 — Release hardening

- **Owner:** Local (build/sign/device), Cloud (config review) · **Depends on:** P01–P19
- **Scope:** R8/ProGuard rules verified, release build, signing kept out of Git, version `1.0.0`, changelog/release checklist, accessibility and OEM pass per `MANUAL_TEST_CHECKLIST.md`.
- **Acceptance:** emulator + physical-device matrix executed by local agent; untested/emulator-tested/device-tested tracked separately.

---

## Current status

| Phase | Status |
|---|---|
| P00 Documentation foundation | Locally verified (docs only) |
| P01 Core domain: modules + eligibility rules (part 1) | Locally verified — builds, unit tests pass |
| P02 Eligibility rules (part 2): text length, keywords, privacy | Locally verified — builds, unit tests pass |
| P03 Eligibility rules (part 3): audio output, DND, calls | Locally verified — builds, unit tests pass |
| P04 Announcement format and TTS contract | Locally verified — builds, unit tests pass |
| P05 Queue, grouping and scheduling | Locally verified — builds, unit tests pass |
| P06 Build foundation: Hilt, Application and manifest baseline | Locally verified — debug APK builds; manifest repaired for the tile |
| P07 Notification listener foundation | Locally verified — builds, unit tests pass; device behaviour untested |
| P08 Foreground announcement service and speech pipeline | Repaired and locally verified to compile; **not behaviourally tested** (no device) |
| P09 Preferences foundation (`:core:datastore`) | Locally verified — builds, unit tests pass |
| P10 History storage (`:core:database`) | Locally verified — builds, unit tests pass |
| P11 Setup and onboarding (`:core:setup`, `:app`) | Locally verified to compile; UI not interacted with |
| P12 Dashboard and navigation shell (`:app`/`dashboard`) | Repaired and locally verified to compile; UI not interacted with |
| P13 Applications feature (`:app`/`applications`) | Locally verified to compile; UI not interacted with |
| P14 Devices feature (`:app`/`devices`) | **Gap — no Devices screen exists in the code**, despite the cloud entry; not implemented |
| P15 Global Rules, Voice & Speech settings | Repaired (screen was non-compiling) and locally verified to compile |
| P16 Reading controls and temporary mute | Locally verified to compile; pause = stop + re-speak (no native TTS pause) |
| P17 Priority Announce mode | Locally verified to compile; rule precedence unit-tested |
| P18 History feature UI | **Partial — dashboard search/clear only; no dedicated History screen** |
| P19 Backup & Restore, Diagnostics, logging | **Partial — export/import repository methods only; the two screens were non-compiling and were removed** |
| P19b Quick Settings tile | Repaired against the real `TileService`/`Tile` API and locally verified to compile |
| P20 Release hardening | Not started (blocked by the P14/P18/P19 gaps) |
| L1 Build/test verification of P00–P19b | Complete — `379` unit tests pass, debug APK builds, lint 0 errors (`2026-11-03`) |
