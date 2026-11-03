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
an app actually produces an announcement** (done — see L2 below), then a dedicated History screen
(P18) and Devices screen (P14).

---

## L2 — Persisted settings wired into the announcement service

- **Date:** 2026-11-03
- **Commit:** `98227ed` — `feat(service): read persisted settings in the announcement pipeline (L2)`
- **Branch:** `development`
- **Scope:** Close the largest functional gap left by L1 — the service ran on a hard-coded
  placeholder, so enabling an app in the Applications screen never produced an announcement.
- **Working tree at start:** clean apart from the untracked IDE file
  `.idea/deploymentTargetSelector.xml`, which was again left untouched and not committed.

### The gap

`AnnouncementService` built `SettingsSource(sessionStore)` itself; that class returned
`appEnabled = false` for every package and default format/speech. `:core:datastore` already
owned the real values (P09) and the Applications/Global Rules screens already wrote them (P13/P15),
so the pipeline and the UI were simply not connected.

### What changed

- **`app/.../service/SettingsSource.kt` (rewritten).** Now a `SettingsRepository`-backed seam: it
  mirrors the repository `Flow` into an immutable `HearItSettings` snapshot
  (`observe(scope)`, `reload()`), because the pipeline is synchronous on the main thread while
  DataStore is not. It exposes `forApp` (`resolvedFor`), `priorityFor`, `format`, `speech`,
  `queueSettings`, `queueOverrides` and `isPriorityApp`; `setPriorityApp` now **persists** the
  Priority Announce list through `repository.update` instead of an in-memory set. Until the first
  emission the snapshot is `HearItSettings()`'s default — **all apps disabled** — so a slow or
  failed read loses an announcement rather than inventing one.
- **`app/.../service/AnnouncementService.kt`.** Injects `SettingsSource` from Hilt instead of
  constructing it, owns a `serviceScope` (`SupervisorJob` + main dispatcher) cancelled in
  `onDestroy` to collect the settings flow only while the service lives, promotes the queue to a
  field, and reconfigures it (`queue.updateSettings`) exactly once per settings emission via identity
  comparison. Snapshots now carry the per-app `priorityFor`, the queue now uses the user's grouping
  window / size / delay / gap settings plus per-app grouping overrides, and speech uses the stored
  speed/pitch/voice.
- **`app/.../service/AnnouncementPipeline.kt`.** `tick` takes the announcement format per call
  (`announcementFormat`, defaulting to the constructor value so existing tests are unchanged), so a
  format edit applies to the next announcement without recreating the pipeline.
- **`app/.../di/SetupModule.kt`.** Singleton provider for `SettingsSource` over the existing
  `SettingsRepository` provider.
- **`app/src/test/java/com/jayanth/hearit/service/SettingsSourceTest.kt` (new, 8 tests).** Defaults
  (every app disabled, master-prompt format/speech/queue), enabled-app resolution, global +
  per-app merge, Priority Announce list and per-app priority, stored format/speech, per-app queue
  override, flow mirroring via `observe`, and persistence of `setPriorityApp`.

