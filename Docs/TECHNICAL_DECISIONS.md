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

- **Status:** Accepted, decision for the silent-mode branch added on 2026-11-02 (user direction: phones on silent do not get spoken to).
- **Context:** The master prompt fixes call, DND and output rules loosely; the rest — and the silent-mode branch — was explicitly left open for confirmation because the right default changes the product character. P03 implemented the call/DND overrides and output matching without touching announcement *suppression*. On further user direction, silent mode is now confirmed as a suppression trigger: a phone on silent must not be spoken to.
- **Decision:**
  - **Silence suppression:** an announcement is suppressed when the device is in `RINGER_MODE_SILENT`, regardless of call/DND state. `VIBRATE` is *not* suppressed: the Android notification stream still plays the standard notification sound, and speaking a message while the device vibrates-only would contradict what the user already sees. (This is why `AUDIO_MODE_NORMAL` is not a suppression trigger: it is the default and carries no signal.)
  - **Priority override:** `RINGER_MODE_SILENT` cannot be overridden by Priority Announce — it means the user has deliberately silenced the device, and ADR-011's "stop speaking immediately" contract still applies as soon as the mode changes to silent.
  - **No new code path for `VIBRATE`:** nothing special is done for vibration.
  - **Declared permissions:** unchanged from P06 — no notification/DND/audio permissions are requested by the app; the device state is read read-only with `AudioManager`/`NotificationManager` APIs that need no permission on API 23+.
- **Alternatives considered:** suppressing in `VIBRATE` *and* `SILENT` (rejected: screen-reader and accessibility overlap, and vibrate-only users expect the NOTIFICATION sound); suppressing only in `DND` (rejected: DND is a preference, not a physical constraint — a silent phone still cannot be heard); querying the media session (rejected: the queue/TTS loop starts before the phone is used and media state does not track silent mode).
- **Consequences:** the queue's `pollReady` (and the P08 service loop, which delegates to it) must check the audio mode before returning an item; `AnnouncementQueue` becomes a pure function of its inputs plus `AudioManager` state, so this is fully testable with a stub `AudioManager`. The P07 listener stores the silent flag on each `NotificationSnapshot` to feed the decision; the P08 service loop re-checks on every poll and stops speech the moment the mode becomes silent (ADR-011).
- **Open items:** none from the user's confirmation. Per-channel policies (ADR-017's earlier open question) are not addressed by this decision and remain an explicit next question if the user wants them.

## ADR-018 — Announcement template is structured sections; `:core:tts` starts as a pure contract

- **Status:** Accepted (P04)
- **Context:** Master prompt §7 needs a customizable announcement format (app name, sender, message, custom introduction, pauses between sections, speed) while staying unit-testable without Android TTS. Pauses *between sections* cannot be expressed in one flat string, and missing sender/message must never produce dangling grammar ("Rahul says," spoken with nothing after it).
- **Decision:**
  - The template builder returns `BuiltAnnouncement(sections, pauseBetweenSectionsMillis)` — an ordered list of typed sections (`INTRODUCTION`, `APP_NAME`, `SENDER`, `MESSAGE`) — not a single string. The speaking layer synthesizes each section and inserts the configured gap (Android `playSilentUtterance`, API 21+, within the API 29+ floor) and applies `SpeechSettings(speechRate, pitch, voiceId)`. The default format reproduces §7's example exactly: "WhatsApp. Rahul says, Hey, are you coming today?"
  - Missing-content rules: absent/blank sender omits the whole section; a present sender without a message is spoken plainly ("Rahul.") so the connector never dangles; empty message omits its section; if every enabled section is empty the build is `isEmpty` and the speaker skips the item (an introduction alone still speaks).
  - Defaults: all three sections on, no introduction, `pauseBetweenSectionsMillis = 0` (no artificial delay; punctuation still paces speech), speed/pitch `1.0`, `voiceId = null` (engine default).
  - `:core:tts` is created as a **pure Kotlin/JVM** module holding the template builder and the `TextToSpeechEngine` contract (`isAvailable`, `listVoices`, `supportsLocale`, `speak`/`stop`/`shutdown`, `UtteranceListener` lifecycle with stable `TtsErrorCode`s). The Android `TextToSpeech` wrapper joins this module when P08 wires the speech pipeline; no Android plugin until then (ADR-002: modules introduced when their first code lands).
  - Language-data gaps surface as `LANGUAGE_DATA_MISSING` through the listener — never a silent fallback or translation (§7: native Android TTS only; Kannada text speaks only if an installed voice supports it).
