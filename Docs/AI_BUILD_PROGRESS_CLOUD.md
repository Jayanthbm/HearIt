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

## Phase log

| Phase | Status | Commit |
|---|---|---|
| P00 Documentation foundation | Cloud complete, not locally verified | `b9d4b9a` |
| P01 Core domain foundation | Cloud complete, not locally verified | `6915b28` |
| P02 Eligibility rules (part 2) | Cloud complete, not locally verified | `bc29b2a` |
| P03 Eligibility rules (part 3) | Cloud complete, not locally verified | `5140f52` |
| P04 Announcement format and TTS contract | Cloud complete, not locally verified | `2b7d44b` |
