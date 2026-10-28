# Directive: Phase 1 — Project Setup & Shell

## Goal
Stand up a buildable, installable Android app shell with the correct identity, signing scaffold, overlay service stub, and navigation — with no ML functionality yet.

## Inputs
- `applicationId`: `com.yaqeengulf.deepfake`
- `minSdk`: 29
- App name: Yaqeen Gulf
- A release keystore (see edge cases — may not exist yet)

## Tools / scripts to use
- Android Studio / Gradle CLI to scaffold the project (Kotlin, Jetpack Compose, Hilt, Room)
- `execution/build_signed_apk.sh` (or `.ps1`) for the eventual signed build — not needed to produce output yet, but the `signingConfigs` block should be wired now so Phase 6 doesn't require restructuring the build file

## Outputs
- A Gradle project with `applicationId = com.yaqeengulf.deepfake`, `minSdk = 29`
- `signingConfigs` block referencing a keystore path via `gradle.properties` (not committed)
- Floating Overlay Service implemented as a stub, requesting `SYSTEM_ALERT_WINDOW` at runtime
- Basic navigation: Main Dashboard (manual scan entry points — image/text/audio, non-functional placeholders are fine), Settings screen (overlay toggle, image-model selector placeholder — wiring comes in Phase 3)

## Edge cases
- **No release keystore exists yet:** don't block Phase 1 on this. Build and verify a debug-signed APK for now; generate the release keystore via `keytool` when you actually need a release build (Phase 6), and confirm with the user before doing so since a lost or overwritten keystore can't be recovered.
- **Overlay permission (`SYSTEM_ALERT_WINDOW`) prompt:** build the request flow now, but do not gate any other Phase 1 screen or navigation on the user granting it — this is a hard requirement carried through the whole project (build plan §6), not just a later-phase concern.
- **Do not wire real ML entry points yet** — Phase 1's dashboard buttons can navigate to empty/placeholder screens. Wiring them to actual capture flows is Phase 5.