- **Alternatives considered:** flattening sections into one punctuation-joined string — rejected: pauses would then be impossible without re-splitting later in the service. A template DSL with configurable connector wording ("says") — rejected: §7's configurable knobs are sections, intro, pauses and speed; wording belongs to string resources when the UI lands. Making `:core:tts` an Android library on day one — rejected: P04's contract has no Android types and the build wiring lands in P06/P08; an early AGP plugin would be unverifiable cloud-side.
- **Consequences:** every template branch is JVM-tested without an engine; P05 can extend `SectionKind` with a grouped-message count (§8); P08 implements the contract with utterance-id-based progress callbacks; the eligibility pipeline stays untouched (content building is never a skip rule).

## ADR-019 — Queue defaults and scheduling semantics

- **Status:** Accepted (master prompt §8; numeric defaults subject to product confirmation)
- **Context:** §8 fixes the queue's controls — grouping window, delay, max messages, min gap, priority tiers (Critical/High/Normal/Low), expiry, global/per-app size limits, overflow (discard oldest/newest, keep highest) and dedup of notification updates — but gives explicit defaults only for global 50, per-app 10, expiry 30 minutes and discard-oldest. The mechanical semantics (window measurement, eviction ties, expiry clock, gap reference point, what dedup means) had to be decided before `:core:queue` could be deterministic.
- **Decision:**
  - **Defaults (all settings-surface values the local agent can adjust):** grouping window 5 s, delay 0, max 3 messages per announcement, min gap 0, updates of announced notifications not repeated (`repeatNotificationUpdates = false`, §10), default app priority Normal.
  - **Grouping** runs at selection time over pending items: highest priority first (equal priority FIFO by arrival), then same-package companions measured from the first member's queue-entry time, window boundary inclusive, capped at `maxMessagesPerAnnouncement`; leftovers form the next announcement rather than being dropped. Per-app overrides change only the grouping window, with null inheriting the global value (ADR-016 pattern).
  - **Size limits count notification items, not grouped announcements**; the per-app limit is one global value applied per package (with discard-oldest it is exactly "retain latest N"); the global limit applies after per-app trimming. Shrinking limits via `updateSettings` re-enforces immediately.
  - **`KEEP_HIGHEST_PRIORITY` evicts the lowest priority; ties evict the newest first**, protecting items that already waited.
  - **Expiry ages from queue entry**, never from `postTimeMillis` (an item held through a call exists precisely to be announced later); call-held items age from hold time and restart their wait clock only if re-enqueued after release. The boundary is `>=`, announced-key dedup memory uses the same window, and held items are excluded from size limits but not from expiry.
  - **Min gap** measures from the previous announcement's completion to the next start; while an announcement is speaking the queue never returns another (never-interrupt default).
  - **History vocabulary:** accepted and held items emit `Queued` (a hold carries its `SkipReason`), an update replacing pending/held content emits `Replaced`, and expired/overflow/deduplicated items emit `Skipped` with the new reasons `EXPIRED`, `OVERFLOW` and `DUPLICATE_UPDATE` (added to the shared `SkipReason` in P05).
  - **`AnnouncementQueue` is a single-writer state machine** (architecture §3: one actor/scope, no locks), takes time only from the injected `Clock` in `:core:common`; dedup applies when `isUpdate && !repeatNotificationUpdates` and a same-key pending item is replaced in place, keeping its arrival order.
  - **Grouped announcements** are built by the list overload of `AnnouncementTemplate.build`: app name, then "N messages." (only when N > 1, always plural), then each sender/message in arrival order; single-package lists only.
