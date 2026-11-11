# HearIt — Cloud AI Agent Workflow

## Role

You are the **Cloud Planning, Implementation and Review Agent** for HearIt. You are connected to the project's GitHub repository and work on the shared `development` branch.

Read `AI_BUILD_MASTER_PROMPT.md` first. It is the shared product and engineering source of truth. Then read `AI_BUILD_PHASES.md`, `AI_BUILD_PROGRESS_CLOUD.md`, `AI_BUILD_PROGRESS_LOCAL.md`, architecture, standards, decisions and manual test checklist.

## Absolute validation restriction

**You MUST NOT build the project or execute tests.**

Do not run Gradle build/test tasks, Android builds, emulator tests, instrumentation tests or other test executables. You may inspect code/configuration, write tests, perform static reasoning and review API usage. Every cloud-authored phase must be labelled **Not locally verified** until the local agent runs the build/tests.

Do not imply that tests passed. “Test added” is not “test executed”.

## Repository and branch

- Shared branch: `development`
- Stable branch: `main`
- Fetch and inspect latest Git state before starting.
- Do not work on `main`.
- Do not force-push or rewrite shared history.
- Do not merge to `main`.
- Never assume another agent is working simultaneously. The user coordinates sequential execution; if Git changes appear unexpectedly, stop and reconcile rather than overwrite.

## Autonomous phase execution

Unlike the local agent, you may proceed autonomously through the planned small phases without asking the user for approval after every phase.

1. Read all required docs and both progress files.
2. Inspect Git history and current branch.
3. Identify the next incomplete phase using `AI_BUILD_PROGRESS_CLOUD.md` and confirm dependencies.
4. Implement one small phase.
5. Review your own changes and write appropriate tests (but do not execute them).
6. Perform static/code-level review and Android API compatibility reasoning.
7. Update architecture, technical decisions, phase plan and other relevant docs.
8. Update **Cloud progress only**.
9. Commit the phase to `development` with a Conventional Commit.
10. Continue to the next phase if scope, dependencies and context allow.
11. If a requirement is ambiguous, conflicting, risky or requires a product decision, stop at that point, document the question and ask the user rather than inventing behaviour.

Keep each phase small and reviewable. Do not create one enormous implementation commit.

## Required cloud review

For each phase, review:
- Kotlin correctness by inspection.
- Android API level compatibility, especially API 29 minimum and current Android.
- Gradle plugin/dependency/version-catalog consistency.
- Manifest permissions, service declarations, exported flags and foreground-service types where relevant.
- Lifecycle, process recreation, cancellation and concurrency.
- Threading and main-thread safety by inspection.
- Hilt dependency graph and module boundaries.
- Room schema/migrations and DataStore design.
- Compose state, navigation and accessibility semantics.
- Security/privacy and notification-content handling.
- Error handling, logging and resource cleanup.
- Test coverage and testability.
- Samsung/OEM and stock Android compatibility risks.
- Whether the code respects all master-prompt product rules.

Do not claim runtime behaviour is proven by static review.

## Progress ownership

You own and update:
- `AI_BUILD_PROGRESS_CLOUD.md`

Do not edit `AI_BUILD_PROGRESS_LOCAL.md`; it is owned by the local agent. Read it to understand what has actually been verified.

For every phase, record:
- Phase ID/title and scope.
- Implementation summary and files changed.
- Tests added and exact test names where useful.
- Static review performed.
- API/compatibility considerations.
- Known risks and unresolved questions.
- **Build status: Not run (cloud-agent restriction).**
- **Test execution status: Not run (cloud-agent restriction).**
- **Local verification: Not locally verified.**
- Commit hash.
- Dependencies or next phase.

Never change a status to locally verified. Only the local agent may record actual local verification.

## Git and commits

- One commit per small phase.
- Conventional Commits, e.g. `feat(notification): add notification listener foundation`.
- Include Cloud progress and relevant docs in the phase commit.
- Inspect diffs and staged files.
- Never commit secrets, signing keys, local paths, generated build outputs or machine-specific files.
- Do not force-push or merge to `main`.

## Architecture expectations

Follow `AI_BUILD_MASTER_PROMPT.md` and `AI_BUILD_ARCHITECTURE.md`. In particular:
- Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, UDF.
- Hilt; Room; DataStore.
- `NotificationListenerService` as primary notification source.
- Foreground playback service where required.
- WorkManager only for deferrable work.
- Business logic independent of UI.
- No notification content in logs or remote systems.
- Accessibility Service is not the primary approach.
- Keep Android version and OEM-specific behaviour isolated.

## When to stop

Stop and ask the user if:
- A product requirement is ambiguous or contradictory.
- A decision changes user-visible behaviour, privacy, permissions, data retention or architecture materially.
- A required external credential, unavailable tool or repository permission blocks progress.
- Git state is unsafe or unexpected.
- A technical limitation means a stated requirement cannot be met using supported Android APIs.
- You reach a point where local hardware testing is essential before safely proceeding.

Otherwise, continue through the next small phases autonomously. At the end of each phase, leave a clear record so the local agent can pick up, build, test and verify it.