### Commands actually run (exact)

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests "com.jayanth.hearit.service.*"` (first attempt) | **FAILED** — compile errors: the trailing lambda bound to the new last parameter in `tick`, and a wrong `AppEligibilityOverride` import in the new test |
| same command after fixes | **PASSED** |
| `./gradlew test assembleDebug lintDebug --console=plain` | **PASSED** — 387 tests, 0 failures, 0 skipped; `app-debug.apk` (34.5 MB); lint **0 errors, 57 warnings** |

Per-module totals: `:core:rules` 88, `:app` **70** (62 + 8 new), `:core:queue` 46, `:core:datastore` 32,
`:core:setup` 32, `:core:tts` 32, `:core:notifications` 30, `:core:common` 29, `:core:database` 28.

### Behaviour now reachable (unit-level)

An app the user enables in Applications is reported as `appEnabled = true` to the rules, its
per-app eligibility/priority overrides apply, its Priority Announce listing and priority are
honoured, the announcement uses the configured format and speech settings, and the queue uses the
configured grouping/size/delay/gap values. Combined with the L1 fix that added
`QueueEvent.Announced`, the pipeline is no longer structurally silent.

### Emulator verification

Not performed (no emulator started, no UI interaction).

### Physical-device verification

**Not tested.** Notification access, TTS, Bluetooth/wired routing, calls, DND, reboot/background
behaviour still require a physical device per `MANUAL_TEST_CHECKLIST.md`.

### Known limitations / risks (remaining wiring gaps)

- **History is still not written.** `QueueEvent`s and `EligibilityDecision`s produced by the
  pipeline are not yet recorded through `HistoryRepository`; the history UI therefore shows only
  what earlier phases recorded (nothing, end-to-end). This is the next honest wiring phase.
- **Call-held items are never released.** `AnnouncementPipeline.releaseHeld(state) { … }` exists and
  is unit-tested, but the service does not call it after a call ends, so `queueDuringCall` items
  stay held until they expire.
- **Audio focus policy is not applied by the player.** `EligibilitySettings.audioFocus` is resolved
  and stored but the service requests no focus yet (P08 scope item, unimplemented).
- **Audio-output disconnection** only stops speech; it does not re-check the output rule through
  `onStateChanged`'s `OUTPUT_LOST` reason.
- Physical output *selection* is still missing (P14 has no implementation), so the allowed-output
  rule can only see connected devices.
- Test-announcement from setup/dashboard continues to bypass the queue (by design); it uses the
  stored speech settings only.

### Next phase (proposed)

**L3 — record history from the pipeline** (done — see L3 below), then the missing **P14 Devices** and
**P18 History** screens.

---

## L3 — History recorded from the announcement pipeline

- **Date:** 2026-11-03
- **Commit:** `68e7364` — `feat(history): record pipeline outcomes in history storage (L3)`
- **Branch:** `development`
- **Scope:** Close the second functional gap named in L2 — the pipeline produced `QueueEvent`s and
  eligibility decisions that nothing persisted, so `HistoryRepository` (P10) was never written to and
  the dashboard's recent-history section could only ever be empty.
- **Working tree at start:** clean apart from the untracked IDE file, left untouched again.

### What changed

- **`app/.../service/AnnouncementPipeline.kt`.** New `HistoryRecord(snapshot, status, reason,
  priority)` value and an `onRecord` sink (default no-op), called inline from the pipeline's existing
  single-threaded loop. Recorded now:
  - every `EligibilityDecision.Skip` — `SKIPPED` with the rule's reason (ADR-007: collection is
    independent of eligibility);
  - every queue event — `QUEUED` on accept/hold, `Replaced`, `Deduplicated`, `Expired`,
    `OverflowDiscarded`, `Announced`;
  - the sweep events `pollReady()`/`markStarted()` return, which were previously discarded;
  - `onSpeechFinished(spoken)` now distinguishes completion from a stop/skip/failure: the queue
    reports any completion as `ANNOUNCED`, so a non-spoken finish is overridden to `INTERRUPTED`;
  - a group retired because every enabled section was empty is overridden from `ANNOUNCED` to
    `SKIPPED` — it was never spoken.
- **`app/.../service/AnnouncementService.kt`.** Injects `HistoryRepository`, hands pipeline records to
  an `UNLIMITED` `Channel` drained by **one** consumer coroutine (rows land in pipeline order, which
  update-handling depends on; the main thread never waits on Room), records with the app's own
  `historyFor(packageName)` (storage mode / update handling), and runs the retention sweep
  (`history.prune`) off the main thread whenever the settings snapshot changes. `finishSpeech` takes
  `spoken`, and the engine's `onError`, `ACTION_SKIP` and a rejected `speak` all pass `false`.
- **`app/.../di/DashboardModule.kt`.** Comment corrected: the database is now written, only the
  dedicated History screen is still pending.
- **`app/src/test/.../AnnouncementPipelineHistoryTest.kt` (new, 7 tests).** Skip reasons reach the
  sink, accept records `QUEUED`, completion records `ANNOUNCED`, a stopped utterance records
  `INTERRUPTED` (never `ANNOUNCED`), an empty-section group records `SKIPPED`, rules still decide the
  reason, and priority is carried through.

### Commands actually run (exact)

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests "com.jayanth.hearit.service.*"` | **PASSED** (first attempt; no fixes needed after the L3 edits) |
| `./gradlew test assembleDebug lintDebug --console=plain` | **PASSED** — 394 tests, 0 failures, 0 skipped; `app-debug.apk`; lint **0 errors, 57 warnings** |

