# Directive: Phase 7 — Result UI & Polish

*Renumbered from the original Phase 6. This phase now follows Phase 6 — Cloud Source-Check, but is otherwise unchanged: it covers the on-device result presentation, scan history, and final signed release build. It does not depend on Phase 6's cloud feature to function — the on-device pipeline from Phases 3–5 remains the primary, always-available path.*

## Goal
Ship a feature-complete v1: a result UI that presents the verdict, confidence, and XAI evidence clearly; a scan history; and a signed release APK ready for sideload distribution.

## Inputs
- Working capture → inference → XAI pipeline from Phases 3–5
- Known model limitations from build plan §11 (must be surfaced, not just documented)
- A release keystore (generate now if it doesn't exist yet — see edge cases)
- (Context only, not a dependency) Phase 6's Cloud Source-Check feature, if built — see integration note below

## Tools / scripts to use
- Jetpack Compose bottom sheet / overlay card
- Room (scan history schema)
- `execution/build_signed_apk.sh` (or `.ps1`) for the final release build

## Outputs
- Result card: verdict (✅ Real / ⚠️ Suspicious / ❌ Fake) + confidence %, modality-specific evidence view (heatmap / highlighted text / waveform), action buttons (Dismiss / Save to History / Share Report)
- Room-backed scan history, browsable from the Main Dashboard
- Overlay permission messaging wired per Phase 5's directive
- A signed release APK, ready to distribute via sideload (e.g. GitHub Releases)

## Integration note — Cloud Source-Check (if Phase 6 was built)
If the Cloud Source-Check feature exists in the codebase by the time this phase starts:
- Its result card must remain **visually distinct** from the on-device result card built in this phase — different header color/icon/label (e.g. a cloud icon and "Cloud Source-Check" label vs the on-device shield icon), so a user never confuses a Gemini/Groq-backed verdict with an on-device model verdict.
- Cloud Source-Check results may optionally be saved to the same Room scan history table, but **must carry a `source: "cloud"` vs `source: "on_device"` field** (or equivalent) so history list items are clearly labeled and filterable.
- Do not let this phase's polish work quietly blend the two result types into one undifferentiated "scan result" concept — they represent different evidence with different trust guarantees, and the UI must keep that distinction legible.

## Edge cases — model limitations that must change UI behavior, not just appear in a comment
- **ADM-generated images (~80% accuracy vs. 95–100% on other generators):** implement an actual lower-confidence display band for this case — don't present it with the same visual confidence as a 97%+ result.
- **English text detection (ISOT-trained):** avoid in-app copy that over-claims accuracy for English text scans specifically; a general "results reflect model performance, not a guarantee" framing is safer than a bare percentage for this modality.
- **Audio spoof detection (validated only against ASVspoof attack types A01–A06):** consider a note in the result card or an info tooltip that newer/unseen synthesis techniques haven't been evaluated.
- **Arabic audio model (no enforced speaker-leakage control in training):** documented limitation — surfacing this directly in the UI is optional, but don't contradict it in marketing-style copy elsewhere in the app (e.g. don't claim it's been rigorously speaker-independent-tested).

## Other edge cases
- **No release keystore exists yet:** generate one via `keytool`, confirming with the user first — this file is irreplaceable once lost or overwritten, unlike any script or build artifact in this project.
- **Signed APK builds successfully but installs fail on a test device:** check for a signature mismatch against a previously-installed debug build of the same `applicationId` before assuming a build config error — Android won't allow installing over a differently-signed package with the same ID.
