# Graph Report - yaqeen-gulf  (2026-09-26)

## Corpus Check
- 95 files · ~311,137 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 751 nodes · 1213 edges · 43 communities (30 shown, 8 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 8 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- FloatingOverlayService
- VerificationOrchestrator.kt
- DashboardViewModel
- ImageModelType
- verify_onnx.py
- ModelManager
- AudioCaptureManager
- Yaqeen Gulf (يقين الخليج): Multimodal On-Device Deepfake & AI-Generated Media Verification System
- gradlew
- EndToEndIntegrationSmokeTest.kt
- Yaqeen Gulf — Finalized App Scope & Build Plan
- YaqeenAccessibilityService
- XaiPostProcessorUnitTest
- ModelInferenceVerificationTest
- Directive: Phase 6 — Cloud Source-Check (Opt-In)
- Agent Instructions — Yaqeen Gulf
- ContentCredentialsResult
- Yaqeen Gulf: Real-Time On-Screen Detection & Floating HUD Overlay Subsystem
- Directive: Phase 7 — Result UI & Polish
- Directive: Phase 2 — ONNX Export & XAI Tensor Export
- Directive: Phase 5 — Capture Pipelines
- LanguageRouterUnitTest
- Directive: Phase 0 — Graphify Setup
- Directive: Phase 1 — Project Setup & Shell
- Directive: Phase 3 — Inference Engine & Preprocessing (Android)
- Directive: Phase 4 — XAI Post-Processing (Android)
- ModelManagerMultiSwitchSmokeTest
- dump_xai_artifacts.py
- Phase 2 — ONNX Export & XAI Tensor Verification Report
- proxy.test.js
- export_fc_weights.py
- copy_onnx_to_assets.py
- rules/graphify.md
- workflows/graphify.md
- CloudCheckResult
- ImagePreprocessor.kt
- generate_thesis_figures.py
- vercel.json

## God Nodes (most connected - your core abstractions)
1. `DashboardViewModel` - 32 edges
2. `FloatingOverlayService` - 26 edges
3. `ImageModelType` - 20 edges
4. `ModelManager` - 20 edges
5. `ScanHistoryEntity` - 19 edges
6. `YaqeenAccessibilityService` - 16 edges
7. `SettingsViewModel` - 16 edges
8. `AppDatabase` - 15 edges
9. `Yaqeen Gulf (يقين الخليج): Multimodal On-Device Deepfake & AI-Generated Media Verification System` - 15 edges
10. `ModelType` - 14 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --references--> `SettingsViewModel`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/MainActivity.kt → app/src/main/java/com/yaqeengulf/deepfake/ui/settings/SettingsViewModel.kt
- `YaqeenNavHost()` --calls--> `SettingsScreen()`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/MainActivity.kt → app/src/main/java/com/yaqeengulf/deepfake/ui/settings/SettingsScreen.kt
- `YaqeenNavHost()` --references--> `SettingsViewModel`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/MainActivity.kt → app/src/main/java/com/yaqeengulf/deepfake/ui/settings/SettingsViewModel.kt
- `YaqeenApp` --references--> `AppDatabase`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/YaqeenApp.kt → app/src/main/java/com/yaqeengulf/deepfake/data/local/AppDatabase.kt
- `DashboardViewModel` --references--> `ScanHistoryEntity`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/ui/dashboard/DashboardViewModel.kt → app/src/main/java/com/yaqeengulf/deepfake/data/local/ScanHistoryEntity.kt

## Import Cycles
- None detected.

## Communities (43 total, 8 thin omitted)

### Community 0 - "FloatingOverlayService"
Cohesion: 0.14
Nodes (10): FloatingOverlayService, OnTouchListener, com, Context, IBinder, Intent, MotionEvent, Service (+2 more)

### Community 1 - "VerificationOrchestrator.kt"
Cohesion: 0.06
Nodes (24): AppDatabase, Context, Flow, ScanHistoryDao, ScanHistoryEntity, CropResolutionTier, TIER_1_ACCESSIBILITY_NODE, TIER_2_CV_CONTOUR (+16 more)

### Community 2 - "DashboardViewModel"
Cohesion: 0.05
Nodes (37): ImageCaptureManager, Bitmap, Context, Result, Uri, android, MainActivity, YaqeenNavHost() (+29 more)

### Community 3 - "ImageModelType"
Cohesion: 0.07
Nodes (22): Flow, PreferencesKeys, UserPreferencesRepository, ImageModelType, EFFICIENTNET_B3, SWIN_T, Modality, AUDIO (+14 more)

### Community 4 - "verify_onnx.py"
Cohesion: 0.09
Nodes (19): export(), export_audio.py Exports fine-tuned SpectrogramCNN models to ONNX: 1.…, SpectrogramCNN, SpectrogramCNNWithCAM, EfficientNetB3WithCAM, export(), export_image_effnet.py Exports fine-tuned EfficientNet-B3 to ONNX with explicit…, export() (+11 more)

### Community 5 - "ModelManager"
Cohesion: 0.07
Nodes (25): Bitmap, FloatArray, FloatBuffer, OrtEnvironment, OrtSession, ModelManager, ModelInferenceResult, ModelType (+17 more)

### Community 6 - "AudioCaptureManager"
Cohesion: 0.13
Nodes (14): AudioCaptureManager, ByteArray, Context, FloatArray, Result, Uri, Bitmap, FloatArray (+6 more)

### Community 7 - "Yaqeen Gulf (يقين الخليج): Multimodal On-Device Deepfake & AI-Generated Media Verification System"
Cohesion: 0.04
Nodes (47): 10.1 Zero-Billing Guarantee & Persistent Rate Limiting, 10.2 Server-Side Request Forgery (SSRF) Prevention (`ssrf.js`), 10.3 Indirect Prompt Injection Hardening, 10. Opt-In Cloud Source-Check Subsystem (`cloud_proxy/`), 11.1 Jetpack Compose Result Bottom Sheet (`ResultBottomSheet.kt`), 11.2 Persistent Storage (`AppDatabase.kt`), 11.3 Cryptographic Release Signing & Distribution, 11. User Interface, Scan History, and Release Packaging (+39 more)

### Community 8 - "gradlew"
Cohesion: 0.70
Nodes (4): gradlew script, die(), save(), warn()

### Community 9 - "EndToEndIntegrationSmokeTest.kt"
Cohesion: 0.09
Nodes (17): LanguageRouter, ScriptType, ARABIC, ENGLISH, AudioCamPostProcessor, FloatArray, AudioFcWeights, EffnetFcWeights (+9 more)

### Community 15 - "Yaqeen Gulf — Finalized App Scope & Build Plan"
Cohesion: 0.09
Nodes (22): 0. App identity, 10. Detailed implementation plan, 11. Known model limitations to surface in the UI (not blockers, but disclose), 12. Instructions for the AI coding agent, 1. Project overview, 2. Distribution: signed APK, not Play Store, 3.1 Checkpoint source paths, 3. Finalized model inventory (+14 more)

### Community 16 - "YaqeenAccessibilityService"
Cohesion: 0.14
Nodes (9): AccessibilityEvent, AccessibilityNodeInfo, AccessibilityService, Bitmap, com, Rect, YaqeenAccessibilityService, TakeScreenshotCallback (+1 more)

### Community 18 - "ModelInferenceVerificationTest"
Cohesion: 0.29
Nodes (3): OrtEnvironment, OrtSession, ModelInferenceVerificationTest

### Community 19 - "Directive: Phase 6 — Cloud Source-Check (Opt-In)"
Cohesion: 0.18
Nodes (10): Abuse Prevention & Prompt Injection Hardening, Architecture, Directive: Phase 6 — Cloud Source-Check (Opt-In), Edge cases, Goal, Inputs, Outputs, Stop condition (per build plan pacing rule) (+2 more)

### Community 20 - "Agent Instructions — Yaqeen Gulf"
Cohesion: 0.20
Nodes (9): 1. Two separate trees — do not conflate them, 2. Directives for this project, 3. Execution scripts to check for before writing new ones, Agent Instructions — Yaqeen Gulf, File Organization, Operating Principles, Self-annealing loop, Summary (+1 more)

### Community 21 - "ContentCredentialsResult"
Cohesion: 0.12
Nodes (18): C2paProvenanceManager, ByteArray, Context, Uri, ContentCredentialsResult, NotPresent, TamperedOrUntrusted, Verified (+10 more)

### Community 22 - "Yaqeen Gulf: Real-Time On-Screen Detection & Floating HUD Overlay Subsystem"
Cohesion: 0.08
Nodes (24): 1. System Architecture & Component Interactions, 2.1 WindowManager Layout Configurations, 2.2 Touch Event Processing & Edge Snapping Physics, 2. Window Manager Parameters & Lifecycle Mechanics, 3.1 Tier 1: Accessibility Node Tree Traversal, 3.2 Tier 2: Computer Vision Saliency & Luminance Variance Fallback, 3.3 Tier 3: Central Square Fallback, 3. Autonomous Three-Tier Image Localization & Cropping (+16 more)

### Community 23 - "Directive: Phase 7 — Result UI & Polish"
Cohesion: 0.22
Nodes (8): Directive: Phase 7 — Result UI & Polish, Edge cases — model limitations that must change UI behavior, not just appear in a comment, Goal, Inputs, Integration note — Cloud Source-Check (if Phase 6 was built), Other edge cases, Outputs, Tools / scripts to use

### Community 24 - "Directive: Phase 2 — ONNX Export & XAI Tensor Export"
Cohesion: 0.25
Nodes (7): Directive: Phase 2 — ONNX Export & XAI Tensor Export, Edge cases & Learnings, Goal, Inputs, Outputs, Per-model export requirements, Tools / scripts to use

### Community 25 - "Directive: Phase 5 — Capture Pipelines"
Cohesion: 0.25
Nodes (7): Directive: Phase 5 — Capture Pipelines, Edge cases, Goal, Inputs, Outputs, Status & Verification (Completed), Tools / scripts to use

### Community 27 - "Directive: Phase 0 — Graphify Setup"
Cohesion: 0.29
Nodes (6): Directive: Phase 0 — Graphify Setup, Edge cases, Goal, Inputs, Outputs, Tools / scripts to use

### Community 28 - "Directive: Phase 1 — Project Setup & Shell"
Cohesion: 0.29
Nodes (6): Directive: Phase 1 — Project Setup & Shell, Edge cases, Goal, Inputs, Outputs, Tools / scripts to use

### Community 29 - "Directive: Phase 3 — Inference Engine & Preprocessing (Android)"
Cohesion: 0.29
Nodes (6): Directive: Phase 3 — Inference Engine & Preprocessing (Android), Edge cases, Goal, Inputs, Outputs, Tools / scripts to use

### Community 30 - "Directive: Phase 4 — XAI Post-Processing (Android)"
Cohesion: 0.29
Nodes (6): Directive: Phase 4 — XAI Post-Processing (Android), Edge cases, Goal, Inputs, Outputs, Tools / scripts to use

### Community 32 - "dump_xai_artifacts.py"
Cohesion: 0.40
Nodes (3): magma_colormap(), process_audio_sample(), dump_xai_artifacts.py Uses PIL and onnxruntime directly without matplotlib to…

### Community 33 - "Phase 2 — ONNX Export & XAI Tensor Verification Report"
Cohesion: 0.33
Nodes (5): 1. Classifier Weights & Audio Head Architecture for CAM, 2. Real ONNX Runtime (JVM / Android Runtime) Execution Verification, Key Observations for Phase 3 Model Manager:, Overall Result: ALL 6 MODELS PASSED VERIFICATION (TOLERANCE < 1e-3), Phase 2 — ONNX Export & XAI Tensor Verification Report

### Community 34 - "proxy.test.js"
Cohesion: 0.08
Nodes (43): handler(), server, extractArticleText(), checkDailyKillSwitch(), checkIpRateLimit(), getRedisClient(), memoryStore, resetMemoryStore() (+35 more)

### Community 35 - "export_fc_weights.py"
Cohesion: 0.67
Nodes (3): export_weights(), export_fc_weights.py Exports classifier weights for CAM post-processing: 1.…, save_json()

### Community 39 - "CloudCheckResult"
Cohesion: 0.16
Nodes (4): CloudCheckResult, CloudSourceCheckRepository, Result, CloudSourceCheckUnitTest

### Community 40 - "ImagePreprocessor.kt"
Cohesion: 0.60
Nodes (3): ImagePreprocessor, Bitmap, FloatBuffer

## Knowledge Gaps
- **171 isolated node(s):** `PreferencesKeys`, `Verified`, `TamperedOrUntrusted`, `NotPresent`, `TIER_1_ACCESSIBILITY_NODE` (+166 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 293 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ImageModelType` connect `ImageModelType` to `VerificationOrchestrator.kt`, `DashboardViewModel`, `ModelManager`?**
  _High betweenness centrality (0.090) - this node is a cross-community bridge._
- **Why does `DashboardViewModel` connect `DashboardViewModel` to `FloatingOverlayService`, `VerificationOrchestrator.kt`, `ImageModelType`, `CloudCheckResult`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Why does `ScanHistoryEntity` connect `VerificationOrchestrator.kt` to `DashboardViewModel`?**
  _High betweenness centrality (0.038) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `ScanHistoryEntity` (e.g. with `.testScanHistoryEntityDefaultSourceTypeIsOnDevice()` and `.testScanHistoryEntityExplicitCloudSourceType()`) actually correct?**
  _`ScanHistoryEntity` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PreferencesKeys`, `Verified`, `TamperedOrUntrusted` to the rest of the system?**
  _171 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FloatingOverlayService` be split into smaller, more focused modules?**
  _Cohesion score 0.13725490196078433 - nodes in this community are weakly interconnected._
- **Should `VerificationOrchestrator.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.05711263881544157 - nodes in this community are weakly interconnected._