Per-module totals: `:core:rules` 88, `:app` **77** (70 + 7 new), `:core:queue` 46, `:core:datastore` 32,
`:core:setup` 32, `:core:tts` 32, `:core:notifications` 30, `:core:common` 29, `:core:database` 28.

### Behaviour now reachable (unit-level)

With an app enabled and the service running, each notification now produces history rows with the
right status and reason: skipped items keep their rule reason, queued items are visible before
speech, completed announcements are `ANNOUNCED`, and stopped/skipped/failed speech is `INTERRUPTED`.
The dashboard's recent-history section reads these rows.

### Emulator verification

Not performed.

### Physical-device verification

**Not tested.**

### Known limitations / risks

- **Recording only happens while the service runs.** The listener forwards snapshots through
  `AnnouncementService`, so a stopped service records nothing — consistent with "nothing is collected
  when HearIt is not running", but worth stating.
- `HistoryStatus.PENDING` is still never emitted: the queue does not re-report items that merely keep
  waiting after their accept event.
- **Call-held items are still never released** (`releaseHeld` remains uncalled by the service), so
  `queueDuringCall` items are recorded `QUEUED`/`CALL_ACTIVE` and then expire.
- The retention sweep uses the global `HistorySettings`; per-app retention is not overridable by
  design (`HistorySettings.retentionDays` is global-only), so this is correct rather than a gap.
- **P18 History screen still does not exist** — only the dashboard's recent list and its search/clear
  actions read the new rows. **P14 Devices** still has no implementation.
- Audio-focus policy is still not applied by the player; output disconnection still only stops speech.

### Next phase (proposed)

**L4 — the dedicated History screen (P18)** (done — see L4 below), then **P14 Devices**.

---

## L4 — Dedicated History screen (P18)

- **Date:** 2026-11-03
- **Commit:** `4c3d688` — `feat(history): add the dedicated History screen with search and paging (L4)`
- **Branch:** `development`
- **Scope:** The History tab still showed the `DestinationPlaceholder`; only the dashboard's five-row
  summary could read the rows L3 started writing. This phase builds the real screen: search, status
  filter, sort, paged loading, per-entry delete, clear-all with confirmation, and the global history
  settings. Content/sender search needed a storage-layer addition, so `HistoryQuery` gained
  `searchText`.
- **Working tree at start:** clean apart from the untracked IDE file, left untouched again.

### What changed

**`:core:database` (search support)**

- `HistoryQuery.searchText` (validated: non-blank when set) documented as a case-insensitive
  `LIKE` over app name, sender and stored message text — so `METADATA_ONLY` rows cannot match on
  content, by design.
- `HistoryDao.page` gained the matching `AND (:searchText IS NULL OR (... LIKE ...))` clause;
  `RoomHistoryRepository` passes it through. No schema change, no migration.
- `HistoryPolicyTest.query_blankSearchText_isRejected` added.

**`:app` — new `history` package**

- `HistoryModels.kt` — `HistoryFilter`, `HistorySortOrder`, `HistoryItem`, `HistorySettingsState`
  and `HistoryUiState` (with `isFiltered` so empty-state copy can differ).
- `HistoryQueryMapper.kt` (pure) — filter → `HistoryQuery` (trimmed search, status, sort, page size
  50, offset) and `HistoryEntry` → `HistoryItem`. JVM-tested, so the screen and ViewModel never
  build a query or SQL by hand.
- `HistoryViewModel.kt` — Hilt VM: pages through `HistoryRepository` (never the whole table), resets
  and **cancels the in-flight read** on a filter change so results can never mix filters, appends
  pages via load-more, deletes one entry or all of them, and writes storage mode / update handling /
  retention through `SettingsRepository`. Failures surface as `unavailable`; cancellation is
  rethrown so a superseded read cannot mark history broken.
