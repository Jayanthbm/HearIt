# Cloud Development Progress

Tracks cloud implementation, code review and theoretical validation. Cloud work is not locally verified.

**Global build status: Not run (cloud-agent restriction).**
**Global test execution status: Not run (cloud-agent restriction).**
**All entries below: Not locally verified.**

---

## P00 — Documentation foundation

- **Date:** 2026-10-31
- **Scope:** Replace every placeholder document under `Docs/` with content derived from `AI_BUILD_MASTER_PROMPT.md`.
- **Implementation summary / files changed:**
  - `Docs/AI_BUILD_PHASES.md` — full phase plan P00–P20 with owners, dependencies, deliverables and acceptance criteria.
  - `Docs/AI_BUILD_ARCHITECTURE.md` — module layout, data flow, services, threading, DI, storage, privacy, compatibility, testing architecture.
  - `Docs/GLOBAL_APP_STANDARDS.md` — Kotlin/architecture/Compose/a11y/logging/security/testing/build standards.
  - `Docs/TECHNICAL_DECISIONS.md` — ADR-001…ADR-014, including the proposed rule precedence order (ADR-006).
  - `Docs/MANUAL_TEST_CHECKLIST.md` — device matrix + 11 checklist sections covering emulator and physical-device testing.
  - `Docs/CHANGELOG.md` — Keep-a-Changelog structure with `1.0.0` target and `Unreleased` section.
  - `Docs/AI_BUILD_PROGRESS_CLOUD.md` — this file.
- **Tests added:** none (documentation only).
- **Static review performed:** every requirement in the master prompt was cross-checked into the phase plan, an ADR or the manual checklist; placeholder-only documents eliminated; no product behaviour invented — precedence beyond the master prompt's explicit statements is recorded as *Proposed* in ADR-006 for confirmation.
- **API/compatibility considerations:** none in this phase (no code).
- **Known risks / unresolved questions:**
  - ADR-006 (rule precedence order) fixes user-visible skip reasons and should be confirmed by the user before P17 (Priority Announce) lands.
  - Module list in the architecture doc is the target state; modules are created incrementally per ADR-002.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `b9d4b9a` — `docs: complete foundation documentation set (P00)`
- **Next phase:** P01 — `:core:common` + `:core:rules` module foundation and eligibility rules (part 1).

---

## P01 — Core domain: module foundation and eligibility rules (part 1)

