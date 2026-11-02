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

## Phase P03 — Eligibility rules (part 3): audio output, DND and calls (2026-10-31)

- **Files changed:**
  - **`core/common/.../model/AudioOutput.kt`** (new) — `AudioOutputKind` enum (`WIRED`, `BLUETOOTH`, `PHONE_SPEAKER`, `OTHER`) and `AudioOutput(kind, id, name)` with no Android types; `id` null fails an explicit allow-list, `name` never matches.
  - **`core/common/.../model/AudioOutputSettings.kt`** (new) — whole-group settings `allowWired=true`, `allowBluetooth=true`, `allowPhoneSpeaker=false` (master-mandated), `allowedBluetoothIds=null`, `allowUnidentifiedOutput=true` (ADR-017).
  - **`core/common/.../model/RuntimeState.kt`** — three required, no-default fields: `isCallActive`, `isDndActive`, `activeOutput: AudioOutput?` (null = unobserved → allowed).
  - **`core/common/.../model/NotificationSnapshot.kt`** — `isSilentChannel: Boolean = false` added so channel policy is modelled yet provably distinct from DND.
  - **`core/common/.../model/EligibilitySettings.kt`** — `respectDnd=true` and `queueDuringCall=false` (field-level inheritance like screen/lock) plus `audioOutput` as a whole group in `EligibilitySettings`, `AppEligibilityOverride` and `resolveWith`/`inheritsEveryValue`.
  - **`core/common/.../model/SkipReason.kt`** — `CALL_ACTIVE`, `DND`, `OUTPUT_NOT_ALLOWED` (17 total).
  - **`core/rules/.../EligibilityDecision.kt`** — new `Queue(reason)` variant with the P05 re-evaluation contract in its KDoc.
  - **`core/rules/.../EligibilityRules.kt`** — pipeline now runs steps 1, 2, 3, 4, 5, 7, 8, 9, 10; only step 6 (hide content, P04) remains. Call returns `Queue(CALL_ACTIVE)` when `queueDuringCall`, DND reads only `state.isDndActive`, output evaluated last via new `evaluateOutput` helper.
  - **Docs:** new **ADR-017** in `Docs/TECHNICAL_DECISIONS.md`; ADR-006 implementation note (all steps except 6 done; Queue landed in P03); ADR-004 note (Queue variant early, Clock/queue machinery still P05); phase status table and CHANGELOG.
- **Static review performed:**
  - *Kotlin correctness:* reviewed by inspection — precedence order matches ADR-006 exactly (call → DND → privacy → keyword → length → screen/lock → output); smart-cast on `output.id` is safe (data-class `val`); all existing `RuntimeState(...)` constructions across the four prior test files updated to the seven-field form; `activeOutput = null` needs no import; enum names (`ScreenCondition.OFF/BOTH`, `LockCondition.BOTH`) verified against definitions; constructor named-argument names verified against each model class.
  - *Master-prompt conformance:* never speak during a call; queueing off by default and never bypassing (tested vs app-disabled, mute and Priority mode); DND on by default with per-app lift; phone speaker off by default; Priority Announce bypasses screen/lock only (tested vs call, DND and output).
  - *Android/API considerations:* pure Kotlin; the interruption-filter → `isDndActive` and channel → `isSilentChannel` mappings are explicitly the platform layer's job (P07/P10).
- **Tests added (written, NOT executed) — 36 total:**
  - `CallAndDndEligibilityTest` (16) — `evaluate_callActive_skipsWithCallActiveReason`, `evaluate_callActive_queueDuringCallEnabled_queuesInsteadOfSkipping`, `evaluate_queueDuringCallIsOffByDefault_andCallInactive_announces`, `evaluate_callActive_appDisabled_reportsAppDisabledFirst`, `evaluate_callActive_muted_reportsMutedFirst`, `evaluate_callActive_priorityModeActiveStillReportsCallActive`, `evaluate_dndActive_respectedByDefault_skipsWithDndReason`, `evaluate_dndActive_perAppOverrideLiftsDnd_announces`, `evaluate_dndInactive_announces`, `evaluate_dndActive_muted_reportsMutedFirst`, `evaluate_dndActive_callActive_reportsCallActiveFirst`, `evaluate_dndActive_privacyExcluded_reportsDndBeforePrivacy`, `evaluate_dndActive_priorityModeActiveStillReportsDnd`, `evaluate_silentChannel_dndInactive_announces`, `evaluate_silentChannel_dndActive_reportsDeviceDndReason`, `evaluate_callEnds_queuedItemIsReEvaluatedThroughTheFullPipeline`.
  - `AudioOutputEligibilityTest` (17) — `evaluate_defaultSettings_wiredOutput_announces`, `evaluate_defaultSettings_bluetoothOutput_announces`, `evaluate_defaultSettings_phoneSpeaker_skipsWithOutputNotAllowed`, `evaluate_activeOutputNotObserved_announces`, `evaluate_phoneSpeakerAllowed_announces`, `evaluate_wiredNotAllowed_skips`, `evaluate_bluetoothNotAllowed_skips`, `evaluate_unidentifiedOutput_allowedByDefault_announces`, `evaluate_unidentifiedOutputNotAllowed_skips`, `evaluate_bluetoothAllowlist_matchingId_announces`, `evaluate_bluetoothAllowlist_otherId_skips`, `evaluate_bluetoothAllowlist_deviceWithoutId_skips`, `evaluate_priorityModeActive_phoneSpeakerStillSkips`, `evaluate_mutedAndNotAllowedOutput_reportsMutedFirst`, `evaluate_callActiveAndNotAllowedOutput_reportsCallActiveFirst`, `evaluate_dndActiveAndNotAllowedOutput_reportsDndFirst`, `evaluate_screenOnAndNotAllowedOutput_reportsScreenOnFirst`.
  - `EligibilitySettingsTest` (+3) — `defaults_matchMasterPrompt_respectDndOnAndCallQueueingOff`, `resolveWith_dndAndCallFlags_inheritIndependently`, `resolveWith_audioOutputOverride_replacesOnlyThatGroup`.
  - Existing tests updated for the new required `RuntimeState` fields: `EligibilityRulesTest`, `TextLengthEligibilityTest`, `KeywordFilterEligibilityTest`, `PrivacyEligibilityTest` (expectations unchanged — defaults are neutral).
- **Known risks / unresolved questions (need user/local input):**
  - Open question (flagged in ADR-017): should silent-channel notifications be announced, skipped, or left alone? Master prompt §6 only requires DND ≠ channel; no behaviour implemented either way.
  - Platform mapping of `isDndActive`/`activeOutput` arrives in P07/P10 — the rules stay inert until then.
  - The build has never been compiled by the cloud agent; seven-field `RuntimeState` and the new files are unverified until the local agent runs Gradle.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `5140f52` — `feat(rules): add call, DND and audio-output eligibility rules (P03)`
- **Next phase:** P04 — announcement format and TTS contract (needs TTS contract module), or P06 (build foundation) if the local agent prefers to compile P01–P03 first.

---

## Phase P04 — Announcement format and TTS contract (2026-11-01)

- **Files changed:**
  - **`settings.gradle.kts`** — `include(":core:tts")`; the new module sits beside `:core:common` and `:core:rules`.
  - **`core/tts/build.gradle.kts`** (new) — Kotlin/JVM module mirroring `:core:rules` (JVM 11, `:core:common` implementation dep, JUnit test dep); no Android plugin until the wrapper lands (ADR-002/ADR-018).
  - **`core/tts/.../AnnouncementFormat.kt`** (new) — §7's configurable knobs: `includeAppName`/`includeSender`/`includeMessage` (all on), `introduction` (null = none), `pauseBetweenSectionsMillis` (≥ 0, default 0).
  - **`core/tts/.../AnnouncementTemplate.kt`** (new) — `SectionKind` (`INTRODUCTION`, `APP_NAME`, `SENDER`, `MESSAGE`), `AnnouncementSection`, `BuiltAnnouncement(sections, pause)` with `isEmpty`, and the pure `AnnouncementTemplate.build(snapshot, format)`. Default output reproduces §7 exactly: "WhatsApp. Rahul says, Hey, are you coming today?" Missing/blank sender omits the section; sender without message speaks "Rahul." (no dangling `says,`); message verbatim, never truncated; all-empty → `isEmpty` so the speaker skips.
  - **`core/tts/.../TextToSpeechEngine.kt`** (new) — `SpeechSettings(speechRate=1.0, pitch=1.0, voiceId=null, all > 0 required)`, `TtsVoice(id, displayName, locale, isNetworkConnectionRequired)`, `TtsErrorCode` (`ENGINE_UNAVAILABLE`, `LANGUAGE_DATA_MISSING`, `SYNTHESIS_FAILED`, `INTERRUPTED`), `TtsError`, `UtteranceListener` (exactly one terminal callback), and the `TextToSpeechEngine` boundary: `isAvailable`, `listVoices`, `supportsLocale`, `speak(announcement, settings, listener): Boolean`, `stop`, `shutdown`. One announcement in progress; stop/replacing speak → `INTERRUPTED`; rejected speak never invokes the listener. No Android types; native TTS only, no translation (§7).
  - **Docs:** **ADR-018** added; `AI_BUILD_ARCHITECTURE.md` `:core:tts` row now reads Kotlin/JVM contract module (wrapper joins with the speech service); phase status table and CHANGELOG updated.
- **Static review performed:**
  - *Kotlin correctness:* reviewed by inspection — builder is a stateless `object` with only `String`/`List` operations; `snapshot.sender?.trim().orEmpty()` handles null safely; format invariants enforced in `init` blocks; `assertThrows(IllegalArgumentException::class.java)` matches the JUnit 4.13 usage already present in P01 tests; module build file copied from `:core:rules` conventions; `java.util.Locale` is JVM stdlib, no Android imports anywhere in the module.
  - *Master-prompt conformance:* default format = app + sender (if available) + message; missing sender/message handled gracefully by construction; speed/pitch/voice configurable with engine-fallback defaults; unavailable language data is an explicit `LANGUAGE_DATA_MISSING` error, never a silent fallback; template building is never a skip rule (eligibility untouched).
  - *Android/API considerations:* contract itself uses no Android APIs; when the wrapper lands (P08), `playSilentUtterance` is API 21+ (within the API 29 floor) and `setSpeechRate`/`setPitch` take the same multiplier semantics as `SpeechSettings`; utterance-id-based callbacks map 1:1 to `UtteranceListener`.
  - *Security/privacy:* `TtsError` carries only a stable code — never notification text; no logging was introduced; message content flows snapshot → template → engine contract only.