- `HistoryScreen.kt` — `HistoryRoute` (refreshes on `ON_RESUME` because the service keeps writing)
  and a stateless `HistoryScreen`: search field with an honest note about what can match, status and
  sort pickers as accessible single-choice dialogs, global history settings rows, a `LazyColumn` of
  rows (app · status, reason, sender, stored message text, relative time, update marker, delete),
  load-more / end-of-history footer, and confirmation dialogs for both delete paths. Every target is
  ≥48 dp, every string is a resource, and the per-row delete carries a content description naming
  the app.
- `HistoryLabels.kt` — exhaustive status and skip-reason label mapping (adding a reason to the domain
  cannot silently fall back to a generic label).
- Wiring: `DashboardDestination.HISTORY` now renders the feature instead of the placeholder;
  `DashboardRoute`/`MainActivity` take a `HistoryViewModel`.
- `DashboardViewModel` search now uses `searchText`, so the dashboard matches sender/content too
  instead of app name only (its stale “empty on a fresh install” comment was corrected).
- 52 new string resources (screen copy plus one label per skip reason).

**Tests**

- `HistoryQueryMapperTest` (new, 8 tests): blank search sends no filter, trimming, status/offset
  pass-through, page size, all three sort orders, full field mapping, metadata-only rows keep no
  content, order preserved.