- **Date:** 2026-10-31
- **Scope:** Create the first two business-logic modules as pure Kotlin/JVM libraries (`:core:common`, `:core:rules`), implement the eligibility pipeline skeleton with the rule families that already exist in the master prompt (enabled-app list, temporary mute, screen/lock incl. Priority Announce override), and wire everything into the Gradle build.
- **Implementation summary / files changed:**
  - **Gradle wiring:** `settings.gradle.kts` now includes `:core:common`, `:core:rules`; `gradle/libs.versions.toml` adds the `kotlin-jvm` plugin alias (Kotlin `2.2.10`); root `build.gradle.kts` applies it `apply false`; `app/build.gradle.kts` adds `implementation(project(":core:common"))` and `implementation(project(":core:rules"))`.
  - **`core/common/build.gradle.kts`, `core/rules/build.gradle.kts`, `core/*/.gitignore`:** pure Kotlin/JVM modules, `jvmTarget = JVM_11` (matches the app's Java 11 compatibility), `testImplementation(libs.junit)`; `:core:rules` depends on `:core:common`.
  - **`core/common/.../model/NotificationSnapshot.kt`** — immutable, Android-free snapshot (`key`, `packageName`, `appName`, `sender?`, `messageText`, `channelId?`, `category?`, `postTimeMillis`, `isUpdate`) with `require` validation of identity fields; message text is stored whole, never truncated.
  - **`core/common/.../model/EligibilitySettings.kt`** — `ScreenCondition` (`ON`/`OFF`/`BOTH`, default `OFF`) and `LockCondition` (`LOCKED`/`UNLOCKED`/`BOTH`, default `LOCKED`) as two independent conditions, plus `AppEligibilityOverride` (null = inherit) and `EligibilitySettings.resolveWith(override)` which merges each condition independently.
  - **`core/common/.../model/ResolvedSettings.kt`** — `appEnabled` (default **false**, master prompt §5), `priorityAnnounceListed`, merged `eligibility`.
  - **`core/common/.../model/RuntimeState.kt`** — `screenOn`, `isLocked`, `temporaryMuteActive`, `priorityAnnounceActive`, all required with no defaults.
  - **`core/common/.../model/SkipReason.kt`** — `APP_DISABLED`, `MUTED`, `SCREEN_ON`, `SCREEN_OFF`, `UNLOCKED`, `LOCKED`; stable identifiers, UI wording stays in string resources. Additional reasons are added by P02/P03.
  - **`core/rules/.../EligibilityDecision.kt`** — `sealed interface` with `Announce` and `Skip(reason)` (the `Queue(reason)` variant lands with P05, per the ADR-004 implementation note).
  - **`core/rules/.../EligibilityRules.kt`** — `evaluate(snapshot, settings, state)` as a pure function: (1) app disabled, (2) temporary mute, (9) screen then lock unless `priorityAnnounceActive && priorityAnnounceListed`; first failure wins; comments mark where ADR-006 steps 3-8 and 10 get inserted.
- **Tests added (written, NOT executed):**
  - `EligibilityRulesTest` — `evaluate_defaultConditions_screenOffAndLocked_announces`, `evaluate_screenOn_skipsWithScreenOnReason`, `evaluate_unlocked_skipsWithUnlockedReason`, `evaluate_screenOnAndUnlocked_reportsScreenOnBeforeUnlocked`, `evaluate_screenConditionBoth_announcesWhileScreenIsOn`, `evaluate_lockConditionBoth_announcesWhileUnlocked`, `evaluate_screenConditionOnButScreenOff_skipsWithScreenOffReason`, `evaluate_lockConditionUnlockedButDeviceLocked_skipsWithLockedReason`, `evaluate_customConditions_screenOnAndUnlocked_announces`, `evaluate_appDisabled_skipsEvenWhenStateAllows`, `evaluate_appDisabledAndMuted_reportsAppDisabledFirst`, `evaluate_muted_skipsEvenWhenStateAllows`, `evaluate_mutedWhileScreenOn_reportsMutedBeforeScreen`, `evaluate_priorityModeActiveAndListed_announcesWhileScreenOnAndUnlocked`, `evaluate_priorityModeActiveButAppNotListed_stillSkipsWithScreenOnReason`, `evaluate_appListedButPriorityModeInactive_stillSkipsWithScreenOnReason`, `evaluate_priorityModeActiveAndListedButMuted_stillSkipsWithMutedReason`, `evaluate_priorityModeActiveAndListedButAppDisabled_stillSkipsWithAppDisabledReason`.
  - `EligibilitySettingsTest` — `resolveWith_nullOverride_inheritsAllGlobalValues`, `resolveWith_noFieldsSet_inheritsEveryValue`, `resolveWith_onlyScreenOverridden_keepsGlobalLockCondition`, `resolveWith_onlyLockOverridden_keepsGlobalScreenCondition`, `resolveWith_bothFieldsOverridden_replacesBothValues`, `defaults_matchMasterPrompt_screenOffAndLocked`.
  - `ResolvedSettingsAndSnapshotTest` — `resolvedSettings_allAppsDisabledByDefault`, `snapshot_defaults_leaveOptionalContentEmpty`, `snapshot_blankKey_isRejected`, `snapshot_blankPackageName_isRejected`, `snapshot_negativePostTime_isRejected`.
- **Static review performed:**
  - *Kotlin correctness:* reviewed by inspection; one compile error found and fixed during self-review (a test called `inheritsEveryValue` — a property of `AppEligibilityOverride` — on an `EligibilitySettings` result).
  - *Precedence:* implementation follows ADR-006 exactly for the three implemented families; Priority Announce bypasses screen/lock only; mute and app-disabled still win in Priority mode (master prompt §10/§11).
  - *Module/Gradle consistency:* version-catalog alias, root `apply false`, module includes and `:app` project dependencies all added together; `RepositoriesMode.FAIL_ON_PROJECT_REPOS` respected (no repositories in module files); `jvmTarget 11` matches `sourceCompatibility 11`.
  - *Testability:* pure JVM tests (JUnit 4 from the existing catalog entry), no Android types in either module, fixed inputs and no clock.
- **API/compatibility considerations:** no Android APIs used; `NotificationSnapshot` is deliberately a pure model so the P07 listener layer does the (cheap, main-thread) `Notification` → snapshot extraction outside the rules. `NotificationSnapshot.isUpdate` and `key` stability are defined here for the P05 dedup and P06 history update logic.
- **Known risks / unresolved questions:**
  - ADR-006 is still **Proposed** — skip-reason wording becomes user-visible in history and needs user/local confirmation before P17.
  - `SkipReason` contains only the six reasons implemented so far; P02/P03 extend the enum.
  - The build has never been compiled by the cloud agent; AGP 9.3.3 + Kotlin 2.2.10 Kotlin/JVM module wiring is unverified until the local agent runs Gradle.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** (recorded below after commit)
- **Next phase:** P02 — eligibility rules part 2 (text length, keywords, privacy) once P01 compiles locally; alternatively P06 (build foundation) can proceed in parallel.

---

## Phase log

| Phase | Status | Commit |
|---|---|---|
| P00 Documentation foundation | Cloud complete, not locally verified | `b9d4b9a` |
| P01 Core domain foundation | Cloud complete, not locally verified | (recorded after commit) |