- **Alternatives considered:** ageing from `postTimeMillis` — rejected: it punishes items held through a call, which exist to be announced later. Interrupting speech for a higher-priority arrival — rejected: §8 makes never-interrupt the default. Counting limits per group — rejected: limits would silently depend on grouping config. A separate dedup window — rejected: no value is specified anywhere, so the expiry window is the one queue-age number §8 gives. A format toggle for the message-count section — rejected: §8 mandates the count for grouped announcements and §7's configurable list does not include it.
- **Consequences:** every scheduling rule is deterministic and JVM-unit-testable; history never loses discarded or expired items; P08's service loop is a thin driver (poll → speak → markStarted/markCompleted → release holds); defaults can be tuned from settings without touching queue logic.

## ADR-020 — Hilt/KSP foundation and manifest baseline

- **Status:** Accepted (master prompt tech stack; versions current at time of writing)
- **Context:** P06 wires Hilt before any injected component exists, and commits the manifest baseline while the services that belong in it arrive in P07 (listener, ADR-003) and P08 (announcement, ADR-010). Version pins matter because the cloud agent cannot build to verify dependency resolution.
- **Decision:**
  - **KSP 2.3.13 + Hilt 2.60.1**, wired conventionally: catalog entries, `apply false` at the root, `alias(libs.plugins.ksp)`/`alias(libs.plugins.hilt)` in `:app`, `implementation(libs.hilt.android)` + `ksp(libs.hilt.compiler)`. KSP 2.3.x is decoupled from the Kotlin version and explicitly supports Kotlin 2.2.10 and AGP 9 built-in Kotlin (the AGP 9 R-class fix landed in 2.3.10); Dagger declares KSP support stable from 2.60 with KSP ≥ 2.3.9; Hilt 2.59+ requires AGP 9 (project is on 9.3.3) and raises minSdk to 23 (project is on 29). No kapt anywhere: `org.jetbrains.kotlin.kapt` is incompatible with AGP 9 built-in Kotlin.
  - **No `org.jetbrains.kotlin.android` in `:app`** — AGP 9 enables built-in Kotlin by default and fails if the legacy plugin is also applied; the existing layout already relies on this.
  - **The Hilt Gradle plugin is applied** (optional with KSP, but without it components would need generated base-class forms such as `@HiltAndroidApp(MultiDexApplication::class)`), so components use the plain `@HiltAndroidApp` / `@AndroidEntryPoint` annotations.
  - **Manifest baseline:** `.HearItApplication` registered; both service *slots* declared ahead of their classes with `tools:ignore="Instantiatable"` and comments fixing the paths P07/P08 must create (`com.jayanth.hearit.core.notifications.HearItNotificationListenerService`, `.service.AnnouncementService`). Listener: `exported="true"` + signature-level `BIND_NOTIFICATION_LISTENER_SERVICE` — the mandatory pattern on API 29–37; only the system binds, and only after the user grants notification access (ADR-003, setup flow). Announcement: `exported="false"`, `foregroundServiceType="mediaPlayback"` (attribute exists from API 29 = minSdk floor, enforced from targetSdk 34) — ADR-010.
  - **Only install-time NORMAL permissions land now** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — required by the mediaPlayback type and silently granted, so nothing is "requested" from the user). `POST_NOTIFICATIONS` and `BLUETOOTH_CONNECT` are deliberately deferred until the setup flow that explains them exists (P06 acceptance criterion: no permission requested before it is explained by setup flow).
- **Alternatives considered:** declaring the service slots only when their classes exist (P07/P08) — rejected: the phase scope puts the listener slot and foreground-service types in P06 so the exported/permission/type review happens once, up front; dormant declarations cannot be bound or started before notification access is granted or the app starts the service. Dropping `tools:ignore` — rejected: a full `lint` run would otherwise fail on `Instantiatable` for the intentionally-later classes. kapt — rejected: incompatible with AGP 9 built-in Kotlin. Pinning KSP in the old `2.2.10-2.0.2` scheme — rejected: that KSP1 line is removed/unsupported and predates AGP 9.
- **Consequences:** the Hilt graph can grow from P07 onward without build-file churn; P07 must land its listener class at the manifest path (or edit the manifest); P08 must land `.service.AnnouncementService` before starting the FGS and must still honour API 34+ runtime type rules; dangerous permissions arrive later, each with its setup-flow explanation; the local agent must confirm dependency resolution and Hilt/KSP processing on first build.

## ADR-021 — Settings persistence: flat DataStore preferences with explicit inheritance