### Commands actually run (exact)

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests "com.jayanth.hearit.history.*" :core:database:test` | **PASSED** (Hilt graph compiles with the new ViewModel) |
| `./gradlew test assembleDebug lintDebug --console=plain` | **PASSED** — 403 tests, 0 failures, 0 skipped; `app-debug.apk`; lint **0 errors, 58 warnings** |

Per-module totals: `:core:rules` 88, `:app` **85** (77 + 8), `:core:queue` 46, `:core:datastore` 32,
`:core:setup` 32, `:core:tts` 32, `:core:notifications` 30, `:core:common` 29,
`:core:database` **29** (28 + 1).

### Emulator verification

**Not performed.** The History screen has never been rendered or interacted with; TalkBack order,
scroll behaviour for long lists and the dialogs are compile-verified only.

### Physical-device verification

**Not tested.**

### Known limitations / risks

- **Search matches only what was stored.** With `METADATA_ONLY` (global or per app) the body was
  never written, so content search cannot find those rows; `LIKE` is case-insensitive for ASCII only
  (SQLite behaviour), and there is no FTS index yet.
- **No per-app filter in the UI.** The repository supports `packageName`/`sender`/date-range filters
  and `HistoryQuery` carries them, but this screen exposes status + free text + sort; a per-app and
  date-range picker remains future work.
- **Storage-mode changes do not rewrite existing rows** — only new notifications are affected.
  Retention is global-only by design (`HistorySettings.retentionDays`), and the sweep runs from the
  service, so a device that never runs the service never prunes.
- No biometric lock / secure-screen handling for history (P19 scope), and the dashboard still keeps
  its own five-row summary with app-outcome-time only.
- **P14 Devices** still has no implementation; audio focus and call-held release remain unwired.

### Next phase (proposed)

**L5 — the Devices feature (P14)** (done — see L5 below), then call-held release + audio focus.

---

## L5 — Devices feature (P14)

- **Date:** 2026-11-03
- **Commit:** recorded in the follow-up `docs(progress)` commit
- **Branch:** `development`
- **Scope:** The Devices tab was the last `DestinationPlaceholder`. The model and rules already had
  `AudioOutputSettings.allowedBluetoothIds` (persisted by the codec since P09), but there was no UI,
  no remembered-device list, and no way for a user to pick which Bluetooth output is allowed.
- **Working tree at start:** clean apart from the untracked IDE file, left untouched again.

### What changed

**`:core:common` / `:core:datastore` (persistence)**

- New `KnownAudioDevice(id, label, isBluetooth)` — platform id + display name only, never content.
- `HearItSettings.knownDevices` (newest first).
- `SettingsCodec` persists it under `devices.known` as `id\u001Elabel\u001EisBluetooth` entries: both
  separators are stripped from values on encode, malformed or duplicate entries are skipped on
  decode, and no other key changes meaning, so `SCHEMA_VERSION` stays 1 with no migration.

**`:app` — new `devices` package**

- `DeviceSelectionEditor.kt` (pure): merge connected outputs into the remembered list (connected
  first, fresh labels, outputs without an id skipped, capped at 12), allow/disallow one Bluetooth
  device, switch output categories, forget a device. The allow-list normalises to `null` (“any
  device”) whenever every known device is allowed again, so a headset connected tomorrow is never
  silently excluded.
- `DevicesViewModel.kt`: probes connected outputs on init and on resume, persists newly seen devices
  only when the merged list actually changed, and writes every allow/forget decision through
  `SettingsRepository` — the same store the eligibility rules read.
- `DevicesScreen.kt`: allowed-output-type switches (Bluetooth, wired, phone speaker, unrecognised),
  a warning when Bluetooth is on but every device is unchecked, “Connected now” rows with as many
  enabled Switch controls as the platform can identify, the remembered list with connected state and
  a Forget action, a rescan button, and copy stating that Android owns routing while HearIt only
  decides whether to speak.
- Wiring: `DashboardDestination.DEVICES` renders the feature; `DashboardRoute`/`MainActivity` take a
  `DevicesViewModel`.
- 22 new string resources; the five stale `devices_*` strings (including the “nothing routes through
  this list until P14 finishes” notice) were removed.

**Tests**

- `DeviceSelectionEditorTest` (new, 13 tests): merge order/labels/no-id skip/no duplicates/cap,
  `null` semantics, materialising the allow-list, collapsing back to “any”, empty set when all
  devices are unchecked, forgetting, and category isolation.
- `SettingsDeviceCodecTest` (new, 7 tests): remembered devices and the allow-list round-trip, empty
  set vs `null`, malformed entries ignored, separators sanitised, duplicates dropped.

### Commands actually run (exact)

| Command | Result |
|---|---|
| `./gradlew test assembleDebug lintDebug --console=plain` | **PASSED** — 423 tests, 0 failures, 0 skipped; `app-debug.apk`; lint **0 errors, 52 warnings** |

Per-module totals: `:core:rules` 88, `:app` **98** (85 + 13), `:core:queue` 46, `:core:datastore`
**39** (32 + 7), `:core:setup` 32, `:core:tts` 32, `:core:notifications` 30, `:core:common` 29,
`:core:database` 29.

### Emulator verification

**Not performed.** The Devices screen has never been rendered; no Bluetooth device was connected or
removed during testing, so the resume-triggered rescan is compile-verified only.

### Physical-device verification

**Not tested.** Bluetooth discovery, `BLUETOOTH_CONNECT` on API 31+, device names and
connect/disconnect handling are device-only behaviours.

### Known limitations / risks

- **Routing stays Android's.** HearIt never forces audio to a device; it only decides whether to speak
  on the active output. A Bluetooth device the user has not permitted can still be the active route —
  announcements are then skipped with `OUTPUT_NOT_ALLOWED`, which is the documented rule.
- **Per-device selection is Bluetooth-only.** Wired and speaker outputs are category settings (that
  is what the rule implements); their rows are informative and have no switch.
- **Devices without a platform id cannot be allow-listed** (no id to match), so their rows show the
  category decision instead of a switch.
- The remembered list is capped at 12 and forgotten devices reappear if connected again; there is no
  manual “add device” path because the platform reports what is actually present.
- This screen does not request `BLUETOOTH_CONNECT`; setup owns that request, so device *names* may be
  missing until it is granted.
- **Still open:** call-held release after a call, audio-focus application, audio-focus’s own UI, and
  the P19 Backup & Restore / Diagnostics screens.

### Next phase (proposed)

**L6 — close the two remaining behavioural gaps in the service**: release call-held items after a call
ends (`AnnouncementPipeline.releaseHeld` is implemented and tested but never called) and apply the
per-app audio-focus policy in the player, which is stored and resolved but never requested.