- **Tests added (written, NOT executed) — 25 total:**
  - `AnnouncementTemplateTest` (17) — `build_defaultFormat_producesMasterPromptExample`, `build_pauseBetweenSections_propagates`, `build_negativePause_isRejected`, `build_missingSender_omitsSenderSection`, `build_blankSender_treatedAsMissing`, `build_missingMessage_omitsMessageSection`, `build_senderWithoutMessage_isSpokenWithoutConnector`, `build_missingSenderAndMessage_announcesAppOnly`, `build_appNameDisabled_skipsAppSection`, `build_senderDisabled_skipsSenderSection`, `build_messageDisabled_usesPlainSenderWithoutConnector`, `build_introduction_isFirstSection`, `build_blankIntroduction_ignored`, `build_introductionOnly_whenAllSectionsDisabled_stillAnnounces`, `build_everythingDisabledWithoutIntroduction_isEmpty`, `build_disabledSectionsWithMissingContent_isEmpty`, `build_messageText_isSpokenVerbatim`.
  - `TextToSpeechContractTest` (8) — `speechSettings_defaults_matchMasterPrompt`, `speechSettings_zeroRate_isRejected`, `speechSettings_negativePitch_isRejected`, `speechSettings_customValues_arePreserved`, `builtAnnouncement_negativePause_isRejected`, `builtAnnouncement_noSections_isEmpty`, `builtAnnouncement_withSections_isNotEmpty`, `ttsVoice_networkRequirement_defaultsToFalse`.
- **Known risks / unresolved questions (need user/local input):**
  - The `TextToSpeechEngine` contract has no implementation yet (P08) — behaviour such as silent-utterance pauses, voice enumeration and locale detection is pinned only at the contract level.
  - `SectionKind` will likely grow a grouped-message count for §8 (P05 extends, does not break, the builder).
  - `:core:tts` must switch from Kotlin/JVM to an Android library when the wrapper joins; the settings/Gradle change is intentionally deferred to that phase.
  - The build has never been compiled by the cloud agent; the new module's Gradle wiring is unverified until the local agent runs Gradle.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `2b7d44b` — `feat(tts): add announcement template builder and TTS contract (P04)`
- **Next phase:** P05 — queue, grouping and scheduling (depends on P01+P04, both complete), or P06 (build foundation) in parallel.

---

## Phase P05 — Queue, grouping and scheduling (2026-11-01)