- **Status:** Accepted (P09; master prompt section 16 "DataStore for preferences")
- **Context:** Settings shape the whole pipeline (rules, queue, format, speech, history) and must expose "inherits global" per condition/group, survive restarts, and stay testable without a device. P08 shipped placeholders (`SettingsSource` returning all-disabled defaults, `SessionStore` with SharedPreferences flags) only because the storage layer did not exist yet.
- **Decision:**
  - **DataStore (Preferences) 1.3.0** is the only settings store; Room stays reserved for history (P10). Coroutines stay a **transitive** dependency of DataStore rather than a pinned version: coroutines 1.11.0 is built against Kotlin 2.2.20 while this project compiles with 2.2.10, which risks "compiled with a newer Kotlin" metadata errors. If the local build reports that, bump KGP to 2.2.20 (AGP 9 allows >= 2.2.10) rather than downgrading DataStore.
  - **Flat, string-only key schema** owned by `SettingsCodec` (pure, JVM-tested): global keys `elig.*` / `queue.*` / `format.*` / `speech.*` / `history.*` / `apps.*`; per-app keys `app:<package>:<field>` (packages cannot contain `:`, so `split(':', limit = 3)` is unambiguous); lists joined by the ASCII unit separator (stripped from values on encode); `version` for future migrations.
  - **An absent per-app key means "inherit"** - the storage-level expression of "Inherits global" (master prompt section 5). Whole-group overrides (text length, keywords, privacy, output) are detected by their marker key and decoded as complete groups with model defaults for missing fields, because the encoder always writes the whole group. `AppOverride.inheritsEveryValue` is therefore a pure absence check, with no parallel flags to drift.
  - **Tolerant decode:** unknown keys are ignored; unknown enum names and invalid numbers fall back to documented defaults, so a corrupted or newer store can never crash the app.
  - **Aggregate model lives in `:core:datastore`** (`HearItSettings`) because it combines `:core:common` models with `:core:tts` format/speech. Typed accessors (`resolvedFor`, `queueFor`, `historyFor`, `priorityFor`) apply inheritance so no consumer merges by hand.
  - **New `:core:common` models:** `HistorySettings`/`AppHistoryOverride` (section 12 storage modes, update handling, retention global-only, optional auth default off) and `AudioFocusPolicy` (DUCK default / PAUSE / NONE) added to `EligibilitySettings` and its per-app override, closing the P08 audio-focus settings gap.
  - **Session/runtime flags stay out of settings:** `SessionStore` (explicit-start flag, temporary mute) remains separate - it is runtime session state, not a user preference, and the service start path needs it synchronously without a settings read. Revisit if P16 turns mute into a real preference.
  - **Update semantics:** `SettingsRepository.update` is a read-modify-write for single-writer use from the settings UI; concurrent writers are out of scope until a screen can write settings simultaneously (a compare-and-set store can be added behind `PreferenceStore` without changing the codec).
- **Alternatives considered:** one JSON blob per settings object (rejected: no partial observability and a silent format coupling); Room for settings (rejected: section 16 reserves Room for history); raw per-field DataStore keys with no codec (rejected: no single pure place to prove defaults, inheritance and round-trips); per-app overrides stored as full copies (rejected: destroys "inherit" semantics the UI must show); explicitly pinning coroutines (rejected for now: metadata risk outweighs the small benefit).
- **Consequences:** settings round-trip and defaults are proved by pure JVM tests; adding a setting means touching the codec and its tests; `:app` keeps `SettingsSource`/`SessionStore` placeholders until P10/P13 wire the repository (no unused dependency in `:app` yet); `SCHEMA_VERSION` must be bumped with a migration once key meanings change.

## ADR-022 — History storage: Room schema, storage modes and migration policy

