# Technical Decisions

Architecture Decision Records (ADRs), technical choices, alternatives and their reasoning.
Format: one ADR per decision — context, decision, alternatives, consequences. Newer user-approved decisions supersede older ones and must say so.

---

## ADR-001 — Native Kotlin + Jetpack Compose stack

- **Status:** Accepted (master prompt §16)
- **Context:** v1 Android-only app, phones, portrait-first, no backend.
- **Decision:** Kotlin, Jetpack Compose, Material 3 Expressive, Navigation Compose, MVVM, StateFlow/UDF, single activity, repository pattern, Hilt, Room, DataStore, coroutines.
- **Alternatives:** XML/Views + Fragment MVVM (more boilerplate, two UI paradigms); Flutter/RN (not native TTS/notification-listener friendly, violates "native Kotlin").
- **Consequences:** one UI paradigm end-to-end; Hilt/KSP wiring is a build-level dependency that must be verified locally.

## ADR-002 — Modules introduced incrementally, not as empty skeletons

- **Status:** Accepted, revisits recorded here
- **Context:** Master prompt proposes a feature/core module layout but warns against needless fragmentation.
- **Decision:** Modules follow `AI_BUILD_ARCHITECTURE.md`, but each is created in the same phase that first puts code in it. Pure logic goes to Kotlin/JVM modules (`:core:common`, `:core:rules`, `:core:queue`) so it is testable without an emulator; anything using Android APIs is an Android library.
- **Alternatives:** create all 16 modules up front (high build-wiring risk for an agent that cannot compile, empty modules rot); single `:app` module (business logic would be entangled with UI and untestable in isolation).
- **Consequences:** module list grows phase by phase; build files added by cloud are small and reviewable; local agent verifies compilation each handoff.

## ADR-003 — NotificationListenerService is the only collection source

- **Status:** Accepted (master prompt §3, §16)
- **Decision:** Android `NotificationListenerService` for notifications; native Android `TextToSpeech`; no translation layer.
- **Alternatives:** Accessibility Service (policy/privacy risk, explicitly excluded as primary approach); OEM APIs (not portable); floating-screen-read approaches (fragile, privacy-hostile).
- **Consequences:** user must grant notification access; listener receives callbacks on the main thread so extraction must stay cheap; TTS language support depends on the installed engine, which UI copy must state honestly.

## ADR-004 — Eligibility as a deterministic, side-effect-free rules pipeline

- **Status:** Accepted (master prompt §6)
- **Context:** Many interacting conditions (screen, lock, output, length, DND, calls, keywords, privacy, mute, priority mode) with required precedence.
- **Decision:** one pure function `evaluate(snapshot, settings, state) -> Decision` in `:core:rules`. Inputs are immutable, time comes from an injected `Clock`, output is `Announce | Skip(reason) | Queue(reason)`. No Android types inside. The pipeline never mutates queue/history state itself.
- **Implementation note (P01/P03):** `Announce | Skip(reason)` landed in P01; the `Queue(reason)` variant landed early in P03 because ADR-006 step 3 needs it for call-time queueing (originally planned for P05 — P03's scope requires the per-app call-queueing decision now). The queue machinery that consumes it, plus the injected `Clock`, still land in P05 (queue expiry needs time; nothing before it does).
- **Alternatives:** scattered `if` checks inside the service (untestable, precedence drifts); rule objects with hidden ordering (hard to audit).
- **Consequences:** every skip reason is explicit and observable in history; precedence is centralized in ADR-006; unit tests can cover every branch without a device.

## ADR-005 — Single shared queue with priority tiers

- **Status:** Accepted (master prompt §8)
- **Decision:** one pending queue shared across apps. Priority Critical > High > Normal > Low; a higher-priority pending item is placed ahead of lower-priority pending items immediately; equal priority is FIFO. Current speech is never interrupted by default. Grouping, delay, min gap, size limits, expiry and overflow (discard oldest / discard newest / keep highest priority) are queue concerns, not rule concerns.
- **Alternatives:** per-app queues (breaks "different apps use a shared pending queue"); interrupt-on-priority (explicitly not the default).
- **Consequences:** ordering is fully deterministic and unit-testable; discarded/expired items keep a history status + reason; any future "interrupt" setting must document its semantics before implementation (master prompt §8).

## ADR-006 — Rule precedence order

- **Status:** Accepted (refined by cloud agent on user direction, 2026-10-31; supersedes the P00 *Proposed* order)
- **Context:** The master prompt fixes some relations (Priority Announce overrides **only** screen/lock; queueing must not bypass rules; mute is a global override; never speak during a call; respect DND by default) but does not enumerate a full order. The order decides which reason history shows when several rules fail at once, so it is a user-visible product decision, not just an implementation detail.
- **Principle:** group rules by kind and report the most useful reason for *this* occurrence:
  1. **App eligibility** — is this app announced at all?
  2. **Transient device state** (mute → call → DND) — explains why *nothing* was said right now; these are cheap flags and the master prompt already forces mute ahead of content rules.
  3. **Content rules** (privacy → keyword → length) — explains persistent filtering of this app's content; within the group the most specific, user-authored reason wins (privacy flags, then the user's own keyword, then the generic length limit).
  4. **Environment** (screen/lock) — the only step Priority Announce bypasses.
  5. **Output routing** — evaluated only at the moment speech would start (and re-checked while speaking, ADR-011).
