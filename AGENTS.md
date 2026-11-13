# Hear It — project working notes

## Verification
- Build: `./gradlew :app:assembleDebug --console=plain`
- Wear module: `./gradlew :wear:assembleDebug --console=plain`
- Install (debug device): `adb -s 192.168.31.106:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
- Do not commit or push unless the user asks.

## UI conventions (apply to every screen)

### Text must not be truncated by surprise
- Fixed headings/labels: `maxLines = 1` with ellipsis is fine.
- Explanatory subtitles: allow **2 lines** (`maxLines = 2`) with ellipsis only as a last
  resort. If a 2-line subtitle still truncates, **shorten the copy** instead of growing
  the layout.
- User-supplied text (device names, app names, keywords): up to 2 lines + ellipsis.
- If a label renders as "This phone's speak…", rename it (e.g. `Speaker`) rather than
  letting the ellipsis show. Static copy must never end with "…".

### Spacing tokens
- Screen: 16dp horizontal, 8dp vertical padding, 12dp between sections.
- Card: 16dp padding, 10dp gap after the header, 20dp corner radius.
- Section header: 28–32dp icon tile + `titleSmall` bold one-line title + 11sp subtitle.

### Colour
- Never hardcode `Color(0xFF…)` for surfaces, chips, badges or icons. Use
  `MaterialTheme.colorScheme` roles (`primary`, `primaryContainer`, `tertiary`, `error`,
  `onSurfaceVariant`). Semantic status colours must follow the theme.

### Copy
- One-line headings, max two-line subtitles, sentence case, no trailing ellipsis in
  static strings.

## Settings behaviour

### Dependencies between screens
- If a global switch makes a downstream option unreachable (OTP shield vs "Read codes
  clearly", "Hide message body" vs "Message content", Bluetooth category off vs a
  per-device switch), **disable the downstream control and say why**, with a link to the
  owning screen. Do not leave a toggle that silently does nothing.
- Keep one source of truth per rule. Blocklists, update handling and test-speech actions
  must not be duplicated in two screens.
- Settings that affect runtime belong in `HearItSettings` so backup/restore covers them.
  Anything kept only in `SessionStore` (e.g. Wear companion prefs) must be documented as
  not backed up.

### Repeated fixes
- When fixing a layout/copy/colour issue on one screen, check the other settings screens
  (Playback & Gestures, Global Eligibility Rules, Active Intelligence, Notification
  History, Backup & Restore, System Diagnostics, Voice & Speech, Devices) for the same
  pattern and fix it there too.
