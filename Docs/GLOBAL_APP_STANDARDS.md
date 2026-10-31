# Global Application Standards

Common Kotlin, Android, Compose, Material 3 Expressive, testing, accessibility, security and code quality standards. These apply to every module and every phase unless a newer user-approved decision overrides them.

## Kotlin and code design

- Kotlin only; no new Java sources. Idiomatic Kotlin: immutability first (`val`, immutable collections), no nullable platform types in public APIs.
- Small, focused classes and functions. SOLID where useful; prefer composition. Business logic is independent of Android and UI wherever the platform allows.
- No blocking of the main thread. No `Thread.sleep`, no synchronous I/O, no long-running work in composable or ViewModel initialisers.
- Structured concurrency: one `CoroutineScope` per lifecycle owner, cancelled at teardown; `SupervisorJob` for services; no global scopes.
- Sealed interfaces/classes for events, decisions and outcomes; exhaustive `when`.
- No swallowed exceptions. Catch only where recovery is possible, always log or surface the failure; never `runCatching {}.getOrNull()` around user-visible operations without recording why.
- `require`/preconditions for programmer errors; typed result/decision types for expected failures.
- KDoc on every public API and on any non-obvious rule or algorithm.
- No magic numbers: named constants for every threshold (windows, limits, delays).

## Architecture

- MVVM + unidirectional data flow. ViewModel exposes `StateFlow<UiState>`; UI emits typed events.
- Repository pattern between features and data sources. Single source of truth per datum.
- Hilt for DI with constructor injection; no service locator, no global mutable state.
- Room for structured data, DataStore for preferences. Never SharedPreferences for new work.
- Feature-oriented modules as defined in `AI_BUILD_ARCHITECTURE.md`; dependencies point downward only.

## Jetpack Compose and Material 3 Expressive

- Material 3 Expressive with dynamic colour where available; theme supports Light, Dark and System (default System).
- Stateless composables receiving state and callbacks; state hoisted to ViewModel.
- No business logic in composables; no direct repository/service calls from UI.
- Stable/immutable `UiState`; avoid `derivedStateOf` chains that hide state sources.
- Navigation Compose with type-safe routes where practical; single activity.
- No hardcoded user-facing strings — every visible string comes from resources with `stringResource` (keeps the app localisation-ready).

## Accessibility (mandatory for every UI phase)

- All interactive targets ≥ 48 dp with adequate spacing.
- TalkBack: meaningful `contentDescription`, correct semantics/role/state (toggles expose on/off, current value announced), no icon-only control without a label.
- Status conveyed by text/icon, never by colour alone.
- Supports large fonts (font scale up to 2.0 without clipping or overlap) and screen-reader-friendly reading order.
- Respects reduced-motion settings; no essential information in animations.
- Destructive actions (clear history, deselect all, restore) require confirmation with a clear description of the consequence.

## Notifications, services and background rules

- `NotificationListenerService` is the only primary notification source. Accessibility Service is not used for collection.
- Foreground service for ongoing playback with the correct `foregroundServiceType`; persistent notification exposes only user-configured actions.
- WorkManager only for deferrable work; never for real-time speech.
- Opening the app never silently starts the service; Start requires prerequisites, Stop is always available.
- All foreground-service, permission and receiver declarations have explicit `android:exported` values.

## Logging and diagnostics

- Structured local logs by category: `Notification`, `Rules`, `Queue`, `Audio`, `TTS`, `Service`.
- **Never log notification content, message text, OTPs, sender names or any user content.** Log identifiers, counts, enums and reasons only.
- Log levels configurable; default retention 7 days; diagnostics screen exposes state and local log export.
- No remote analytics, crash or logging SDK in v1.

## Security and privacy

- All data stays on device. No network permission is added unless a phase explicitly requires it (v1 does not).
- No secrets, credentials, API keys or signing material in Git.
- Backups are settings-only, user-confirmed, file-picker based, no history content.
- Optional biometric/device-credential gate and secure-screen mode are off by default.
- File writes use scoped storage APIs available at minSdk 29.

## Testing standards

- Every pure-logic change ships JVM unit tests in the same commit.
- Tests are deterministic: fixed clocks (`Clock` abstraction), no sleeps, no reliance on device locale/timezone.
- Table-driven tests for rule precedence; one test per documented rule and per skip reason.
- Room migrations get migration tests; DataStore logic gets round-trip tests.
- UI phases add Compose tests for the critical paths.
- Naming: `given_when_then` or `method_condition_expectedResult`; test names state the rule they prove.

## Build, quality and versions

- Version catalog (`gradle/libs.versions.toml`) is the single source for dependency versions; no hardcoded versions in module build files.
- One Gradle module per responsibility as in the architecture doc; module build files stay minimal and identical in shape to their peers.
- R8 enabled for release with reviewed keep rules; release signing outside Git.
- Static analysis (Detekt/Android Lint) added when the local toolchain is confirmed; findings must not be suppressed without a documented reason.

## Documentation and changelog discipline

- Requirements: master prompt. Plan: `AI_BUILD_PHASES.md`. Decisions: `TECHNICAL_DECISIONS.md`.
- Every phase updates the changelog and the correct progress file; the cloud agent updates only `AI_BUILD_PROGRESS_CLOUD.md`.
- Never claim a build, test run or device check that was not actually executed.
