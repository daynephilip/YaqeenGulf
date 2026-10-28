# Graph Report - yaqeen-gulf  (2026-09-09)

## Corpus Check
- 82 files · ~207,778 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 557 nodes · 893 edges · 41 communities (29 shown, 7 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- FloatingOverlayService
- VerificationOrchestrator.kt
- DashboardViewModel
- ImageModelType
- verify_onnx.py
- ModelManager
- AudioCaptureManager
- ImageCamPostProcessor
- gradlew
- EndToEndIntegrationSmokeTest.kt
- Yaqeen Gulf — Finalized App Scope & Build Plan
- XaiPostProcessorUnitTest
- ModelInferenceVerificationTest
- Directive: Phase 6 — Cloud Source-Check (Opt-In)
- Agent Instructions — Yaqeen Gulf
- AudioMelPreprocessor
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
- check.js
- export_fc_weights.py
- copy_onnx_to_assets.py
- rules/graphify.md
- workflows/graphify.md
- CloudCheckResult
- .loadBitmapFromUri
- dependencies
- vercel.json

## God Nodes (most connected - your core abstractions)
1. `ImageModelType` - 19 edges
2. `ModelManager` - 19 edges
3. `DashboardViewModel` - 19 edges
4. `WordPieceTokenizer` - 15 edges
5. `FloatingOverlayService` - 15 edges
6. `SettingsViewModel` - 15 edges
7. `AppDatabase` - 14 edges
8. `Yaqeen Gulf — Finalized App Scope & Build Plan` - 14 edges
9. `ModelType` - 13 edges
10. `ScanHistoryEntity` - 12 edges

## Surprising Connections (you probably didn't know these)
- `MainActivity` --references--> `SettingsViewModel`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/MainActivity.kt → app/src/main/java/com/yaqeengulf/deepfake/ui/settings/SettingsViewModel.kt
- `YaqeenNavHost()` --references--> `SettingsViewModel`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/MainActivity.kt → app/src/main/java/com/yaqeengulf/deepfake/ui/settings/SettingsViewModel.kt
- `YaqeenApp` --references--> `AppDatabase`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/YaqeenApp.kt → app/src/main/java/com/yaqeengulf/deepfake/data/local/AppDatabase.kt
- `DashboardViewModel` --references--> `ScanHistoryEntity`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/ui/dashboard/DashboardViewModel.kt → app/src/main/java/com/yaqeengulf/deepfake/data/local/ScanHistoryEntity.kt
- `DashboardViewModel` --references--> `ImageModelType`  [EXTRACTED]
  app/src/main/java/com/yaqeengulf/deepfake/ui/dashboard/DashboardViewModel.kt → app/src/main/java/com/yaqeengulf/deepfake/domain/model/ImageModelType.kt

## Import Cycles
- None detected.

## Communities (41 total, 7 thin omitted)

### Community 0 - "FloatingOverlayService"
Cohesion: 0.11
Nodes (12): AccessibilityEvent, AccessibilityNodeInfo, AccessibilityService, YaqeenAccessibilityService, FloatingOverlayService, OnTouchListener, Context, IBinder (+4 more)

### Community 1 - "VerificationOrchestrator.kt"
Cohesion: 0.08
Nodes (18): AppDatabase, Context, Flow, ScanHistoryDao, ScanHistoryEntity, Bitmap, FloatArray, UnifiedScanResult (+10 more)

### Community 2 - "DashboardViewModel"
Cohesion: 0.08
Nodes (24): android, MainActivity, YaqeenNavHost(), DashboardScreen(), ModalityCard(), DashboardViewModel, Factory, android (+16 more)

### Community 3 - "ImageModelType"
Cohesion: 0.08
Nodes (19): Flow, PreferencesKeys, UserPreferencesRepository, ImageModelType, EFFICIENTNET_B3, SWIN_T, Modality, AUDIO (+11 more)

### Community 4 - "verify_onnx.py"
Cohesion: 0.09
Nodes (19): export(), export_audio.py Exports fine-tuned SpectrogramCNN models to ONNX: 1.…, SpectrogramCNN, SpectrogramCNNWithCAM, EfficientNetB3WithCAM, export(), export_image_effnet.py Exports fine-tuned EfficientNet-B3 to ONNX with explicit…, export() (+11 more)

### Community 5 - "ModelManager"
Cohesion: 0.07
Nodes (23): Bitmap, FloatArray, FloatBuffer, OrtEnvironment, OrtSession, ModelManager, ModelInferenceResult, ModelType (+15 more)

### Community 6 - "AudioCaptureManager"
Cohesion: 0.13
Nodes (15): AudioCaptureManager, ByteArray, Context, FloatArray, Result, Uri, Bitmap, FloatArray (+7 more)

### Community 7 - "ImageCamPostProcessor"
Cohesion: 0.41
Nodes (4): ImageCamPostProcessor, Bitmap, FloatArray, ImageXaiResult

### Community 8 - "gradlew"
Cohesion: 0.70
Nodes (4): gradlew script, die(), save(), warn()

### Community 9 - "EndToEndIntegrationSmokeTest.kt"
Cohesion: 0.13
Nodes (12): LanguageRouter, ScriptType, ARABIC, ENGLISH, AudioFcWeights, EffnetFcWeights, FcWeightsLoader, Context (+4 more)

### Community 15 - "Yaqeen Gulf — Finalized App Scope & Build Plan"
Cohesion: 0.09
Nodes (22): 0. App identity, 10. Detailed implementation plan, 11. Known model limitations to surface in the UI (not blockers, but disclose), 12. Instructions for the AI coding agent, 1. Project overview, 2. Distribution: signed APK, not Play Store, 3.1 Checkpoint source paths, 3. Finalized model inventory (+14 more)

### Community 18 - "ModelInferenceVerificationTest"
Cohesion: 0.29
Nodes (3): OrtEnvironment, OrtSession, ModelInferenceVerificationTest

### Community 19 - "Directive: Phase 6 — Cloud Source-Check (Opt-In)"
Cohesion: 0.18
Nodes (10): Abuse Prevention & Prompt Injection Hardening, Architecture, Directive: Phase 6 — Cloud Source-Check (Opt-In), Edge cases, Goal, Inputs, Outputs, Stop condition (per build plan pacing rule) (+2 more)

### Community 20 - "Agent Instructions — Yaqeen Gulf"
Cohesion: 0.20
Nodes (9): 1. Two separate trees — do not conflate them, 2. Directives for this project, 3. Execution scripts to check for before writing new ones, Agent Instructions — Yaqeen Gulf, File Organization, Operating Principles, Self-annealing loop, Summary (+1 more)

### Community 21 - "AudioMelPreprocessor"
Cohesion: 0.36
Nodes (4): AudioMelPreprocessor, FloatArray, FloatBuffer, DoubleArray

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

### Community 34 - "check.js"
Cohesion: 0.11
Nodes (31): handler(), server, extractArticleText(), checkDailyKillSwitch(), checkIpRateLimit(), getRedisClient(), memoryStore, resetMemoryStore() (+23 more)

### Community 35 - "export_fc_weights.py"
Cohesion: 0.67
Nodes (3): export_weights(), export_fc_weights.py Exports classifier weights for CAM post-processing: 1.…, save_json()

### Community 39 - "CloudCheckResult"
Cohesion: 0.16
Nodes (4): CloudCheckResult, CloudSourceCheckRepository, Result, CloudSourceCheckUnitTest

### Community 43 - ".loadBitmapFromUri"
Cohesion: 0.43
Nodes (5): ImageCaptureManager, Bitmap, Context, Result, Uri

### Community 44 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, cheerio, dotenv, @google/generative-ai, groq-sdk, ipaddr.js, @upstash/redis

## Knowledge Gaps
- **113 isolated node(s):** `PreferencesKeys`, `EFFICIENTNET_B3`, `SWIN_T`, `IMAGE`, `TEXT` (+108 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 205 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ImageModelType` connect `ImageModelType` to `VerificationOrchestrator.kt`, `DashboardViewModel`, `ModelManager`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **Why does `DashboardViewModel` connect `DashboardViewModel` to `VerificationOrchestrator.kt`, `ImageModelType`, `CloudCheckResult`?**
  _High betweenness centrality (0.065) - this node is a cross-community bridge._
- **Why does `AudioCaptureManager` connect `AudioCaptureManager` to `EndToEndIntegrationSmokeTest.kt`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `WordPieceTokenizer` (e.g. with `.testWordPieceTokenizerArabic()` and `.testWordPieceTokenizerEnglish()`) actually correct?**
  _`WordPieceTokenizer` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PreferencesKeys`, `EFFICIENTNET_B3`, `SWIN_T` to the rest of the system?**
  _113 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FloatingOverlayService` be split into smaller, more focused modules?**
  _Cohesion score 0.10685483870967742 - nodes in this community are weakly interconnected._
- **Should `VerificationOrchestrator.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08305647840531562 - nodes in this community are weakly interconnected._