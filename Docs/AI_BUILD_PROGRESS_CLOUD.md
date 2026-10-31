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
- **Commit:** `6915b28` — `feat(rules): add core domain modules and eligibility pipeline (P01)`
- **Next phase:** P02 — eligibility rules part 2 (text length, keywords, privacy) once P01 compiles locally; alternatively P06 (build foundation) can proceed in parallel.

---

## P02 — Eligibility rules (part 2): text length, keywords and privacy

- **Date:** 2026-10-31
- **Scope:** Implement ADR-006 steps 5, 7 and 8 (refined numbering, see the amendment below) in `:core:rules` plus their settings groups in `:core:common`, with the full test coverage the phase plan requires.
- **Implementation summary / files changed:**
  - **`core/common/.../model/TextLengthSettings.kt`** (new) — `maxLengthChars: Int?` (null/≤0 = no limit) and `LengthScope { MESSAGE, WHOLE_NOTIFICATION }`, default scope `MESSAGE` (master prompt recommends per message for messaging apps).
  - **`core/common/.../model/KeywordFilterSettings.kt`** (new) — `enabled` (**false** by default, master prompt §6), `keywords`, `KeywordMatchMode { WHOLE_WORD, PHRASE }`.
  - **`core/common/.../model/PrivacySettings.kt`** (new) — `excludeApp`, `skipOtp`, `skipBanking`, `skipPassword`, `skipPayment`, `hideContent` (content flag for P04, **not** a skip rule), `sensitiveKeywords`; defaults per ADR-015 as refined: `skipOtp`/`skipPassword` **on**, banking/payment/exclude/hide **off**.
  - **`EligibilitySettings.kt`** (edited) — the three groups join `resolveWith` with group-level inheritance (ADR-016); `AppEligibilityOverride` gains the three nullable group fields and `inheritsEveryValue` covers them.
  - **`SkipReason.kt`** (edited) — adds `TOO_LONG`, `KEYWORD_FILTERED`, `PRIVACY_EXCLUDED_APP`, `PRIVACY_OTP`, `PRIVACY_PASSWORD`, `PRIVACY_PAYMENT`, `PRIVACY_BANKING`, `PRIVACY_SENSITIVE_KEYWORD` (14 reasons now).
  - **`core/rules/.../KeywordMatcher.kt`** (new) — `Locale.ROOT` case folding; whole-word mode uses an explicit `Char.isLetterOrDigit` boundary check instead of regex `\b`, so it behaves correctly for Devanagari keywords (the master prompt targets Hindi users).
  - **`core/rules/.../SensitiveContentDetector.kt`** (new) — privacy detection with documented ADR-006 step-5 sub-order: excluded → OTP → password → payment → banking → custom keyword. OTP heuristic = OTP-ish term present OR the whole message is a 4-8 digit code. Built-in term lists are short and conservative and only apply when the matching toggle is on (OTP/password on by default per ADR-015).
  - **`core/rules/.../EligibilityRules.kt`** (edited) — inserts steps 5 (privacy), 7 (keyword) and 8 (length) between mute and screen/lock exactly as the refined ADR-006 orders them (keyword before length so the user's own rule is the reported reason); `evaluateTextLength` skips the whole item (sender+body for `WHOLE_NOTIFICATION`, body only for `MESSAGE`; app name excluded as metadata). `hideContent` returns `Announce` by design.
- **Tests added (written, NOT executed):**
  - `TextLengthEligibilityTest` (10): `evaluate_noLimitConfigured_announcesLongMessage`, `evaluate_nonPositiveLimitTreatedAsUnlimited_announces`, `evaluate_messageOverLimit_skipsWithTooLongReason`, `evaluate_messageExactlyAtLimit_announces`, `evaluate_wholeNotificationScope_countsSenderAndBodyTogether`, `evaluate_messageScope_ignoresSenderLength`, `evaluate_tooLongItem_isSkippedWithoutTruncatingTheSnapshot`, `evaluate_privacyAndTooLong_bothApply_reportsPrivacyFirst`, `evaluate_keywordFilterAndTooLong_bothApply_reportsKeywordFilteredFirst` (+ scope isolation case).
  - `KeywordFilterEligibilityTest` (11): `evaluate_filterDisabled_keywordPresent_announces`, `evaluate_filterEnabled_noKeywords_announces`, `evaluate_filterEnabledMatchingKeywordIsCaseInsensitive_skips`, `evaluate_wholeWordMode_substringInsideWordDoesNotMatch_announces`, `evaluate_wholeWordMode_delimitedWordMatches_skips`, `evaluate_phraseMode_substringInsideWordMatches_skips`, `evaluate_multipleKeywords_secondOneMatches_skips`, `evaluate_blankKeywordEntries_areIgnored_announces`, `evaluate_devanagariKeywordWholeWord_matchesBoundaries`, `evaluate_mutedAndKeywordMatch_reportsMutedFirst`, `evaluate_sensitiveKeywordAndFilterMatch_reportsPrivacyFirst`.
  - `PrivacyEligibilityTest` (17): `evaluate_allPrivacyTogglesOff_financialTextStillAnnounces`, `evaluate_defaults_skipOtpAndPasswordOn_butRoutinePaymentTextStillAnnounces`, `evaluate_defaults_skipOtpPasswordContent_outOfTheBox`, `evaluate_excludeApp_skipsEvenWhenEnabled`, `evaluate_otpTermPresent_skipsWithPrivacyOtp`, `evaluate_messageIsOnlyNumericCode_skipsWithPrivacyOtp`, `evaluate_otpDetectionIsCaseInsensitive_skips`, `evaluate_passwordTermPresent_skipsWithPrivacyPassword`, `evaluate_paymentTermPresent_skipsWithPrivacyPayment`, `evaluate_bankingTermPresent_skipsWithPrivacyBanking`, `evaluate_customSensitiveKeywordMatches_skipsWithPrivacySensitiveKeyword`, `evaluate_otpAndPasswordBothPresent_reportsOtpFirst`, `evaluate_paymentAndBankingBothPresent_reportsPaymentFirst`, `evaluate_priorityAnnounceActiveAndListed_cannotBypassPrivacy`, `evaluate_mutedAndPrivacyMatch_reportsMutedFirst`, `evaluate_appDisabledAndPrivacyMatch_reportsAppDisabledFirst`, `evaluate_hideContentOnly_isNotASkipRule_announces` (15 cases).
  - `EligibilitySettingsTest` (extended, 5 new): `defaults_matchMasterPrompt_keywordsDisabledAndNoLengthLimit`, `resolveWith_nullGroups_inheritAllGlobalGroups`, `resolveWith_textLengthOverride_replacesOnlyThatGroup`, `resolveWith_privacyOverride_replacesOnlyThatGroup`, `resolveWith_groupOverride_marksAppAsNotInheritingEveryValue`.
- **Static review performed:**
  - *Kotlin correctness:* reviewed by inspection — precedence order matches ADR-006, group merge is null-safe, matcher returns false for blank keywords, no mutable shared state anywhere (`KeywordMatcher`/`SensitiveContentDetector` are stateless objects with `val` lists).
  - *Master-prompt conformance:* skip-never-truncate proven by construction (the pipeline only ever returns a decision; text is never rewritten); keyword filtering off by default; Priority Announce bypasses only screen/lock (test coverage for privacy vs Priority mode); DND vs silent-channel distinction deliberately **not** modelled here — it belongs to P03 where the notification policy can be inspected.
  - *Android/API considerations:* pure Kotlin + `java.util.Locale`, no Android APIs; `Char.isLetterOrDigit` is script-aware, so the Devanagari case does not rely on ASCII regex boundaries.
  - *Performance:* all matching is linear over the notification text and runs only on notification callbacks; acceptable for typical notification sizes.
- **Known risks / unresolved questions (need user/local input):**
  - ~~ADR-015 defaults~~ **resolved** — revised and accepted below (OTP/password on, financial rules off, no length limit).
  - ~~ADR-006 ordering~~ **resolved** — refined and accepted below (device states before content rules; keyword before length).
  - Builtin term lists are English-only and deliberately short; false negatives are expected and covered by custom sensitive keywords. Detection is documented as imperfect.
  - `hideContent` needs P04 before it has any effect; until then it is inert.
  - **Verification still pending:** build and tests must be run by the local agent; when they pass, update Build status / Test execution status / Local verification here and in `AI_BUILD_PROGRESS_LOCAL.md`.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `bc29b2a` — `feat(rules): add text length, keyword and privacy rules (P02)`
- **Next phase:** P03 — eligibility rules part 3 (audio output, DND, active call), or P06 (build foundation) if the local agent prefers to compile P01+P02 first.

---

## Amendment — ADR-006 / ADR-015 refined on user direction (2026-10-31)

- **Trigger:** user instruction to replace both *Proposed* ADRs with better options, and to update the docs once built.
- **ADR-006 (now Accepted):** precedence regrouped by rule kind — app eligibility (1) → transient device states mute/call/DND (2-4) → content rules privacy/keyword/length (5-8) → environment screen/lock with the Priority bypass (9) → output routing (10). Two behaviour-visible refinements: DND moved **ahead of** content rules so a one-off occurrence reports the transient cause, and keyword moved **ahead of** length so the user's own rule is the reported reason. Sub-order inside privacy unchanged: excluded → OTP → password → payment → banking → keyword. Alternatives are documented in `Docs/TECHNICAL_DECISIONS.md`.
- **ADR-015 (now Accepted):** v1 defaults = `skipOtp` **on**, `skipPassword` **on** (high-precision, worst failure mode is reading a code aloud), `skipBanking`/`skipPayment` **off** (low-precision, would hide alerts users want), `excludeApp`/`hideContent` **off**, `sensitiveKeywords` empty, `maxLengthChars` **null** (with skip-never-truncate a numeric default would silently hide long notifications; UI recommends ~500 chars), scope `MESSAGE`, keyword filtering off as mandated.
- **Code/test edits applied with the ADRs:** `PrivacySettings` defaults; keyword block moved before the length block in `EligibilityRules`; step-number KDoc updated across `EligibilityRules`, `SensitiveContentDetector`, `PrivacySettings`, `TextLengthSettings`, `KeywordMatcher` and the test files; `evaluate_keywordFilterAndTooLong_bothApply_reportsTooLongFirst` renamed to `..._reportsKeywordFilteredFirst` with the matching expectation; `PrivacyEligibilityTest` gained two default-behaviour tests (17 total) and its "all toggles off" case now switches the toggles off explicitly; `EligibilitySettingsTest` asserts the ADR-015 defaults directly.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified — the local agent records results here after the build.
- **Commit:** `7bb6fff` — `docs(rules): refine ADR-006 precedence and ADR-015 defaults (P02 amend)`

---

## Phase log

| Phase | Status | Commit |
|---|---|---|
| P00 Documentation foundation | Cloud complete, not locally verified | `b9d4b9a` |
| P01 Core domain foundation | Cloud complete, not locally verified | `6915b28` |
| P02 Eligibility rules (part 2) | Cloud complete, not locally verified | `bc29b2a` |