- **Files changed:**
  - **`settings.gradle.kts`** — `include(":core:queue")` (alphabetical: common, queue, rules, tts).
  - **`core/common/.../time/Clock.kt`** (new) — `fun interface Clock { fun nowMillis(): Long }`; injected time source so grouping windows, delay, min gap and expiry are deterministic; the Android service will back it with `SystemClock.elapsedRealtime()`, domain code never reads the system clock.
  - **`core/common/.../model/AppPriority.kt`** (new) — CRITICAL(3)/HIGH(2)/NORMAL(1)/LOW(0) with `rank` for queue ordering.
  - **`core/common/.../model/HistoryStatus.kt`** (new) — §12 vocabulary: `ANNOUNCED`, `SKIPPED`, `QUEUED`, `PENDING`, `INTERRUPTED` (queue emits QUEUED/SKIPPED; P08/P10 wire the rest).
  - **`core/common/.../model/QueueSettings.kt`** (new) — `QueueSettings` (validated init: window/delay/gap ≥ 0, maxMessages ≥ 1, expiry > 0, both limits ≥ 1; `resolveWith` returns `this` for a null override) + `AppQueueOverride(groupingWindowMillis: Long?)` with `inheritsEveryValue` + `OverflowPolicy` (DISCARD_OLDEST default, DISCARD_NEWEST, KEEP_HIGHEST_PRIORITY). Defaults: 5 s window, 0 delay, 3 messages, 0 gap, 30 min expiry, 50 global, 10 per-app, `repeatNotificationUpdates = false` (§8/§10 master-suggested where given).
  - **`core/common/.../model/SkipReason.kt`** — appended `EXPIRED`, `OVERFLOW`, `DUPLICATE_UPDATE` and a header note; no `when` anywhere matches exhaustively, so existing code is unaffected.
  - **`core/queue/build.gradle.kts`** (new) — exact mirror of `:core:tts` conventions (kotlin-jvm, JVM 11, `:core:common` implementation dep, JUnit test dep); no Android plugin (ADR-002).
  - **`core/queue/.../AnnouncementQueue.kt`** (new) — single-writer state machine: `updateSettings`, `enqueue` (sweep → dedup → replace-in-place → queue → enforce limits), `hold`, `releaseBlocked`, `pollReady` (peek-only; selection id stable while the group is unchanged; null while speaking or inside the min gap), `markStarted` (removes the group, records announced keys, rejects stale ids), `markCompleted` (starts the gap clock), `pendingCount`/`heldCount`. Private `sweepExpired` (runs before every operation, `>=` boundary, invalidates stale selections, prunes announced memory), `enforceLimits` (items not groups; per-app pass then global), `pickVictim` (oldest / newest / lowest-priority with ties evicted newest-first). Deliberately lock-free (architecture §3: one actor/scope); documented as not thread-safe by design.
  - **`core/queue/.../QueueEvent.kt`** (new) — sealed `QueueEvent` (snapshot, historyStatus, reason): `Queued(reason?)` and `Replaced` → QUEUED; `Deduplicated(DUPLICATE_UPDATE)`, `Expired(EXPIRED)`, `OverflowDiscarded(OVERFLOW)` → SKIPPED — history never loses a discarded/expired item (§8/§12).
  - **`core/tts/.../AnnouncementTemplate.kt`** — `SectionKind.MESSAGE_COUNT` added (verified no exhaustive `when` over `SectionKind` exists); `build(snapshot)` now delegates to the new `build(snapshots: List, format)` which requires non-empty, single-package lists and emits app name, "N messages." (only when > 1, always plural) then per-item sender/message in arrival order. No new format knob (§7's list unchanged).
  - **`core/rules/.../EligibilityDecision.kt`** — `Queue(reason)` KDoc now cross-references `AnnouncementQueue.hold`/`releaseBlocked` as the P05 implementation of the contract.
  - **Docs:** **ADR-019** added (defaults + scheduling semantics and alternatives); `AI_BUILD_ARCHITECTURE.md` step 4 extended (dedup of already-announced updates, call-blocked hold-and-release for full re-evaluation); phase status table (P05 complete, P06–P20 remaining); CHANGELOG P05 entry after P04.
- **Static review performed:**
  - *Kotlin correctness:* reviewed by inspection — constructor-parameter/property shadowing (`private var settings = settings`) is the idiomatic pattern; `require` contracts enable smart-cast of the local `lastSelection`; ordering uses `minWithOrNull(compareByDescending { rank }.thenBy { seq })` and eviction `minWith(compareBy { rank }.thenByDescending { seq })` (ties → newest, per ADR-019); `data class` equality makes `previous.second == group` a correct selection-stability check; `mutableList.removeAll(Collection)` and `entries.removeAll { }` are stdlib; only one `FakeClock` exists in the queue test package; all `NotificationSnapshot` named arguments verified against the real constructor.
  - *Test bugs found and fixed during review:* six ordering tests originally enqueued several same-package items inside one grouping window, which would have merged them into a single announcement (grouping at poll time) and failed their own assertions — rewritten to use distinct packages or arrivals outside the window; a never-interrupt assertion also pointed at the wrong item. The rewritten `AnnouncementQueueOrderingTest` is the version committed.
  - *Master-prompt conformance:* priority first with FIFO ties; same-app grouping with inclusive window, cap and leftovers waiting; delay/min-gap/never-interrupt scheduling; dedup only for announced keys with pending updates replaced in place; held items excluded from size limits but not from expiry; every removal reported with status + reason (§8/§10/§12).
  - *Scope/privacy:* no Android types, no logging, no eligibility-rule changes (hold/release only store and hand back); message text flows snapshot → queue → template only.
- **Tests added (written, NOT executed) — 59 total:**
  - `QueueSettingsTest` (10) — `defaults_matchSuggestedMasterPromptValues`, `defaults_neutralSchedulingValues`, `negativeGroupingWindow_isRejected`, `nonPositiveMaxMessagesPerAnnouncement_isRejected`, `nonPositiveExpiry_isRejected`, `nonPositiveSizeLimits_areRejected`, `resolveWith_nullOverride_inheritsEverything`, `resolveWith_windowOverride_appliesOnlyThatField`, `override_inheritsEveryValue_isTrueOnlyWhenWindowNull`, `override_negativeWindow_isRejected`.
  - `AnnouncementQueueOrderingTest` (13) — `enqueue_returnsQueuedEventWithQueuedStatusAndNoReason`, `poll_withEmptyQueue_returnsNoAnnouncement`, `poll_equalPriorityItems_comeOutInArrivalOrder`, `poll_higherPriority_jumpsAheadOfEarlierItems`, `poll_priorityOrder_isCriticalThenHighThenNormalThenLow`, `poll_beforeDelayElapses_returnsNoAnnouncement`, `poll_afterDelayElapses_returnsAnnouncement`, `poll_beforeMinGapElapsed_returnsNoAnnouncement`, `poll_afterMinGapElapsed_returnsNextAnnouncement`, `poll_whileSpeaking_returnsNoAnnouncement_neverInterrupts`, `markStarted_withStaleIdFromOlderPoll_isRejected`, `markStarted_twiceWithoutCompletion_isRejected`, `markCompleted_withNonSpeakingId_isRejected`.
  - `AnnouncementQueueGroupingTest` (9) — `poll_sameAppWithinWindow_groupsIntoOneAnnouncement`, `poll_groupItems_keepArrivalOrderNotAlphabetical`, `poll_maxMessagesPerAnnouncement_capsGroup_andLeftoversWaitForNext`, `poll_windowBoundary_isInclusive`, `poll_itemJustOutsideWindow_startsNextGroup`, `poll_differentApps_neverGroup`, `poll_companionNotYetReady_isExcludedUntilDelayElapses`, `poll_perAppGroupingWindowOverride_isApplied`, `poll_selectionTakesHighestPriorityApp_andOnlyItsCompanions`.
  - `AnnouncementQueueOverflowExpiryTest` (9) — `enqueue_overGlobalMaxSize_discardsOldest_withOverflowHistory`, `enqueue_overPerAppMaxSize_evictsOnlyThatAppsItems`, `enqueue_discardNewest_refusesTheIncomingItem`, `enqueue_keepHighestPriority_evictsTheLowestPriorityItem`, `enqueue_keepHighestPriority_tiesEvictTheNewestAmongEquals`, `enqueue_exactlyAtLimit_doesNotDiscard`, `enqueue_atExpiryBoundary_emitsExpiredEvent_withHistoryStatusAndReason`, `poll_afterExpiry_returnsNoAnnouncement_andReportsExpiry`, `updateSettings_shrinkingGlobalLimit_enforcesImmediately`.
  - `AnnouncementQueueDedupAndHoldTest` (11) — `enqueue_updateOfRecentlyAnnouncedKey_deduplicates`, `enqueue_updateOfAnnouncedKey_announcesAgainWhenConfigured`, `enqueue_updateWhilePending_replacesInPlace`, `enqueue_updateAfterDedupWindow_announcesAgain`, `enqueue_updateNeverAnnounced_isNotDeduplicated`, `markStarted_recordsAnnouncement_forLaterDedup`, `hold_emitsQueuedEvent_withCallReasonAndQueuedStatus`, `hold_isNotPollable_andDoesNotCountTowardSizeLimits`, `hold_updateOfSameHeldKey_replacesInPlace`, `releaseBlocked_returnsHeldItemsInHoldOrder_andClears`, `releaseBlocked_expiredHeldItems_areSweptNotReturned`.
  - `AnnouncementTemplateGroupTest` (7) — `build_threeMessages_announcesAppCountThenContentInOrder`, `build_singleSnapshotInList_producesExactlyTheSingleSections`, `build_groupWithMissingSenders_omitsSectionsCleanly`, `build_groupWithMessagesDisabled_announcesCountAndPlainSenders`, `build_groupWithBlankContent_stillAnnouncesAppAndCount`, `build_emptyList_isRejected`, `build_mixedPackages_areRejected`.
- **Known risks / unresolved questions (need user/local input):**
  - Every numeric default (5 s window, 3 messages, 0 delay/gap) is ADR-019 "subject to product confirmation" — user may tune them from settings later.
  - ADR-017 (silent-channel announcement behaviour) remains open — unchanged by this phase.
  - `HistoryStatus.PENDING`/`ANNOUNCED`/`INTERRUPTED` are defined but not yet emitted (P08/P10); `HeldItem.reason` uses the hold's `SkipReason`, re-verified on release only by the pipeline in P08.
  - The `:core:queue` Gradle wiring and every new file have never been compiled — unverified until the local agent runs Gradle.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `6364676` — `feat(queue): add queue grouping scheduling and dedup (P05)`
- **Next phase:** P06 — build foundation: Hilt, Application and manifest baseline.

---

## Phase P06 — Build foundation: Hilt, Application and manifest baseline (2026-11-02)

- **Files changed:**
  - **`gradle/libs.versions.toml`** — versions `ksp = "2.3.13"`, `hilt = "2.60.1"`; libraries `hilt-android` / `hilt-compiler` (`com.google.dagger`, version.ref `hilt`); plugins `ksp` (`com.google.devtools.ksp`) and `hilt` (`com.google.dagger.hilt.android`).
  - **`build.gradle.kts`** (root) — `alias(libs.plugins.ksp) apply false` and `alias(libs.plugins.hilt) apply false`, following the existing root convention.
  - **`app/build.gradle.kts`** — `alias(libs.plugins.ksp)` + `alias(libs.plugins.hilt)` in `plugins`; `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)` in `dependencies`. Deliberately NO `org.jetbrains.kotlin.android` (AGP 9 built-in Kotlin; applying the legacy plugin fails the build) and no kapt (incompatible with built-in Kotlin).
  - **`app/src/main/java/com/jayanth/hearit/HearItApplication.kt`** (new) — `@HiltAndroidApp class HearItApplication : Application()`, documented as the DI root, intentionally empty beyond that (no startup god-object).
  - **`app/src/main/java/com/jayanth/hearit/MainActivity.kt`** — `@AndroidEntryPoint` + import + one-line KDoc.
  - **`app/src/main/AndroidManifest.xml`** — `android:name=".HearItApplication"`; two install-time NORMAL permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`); notification-listener service slot (`com.jayanth.hearit.core.notifications.HearItNotificationListenerService`, `exported="true"`, signature-level `BIND_NOTIFICATION_LISTENER_SERVICE`, the standard listener intent-filter, `android:label="@string/app_name"`); announcement-service slot (`.service.AnnouncementService`, `exported="false"`, `foregroundServiceType="mediaPlayback"`); both slots `tools:ignore="Instantiatable"` with comments fixing the P07/P08 class-path contract; ADR cross-refs verified (ADR-010 FGS, ADR-002 module-with-first-code, ADR-003 listener).
  - **Docs:** ADR-020 appended (versions, wiring, slots, deferred dangerous permissions + alternatives); CHANGELOG P06 entry; phases status table (P06 complete, P07–P20 remaining).
- **Static review performed:**
  - Full diff reviewed line-by-line; two defects found and fixed before commit: a stale "ADR-008" reference in the announcement-slot comment (→ ADR-010) and an overclaim in the `HearItApplication` KDoc about Hilt receiver support (reworded to activities now, services when they arrive).
  - Version compatibility researched from primary sources (KSP releases + compatibility table, Maven Central metadata, Dagger releases + Hilt setup docs, Android's AGP 9 built-in-Kotlin migration doc): KSP 2.3.x is decoupled from the Kotlin version and supports Kotlin 2.2.10 / AGP 9 / Gradle 9 (the AGP 9 R-class fix landed in 2.3.10); the old `2.2.10-2.0.2` KSP1 line is removed/unsupported; Hilt 2.60.1 is latest stable, requires AGP 9 (min 9.0.0 since 2.59.1) and minSdk 23 (project: 29), with stable KSP support from 2.60/KSP 2.3.9.
  - Grep-verified: no `kapt`/`kotlin-android` anywhere in build files; no stray wrong ADR references remain in app or docs.
  - Manifest acceptance review (API 29–37): `exported` explicit on every intent-filter component (mandatory since targetSdk 31); listener exported+signature-permission is the mandatory system-only pattern; `foregroundServiceType` exists from API 29 (minSdk floor) and is enforced from targetSdk 34, backed by the declared normal permission; only NORMAL permissions declared — dangerous `POST_NOTIFICATIONS`/`BLUETOOTH_CONNECT` deferred until their setup flow exists (acceptance criterion).
  - Catalog accessor names checked against Gradle's dashed-key mapping (`hilt-android` → `libs.hilt.android`; plugin `hilt` → `libs.plugins.hilt`).
- **Tests added (written, NOT executed) — 0:** P06 adds build wiring, an application class, one annotation and manifest declarations — there is no JVM-testable logic to cover; manifest behaviour is exercised by the local agent's first build/install. Recorded honestly rather than adding a meaningless test.
- **Known risks / unresolved questions (need user/local input):**
  - Dependency resolution and Hilt/KSP code generation have never run — the first local build is the verification point (KSP needs a JDK 17+ Gradle daemon, which Gradle 9 already mandates).
  - The two service slots intentionally reference classes that do not exist yet; P07/P08 must land them at the exact documented paths or edit the manifest. `tools:ignore="Instantiatable"` suppresses only that deliberate gap.
  - `android:allowBackup="true"` (template default) may deserve a privacy review once P10 stores notification content locally — flagged, not changed (privacy decisions need the user).
  - ADR-017 open question unchanged; ADR-019 numeric defaults still await product confirmation.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `2abd6f4` — `feat(app): add Hilt foundation and manifest baseline (P06)`
- **Next phase:** P07 — notification listener foundation (`:core:notifications` Android library module; extraction into `NotificationSnapshot`, permission-revocation handling, no content in logs).

---

## Phase P07 — Notification listener foundation (2026-11-02)

- **Files changed:**
  - **`gradle/libs.versions.toml`** — `android-library` plugin alias (`com.android.library`, AGP 9.3.3); **`build.gradle.kts`** (root) — `apply false`; **`settings.gradle.kts`** — `:core:notifications` (alphabetical: common, notifications, queue, rules, tts).
  - **`core/notifications/build.gradle.kts`** (new) — Android library module: namespace `com.jayanth.hearit.core.notifications`, compileSdk 37 / minSdk 29, Java 11, `:core:common` implementation dependency, JUnit test dependency. No Kotlin Android plugin (AGP 9 built-in Kotlin), no Hilt yet (no injection target before the P08 pipeline).
  - **`RawNotification.kt`** (new) — plain-Kotlin carrier of platform values (package, label, tag, id, title/text/bigText/subText, channel, silent flag, category, post time) so extraction rules are JVM-testable.
  - **`NotificationSink.kt`** (new) — `onPosted(snapshot)` / `onRemoved(key)` contract for the P08 pipeline; the listener forwards everything regardless of eligibility (ADR-007).
  - **`NotificationKeyTracker.kt`** (new) — bounded (500 keys, least-recently-marked eviction) identity memory: first mark = new, later marks = update, forget on removal makes the next post new again.
  - **`NotificationExtractor.kt`** (new) — pure extraction: `package|tag|id` identity, app-name fallback to package name, sender suppressed when it merely repeats the app name (case-insensitive), bigText → text → empty with trim/blank-to-null, post-time fallback to the caller's clock, channel/category pass-through, `isUpdate` from the tracker; `keyOf` helpers exposed.
  - **`StatusBarNotificationMapper.kt`** (new) — the only Android-aware step: `EXTRA_*` extras → `RawNotification`; documented limitation: cross-package channel silence is unreadable on public API 29+, so `channelIsSilent` stays false from this source.
  - **`HearItNotificationListenerService.kt`** (new) — the P06 manifest slot now resolves to a real class: `onNotificationPosted`/`onNotificationRemoved`, own-package exclusion, app-label resolution with `NameNotFoundException` fallback, one guarded rebind 5 s after a disconnect (`SecurityException` swallowed because P11's setup flow reports revoked access), handler cleanup in `onDestroy`, no logging at all.
  - **`app/build.gradle.kts`** — `implementation(project(":core:notifications"))`; **`app/src/main/AndroidManifest.xml`** — listener slot comment updated and `tools:ignore="Instantiatable"` removed (the class exists now; only the P08 service slot keeps it).
  - **Docs:** changelog P07 entry; phases status table (P07 complete, P08–P20 remaining).
- **Static review performed:**
  - Full module reviewed line-by-line. Two defects found and fixed before commit: the private `trimmedOrNull` extension was declared inside the companion object (fragile resolution from the instance body) and was moved to file scope, with a formatting artifact from the move repaired; the tracker eviction test asserted a key was still remembered after the same call had already evicted it — assertions rewritten to the documented least-recently-marked order, and the test count re-verified by hand (30 = 22 extractor + 8 tracker).
  - Android API review: `NotificationListenerService` override nullability (platform parameters widened to nullable), `StatusBarNotification.notification`/`extras` platform-null guarded, `EXTRA_*` char sequences normalised to strings, `requestRebind` `SecurityException` the only caught exception and only for genuinely revoked access, no `Log` calls anywhere (content can never reach logcat), callbacks cheap on the main thread per architecture §5.
  - Acceptance review: missing title/text/extras covered by tests; updates distinguished from new posts (same key → update; removal → new again; tag/id variants tested); the listener contains no speech, rules, queue or history logic (single sink call, no dependency on `:core:rules`/`:core:queue`/`:core:tts`).
  - Scope/privacy: nothing logged, nothing persisted, own notifications excluded, no new permissions (notification access is granted through the system settings flow in P11).
- **Tests added (written, NOT executed) — 30 total:**
  - `NotificationExtractorTest` (22) — `extract_fullNotification_mapsEveryField`, `extract_missingTitle_senderIsNull`, `extract_blankTitle_senderIsNull`, `extract_missingText_messageTextIsEmpty`, `extract_missingEverything_producesValidSnapshot`, `extract_bigTextIsPreferredOverText`, `extract_blankBigText_fallsBackToText`, `extract_titleEqualToAppName_suppressesSender`, `extract_titleDifferingOnlyInCaseFromAppName_suppressesSender`, `extract_titleAndTextAreTrimmed`, `extract_blankAppLabel_fallsBackToPackageName`, `extract_missingAppLabel_fallsBackToPackageName`, `extract_missingPostTime_fallsBackToNow`, `extract_taglessKey_hasEmptyTagSegment`, `extract_silentChannelFlag_passesThrough`, `extract_secondPostOfSameKey_isUpdate`, `extract_thirdPostOfSameKey_isStillUpdate`, `extract_differentIds_areNotUpdates`, `extract_sameIdDifferentTag_isNotAnUpdate`, `extract_afterForget_isNewAgain`, `extract_tracksAtMostTheTrackerCeiling`, `keyOf_partsBuildTheStableIdentity`.
  - `NotificationKeyTrackerTest` (8) — `markSeen_firstTime_returnsFalse`, `markSeen_secondTime_returnsTrue`, `markSeen_thirdTime_returnsTrue`, `forget_makesTheNextPostNew`, `exceedingCapacity_evictsTheLeastRecentlyMarked`, `reMarking_refreshesRecency`, `clear_resetsEverything`, `nonPositiveCapacity_isRejected`.
- **Known risks / unresolved questions (need user/local input):**
  - First build of the new Android library is unverified: AGP 9 built-in Kotlin must pick up `src/main/kotlin` and `src/test/kotlin` in an Android library module, and the module must sit on `:app`'s runtime classpath for the manifest slot to resolve.
  - `channelIsSilent` cannot be read cross-package on public API 29+, so the listener always reports false (assumed audible); the snapshot field remains for sources that can provide it. ADR-017's silence rule is device-level and unaffected.
  - Emission goes to an optional in-process `NotificationSink` (null until P08); tracker memory is lost on process death, so an update after a listener restart counts as new (P10 history keyed by the same identity mitigates).
  - ADR-019 numeric defaults confirmed by the user on 2026-11-02; the ADR-017 silent-mode decision was added this session.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `2b2f335` — `feat(notifications): add notification listener foundation (P07)`
- **Next phase:** P08 — foreground announcement service and speech pipeline.

---

## Phase P08 — Foreground announcement service and speech pipeline (2026-11-02)

- **Files changed:**
  - **`app/build.gradle.kts`** — `:core:queue` and `:core:tts` added to `:app` (the pipeline uses rules, queue and template directly).
  - **`app/src/main/java/com/jayanth/hearit/service/AnnouncementPipeline.kt`** (new, pure) — eligibility routing per snapshot (Announce → enqueue, Queue(reason) → hold, Skip → decision only), `tick` (poll → build → mark started; speakable-empty groups retired silently with a 32-step guard), `onSpeechFinished` (mark completed → min gap), `onStateChanged` (re-evaluates the speaking group; returns `StopSpeech(ELIGIBILITY_LOST | DEVICE_SILENT)`), `releaseHeld` (call-held items re-run through the **full** pipeline). Actions are data; the service owns the engine.
  - **`app/.../service/AnnouncementService.kt`** (new) — `@AndroidEntryPoint` foreground service, `mediaPlayback` type, explicit `ACTION_START`/`ACTION_STOP`, persisted start flag (system recreation with `intent == null` and no start flag stops itself — never auto-announces), 750 ms main-thread tick (idle-cheap; no blocking calls), stop-on-change checks while speaking, `AudioDeviceCallback.onAudioDevicesRemoved` → engine stop, `NotificationRelay` registration/clearing, no logging.
  - **`app/.../service/AndroidTextToSpeechEngine.kt`** (new) — implements the P04 `TextToSpeechEngine`: sections spoken in order with `pauseBetweenSectionsMillis` between them, one request in flight (replacing `speak`/`stop` ends the previous request with `INTERRUPTED`), exactly one terminal callback per accepted request, unavailable engine / empty announcement returns false without notifying, `LANGUAGE_DATA_MISSING` instead of silent fallback, voice/speed/pitch applied.
  - **`app/.../service/AnnouncementNotification.kt`** (new) — low-importance channel `announcement_service`, ongoing notification with Stop action and statically worded service state only (never notification content), content intent to `MainActivity`, `setSilent`/`setShowWhen(false)`.
  - **`app/.../service/RuntimeStateReader.kt`** (new) — permission-free reads: `PowerManager.isInteractive`, `KeyguardManager.isKeyguardLocked`, `NotificationManager.currentInterruptionFilter` (≠ ALL ⇒ DND), call proxy via `AudioManager.mode` (`IN_CALL`/`IN_COMMUNICATION`), session mute, active output preference Bluetooth → wired → speaker with `address`/`productName`, `null` when unobserved (allowed by rules).
  - **`app/.../service/SettingsSource.kt`** (new, placeholder until P09/P13) — master-prompt defaults: **all apps disabled**, default `AnnouncementFormat` and `SpeechSettings`.
  - **`app/.../service/SessionStore.kt`** (new, temporary) — SharedPreferences start/mute flags until P09 DataStore; the only reader/writer of that file.
  - **`app/.../service/SpeechModule.kt`** (new) — Hilt `SingletonComponent` providers for `TextToSpeechEngine` (Android wrapper) and `AudioManager`.
  - **`app/src/main/res/values/strings.xml`**, **`app/src/main/res/drawable/ic_notification.xml`** — channel/notification strings (service state only) and a monochrome notification icon.
  - **`app/src/main/AndroidManifest.xml`** — announcement service slot now names a real class: `tools:ignore="Instantiatable"` removed and the now-unused `xmlns:tools` declaration dropped.
  - **`core/notifications/.../NotificationRelay.kt`** (new) + listener update — process-wide bridge (single `@Volatile` sink, cleared on destroy) so the system-created listener feeds the service-created pipeline; per-instance `sink` still wins for tests.
  - **`:core:queue` purity fix (P07 defect):** `DeviceAudioState.kt` (new pure interface), `AnnouncementQueue` constructor `(clock, settings, appOverrides, deviceAudio = DeviceAudioState { false })`, `canAnnounce` now a derived property; the `android.media.AudioManager` import is gone. **`AnnouncementQueueSilenceTest.kt`** (new) covers the gate.
  - **Docs:** changelog P08 entry (including the P07 defect note); phases status table (P08 complete, P09–P20 remaining).
- **Static review performed:**
  - **Critical defect found and fixed before commit:** P07's silence gate put `android.media.AudioManager` into `:core:queue`, a **pure Kotlin/JVM module** with no Android classpath — that cannot compile, and the queue's test constructors would also have bound the wrong positional parameter. Replaced with the pure `DeviceAudioState` fun interface (default non-silent so existing positional test calls stay valid), Android adapter moved into the service, and grep-verified: no Android imports remain in `core/queue`, no `canAnnounce()` function calls remain, `tools:` no longer referenced in the manifest. The P07 progress record above is left as written (audit trail); this record corrects it.
  - Pipeline review: `tick` marks the group started before returning, so a group can never be polled twice; empty groups are retired with `markStarted` + `markCompleted`; `onSpeechFinished` is idempotent (guards on the active group); stop checks compare against fresh settings and state; release re-evaluates with the **current** state (never the held decision).
  - Service review: main thread does no blocking work (pure rules/queue + async engine), tick removed in `onDestroy` via `removeCallbacksAndMessages(null)`, device callback unregistered, relay cleared before `super.onDestroy`, engine shut down, `currentAction !== action` identity guard prevents double completion, rejected `speak` retires the selection so nothing stays stuck "speaking", no logging anywhere.
  - Engine review: request identity checked on every callback (stale utterance ids ignored), `finishCurrent` nulls the request **before** notifying so a terminal callback is delivered exactly once, `QUEUE_FLUSH` per section avoids overlap, missing language reported rather than silently substituted.
  - Privacy/scope: notification content never logged, never written, never shown in the persistent notification; no new permissions (call detection is a permission-free proxy; DND read via interruption filter, see risks); `SettingsSource` deliberately announces nothing until the real settings layer exists.
- **Tests added (written, NOT executed) — 21 total:**
  - `AnnouncementPipelineTest` (17) — `onNotification_announceable_enqueuesAndReportsAnnounce`, `onNotification_appDisabled_skipsWithoutQueueing`, `onNotification_muted_skips`, `onNotification_callActiveWithQueueing_holdsInsteadOfQueueing`, `onNotification_updateOfPendingItem_replacesInPlace`, `tick_emptyQueue_returnsNull`, `tick_enqueuedItem_returnsSpeakWithBuiltAnnouncement`, `tick_whileSpeaking_returnsNull`, `onSpeechFinished_clearsSpeaking_andStartsTheGap`, `onSpeechFinished_withoutSpeech_isNoOp`, `onStateChanged_withoutSpeech_returnsNoActions`, `onStateChanged_screenTurnsOn_stopsSpeech`, `onStateChanged_deviceBecomesSilent_stopsSpeech`, `onStateChanged_eligibilityStillMatching_keepsSpeaking`, `tick_everySectionDisabled_retiresGroupWithoutSpeaking`, `releaseHeld_callEnded_reEvaluatesAndEnqueues`, `releaseHeld_stillBlocked_holdsAgainThroughTheFullPipeline`.
  - `AnnouncementQueueSilenceTest` (4) — `canAnnounce_tracksDeviceSilence`, `poll_silentDevice_returnsNoAnnouncement`, `poll_afterSilenceEnds_announcesTheWaitingItem`, `silentDevice_holdsTheSelectionBack_soNoIdIsIssued`.
- **Known risks / unresolved questions (need user/local input):**
  - First `@AndroidEntryPoint` service + first Hilt `@Module` are unverified: Hilt/KSP must process the app module and inject `TextToSpeechEngine`/`AudioManager`; the first local build is the verification point.
  - **DND detection may be restricted:** `NotificationManager.getCurrentInterruptionFilter()` can report `ALL` to apps without policy access, in which case the DND rule would never fire. If the local agent observes that, the choice is to request notification-policy access in the setup flow (a user-facing permission decision) or document a weaker DND guarantee — needs user direction (privacy-relevant).
  - `mediaPlayback` foreground service type for TTS is the ADR-010 decision; on API 34+ the system may scrutinise media-only usage, and the persistent notification may not display until `POST_NOTIFICATIONS` is explained and requested in P11. Both are expected until then.
  - Call detection is an `AudioManager.mode` proxy, not telephony state (avoids `READ_PHONE_STATE`); some VoIP call styles may not set `MODE_IN_CALL`/`MODE_IN_COMMUNICATION`.
  - End-to-end briefing is impossible by design until P09 (DataStore settings) and P13 (app selection): `SettingsSource` returns `appEnabled = false`, so snapshots are correctly skipped. `SessionStore` is a temporary store replaced by P09.
  - P07 record correction recorded above; ADR-017 decision (silence) and ADR-019 defaults remain as confirmed on 2026-11-02.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `84b46b1` — `feat(service): add foreground announcement service and pipeline (P08)`
- **Next phase:** P09 — preferences foundation (`:core:datastore`), which replaces `SettingsSource`/`SessionStore` placeholders.

---

## Phase P09 — Preferences foundation (`:core:datastore`) (2026-11-02)

- **Files changed:**
  - **`gradle/libs.versions.toml`** — `datastorePreferences = "1.3.0"` + `androidx-datastore-preferences` library (no explicit coroutines pin, see risks); **`settings.gradle.kts`** — `:core:datastore` (alphabetical: common, datastore, notifications, queue, rules, tts).
  - **`core/datastore/build.gradle.kts`** (new) — Android library: namespace `com.jayanth.hearit.core.datastore`, compileSdk 37 / minSdk 29, Java 11, `:core:common` + `:core:tts` + DataStore Preferences, JUnit tests.
  - **`core/datastore/.../SettingsCodec.kt`** (new, pure) — flat versioned string schema: global `elig.*`/`queue.*`/`format.*`/`speech.*`/`history.*`/`apps.*`, per-app `app:<package>:<field>` where **absence = inherit**, group markers for whole-group overrides, unit-separator lists (separator stripped from values on encode), tolerant decode (unknown keys ignored, invalid enum/number → documented default, never throws).
  - **`core/datastore/.../HearItSettings.kt`** (new) — aggregate model + `AppOverride` (+ `inheritsEveryValue`) with typed accessors `resolvedFor`/`queueFor`/`historyFor`/`priorityFor`, and master-prompt defaults (all apps disabled, keywords off, speaker off, full-content history, ADR-019 queue).
  - **`core/datastore/.../PreferenceStore.kt`** (new) — seam (`values` Flow, `read`, `replaceAll`) + `DataStorePreferenceStore` (string-only reads, atomic replace-all inside `edit`).
  - **`core/datastore/.../SettingsRepository.kt`** (new) — interface (`settings` Flow, `current`, `update(transform)`) + `DefaultSettingsRepository`, and the single process-wide DataStore file behind `HearItSettingsDataStore.repository(context)`.
  - **`core/common/.../HistorySettings.kt`** (new) — `HistoryStorageMode` (FULL_CONTENT default / METADATA_ONLY / DISABLED), `HistoryUpdateHandling` (UPDATE_ENTRY default / NEW_ENTRY), `HistorySettings` (global retention, auth off by default) + `AppHistoryOverride` with `resolveWith` inheritance.
  - **`core/common/.../AudioFocusPolicy.kt`** (new) — DUCK default / PAUSE / NONE; added to `EligibilitySettings` and `AppEligibilityOverride` (+ `resolveWith` + `inheritsEveryValue`), closing P08's audio-focus settings gap.
  - **`app/.../service/SessionStore.kt`** — KDoc corrected: session/runtime flags are deliberately separate from DataStore settings (ADR-021), no longer "replaced by P09".
  - **Docs:** **ADR-021** (schema, inheritance-by-absence, tolerant decode, transitive coroutines, session-state split, single-writer update semantics + alternatives); changelog P09 entry; phases status table (P09 complete, P10–P20 remaining).
- **Static review performed:**
  - **Serious regression found and repaired during this phase:** `Docs/TECHNICAL_DECISIONS.md` had been **overwritten wholesale** by an earlier ADR-017 edit in the previous session (a full-file write of only the new ADR-017 section), which deleted ADR-001–ADR-020 from the branch. The complete text was recovered from commit `bec9617`, the new ADR-017 decision block was spliced back in, and ADR-021 appended — verified afterwards: **21 ADR sections present (ADR-001…ADR-021)**. This is recorded here because the loss was already pushed; the restore is part of commit `996f9a0`.
  - Codec review: encode/decode symmetry proven by round-trip tests for defaults, a fully customized aggregate and per-app overrides; group markers match what the encoder always writes; `split(':', limit = 3)` is unambiguous because package names cannot contain `:`; list separator is stripped on encode so user text round-trips; every numeric decode is clamped to the model's `require` bounds (comparison with `QueueSettings()` defaults), so a corrupt store cannot throw from a constructor.
  - Purity sweep: `grep` confirms **no `import android.`** in `:core:common`, `:core:queue`, `:core:rules` or `:core:tts`; the DataStore/Android surface is confined to `PreferenceStore`/`SettingsRepository`.
  - Test/assertion review: two JUnit float assertions were written with the legacy two-argument overload (deprecated) — corrected to the delta form before commit; an accidental non-existent import (`RESERVED_NOT_SET`) was removed from `HearItSettingsTest`.
  - Scope/privacy: no new permissions; no notification content involved; settings are user data but contain no message text (only keywords the user typed).
- **Tests added (written, NOT executed) — 28 total:**
  - `SettingsCodecTest` (17) — `encode_thenDecode_defaults_roundTrip`, `decode_emptyMap_returnsShippedDefaults`, `encode_thenDecode_customGlobalSettings_roundTrip`, `encode_writesSchemaVersion`, `decode_unknownKeys_areIgnored`, `decode_invalidEnum_fallsBackToDefault`, `decode_invalidQueueNumbers_fallBackToDefaults`, `decode_invalidSpeechValues_fallBackToDefaults`, `encode_thenDecode_perAppOverride_roundTrip`, `decode_absentPerAppFields_areInherited`, `encode_packageNameWithDots_roundTrips`, `encode_listValues_preserveCommasSpacesAndNewlines`, `encode_emptyBluetoothAllowList_isDistinctFromAnyBluetooth`, `decode_withoutVersionKey_stillDecodes`, `encode_thenDecode_everyOverflowPolicy`, `encode_nullIntroductionAndRetention_areOmittedAndDecodeNull`, `encode_thenDecode_textLengthNoLimit_roundTripsAsNull`.
  - `HearItSettingsTest` (11) — `defaults_noAppIsEnabled`, `resolvedFor_unknownApp_isDisabled`, `resolvedFor_enabledApp_isEnabledWithGlobalValues`, `resolvedFor_priorityListed_isListedIndependentlyOfEnabled`, `resolvedFor_appOverride_replacesOnlyThatCondition`, `queueFor_withoutOverride_inheritsGlobal`, `queueFor_override_appliesGroupingWindowOnly`, `historyFor_override_appliesStorageMode`, `priorityFor_defaultsToNormal`, `priorityFor_override_returnsOverride`, `appOverride_inheritsEveryValue_trueForEmptyAndFalseWhenAnyFieldSet`.
- **Known risks / unresolved questions (need user/local input):**
  - **Coroutines metadata risk:** DataStore 1.3.0 stays external to Kotlin versioning, but coroutines may resolve transitively to 1.11.0 (built with Kotlin 2.2.20). Under Kotlin 2.2.10 this can surface as a "compiled with a newer Kotlin" error; the documented fix is bumping KGP to 2.2.20 (AGP 9 allows ≥ 2.2.10), not downgrading DataStore.
  - DataStore 1.3.0 notes cite `lifecycle-runtime 2.10.0` for lint checks at AGP ≥ 9.5.0-alpha; harmless at AGP 9.3.3 but the local build should confirm no lint-version complaint.
  - `DefaultSettingsRepository.update` is a single-writer read-modify-write (settings UI only); concurrent writers would need compare-and-set behind `PreferenceStore` (documented in ADR-021).
  - `:app` still uses `SettingsSource` (all apps disabled) and `SessionStore`; the repository is not yet consumed, so nothing end-to-end announces until P10/P13 wire it — intentional, no unused dependency in `:app`.
  - DataStore itself has no JVM `src/test` path without Robolectric/temp-file plumbing, so only the codec and aggregate accessors are unit-tested; the thin adapter is statically reviewed (documented decision).
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `996f9a0` — `feat(datastore): add settings foundation with explicit inheritance (P09)`
- **Next phase:** P10 — history storage (`:core:database`, Room).

## Phase P10 — History storage (`:core:database`, Room) (2026-11-02)

- **Files changed:**
  - **`gradle/libs.versions.toml`** — `room = "2.8.5"` + `androidx-room-runtime`/`-compiler`/`-testing`; **`settings.gradle.kts`** — `:core:database` (alphabetical: common, database, datastore, notifications, queue, rules, tts).
  - **`core/database/build.gradle.kts`** (new) — Android library (namespace `com.jayanth.hearit.core.database`, compileSdk 37 / minSdk 29, Java 11), KSP + `room-compiler`, `:core:common`, JUnit; `RoomSchemaArgProvider` (`CommandLineArgumentProvider`) feeds `room.schemaLocation` so the schema directory is a tracked Gradle input.
  - **`core/database/.../HistoryEntryEntity.kt`** (new) — `history_entries` table (id autoincrement, `key`, package/app/sender, `messageText` nullable, channel/category, post+recorded times, `status`/`reason`/`priority` as enum names, `isUpdate`) with indices for newest-first paging, app+status filtering, newest-per-key lookup; `toEntity`/`toDomain` mappings.
  - **`core/database/.../HistoryDao.kt`** (new) — suspending insert/update/latestFor/deleteById/deleteAll/deleteOlderThan/page/count; pagination uses one nullable-filter query (`:param IS NULL OR ...`) with `LIMIT`/`OFFSET`.
  - **`core/database/.../HearItHistoryDatabase.kt`** (new) — `@Database(version = 1, exportSchema = true)` + `HistoryDatabase.create(context)` (deliberately no destructive-migration fallback).
  - **`core/database/.../HistoryEntry.kt`** (new, pure) — domain model with invariants + `HistoryEntryMapper` (tolerant enum-name mapping: unknown status → `SKIPPED`, unknown reason/priority → null).
  - **`core/database/.../HistoryPolicy.kt`** (new, pure) — `HistoryRecorderPolicy` (`Discard`/`Insert`/`ReplaceExisting`), `HistoryRetentionPolicy` (cutoff = now − days·86 400 000; null = Never), `HistoryQuery` (nullable filters, page size 1..500, validated).
  - **`core/database/.../HistoryRepository.kt`** (new) — `HistoryRepository` interface + `RoomHistoryRepository` (thin glue, injected `Clock`).
  - **Docs:** **ADR-022** (schema, storage modes, update handling, retention, migration policy, layering, sensitivity; **Room 3.0.3 recorded as the current stable line and deliberately deferred** with the switch isolated to build wiring + Room-facing files); changelog P10 entry; phases table (P10 complete, P11–P20 remaining).
- **Static review performed:**
  - Full module reviewed; **one test defect found and fixed before commit:** `decide_usesProvidedRecordTime` read `.entry` through the un-cast `action` variable after a cast used only inside an assertion, which does not smart-cast at use site — rewritten with a local `val entry = (action as HistoryRecordAction.Insert).entry`.
  - Room API review: annotations imported from `androidx.room.*` (2.x package, not the 3.x rename), `@Update` entity carries its primary key, all DAO functions suspend (no main-thread I/O), query parameters nullable inside a single SQL string (no concatenation), indices match the three documented query shapes, and unsupported direct Room usage prevented by keeping policies/domain Room-free.
  - KSP/build review: schema arg provider uses `@InputDirectory`/`@PathSensitive` so builds stay cacheable; KSP 2.3.13 (already pinned) is the AGP 9-compatible line; no kapt anywhere.
  - Purity sweep: `HistoryEntry.kt` and `HistoryPolicy.kt` contain **no `import android.`**; the Android/Room surface is limited to entity/DAO/database/repository.
  - Acceptance review: recording is independent of eligibility (all five `HistoryStatus` values recordable with reasons); storage modes and retention are policy-tested; migrations are policy-defined (version bump + explicit `Migration` + migration test, no destructive fallback) but **migration tests themselves cannot be written correctly before a build exports `schemas/`** — see risks.
- **Tests added (written, NOT executed) — 28 total:**
  - `HistoryPolicyTest` (20) — `decide_fullContent_keepsMessageText`, `decide_metadataOnly_dropsMessageText_butKeepsSender`, `decide_disabled_discardsEverything`, `decide_blankMessageText_storesNull`, `decide_updateEntryWithExistingRow_replacesIt`, `decide_updateEntryWithoutExistingRow_inserts`, `decide_newEntryHandling_alwaysInserts`, `decide_skippedItem_isRecordedWithItsReason`, `decide_queuedAndInterruptedItems_areRecorded`, `decide_preservesUpdateFlagAndPriority`, `decide_usesProvidedRecordTime`, `retention_never_returnsNull`, `retention_zeroDays_cutsOffAtNow`, `retention_thirtyDays_subtractsThirtyDays`, `retention_everyConfiguredOption`, `retention_negativeDays_isRejected`, `query_defaults_areValid`, `query_limitAboveMaximum_isRejected`, `query_negativeOffset_isRejected`, `query_blankPackageName_isRejected`.
  - `HistoryEntryMapperTest` (8) — `statusRoundTrips_forEveryValue`, `unknownStatus_fallsBackToSkipped`, `reasonRoundTrips_forEveryValue`, `unknownReason_isNull`, `priorityRoundTrips_forEveryValue`, `entityRoundTrip_preservesEveryField`, `metadataOnlyEntry_roundTripsWithNullMessage`, `entityWithUnknownStoredStatus_decodesTolerantly`.
- **Known risks / unresolved questions (need user/local input):**
  - **Room major version is a user/local call.** Room 2.8.5 (chosen) is in maintenance mode; `androidx.room3` 3.0.3 is current and mandatory-KSP with a new package and driver API. It was not chosen because its API surface cannot be compile-verified in the cloud. If the local agent prefers Room 3, the change is confined to `core/database/build.gradle.kts` + Room-facing files (domain/policies untouched) — ADR-022 records this.
  - The schema, queries and `schemas/` export are unproven until the first build; a KSP/Room error (e.g. query/column mismatch) is the most likely first failure and is cheap to fix locally.
  - DAO/in-memory database tests and migration tests are deferred by design: they need the exported schema and (for migration) the schema JSON on the test asset path; they belong to the local agent once the module builds.
  - `HistoryRepository` is not wired into the pipeline yet (service history writes arrive with P18 feature wiring or a small P10 follow-up), so no runtime effect until then; no unused dependency was added to `:app`.
  - Per-app history mode is resolved via `HearItSettings.historyFor` (P09); the repository itself takes the resolved `HistorySettings`, so wiring must pass the per-app value, not the global one.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `de56968` — `feat(database): add Room history storage with retention (P10)`
- **Next phase:** P11 — setup and onboarding flow.

## Phase P11 — Setup and onboarding flow (`:core:setup`, `:app`) (2026-11-02)

- **Files changed:**
  - **`settings.gradle.kts`** — `:core:setup` included (alphabetical); **`core/setup/build.gradle.kts`** (new) — pure Kotlin/JVM module, Java 11, JUnit only, no dependencies.
  - **`core/setup/.../SetupStep.kt`** (new, pure) — the documented nine steps in order; `isSkippable` (Bluetooth, battery guidance, outputs, test announcement) and `isRequired` are the single source of the skip rules.
  - **`core/setup/.../SetupPrerequisites.kt`** (new, pure) — facts only (notification access, Bluetooth required/granted, TTS engine + device-language, enabled app count, allowed output count) with `ttsReady`/`hasEnabledApps`/`hasAllowedOutput` derivations and non-negative invariants.
  - **`core/setup/.../SetupBlocker.kt`** (new, pure) — typed blockers (`NotificationAccessMissing`, `BluetoothPermissionMissing`, `TtsEngineUnavailable`, `TtsLanguageMissing`, `NoEnabledApps`, `NoAllowedOutput`) so the UI owns wording and TalkBack reads a precise reason.
  - **`core/setup/.../SetupFlow.kt`** (new, pure) — visible steps (Bluetooth hidden below API 31), per-step `blockersFor`, `canAdvanceFrom`, `canOnlySkip`, `nextStep`/`previousStep` over the visible list, `isFinalStep`, `startBlockers`, `canStart`, and `resolveVisibleStep` so a persisted hidden step can never restore to an invisible screen. Start prerequisites: notification access, TTS engine + language data, ≥1 enabled app, ≥1 allowed output; Bluetooth permission/battery ack/test never gate Start.
  - **`core/setup/.../SetupState.kt` + `SetupStateCodec.kt`** (new, pure) — completed, current step, visited/skipped steps, battery acknowledgement; flat string-map codec with `SCHEMA_VERSION`, sorted sets, tolerant decode (unknown keys/enum names/invalid booleans → documented defaults).
  - **`core/datastore/.../SetupStateRepository.kt`** (new) — `SetupStateRepository` interface + `DefaultSetupStateRepository` over the existing `PreferenceStore` seam, and `HearItSetupDataStore` on its own `hearit_setup` DataStore file (keeping setup progress out of the settings flow). Build file gains `:core:setup`.
  - **`app/.../setup/SetupModels.kt`** (new) — `InstalledApp`, `DetectedOutput`, `TestAnnouncementState` and the immutable `SetupUiState` (step, visible steps, blockers, advance/skip/back/start flags, apps, outputs, permissions, test state, progress counters).
  - **`app/.../setup/SetupProbes.kt`** (new) — probe interfaces plus tiny Android implementations: notification access (`NotificationManagerCompat.getEnabledListenerPackages`), `POST_NOTIFICATIONS` (API 33+), `BLUETOOTH_CONNECT` (API 31+), launcher-app catalog (`queryIntentActivities`, own package excluded, deduped, sorted), connected Bluetooth/wired outputs, and `AndroidSetupLauncher` (notification-access screen, battery-optimisation list, explicit `ACTION_START` via `startForegroundService`).
  - **`app/.../setup/SetupViewModel.kt`** (new) — Hilt ViewModel: reads setup state + settings, gathers facts through probes/engine, persists navigation/skip/acknowledgement, bounded 5 s TTS initialisation poll, test announcement through the engine only, and Start that re-checks `SetupFlow.canStart`, marks completion, persists the session flag, then starts the service.
  - **`app/.../setup/SetupScreen.kt`** (new) — scaffolded Compose flow: step counter, per-step content for all nine steps, permission launchers (`RequestPermission`), refresh on `ON_RESUME`, inline blocker list, Back/Continue/Skip row and the Start button bound to `canStart`; lazy app list; every selectable row is one accessible toggle; all strings from resources.
  - **`app/.../di/SetupModule.kt`** (new) — Hilt singletons for `SettingsRepository`, `SetupStateRepository`, `SessionStore` and all six probes (`:core:*` stays Hilt-free).
  - **`app/.../MainActivity.kt`** — now hosts `SetupRoute` with an activity-scoped `SetupViewModel`; the placeholder greeting and its preview are gone.
  - **`app/src/main/AndroidManifest.xml`** — `POST_NOTIFICATIONS` + `BLUETOOTH_CONNECT` declared with comments tying each to the step that explains and requests it; launcher `<queries>` added for Android 11+ app visibility; service slots unchanged.
  - **`app/src/main/res/values/strings.xml`** — 60+ new setup strings (titles, bodies, statuses, blockers, step actions) and the existing notification strings retained.
  - **Docs:** **ADR-023** (pure gating core, step/skip rules, Start prerequisites, separate DataStore file, explicit Start only, permission timing, launcher-only catalog, test announcement semantics, probes, accessibility); changelog P11 entry; phases table (P11 complete, P12–P20 remaining).
- **Static review performed:**
  - **String-resource cross-check** (scripted): every `R.string.*` reference in `app` resolves to a declared string, no duplicate names, and no setup string is unused. `app_name` is referenced from the manifest via `@string/app_name`, not from Kotlin.
  - **Purity check:** `core/setup` contains no `import android.`; all gating logic is JVM-testable.
  - **Two test defects found and fixed before commit:** `canOnlySkip` was asserted true for the test-announcement step, which has no blockers (so it is never "skip-only") — replaced with the blocked `AUDIO_DEVICES` case; and the codec's stable-order test expected `WELCOME` before `REVIEW` while the codec sorts names alphabetically — the test now compares the sorted parts.
  - **ViewModel cleanup before commit:** removed a placeholder constant and its import, and replaced fully-qualified inline type names with imports.
  - Review notes: composable string lookups are only made in composable context (no `stringResource` inside `map`/`joinToString` lambdas — the review step builds its summaries with an explicit loop), rows use `toggleable` with `Checkbox`/`Switch(..., onCheckedChange = null)` for a single accessible control, the lifecycle owner is obtained by casting `LocalContext` (avoids the deprecated compose `LocalLifecycleOwner` and a new dependency), and the UI never computes gating itself.
- **Tests added (written, NOT executed) — 36 total:**
  - `SetupFlowTest` (21) — documented step order; notification access required/blocked and granted; Bluetooth step hidden below API 31, shown on 31+, skippable when missing, never blocks Start; TTS engine/language blockers; app-selection and output blockers; review reports all Start blockers in a stable order; `canStart` false for each missing prerequisite and true otherwise; next/previous walk the visible order and skip hidden steps; null at the ends; `resolveVisibleStep` fallback; only skippable steps can be passed without their prerequisite; informational steps never block; skip-rule classification.
  - `SetupStateCodecTest` (11) — every key encoded; round-trip; empty store defaults; unknown step name; unknown names in sets ignored; invalid boolean; stable sorted sets; skipped steps round-trip; `visiting`/`skipping`/`completing` semantics.
  - `SetupStateRepositoryTest` (4) — empty store yields fresh-install defaults; update persists and emits; read-modify-write across two updates; corrupted values decode to defaults.
- **Known risks / unresolved questions (need user/local input):**
  - **Compose flow and probes are entirely unverified.** The likely first-build failures are Compose/Material3 API drift (e.g. `HorizontalDivider`, `Scaffold`, `toggleable` signatures) and the permission-launcher wiring; nothing here can be compiled in the cloud.
  - **Hilt graph grew to a 10-dependency ViewModel** (`SettingsRepository`, `SetupStateRepository`, `SessionStore`, six probes, engine). `SetupModule` provides everything except the engine (P08's `SpeechModule`); the first KSP build validates it.
  - **Start currently announces nothing:** `SettingsSource` in the service still returns `appEnabled = false` for every app (P08 placeholder), so starting the foreground service is honest but silent until the settings-backed per-app source lands (P13, with P10 history wiring). The test-announcement step proves the TTS path separately. This is the largest user-visible gap in the milestone.
  - **`POST_NOTIFICATIONS` can be silently denied** after repeated dismissals on API 33+; the review step explains why it is wanted but does not deep-link to app notification settings (deferred with P12's settings shell).
  - **App catalog is launcher-apps-only** (with `<queries>`) by design; apps without a launcher icon cannot be enabled at setup until P13 replaces the probe.
  - **`AudioDeviceInfo.TYPE_BLE_HEADSET` / `TYPE_HEARING_AID`** are used from minSdk 29 as inlined compile-time constants; safe at runtime, but worth a local lint pass.
  - `SetupState` is stored in a second DataStore file while `SessionStore.isStarted` stays in SharedPreferences (ADR-021); a future uninstall-restore review should confirm both behave as intended under backup rules.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `c2cb4d1` — `feat(setup): add nine-step first-launch onboarding flow (P11)`
- **Next phase:** P12 — dashboard and navigation shell.

## Phase P12 — Dashboard and navigation shell (`:app`/`dashboard`) (2026-11-02)

- **Files changed:**
  - **`app/build.gradle.kts`** — added `:core:database`, `:core:datastore`, `:core:setup` project dependencies. **P11 defect repaired:** `SetupViewModel`/`SetupModule`/`HearItSettingsDataStore` and `core.setup.*` were imported by `:app` without the module dependencies, so the first build would have failed on unresolved references; `:core:database` is the P12 addition.
  - **`app/.../dashboard/DashboardDestination.kt`** (new) — the nine destinations as one enum (four tabs + five secondary entries), each with a label resource and an explicit bottom-bar flag, a stable `id` used for saveable state, and `bottomBar`/`secondary`/`fromId` (total: unknown or null restores Home).
  - **`app/.../dashboard/DashboardModels.kt`** (new) — content-free `ServiceSnapshot(isRunning, isSpeaking, pendingCount)`, `RecentAnnouncement(id, appName, status, recordedAtMillis)` and the immutable `DashboardUiState` (loaded, service, enabled-app count, allowed outputs, connected-output label, mute flag, recent history + load/failure flags, test-announcement state) with derived `announcementsActive`/`priorityAnnounceActive`.
  - **`app/.../dashboard/ServiceStatusMonitor.kt`** (new) — `StateFlow` observation seam written only by the service (`markRunning`, `markStopped`, `updateSpeech`), with queue depth clamped at zero; no polling of `ActivityManager`/`SessionStore`.
  - **`app/.../dashboard/ServiceControl.kt`** (new) — `ServiceControl` interface + `AndroidServiceControl`, sending the existing P08 `ACTION_START`/`ACTION_STOP` intents (Stop as a command so the persisted session flag is cleared).
  - **`app/.../dashboard/DashboardViewModel.kt`** (new) — Hilt ViewModel combining the settings flow and the service-status flow, re-reading session mute + connected outputs + newest five history rows on `ON_RESUME`, explicit start/stop events, and the engine-only test announcement (never queued, never recorded). History read failures become `historyUnavailable` instead of being swallowed.
  - **`app/.../dashboard/DashboardScreen.kt`** (new) — `DashboardRoute` (saveable destination, `BackHandler` back to Home, resume refresh) and the stateless `DashboardScreen`: header with Back on secondary screens, custom bottom bar (`selectable`, `Role.Tab`, selected + bold + colour), service card with Start/Stop, status rows (normal announcements, priority announce + note, temporary mute + note, connected output, allowed output types, apps enabled), recent-history section with empty/unavailable/rows states and a "View all history" action, quick actions (test announcement, Applications, Audio devices, Diagnostics) and the five secondary rows, plus an honest placeholder for destinations whose phase has not landed.
  - **`app/.../di/DashboardModule.kt`** (new) — process-singleton `ServiceStatusMonitor`, `AndroidServiceControl`, the first `HearItHistoryDatabase` provider and `RoomHistoryRepository` over a wall-clock `Clock`.
  - **`app/.../MainActivity.kt`** — two activity-scoped ViewModels; routes to `DashboardRoute` once `setup.completed` is true (the setup flow's placeholder completion screen is gone) and to `SetupRoute` otherwise; KDoc records that nothing starts the service implicitly.
  - **`app/.../setup/SetupScreen.kt`** — removed `SetupCompleteContent` and its branch (the shell owns the post-setup surface) and updated the route KDoc.
  - **`app/.../service/AnnouncementService.kt`** — injects `ServiceStatusMonitor`; publishes running state on start, speaking/queue depth after each tick, on speech start/finish and on stop/destroy through a private content-free `publishStatus()`.
  - **`app/.../service/AnnouncementPipeline.kt`** — exposes `pendingCount` (the queue's own count) for the status snapshot.
  - **`app/src/main/res/values/strings.xml`** — 9 navigation labels, 41 dashboard strings and the queue-depth plural; removed the two now-unused `setup_complete_*` strings so no resource is orphaned.
  - **`app/src/test/.../dashboard/DashboardDestinationTest.kt`** and **`ServiceStatusMonitorTest.kt`** (new) — JVM unit tests for the navigation model and the observation seam.
  - **Docs:** **ADR-024** (shell location, saveable destination state instead of Navigation Compose, no new dependencies, read-only service seam, dashboard privacy, honest partial sections, history binding, packaging repair, alternatives, consequences); changelog P12 entry; phases table (P12 cloud complete, P13–P20 remaining); architecture §2 module-table note and §6 navigation/service-observation bullets.
- **Static review performed:**
  - **String-resource cross-check** (scripted): every `R.string.*`/`R.plurals.*` reference in `app` resolves to a declared resource, no duplicate names, and no dashboard/navigation string is unused; the two `setup_complete_*` strings are gone together with their only references.
  - **Dependency-closure check:** every package imported by the new `dashboard` code has a module dependency in `app/build.gradle.kts` (`core:common`, `core:database`, `core:datastore`, `core:setup`, `core:tts` via the engine, `hilt`, `activity-compose`, `material3`, `foundation` transitively through material3); no new artifact or version was added, so the version catalog is untouched.
  - **Hilt graph:** `DashboardViewModel`'s seven dependencies all have providers (`SettingsRepository`, `SessionStore` and `AudioOutputProbe` from `SetupModule`; `HistoryRepository`, `ServiceStatusMonitor` and `ServiceControl` from `DashboardModule`; `TextToSpeechEngine` from `SpeechModule`); the monitor is a `@Singleton` because the service and the UI must share one instance; the database is also a singleton (Room forbids two open instances of the same file).
  - **Lifecycle/threading:** the monitor is updated only from the service's main-thread tick loop and read through `StateFlow`; the dashboard's own reads are cheap binder/preferences calls on the main thread, matching the setup flow; history reads are suspending Room calls off the main thread; `viewModelScope` collection is cancelled with the ViewModel; `ServiceStatusMonitor` writes nothing to disk, so no state leaks between process lifetimes.
  - **Accessibility:** every bottom-bar tab is one `selectable` with `Role.Tab` and a selected state (≥48 dp, `navigationBarsPadding`), secondary rows and buttons are ≥48 dp, status is always text (Active/Inactive, On/Off, queue plural) with colour as reinforcement only, the empty-history copy is explicit rather than a blank area, and every visible string is a resource.
  - **Privacy:** no notification text or sender in the new state, UI or monitor; no logging added anywhere; the recent-history rows carry app name, outcome and relative time only.
  - **No silent start:** `MainActivity`, `DashboardRoute` and the ViewModel contain no implicit service start; only the Start button calls `ServiceControl.startAnnouncements()`, and the service sets the session flag itself.
  - **P11 gap repaired:** missing `:core:datastore`/`:core:setup` dependencies recorded rather than silently left for the local build to discover.
- **Tests added (written, NOT executed) — 16 total:**
  - `DashboardDestinationTest` (8) — `bottomBar_hasTheFourMasterPromptTabs_inOrder`, `secondary_hasTheFiveMasterPromptEntries_inOrder`, `bottomBarAndSecondary_partitionEveryDestination`, `fromId_restoresEveryDestinationExactly`, `fromId_unknownOrMissingId_fallsBackToHome`, `ids_areUniqueAndNeverBlank`, `everyDestination_hasADistinctLabelResource`, `onlyHomeIsSelectedByDefault`.
  - `ServiceStatusMonitorTest` (8) — `initialState_isStoppedAndSilent`, `markRunning_setsRunningOnly`, `updateSpeech_publishesSpeakingAndQueueDepth`, `updateSpeech_negativePendingCount_isClampedToZero`, `updateSpeech_beforeRunning_doesNotFakeAStart`, `markStopped_clearsRunningSpeechAndQueue`, `markStopped_isIdempotent`, `updateSpeech_afterStop_canReportAgainOnceRunning`.
- **Known risks / unresolved questions (need user/local input):**
  - **The whole shell is unverified.** Likely first-build issues are Compose API drift (`Scaffold`, `Surface(tonalElevation)`, `selectable(role=)`, `pluralStringResource`, `navigationBarsPadding`) and the two new `@Test` classes compiling against `:app`'s unit-test configuration; nothing here can be compiled in the cloud.
  - **`rememberSaveable` restores across process recreation only when Android saves the activity state** (backgrounded then reclaimed). A cold launch from the launcher always opens Home — deliberate (ADR-024), but worth confirming on a device against the acceptance wording.
  - **Custom bottom bar instead of `NavigationBar`:** chosen to avoid an unverifiable icon artifact; if the local agent prefers the Material component, only `DashboardBottomBar` changes and the enum already carries the labels.
  - **`ServiceStatusMonitor` is in-process:** exactly right for one process; a service killed without `onDestroy` (e.g. force-stop) leaves the flow at `markRunning` until the process dies, and the dashboard would briefly show Active — acceptable, but a device check of force-stop is worthwhile.
  - **Priority Announce and temporary mute are read-only rows** until P17/P16; recent history stays empty until history writes are wired (P18). Both are documented in the UI copy and ADR-024, not hidden.
  - **`:feature:dashboard` extraction is deferred** (ADR-024): it needs the service seams moved out of `:app` first, so the architecture module table now records the actual location.
  - **History read path is unproven:** the Room database is opened by the dashboard for the first time; a schema/DAO mismatch would surface here first.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `3d4b09d` — `feat(dashboard): add dashboard and bottom navigation shell (P12)`
- **Next phase:** P13 — Applications feature.

## Phase P13 — Applications feature (`:app`/`applications`) (2026-11-02)

- **Files changed:**
  - **`app/.../applications/ApplicationsModels.kt`** (new) — `AppEntry`, the generic `Inherited<T>(override, global)` with `isInherited`/`effective`, the list state (`loading`, `catalogUnavailable`, `query`, `apps`, `visibleApps`, `enabledApps`, `openPackage`, `settings`) and `AppSettingsUiState` (priority, history storage/update handling, DND, call queueing and audio focus, all as `Inherited`, plus `enabledAppCount`/`hasOverrides`).
  - **`app/.../applications/AppFilter.kt`** (new, pure) — case-insensitive search over label **and** package, `AppCatalogMerge` (catalog ∪ already-enabled packages, unknown labels fall back to the package name, sorted ignoring case) and `AppSelection` (select/deselect-all scoped to the visible rows).
  - **`app/.../applications/AppSettingsEditor.kt`** (new, pure) — enable/disable one app, enable/disable a visible set, priority, history storage mode, history update handling, respect-DND, queue-during-call and audio-focus overrides (each nullable = inherit), `resetToGlobal` (overrides only) and `settingsFor(...)`, which projects `HearItSettings` into `AppSettingsUiState`. A private `updateOverride` removes the map entry as soon as every field is inherited again (ADR-021 sparseness).
  - **`app/.../applications/AppCatalog.kt`** (new) — `ApplicationCatalog` + `AndroidApplicationCatalog` (launcher activities, own package excluded, label read defensively, `Dispatchers.IO`) and `AppIconLoader` + `AndroidAppIconLoader` (96 px bitmap on `IO`, missing icon tolerated).
  - **`app/.../applications/ApplicationsViewModel.kt`** (new) — Hilt ViewModel combining the settings flow with a one-off catalog read, publishing list + filtered rows + open-app settings from the two cached inputs, applying every edit through `SettingsRepository.update`, and exposing the icon loader as a data source for rows.
  - **`app/.../applications/ApplicationsScreen.kt`** (new) — `ApplicationsRoute` (list ↔ settings, `BackHandler` back to the list), the list (search field, enabled counter, select-all/deselect-all over the filtered rows, visibility note, lazy rows with icon + label + `Role.Switch` toggle + Settings button, loading/empty/no-match/catalog-failure states) and the per-app screen (enable switch, then inheritance-aware radio groups for priority, history storage, update handling, DND, call queueing and audio focus, plus reset-to-global and a note about the globally configured groups).
  - **`app/.../di/ApplicationsModule.kt`** (new) — singleton `ApplicationCatalog` and `AppIconLoader`.
  - **`app/.../dashboard/DashboardScreen.kt`** — the Apps tab now renders the feature through a caller-supplied `appsContent` slot (the shell stays unaware of the applications package); `DashboardRoute` takes the `ApplicationsViewModel` and supplies `ApplicationsRoute`.
  - **`app/.../MainActivity.kt`** — third activity-scoped ViewModel wired into the dashboard route.
  - **`app/src/main/res/values/strings.xml`** — 10 applications + 26 per-app strings; no hardcoded user-facing text added.
  - **`app/src/main/AndroidManifest.xml`** — `<queries>` comment updated: the same launcher visibility serves setup and the P13 list, and `QUERY_ALL_PACKAGES` is deliberately not requested (ADR-025).
  - **`app/src/test/.../applications/AppFilterTest.kt`** and **`AppSettingsEditorTest.kt`** (new) — JVM unit tests for search/merge/selection and for every override plus the inheritance projection.
  - **Docs:** **ADR-025** (launcher-visible catalog and its limit, pure logic split, sparse overrides, visible inheritance, immediate apply, select-all scope, per-app scope vs P15, icon loading, alternatives, consequences); changelog P13 entry; phases table (P13 cloud complete, P14–P20 remaining); architecture module-table note and §6 feature-module bullet.

- **Static review performed:**
  - **String cross-check** (scripted): every `R.string.*`/`R.plurals.*` reference resolves, no duplicates, no unused string; all 36 new strings are referenced.
  - **Purity:** `AppFilter.kt` and `AppSettingsEditor.kt` contain no `import android.`; the Android surface is the catalog/icon adapters and the Compose screen.
  - **Inheritance:** each editor writes only its own field; `updateOverride` drops the entry when `AppOverride.inheritsEveryValue` holds (clearing a value cannot leave an empty override); `settingsFor` reads the same accessors the pipeline uses (`historyFor`/`resolvedFor`); tests assert resolved values after edits.
  - **Threading:** catalog/icon reads on `Dispatchers.IO`; settings writes are suspending DataStore calls in `viewModelScope`; only visible rows compose (`LazyColumn`); icons load per row via `LaunchedEffect` with a `remember(packageName)` cache.
  - **Accessibility:** two targets per row (row-level `Role.Switch` carrying the app name, 48 dp Settings button), `Role.RadioButton` rows at 48 dp, status never colour-only, inheritance labels spell out the effective global value.
  - **Scope:** no new permissions or dependencies, no notification content read or stored; package metadata only.
  - **Hilt:** all three ViewModel dependencies provided (`SettingsRepository` from `SetupModule`; catalog and icon loader from `ApplicationsModule`), both new providers stateless singletons.
- **Tests added (written, NOT executed) — 28 total:**
  - `AppFilterTest` (11) — `filter_blankQuery_returnsEveryApp`, `filter_matchesLabelIgnoringCase`, `filter_matchesPackageName`, `filter_trimsTheQuery`, `filter_withoutMatch_returnsEmpty`, `merge_keepsCatalogEntriesAndTheirLabels`, `merge_addsEnabledPackageTheCatalogCannotSee`, `merge_sortsByLabelIgnoringCase`, `merge_ignoresBlankEnabledPackage`, `enable_addsTargetsAndKeepsOtherEnabledApps`, `disable_removesOnlyTargets`.
  - `AppSettingsEditorTest` (17) — `toggleEnabled_addsThenRemoves`, `setEnabled_leavesOtherAppsAlone`, `enableAll_addsVisibleAppsAndKeepsOthers`, `disableAll_removesVisibleAppsAndKeepsOthers`, `setPriority_storesSparseOverride`, `setPriorityNull_removesTheEntryAgain`, `setHistoryStorageMode_storesAndInheritsViaNull`, `setHistoryUpdateHandling_storesOverride`, `setRespectDnd_storesOverride`, `setQueueDuringCall_storesOverride`, `setAudioFocus_storesOverride`, `oneOverrideDoesNotClearAnother`, `resetToGlobal_clearsOverridesButKeepsTheAnnouncementSwitch`, `settingsFor_reportsInheritanceAndResolvedValues`, `settingsFor_reflectsOverrides`, `settingsFor_blankLabel_fallsBackToPackageName`, `settingsFor_unrelatedAppKeepsItsOwnInheritance`.

- **Known risks / unresolved questions (need user/local input):**
  - **Unverified UI:** `OutlinedTextField`, `RadioButton`, lazy rows and the icon `Image` path are uncompiled; the row places a toggleable `Row` beside a `TextButton`, which should be checked with TalkBack for focus order and duplicate announcements.
  - **Launcher-only catalog is a product limitation:** apps without a launcher icon are not listed until enabled (reachable only via history once P18 exists). Complete discovery needs `QUERY_ALL_PACKAGES` and an explicit product decision — recorded in ADR-025 rather than decided silently.
  - **Search runs client-side** over the in-memory catalog: fine at phone scale; a very large catalog would want filtering off the main thread.
  - **Priority options follow `AppPriority.entries` declaration order**, so a future enum reorder changes display order (labels stay correct).
  - **Icons are cached per composed row and never evicted:** bounded by what the user scrolls past, but a cache limit is worth adding later.
  - **Single-writer settings** (ADR-021): rapid toggling is serialised by DataStore, but two screens editing settings concurrently would need compare-and-set.
  - **Still silent end-to-end:** the service's `SettingsSource` ignores these settings (P08 placeholder), so enabling apps here does not yet produce announcements — the wiring gap is deliberate and belongs to the phase that connects `resolvedFor`/`queueFor`.
- **Build status:** Not run (cloud-agent restriction).
- **Test execution status:** Not run (cloud-agent restriction).
- **Local verification:** Not locally verified.
- **Commit:** `474ee14` — `feat(applications): add app selection and per-app settings (P13)`
- **Next phase:** P14 — Devices feature.

---

## Phase log

| Phase | Status | Commit |
|---|---|---|
| P00 Documentation foundation | Cloud complete, not locally verified | `b9d4b9a` |
| P01 Core domain foundation | Cloud complete, not locally verified | `6915b28` |
| P02 Eligibility rules (part 2) | Cloud complete, not locally verified | `bc29b2a` |
| P03 Eligibility rules (part 3) | Cloud complete, not locally verified | `5140f52` |
| P04 Announcement format and TTS contract | Cloud complete, not locally verified | `2b7d44b` |
| P05 Queue, grouping and scheduling | Cloud complete, not locally verified | `6364676` |
| P06 Build foundation: Hilt, Application and manifest baseline | Cloud complete, not locally verified | `2abd6f4` |
| P07 Notification listener foundation | Cloud complete, not locally verified | `2b2f335` |
| P08 Foreground announcement service and speech pipeline | Cloud complete, not locally verified | `84b46b1` |
| P09 Preferences foundation (`:core:datastore`) | Cloud complete, not locally verified | `996f9a0` |
| P10 History storage (`:core:database`) | Cloud complete, not locally verified | `de56968` |
| P11 Setup and onboarding (`:core:setup`, `:app`) | Cloud complete, not locally verified | `c2cb4d1` |
| P12 Dashboard and navigation shell (`:app`/`dashboard`) | Cloud complete, not locally verified | `3d4b09d` |
| P13 Applications feature (`:app`/`applications`) | Cloud complete, not locally verified | `474ee14` |