- **Status:** Accepted (P10; master prompt section 12) — Room major-version choice explicitly flagged for user/local confirmation below.
- **Context:** History must record **every** selected-app notification regardless of eligibility (ADR-007), honour full-content/metadata-only/disabled storage modes per app (section 12), keep update-vs-new handling configurable, support filtering/pagination later (P18) and never silently lose data. Room is mandated by section 16.
- **Decision:**
  - **Room 2.8.5** (`androidx.room`) with **KSP** (`ksp(room-compiler)`), no Room Gradle plugin; schemas are exported through a `CommandLineArgumentProvider` (`room.schemaLocation`) so the schema directory stays a tracked Gradle input rather than a caching hazard.
  - **Room 3 track recorded, not taken yet:** `androidx.room3` 3.0.3 is the current stable line (Room 2.x is in maintenance mode), with a new package, mandatory `SQLiteDriver`/`BundledSQLiteDriver`, KSP-only tooling and a generic builder API. In the cloud the code cannot be compiled, and a schema authored against an API surface I cannot verify is a far larger risk than choosing the well-known 2.8.5 annotations. **The switch is isolated to build.gradle.kts plus the annotations/imports and `HistoryDatabase.create`** — domain models, policies and repository logic do not reference Room. Revisit before P18 (history UI) or whenever the local agent can build.
  - **One `history_entries` table** (`id` autoincrement; `key` notification identity; app/sender/content; `postTimeMillis`; `recordedAtMillis`; `status`/`reason`/`priority` as enum **names**; `isUpdate`) with indices for newest-first paging, app+status filtering and newest-per-key lookup.
  - **Storage modes:** `DISABLED` → no row at all; `METADATA_ONLY` → `messageText` null, sender/app/times/status kept; `FULL_CONTENT` → body stored verbatim (never truncated; blank becomes null).
  - **Update handling:** `UPDATE_ENTRY` rewrites the newest row for the same key, `NEW_ENTRY` inserts; `isUpdate` is persisted either way so updates stay distinguishable from new messages (section 12).
  - **Retention:** cutoff = `now - retentionDays * 86_400_000`; `null` retention means Never and prunes nothing. Retention is global (P09 ADR-021); per-app only chooses storage mode/update handling.
  - **Migration policy:** `exportSchema = true` with schemas committed; every schema change bumps the version **and** adds an explicit `Migration` with a migration test. **No `fallbackToDestructiveMigration()`** — history is sensitive and must never be dropped silently.
  - **Pagination:** a single query with nullable `packageName`/`status` filters, newest-first, page size capped at 500 (`HistoryQuery`), so the UI can lazy-load without loading the table.
  - **Layering:** `HistoryEntry`, recorder/retention/query policies and enum mapping are pure JVM code; DAO/database/repository are thin Android glue (the repository reads time only from the injected `Clock`). No Hilt wiring yet — no consumer exists until the history feature (P18) or a service write path is added.
  - **Sensitivity:** history content is never logged; clear-individual/clear-all are first-class API; optional authentication stays a P18 UI concern (default off, section 12).
- **Alternatives considered:** Room 3.0.3 now (deferred — see above: unverifiable API surface in the cloud); destructive migration fallback (rejected: silent data loss); storing enums as ordinals (rejected: brittle across enum edits); one row per status transition (rejected for v1: section 12 describes one entry with a final status; a separate event log can be added later without changing this table); SQLDelight (rejected: section 16 mandates Room).
- **Consequences:** the schema and its queries are statically reviewable but unproven until the local build exports `schemas/`; DAO and migration tests need the exported schema JSON and belong to the local agent (documented in the P10 progress record); when Room 3 is adopted, only build wiring plus this module's Room-facing files change.

## ADR-023 — First-launch setup flow: pure gating core, persisted progress, explicit Start

