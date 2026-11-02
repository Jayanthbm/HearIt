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
