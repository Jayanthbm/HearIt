# HearIt — Reusable Agent Start / Continue Prompt

Copy the prompt below into the AI coding agent you are about to use. Use it together with the repository's `Docs/` directory.

---

You are now working on the HearIt Android project. Before doing anything, read the documentation in the repository's `Docs/` directory.

First identify your role from the workflow document I provide:
- Local agent: follow `AI_BUILD_WORKFLOW_LOCAL.md`.
- Cloud agent: follow `AI_BUILD_WORKFLOW_CLOUD.md`.

Then read, in this order:
1. `AI_BUILD_MASTER_PROMPT.md`
2. Your role-specific workflow document.
3. `AI_BUILD_PHASES.md`
4. `AI_BUILD_PROGRESS_LOCAL.md`
5. `AI_BUILD_PROGRESS_CLOUD.md`
6. `AI_BUILD_ARCHITECTURE.md`
7. `GLOBAL_APP_STANDARDS.md`
8. `TECHNICAL_DECISIONS.md`
9. `MANUAL_TEST_CHECKLIST.md`
10. `CHANGELOG.md`

Project:
- Name: HearIt
- Application ID: `com.jayanth.hearit`
- Kotlin native Android app
- Minimum Android: API 29
- Repository project directory: `HearIt/`
- Shared working branch: `development`
- Stable branch: `main`

### Start / resume instructions

1. Inspect the repository root, current branch, Git status and recent commits.
2. Fetch the latest remote state and compare it with the local branch before editing.
3. Do not discard, reset, stash, overwrite or silently incorporate uncommitted user changes. If the working tree is not clean, stop and report the exact status.
4. Read both progress files and compare their phase status with Git history and the actual code.
5. Identify the next incomplete phase appropriate to your role. Do not assume that a progress-file checkbox alone proves implementation or verification.
6. Summarize:
   - Your role.
   - Current branch and working-tree status.
   - Latest relevant commit.
   - Last completed phase.
   - Next phase and its dependencies.
   - Any mismatch between progress documents and repository state.
7. Follow your role-specific workflow:
   - **Local agent:** implement one small phase, build and execute relevant tests, report results, update both progress files when locally completing a phase, commit, then stop and wait for my approval.
   - **Cloud agent:** implement/review small phases autonomously, add tests but never build or execute tests, update Cloud progress only, label all work Not locally verified, commit each phase and continue unless blocked by a product decision or unsafe repository state.
8. Do not implement the whole application in one pass. Keep changes small, tested where permitted, documented and committed.
9. Do not change product requirements based on assumptions. Ask me if a decision materially affects behaviour, privacy, permissions, data retention, compatibility or architecture.
10. Never claim tests/builds/device checks were performed unless they were actually executed. Distinguish static review, emulator testing and physical-device testing.

### Important coordination rule

The local and cloud agents are never run in parallel. The user coordinates handoff. Always synchronize with the latest `development` branch and progress files before starting.

Begin by reporting your repository/progress assessment and the exact next action. If your role is Local, wait for my approval of the proposed phase before implementing it. If your role is Cloud, proceed autonomously after confirming the repository state is safe.