- **Status:** Accepted (P11; master prompt §4, P06 acceptance criterion).
- **Context:** First launch must walk nine steps, explain every permission before requesting it, allow safe skips, survive process death without losing progress, never start the service silently, and enable Start only when the app could actually announce. The cloud agent cannot run Compose or instrument anything, so as much of the flow as possible has to be pure and JVM-testable.
- **Decision:**
  - **New pure JVM module `:core:setup`** owns the nine `SetupStep`s, the per-step prerequisites (`SetupPrerequisites`), typed blockers (`SetupBlocker`), navigation/skip rules and Start gating (`SetupFlow`) and the persisted-state codec (`SetupStateCodec`). `SetupFlow` is the single authority for "can advance" and "can Start"; the Compose layer only renders its answers (global standards: no business logic in composables).
  - **Step order and skip rules** are the master prompt's: welcome, notification access, Bluetooth permission, TTS, battery guidance, app selection, audio outputs, test announcement, review. Bluetooth is hidden below API 31; Bluetooth, battery guidance, outputs and test are skippable because their default state is already safe; the rest are required.
  - **Start prerequisites** (re-checked when Start is pressed): notification access, TTS engine **and** device-language data, at least one enabled app, at least one allowed output category. The Bluetooth permission, battery acknowledgement and the test announcement never gate Start because none of them prevents speech.
  - **Persistence:** a second DataStore file (`hearit_setup`) reached through the existing `PreferenceStore` seam, holding `SetupState` (completed, current step, visited steps, skipped steps, battery acknowledgement). The codec is tolerant: unknown keys/enum names and invalid booleans decode to documented defaults so a newer or corrupted store cannot break launch. `SessionStore.isStarted` stays in SharedPreferences (ADR-021) because the service needs it synchronously.
  - **Service start only on the explicit Start:** `SetupLauncher.startAnnouncements()` sends `AnnouncementService.ACTION_START` after setup completion and the session flag are persisted. No lifecycle callback, receiver or screen entry starts the service; returning users go to the dashboard (placeholder until P12).
  - **Permission timing:** `POST_NOTIFICATIONS` (API 33+) and `BLUETOOTH_CONNECT` (API 31+) are now declared, and each is explained on its own step before the request is launched; notification access is granted through the system listener screen and refreshed on `ON_RESUME`; no permission is requested anywhere else. Battery guidance opens the system battery-optimisation list (no direct-request permission) and promises nothing about guaranteed background execution.
  - **App selection** uses a launcher-activity query with a manifest `<queries>` element: a deliberately narrower catalog than P13's notification-capable discovery, enough to enable apps at setup without pre-empting that phase.
  - **Test announcement** speaks a fixed resource string straight through `TextToSpeechEngine` — it never touches the queue or history (master prompt §4 step 8).
  - **Platform seams:** `:app` defines small probe interfaces (notification access, `POST_NOTIFICATIONS`, `BLUETOOTH_CONNECT`, app catalog, connected outputs, launcher) with tiny Android implementations, provided as process singletons by a new `SetupModule` alongside the two repositories and the session store. `:core:*` stays Hilt-free.
  - **Accessibility:** each selectable row is a single toggle (`toggleable` with `Checkbox`/`Switch` passing `onCheckedChange = null`), rows are ≥48 dp, status is always text (never colour alone), the app list is lazy, and every visible string is a resource.
- **Alternatives considered:** a Navigation Compose graph now (rejected: one conditional destination is enough until P12 owns the shell); storing setup progress inside `HearItSettings` (rejected: it is UX state, not a preference, and a separate file keeps the settings flow from invalidating it); requesting `POST_NOTIFICATIONS` at first launch without explanation (rejected: violates the P06 criterion); blocking Start on the Bluetooth permission (rejected: announcements work without it, and skipping is explicitly allowed); toasts/snackbars for blockers (rejected: persistent inline text is more accessible); `androidx.hilt` navigation artifacts (not needed — the activity scopes the ViewModel).
- **Consequences:** flow gating, skip rules, Start eligibility and the state codec are JVM-tested (28 setup tests + 4 repository tests, written but not executed in the cloud); the Compose UI, permission launchers, lifecycle refresh and every probe remain unverified until the first build; `:app` now depends on `:core:setup` and the settings/setup repositories; `SettingsSource` still reports every app disabled, so starting the service does not yet announce anything (P13 wiring) while the test announcement proves the TTS path; the app catalog stays launcher-only until P13 broadens it.

## ADR-024 — Dashboard shell: saveable destination state, read-only service observation, no new dependencies