- **Decision:** evaluate in this order, first failure wins and returns its reason:
  1. App not enabled → `APP_DISABLED` (not announced; still recorded in history)
  2. Temporary mute active → `MUTED` (global override, applies in Priority mode too)
  3. Active phone call → `CALL_ACTIVE` (or `QueueForCall` once the per-app call-queueing setting and the queue land; a queued item is fully re-evaluated afterwards)
  4. DND active → `DND` (respected by default; a per-app override may lift it — never silently distinguished from a silent channel, both are checked against the notification's own policy)
  5. Privacy/protected-content rules → `PRIVACY_*`
  6. Hide-content-and-announce-sender mode → does **not** skip; it changes announcement content only
  7. Keyword filter hit → `KEYWORD_FILTERED`
  8. Exceeded text length → `TOO_LONG` (skip whole item, never truncate)
  9. Screen/lock mismatch → `SCREEN_ON` / `UNLOCKED` / … — **evaluated only in normal mode; Priority Announce apps bypass exactly this step**
  10. Active output not allowed → `OUTPUT_NOT_ALLOWED`
- **Alternatives considered:**
  - *Content rules before device states* (privacy first) — rejected: it would blame a permanent config for an occurrence whose real, transient cause is mute/call/DND, making history inconsistent with how mute already behaves.
  - *Length before keyword* (P00 order) — rejected on reason quality: an item that is both long and keyword-matched should name the user's own keyword, not a generic limit; cost differences are irrelevant at notification size.
  - *Output check before screen/lock* — rejected: screen/lock is the state the user is in *now* and explains why nothing was said, and keeping output last means Priority's bypass semantics stay trivial (it relaxes exactly step 9; step 10 still applies).
  - *Priority-of-concerns ordering* (privacy first, then mute) — rejected: "app disabled" must short-circuit everything, and the master prompt requires mute to beat content rules.
- **Consequences:** deterministic, unit-testable precedence; the shown reason is now always either a transient device state or the user's own most specific rule. Confirmed by the user on 2026-10-31.
- **Implementation note:** steps 1-5 and 7-10 are implemented (P01/P02/P03). Step 3 returns `Queue(CALL_ACTIVE)` instead of `Skip` when the per-app call-queueing flag is on (off by default); the queue must re-evaluate the original snapshot through the whole pipeline afterwards. Sub-order inside step 5 (first hit wins): excluded app → OTP → password → payment → banking → custom sensitive keyword. Step 6 (hide content) returns `Announce` and changes announcement content only (P04).

## ADR-007 — History collection is independent of eligibility

- **Status:** Accepted (master prompt §12)
- **Decision:** the listener records history for selected apps before/while rules run; announcement decisions annotate the entry's status and reason rather than gating whether it is stored.
- **Consequences:** history shows skipped items with actionable reasons; storage mode (full/metadata/off) and retention are the only privacy gates on history itself.

## ADR-008 — No backend, no accounts, no analytics in v1

- **Status:** Accepted (master prompt §2)
- **Decision:** local-only. No network permission, no SDKs for crash/analytics/ads/IAP. Backup = local file picker, settings only.
- **Consequences:** privacy story is simple and verifiable; no secrets to manage in Git; Play Store–compatible structure is kept for later without requiring it.

## ADR-009 — DataStore (Preferences) over SharedPreferences

- **Status:** Accepted (master prompt §16)
- **Decision:** Preferences DataStore for all settings, with typed accessors in `:core:datastore` and explicit per-app "inherit" representation rather than overloading defaults.
- **Alternatives:** SharedPreferences (callback-based, easy to misuse off main thread, discouraged); Room for settings (type-safe but heavier and schema-bound).
- **Consequences:** settings reads are suspendable flows; inheritance must be representable (global value vs explicit override) — modelled as a per-app override map with absence = inherit.

## ADR-010 — Foreground service with mediaPlayback type; explicit start only

- **Status:** Accepted (master prompt §4)
- **Decision:** one foreground service hosts queue + TTS + persistent notification (`foregroundServiceType="mediaPlayback"` where required by targetSdk). Start/stop only by explicit user action; boot recovery only within Android's restrictions and only for a session the user already started; WorkManager never used for speech.
- **Alternatives:** in-process-only speech (dies with UI), background starts (restricted on API 29+, OEM-fragile).
- **Consequences:** UI must surface permission/battery guidance honestly without promising OEM background guarantees; process-death handling must not auto-announce on recreate.

## ADR-011 — Stop speech immediately when eligibility stops matching

- **Status:** Accepted (master prompt §6, §9)
- **Decision:** while speaking, re-evaluate the *dynamic* conditions (screen/lock, active output, mute, DND, call). A mismatch stops playback immediately — no pause/resume, no speaker fallback. Static content rules are evaluated before speech starts.
- **Consequences:** requires a state observer wired into the service; disconnect of the selected earphone stops speech (master prompt §6 explicitly forbids pause/resume and speaker fallback).

## ADR-012 — Native TTS only, no translation

- **Status:** Accepted (master prompt §3, §7)
- **Decision:** text is passed unchanged to the installed engine; voice/language availability is detected and reported, unsupported language data degrades gracefully.
- **Consequences:** the app must never promise a language will be spoken; the test-announcement flow exists precisely so users can verify before relying on it.

## ADR-013 — Audio routing honesty

- **Status:** Accepted (master prompt §6)
- **Decision:** observe the active output and match it against the user's allowed set. Do not promise routing to a specific Bluetooth device unless the connected, supported APIs allow it; device list = previously connected devices + connected devices, plus a generic Wired option and an opt-in phone speaker.
- **Consequences:** `:core:audio` isolates OEM variance; feature copy and Diagnostics must explain limitations rather than hide them.

## ADR-014 — Verification split between cloud and local agents

- **Status:** Accepted (workflow docs)
- **Decision:** cloud writes code/tests/docs and performs static review only; it never builds or runs tests. Local builds, runs tests and performs emulator/device checks, then records verification in `AI_BUILD_PROGRESS_LOCAL.md`. Cloud work is labelled *Not locally verified* until then.
- **Consequences:** cloud commits must prefer well-known, low-risk build configuration and small phases; ambiguous product decisions stop the cloud agent and go to the user.

## ADR-015 — Default values for privacy rules and text length

- **Status:** Accepted (user direction on 2026-10-31: replace the conservative "everything off" proposal with better defaults; revisable in P15)
- **Context:** The master prompt §6 lists the privacy controls and a global maximum text length but fixes shipping defaults only for keyword filtering ("disabled by default") and phone speaker (off). Shipping every privacy toggle off — the original P02 proposal — left v1 with an empty privacy story: a hands-free app positioned around privacy would read a one-time code aloud.
- **Decision (v1 defaults):**
  - `skipOtp` **on** — detection is high-precision (explicit OTP terms, or a message that is nothing but a 4-8 digit code), and reading a code aloud is the single worst failure mode for an app used out of sight.
  - `skipPassword` **on** — high-precision whole-word terms (`password`, `passwd`, `passcode`, `credential`); password-reset notices should never be spoken.
  - `skipBanking` and `skipPayment` **off** — deliberately low-precision term families ("bank", "payment", "transaction") that routinely appear in alerts users *want* to hear (e.g. "payment received"); a default skip would silently hide exactly the notifications this app exists to announce. Users opt in globally or per app.
  - `excludeApp` **off**, `hideContent` **off** (§7's default format includes the message), `sensitiveKeywords` **empty**.
  - Text length: `maxLengthChars` **null (no limit)**, scope `MESSAGE` (§6's recommendation for messaging apps). Because the rule *skips* rather than truncates, a numeric default would silently *hide* long notifications — a worse failure than a long announcement — so v1 never hides on length; the settings UI should recommend ~500 characters (≈30-40 s of speech) instead.
  - Keyword filtering stays **off** (mandated by §6).
- **Alternatives considered:**
  - *Everything off* (original proposal) — rejected: no out-of-the-box privacy protection contradicts §6's intent.
  - *Everything on* — rejected: banking/payment false positives would drop routine alerts users actively want.
  - *Numeric length default (e.g. 500)* — rejected: with skip-never-truncate it becomes a silent-hiding rule the user never asked for.
- **Consequences:** v1 announces with OTP/password filtering out of the box, hides nothing for length, and never hides routine financial alerts; every value is one tap away in Settings and must be shown clearly there (P15). Detection stays explicitly imperfect (§6).

## ADR-016 — Per-app overrides inherit content rules as whole groups

- **Status:** Accepted (architecture)
- **Context:** Screen and lock inherit field-by-field (master prompt §5/§6 explicitly require independent inheritance). The text-length, keyword and privacy groups each hold several values.
- **Decision:** an app's override replaces a whole group or inherits it (null); values inside a group are never mixed between global and per-app. `EligibilitySettings.resolveWith` implements this uniformly for all groups.
- **Alternatives:** field-level merging inside groups (more flexible, but doubles the inheritance states the UI must explain and test).
- **Consequences:** the Applications screen can show one clear "Inherits global" state per group; changing granularity later is a settings-layer change only.

## ADR-017 — Device-state rules: call, DND and audio-output defaults

- **Status:** Accepted (P03; choices beyond the master prompt's explicit statements are flagged for confirmation below)
- **Context:** P03 implements ADR-006 steps 3, 4 and 10. The master prompt fixes "never speak during a call", "do not queue during calls by default", "respect Android DND by default; global and per-app override", "phone speaker disabled by default" and "announce only while the active output is allowed", but not what happens for unobserved or unclassifiable outputs, nor how DND stays distinct from silent channels.
- **Decision:**
  - **Call (step 3):** skip with `CALL_ACTIVE` always; a per-app `queueDuringCall` flag (default **off**) converts it to `Queue(CALL_ACTIVE)`. Queuing never bypasses anything: the queue re-evaluates the original snapshot through the **full** pipeline once the call ends (master prompt §6), which a test pins by re-evaluating into both `Announce` and `SCREEN_ON`.
  - **DND (step 4):** `respectDnd` defaults **true** (master-mandated) and inherits field-by-field like screen/lock, so a per-app override can lift it for exactly one app. The rule reads **only** `RuntimeState.isDndActive`; `NotificationSnapshot.channelId`/`isSilentChannel` are never consulted — a silent channel is a property of the notification, DND is a property of the device, and conflating them is a classic Android mistake (channel importance ≠ `getCurrentInterruptionFilter`).
  - **Output (step 10):** defaults allow wired + any Bluetooth, **phone speaker off** (master-mandated), unidentified OEM outputs **allowed**, and a null/unobserved `activeOutput` is treated as **allowed**.
- **Alternatives considered:** default-deny for unidentified outputs — rejected: OEM route names vary widely, and a default-deny would silently mute announcements on routes the user cannot recognize or fix; default allow only degrades when the user has explicitly narrowed the set. Treating an unobserved output as denied — rejected: capture gaps must not silence the app. Announcing-with-restriction based on channel silence — rejected: no master-prompt rule covers it, so `isSilentChannel` is modelled but currently a no-op in eligibility (open question below).
- **Consequences:** DND and call rules are never bypassable by Priority Announce (tested); output restriction is pure opt-in narrowing with one always-safe default (phone speaker off); P07 must map the interruption filter into `isDndActive` and channel importance into `isSilentChannel` faithfully.
- **Open question for the user:** should notifications on silent channels be announced, skipped, or left to the channel's own behaviour? Master prompt §6 only demands that they be *distinguished* from DND; no rule is implemented either way.
