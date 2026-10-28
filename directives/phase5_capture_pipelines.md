# Directive: Phase 5 — Capture Pipelines

## Goal
Wire up two independent ways to get content into the inference pipeline: manual capture (always available, no special permissions) and overlay capture (permission-gated, additive — never required).

## Inputs
- Working inference engine + XAI post-processing from Phases 3–4
- Overlay service stub from Phase 1

## Tools / scripts to use
- Manual: standard Android gallery/file picker intents, Share Sheet intent filter, `AudioRecord` for mic input
- Overlay: `AccessibilityService` (text extraction), `MediaProjection` (screenshot), `AudioPlaybackCaptureConfiguration` (internal audio, min SDK 29 — always available on this app's floor)
- Unicode-script heuristic for the Language Router (Arabic block character check — no ML model)

## Outputs
- End-to-end manual flow: pick/paste/record → inference → XAI result, for all three modalities
- End-to-end overlay flow (when permissions are granted): scroll another app → capture via overlay → inference → XAI result
- Language Router correctly directs text/audio to the EN or AR model based on script detection

## Edge cases
- **Overlay permissions denied or revoked:** per build plan §6, the app must remain fully functional via the manual path with no blocking, nagging, or forced dead-ends. The exact Settings message when the user taps the overlay toggle without permission granted:
  > "Overlay permission not granted — you can still scan content directly from the app. Enable overlay in system settings to scan while browsing other apps."
  with a button deep-linking to the relevant system permission screen. If permission is revoked mid-session, the overlay should stop gracefully (no crash) and this same message should reappear next time the user tries to re-enable it.
- **Mixed-script or ambiguous text input** (e.g. a sentence mixing Arabic and Latin characters): implemented via `LanguageRouter` with a majority-script rule. If Arabic character count exceeds Latin, routes to `TEXT_AR` (AraBERT) and `VOICE_AR`; if Latin exceeds Arabic, routes to `TEXT_EN` (BERT-base) and `VOICE_EN`. If counts are equal, or if no script characters are present (digits/symbols only), defaults to Arabic as the primary Gulf language.
- **Do not gate any manual-path feature on overlay permission state** — these are two fully independent entry points into the same inference pipeline, not a primary/fallback pair.

## Status & Verification (Completed)
- `LanguageRouter`: pure Arabic, pure English, mixed majority, and digit/symbol fallbacks verified by `LanguageRouterUnitTest` (5/5 tests passing).
- `AudioCaptureManager`: 16-bit PCM normalization to 64,000 floats and WAV parsing verified by `AudioCaptureUnitTest` (2/2 tests passing).
- `MediaProjectionCaptureManager`: screenshot extraction via `ImageReader` & internal audio capture via `AudioPlaybackCaptureConfiguration` implemented natively on Android 10+ (API 29+).
- `YaqeenAccessibilityService`: passive node hierarchy text scraping on-demand when user taps floating shield overlay.
- `VerificationOrchestrator`: unified bridge connecting capture inputs to `ModelManager` (Phase 3), `FcWeightsLoader` + XAI post-processors (Phase 4), and `AppDatabase` history (Phase 1).
- `EndToEndIntegrationSmokeTest`: validated full capture $\rightarrow$ preprocessor $\rightarrow$ ONNX session $\rightarrow$ XAI CAM/attention generation pipeline for Image, Arabic Text, and Audio (3/3 tests passing).
- Total test suite: 7 test classes, 22 unit tests passing, zero errors.