- **Status:** Accepted (P12; master prompt §13 and §16, architecture §6).
- **Context:** Returning users must land on a dashboard with bottom navigation (Home, Apps, Devices, History), secondary entries (Global Rules, Voice & Speech, Settings, Backup & Restore, Diagnostics), the master-prompt §13 sections and live service status; navigation state must survive process recreation, every target must be ≥48 dp and TalkBack semantics must hold. Opening the app must never start the service. The cloud agent cannot compile Compose, resolve new artifacts or run an emulator.
- **Decision:**
  - **Where the shell lives:** `:app`'s new `dashboard` package (Compose screen, route, ViewModel, models, service seams) plus one Hilt module. The master prompt's `:feature:dashboard` module is **deferred, not dropped**: the state it needs (`SessionStore`, and now `ServiceStatusMonitor`) is `:app`-owned because it is service lifecycle state (ADR-021), so extracting the module now would require moving those seams into a core module first. The move is mechanical once another feature needs them; the architecture table records the current location.
  - **Navigation:** a flat `DashboardDestination` enum is the single source of tabs, secondary entries, labels and stable ids; `DashboardRoute` keeps the selected id in `rememberSaveable` and resolves it through a total `fromId` that falls back to Home. `BackHandler` returns to Home from a secondary screen. **No Navigation Compose graph yet:** no destination takes an argument or needs a nested back stack, and a navigation artifact version cannot be resolved or compiled in the cloud, whereas saveable state already satisfies the acceptance criterion (it survives configuration change *and* system-initiated process death). The graph replaces this when P13/P14 add parameterised routes.
  - **No new dependencies at all:** the bottom bar is `Surface` + `selectable(role = Role.Tab)` columns instead of Material `NavigationBar` (whose item requires an icon, and the icon set containing Devices/History glyphs is a separate unverifiable artifact); text labels are the accessible names, selection is exposed as semantics *and* bold weight + colour so status is never colour-only; secondary entries are ≥48 dp clickable rows.
  - **Service observation is a read-only seam:** `ServiceStatusMonitor` exposes a `StateFlow<ServiceSnapshot>` (running, speaking, pending count) that **only** `AnnouncementService` writes — `markRunning()` when it reaches the foreground, `markStopped()` on explicit stop and in `onDestroy`, `updateSpeech(...)` after every tick. The dashboard never polls `ActivityManager`, never infers a start, and cannot start anything implicitly; Start/Stop call `ServiceControl`, which sends the existing P08 `ACTION_START`/`ACTION_STOP` (Stop is sent as a command so the persisted session flag is cleared).
  - **Privacy on the dashboard:** the snapshot and the recent-history rows carry no message text or sender — the dashboard shows app name, outcome and relative time, and the speaking section shows state plus queue depth only. The dashboard is the most screen-visible surface, so it stays safe even with full-content history enabled; content display belongs to P18 behind its own settings.
  - **Honest partial sections:** Priority Announce and temporary mute render as read-only status with copy saying their controls arrive with their feature updates (P17/P16), rather than dead toggles. Secondary screens render a placeholder naming the future feature, so navigation, back handling and accessibility can be verified now.
  - **Recent history is real but empty:** `DashboardModule` provides the first `HistoryDatabase`/`HistoryRepository` bindings (wall clock, because recorded times and retention are calendar times), and the dashboard reads the newest five rows. History writes are still unwired (P18), so a fresh install shows the empty copy; a read failure is surfaced as `historyUnavailable` instead of being swallowed.
  - **Packaging repair:** `:app` was missing `:core:datastore` and `:core:setup` (imported by P11 code) — added here together with `:core:database`.
- **Alternatives considered:** Navigation Compose now (rejected above); a `:feature:dashboard` Android library now (deferred — would force moving service-state seams out of `:app`); Material `NavigationBar` + `material-icons-extended` (rejected: new unverifiable dependency, and icons add no meaning over labels); persisting the tab in DataStore (rejected: navigation is transient UI state, and restoring it across a cold *launch* would be surprising); showing message text in recent history (rejected: privacy); auto-starting the service when the dashboard opens (rejected: master prompt §4 forbids silent start); starting with a `PENDING`/`STARTING` status instead of a two-state one (rejected: the monitor is exact in-process, so a dead service must simply read as stopped).
- **Consequences:** the shell, its accessibility semantics and every Compose API used here remain unverified until the local build; the monitor is in-process only, which is correct for a single-process app but would need a different mechanism if playback ever moved to a separate process; the dashboard's Priority Announce and mute rows show fixed values until P16/P17 land; recent history stays empty until recording is wired; `DashboardDestinationTest`/`ServiceStatusMonitorTest` are written but not executed.
