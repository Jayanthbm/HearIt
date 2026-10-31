# HearIt Changelog

All notable changes to HearIt are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Full documentation set: phase plan, architecture, global standards, technical decisions (ADRs), manual test checklist.
- Eligibility rules part 2 (P02): text-length limits (per message / whole notification, skip-never-truncate), case-insensitive whole-word/phrase keyword filtering (off by default), and privacy rules (exclude app, OTP, password, payment, banking, custom sensitive keywords, hide-content flag for P04) with ADR-006 precedence and Priority Announce unable to bypass them. JVM unit tests written — not yet executed (cloud-agent restriction).
- ADR revisions (2026-10-31, on user direction): ADR-006 accepted with precedence regrouped as device states (mute/call/DND) before content rules and keyword before length; ADR-015 accepted with v1 defaults OTP+password skip on, financial skips off, no text-length limit. Code, tests and KDoc updated to match.
- `:core:common` and `:core:rules` Kotlin/JVM modules (P01): notification snapshot, eligibility settings with per-app inheritance, runtime state, skip reasons, and the deterministic announcement-eligibility pipeline covering the enabled-app list, temporary mute and screen/lock with the Priority Announce override. JVM unit tests written for all three rule families — not yet executed (cloud-agent restriction).

### Changed
- Nothing yet.

### Notes
- Cloud-agent changes are **Not locally verified** until the local agent builds and tests them.

## [1.0.0] — not released

Initial release target: direct signed APK distribution, `versionCode 1`.
Feature set defined by `Docs/AI_BUILD_MASTER_PROMPT.md` (notification reading only; no voice commands, no backend, no analytics).

[Unreleased]: https://github.com/Jayanthbm/HearIt/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Jayanthbm/HearIt/releases/tag/v1.0.0
