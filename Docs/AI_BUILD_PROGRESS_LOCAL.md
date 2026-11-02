# Local Development Progress

Tracks actual implementation, builds, tests, physical-device verification and user-approved completion.

**First local build: 2026-11-03.** Before this entry the repository had never been compiled: every
phase P00–P19b was cloud-authored and marked "Not locally verified". This file now records what the
local agent actually ran, what it repaired, and what remains unverified.

---

## L1 — First local build, test execution and repair of cloud-authored phases

- **Date:** 2026-11-03
- **Commit:** `03edf18` — `fix(build): make the project build and pass all unit tests (L1)`
- **Branch:** `development` (in sync with `origin/development` at start; `0 0` ahead/behind)
- **Scope:** Build the accumulated cloud work, execute every JVM unit test, repair whatever prevented
  compilation, and record honest results. No new product features were added.
- **Working tree at start:** one untracked IDE file (`.idea/deploymentTargetSelector.xml`). It was
  left untouched and **not** committed (machine-specific, not project source).

### Commands actually run (exact)

| Command | Result |
|---|---|
| `./gradlew test --console=plain --continue` (first attempt) | **FAILED** — `app/src/main/res/values/strings.xml:1:48: Content is not allowed in prolog` |
| `./gradlew :app:compileDebugKotlin --console=plain --continue` | **FAILED** — JVM target mismatch (Java 25 vs Kotlin 11) in the five Kotlin/JVM modules |
| `./gradlew :app:compileDebugKotlin` (after repairs, iterations) | **PASSED** |
| `./gradlew test --console=plain --continue` (final) | **PASSED** — 379 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew assembleDebug --console=plain` | **PASSED** — `app/build/outputs/apk/debug/app-debug.apk` (34.5 MB) |
| `./gradlew lintDebug --console=plain` | **PASSED** — 0 errors, 57 warnings (37 `UnusedResources`, 6 `GradleDependency`, 3 `PluralsCandidate`, 3 `AndroidGradlePluginVersion`, 2 `UseKtx`, 2 `NewerVersionAvailable`, 2 `InlinedApi`, 1 `RedundantLabel`, 1 `NonConstantResourceId`) |

**Test totals per module (from `*/build/test-results/**/*.xml`):** `:core:rules` 88, `:app` 62,
`:core:queue` 46, `:core:datastore` 32, `:core:setup` 32, `:core:tts` 32, `:core:notifications` 30,
`:core:common` 29, `:core:database` 28 = **379**.

### Defects found and repaired

**Build configuration**

- Five pure Kotlin/JVM modules (`:core:common`, `:core:rules`, `:core:tts`, `:core:queue`,
  `:core:setup`) set `jvmTarget = 11` but left `compileJava` on the daemon JDK (25), so Gradle
  rejected every build. Added matching `java { sourceCompatibility/targetCompatibility = 11 }`.
- `:core:database` declared a Room schema **input directory** (`core/database/schemas`) that did not
  exist; KSP task validation failed. Created the directory with a `.gitkeep`.
- `:app` provides `HearItHistoryDatabase` (Hilt) but lacked the Room runtime on its compile
  classpath. Added `implementation(libs.androidx.room.runtime)`.

**Resources and manifest (never valid XML)**

- `app/src/main/res/values/strings.xml` had been committed as **one line with literal `\n` escapes**
  and `encoding='unicode_escape'`; 243 structural escapes converted to real newlines, encoding set to
  `utf-8`, a raw `&` in "Backup & Restore" escaped, and **7 duplicate string names removed**
  (`nav_backup_restore`, `nav_diagnostics`, `priority_announce_on/off`,
  `dashboard_recent_empty`, `dashboard_recent_unavailable`, `dashboard_view_all_history`).
- `AndroidManifest.xml`: a `<meta-data>` element had its second attribute outside the start tag
  (mismatched tags). The Quick Settings tile slot used a **non-existent permission**
  (`BIND_QUICK_SETTINGS_TOGGLE`) and a non-standard action/metadata. Corrected to
  `android.permission.BIND_QUICK_SETTINGS_TILE`, `android.service.quicksettings.action.QS_TILE` and
  the documented `android.service.quicksettings.TOGGLEABLE_TILE` metadata.
- `res/layout/qs_tile_layout.xml` and `res/xml/quick_settings_config.xml` were malformed and
  described an implementation the platform does not support (`Tile`/`TileService` have no
  RemoteViews or per-tile XML config). Both were deleted; the tile now uses the platform API.

**Source (393 compile errors in 10 files)**

- `PriorityAnnounceTileService` was non-compiling pseudo-code (`@DrawableRes` on a class,
  `android.content.*;`, undefined `stateReader`/`stringResource`, `setQsTile(QsTile)` which does not
  exist, wrong click intent). Rewritten against `TileService`/`Tile` with Hilt-injected
  `ServiceStatusMonitor`, a **no-op while the service is stopped** (no silent start) and a documented
  API-33+ limitation for reflecting state.
- `ServiceStatusMonitor` gained Priority Announce state (`markPriorityAnnounceActive`,
  `isPriorityAnnounceActive`); `ServiceSnapshot` gained `isPriorityAnnounceActive`.
- `AnnouncementService` had invalid `when` branches (assignment used as a branch), smart-cast errors
  on `currentAction`, calls to `engine.pause()/resume()` that the TTS contract does not define, a
  private `NOTIFICATION_ID`, a missing `onBind`, and it never passed the Priority Announce flag into
  `RuntimeStateReader`. Rewritten: explicit `else` start path, local smart casts, Pause = stop current
  utterance / Resume = re-speak it (documented limitation: Android TTS has no native pause), priority
  flag wired into state reads, 24 h expiry retained.
- `DashboarModels`/`DashboardViewModel` were structurally broken (getters outside the class body,
  Compose `stringResource` inside a data class, duplicated `onRefresh`, undefined `context`, missing
  imports). Rewritten as valid state + ViewModel with `@ApplicationContext`, filter→`HistoryQuery`
  mapping and failure reporting instead of swallowed exceptions.
- `DashboardScreen` (148 errors) and `GlobalSettingsScreen` (92 errors) were unwritten fragments
  (`){ supporting = … }`, `Modifier…start = 4.dp`, mid-file imports, undefined callbacks, references
  to a non-existent `ServiceStatusMonitor.State`). Both rewritten as coherent Compose screens;
  Global Rules now has a real `GlobalSettingsViewModel` writing every group through
  `SettingsRepository`, and it is reachable from the GLOBAL_RULES destination.
- `SettingsSource` constructor parameter had no type and the enabled/priority semantics were
  inverted (an app in the Priority list became *enabled*). Fixed: apps stay disabled by default,
  priority listing is separate.
- `AppSettingsEditor` imported `AppOverride` from the wrong package; now imports it from
  `:core:datastore`.

**Logic defects exposed by the tests (6 failures)**

| Test | Cause | Fix |
|---|---|---|
| `EligibilityRulesTest.evaluate_unlocked_skipsWithUnlockedReason` and `…lockConditionUnlockedButDeviceLocked…` | lock skip reason used the *condition* instead of the *actual state* (ternary inverted) | fixed in `EligibilityRules`; reasons now name the device state, like the screen branch |
| `CallAndDndEligibilityTest.evaluate_callEnds_queuedItem…` | test expected `Queue` while the default `queueDuringCall` is **off** (master prompt §6) | test now enables the per-app flag |
| `TextLengthEligibilityTest.evaluate_privacyAndTooLong…` | the test body contained no banking term, so privacy could not win | test body now trips the banking detector |
| `SettingsCodecTest.decode_absentPerAppFields_areInherited` | test expected an all-inherited override to survive a round-trip, contradicting sparse storage (ADR-021) | test asserts the empty override is dropped |
| `AnnouncementPipelineTest.onSpeechFinished…` | `markCompleted` reported no history event for the spoken group | added `QueueEvent.Announced` (status `ANNOUNCED`) emitted from `markCompleted` |

### Feature status after this phase (honest)

- **Compiles and unit-tested:** eligibility rules (all ADR-006 families), queue/grouping/scheduling,
  announcement template, settings codec + DataStore repository, history storage, setup flow, dashboard
  shell with start/stop/mute/priority/history-search, applications list and per-app overrides.
- **Compiles, not behaviourally verified:** announcement service, listener, TTS wrapper, Quick
  Settings tile, Priority Announce expiry, dashboard Global Rules screen.
- **Not implemented despite the cloud phase table:** a dedicated **Devices** screen (P14) and a
  dedicated **History** screen (P18) do not exist — history search lives on the dashboard only. The
  P19 **Backup & Restore** and **Diagnostics** screens were non-compiling fragments and were removed;
  only the settings export/import repository methods survive. These are recorded as gaps, not claimed
  as done.
- The service still uses the placeholder `SettingsSource` (all apps disabled), so **end-to-end
  announcements are not possible yet** — enabling apps in the UI does not reach the pipeline.

### Emulator verification

Not performed in this phase (no emulator started, no UI interaction testing).

### Physical-device verification

**Not tested.** Notification access, Bluetooth/wired routing, TTS, calls, DND, reboot and background
behaviour all still require a physical device per `MANUAL_TEST_CHECKLIST.md`.

### Known limitations / risks

- UI is compile-verified only; TalkBack focus order, the new alert dialog and the Global Rules screen
  have not been exercised.
- Pause/Resume semantics are stop-and-repeat because Android TTS has no pause API.
- The Quick Settings tile reflects state on API 33+ only (the compile SDK no longer ships `QsTile`).
- Free-text history search matches the app name only; content search needs a Room index.
- `SettingsSource` placeholder: the pipeline receives no user settings yet.
- Lint warnings (57) are untriaged; 37 are unused strings/resources left by the removed screens.

### Next phase (proposed)

P20 release hardening cannot start honestly until the gaps above are closed. Proposed next phase:
**L2 — wire persisted settings into the service (`SettingsSource` → `SettingsRepository`), so enabling
an app actually produces an announcement**, then a dedicated History screen (P18) and Devices screen
(P14).

