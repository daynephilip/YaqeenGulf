# Graph Report - yaqeen-gulf  (2026-09-08)

## Corpus Check
- 70 files · ~203,071 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 488 nodes · 779 edges · 39 communities (28 shown, 6 thin omitted)
- Extraction: 99% EXTRACTED · 1% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- FloatingOverlayService
- AppDatabase
- DashboardViewModel
- ImageModelType
- verify_onnx.py
- ModelManager
- AudioCaptureManager
- VerificationOrchestrator.kt
- gradlew
- EndToEndIntegrationSmokeTest.kt
- Yaqeen Gulf — Finalized App Scope & Build Plan
- WordPieceTokenizer
- XaiPostProcessorUnitTest
- ModelInferenceVerificationTest
- Directive: Phase 6 — Cloud Source-Check (Opt-In)
- Agent Instructions — Yaqeen Gulf
- AudioMelPreprocessor
- LanguageRouter
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
- ImagePreprocessor.kt
- export_fc_weights.py
- copy_onnx_to_assets.py
- rules/graphify.md
- workflows/graphify.md

## God Nodes (most connected - your core abstractions)
1. `ImageModelType` - 19 edges
2. `ModelManager` - 19 edges
3. `DashboardViewModel` - 16 edges
4. `WordPieceTokenizer` - 15 edges
5. `FloatingOverlayService` - 15 edges
6. `AppDatabase` - 14 edges
7. `Yaqeen Gulf — Finalized App Scope & Build Plan` - 14 edges
8. `ModelType` - 13 edges
9. `SettingsViewModel` - 13 edges
10. `ScanHistoryEntity` - 12 edges

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

## Communities (39 total, 6 thin omitted)

### Community 0 - "FloatingOverlayService"
Cohesion: 0.11
Nodes (12): AccessibilityEvent, AccessibilityNodeInfo, AccessibilityService, YaqeenAccessibilityService, FloatingOverlayService, OnTouchListener, Context, IBinder (+4 more)

### Community 1 - "AppDatabase"
Cohesion: 0.13
Nodes (10): AppDatabase, Context, Flow, ScanHistoryDao, ScanHistoryEntity, FloatArray, UnifiedScanResult, VerificationOrchestrator (+2 more)

### Community 2 - "DashboardViewModel"
Cohesion: 0.07
Nodes (26): ImageCaptureManager, Bitmap, Context, Result, Uri, android, MainActivity, YaqeenNavHost() (+18 more)

### Community 3 - "ImageModelType"
Cohesion: 0.09
Nodes (22): Flow, PreferencesKeys, UserPreferencesRepository, ImageModelType, EFFICIENTNET_B3, SWIN_T, Modality, AUDIO (+14 more)

### Community 4 - "verify_onnx.py"
Cohesion: 0.09
Nodes (19): export(), export_audio.py Exports fine-tuned SpectrogramCNN models to ONNX: 1.…, SpectrogramCNN, SpectrogramCNNWithCAM, EfficientNetB3WithCAM, export(), export_image_effnet.py Exports fine-tuned EfficientNet-B3 to ONNX with explicit…, export() (+11 more)

### Community 5 - "ModelManager"
Cohesion: 0.14
Nodes (15): Bitmap, FloatArray, FloatBuffer, OrtEnvironment, OrtSession, ModelManager, ModelInferenceResult, ModelType (+7 more)

### Community 6 - "AudioCaptureManager"
Cohesion: 0.13
Nodes (15): AudioCaptureManager, ByteArray, Context, FloatArray, Result, Uri, Bitmap, FloatArray (+7 more)

### Community 7 - "VerificationOrchestrator.kt"
Cohesion: 0.14
Nodes (12): Bitmap, AudioCamPostProcessor, FloatArray, ImageCamPostProcessor, Bitmap, FloatArray, FloatArray, TextAttentionPostProcessor (+4 more)

### Community 8 - "gradlew"
Cohesion: 0.70
Nodes (4): gradlew script, die(), save(), warn()

### Community 9 - "EndToEndIntegrationSmokeTest.kt"
Cohesion: 0.20
Nodes (8): AudioFcWeights, EffnetFcWeights, FcWeightsLoader, Context, SwinFcWeights, EndToEndIntegrationSmokeTest, FloatArray, OrtEnvironment

### Community 15 - "Yaqeen Gulf — Finalized App Scope & Build Plan"
Cohesion: 0.09
Nodes (22): 0. App identity, 10. Detailed implementation plan, 11. Known model limitations to surface in the UI (not blockers, but disclose), 12. Instructions for the AI coding agent, 1. Project overview, 2. Distribution: signed APK, not Play Store, 3.1 Checkpoint source paths, 3. Finalized model inventory (+14 more)

### Community 16 - "WordPieceTokenizer"
Cohesion: 0.19
Nodes (5): Context, TokenizedInput, WordPieceTokenizer, PreprocessorUnitTest, LongArray

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

### Community 22 - "LanguageRouter"
Cohesion: 0.33
Nodes (4): LanguageRouter, ScriptType, ARABIC, ENGLISH

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

### Community 34 - "ImagePreprocessor.kt"
Cohesion: 0.60
Nodes (3): ImagePreprocessor, Bitmap, FloatBuffer

### Community 35 - "export_fc_weights.py"
Cohesion: 0.67
Nodes (3): export_weights(), export_fc_weights.py Exports classifier weights for CAM post-processing: 1.…, save_json()

## Knowledge Gaps
- **96 isolated node(s):** `PreferencesKeys`, `EFFICIENTNET_B3`, `SWIN_T`, `IMAGE`, `TEXT` (+91 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 177 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `ImageModelType` connect `ImageModelType` to `AppDatabase`, `DashboardViewModel`, `ModelManager`, `VerificationOrchestrator.kt`?**
  _High betweenness centrality (0.125) - this node is a cross-community bridge._
- **Why does `DashboardViewModel` connect `DashboardViewModel` to `AppDatabase`, `ImageModelType`?**
  _High betweenness centrality (0.056) - this node is a cross-community bridge._
- **Why does `AudioCaptureManager` connect `AudioCaptureManager` to `EndToEndIntegrationSmokeTest.kt`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `WordPieceTokenizer` (e.g. with `.testWordPieceTokenizerArabic()` and `.testWordPieceTokenizerEnglish()`) actually correct?**
  _`WordPieceTokenizer` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `PreferencesKeys`, `EFFICIENTNET_B3`, `SWIN_T` to the rest of the system?**
  _96 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `FloatingOverlayService` be split into smaller, more focused modules?**
  _Cohesion score 0.10685483870967742 - nodes in this community are weakly interconnected._
- **Should `AppDatabase` be split into smaller, more focused modules?**
  _Cohesion score 0.13227513227513227 - nodes in this community are weakly interconnected._