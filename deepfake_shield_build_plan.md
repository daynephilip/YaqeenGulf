# Yaqeen Gulf — Finalized App Scope & Build Plan

*Multimodal verification. Unified trust.*

**Purpose of this document:** a complete, self-contained spec for an AI coding agent to build this Android app from. It supersedes any earlier drafts (e.g. `projectspec.md`, prior scope files named `DeepFake Shield`). The tech stack, model list, XAI approach, distribution plan, app identity, and folder structure below are final decisions, not open questions. Where a decision was changed from an earlier draft, the reason is included so the agent doesn't "correct" it back.

---

## 0. App identity

| | |
|---|---|
| App name | **Yaqeen Gulf** |
| Tagline | Multimodal verification. Unified trust. |
| Package name (`applicationId`) | `com.yaqeengulf.deepfake` |
| Min SDK | **29** (Android 10) |

**Why min SDK 29:** `AudioPlaybackCaptureConfiguration` (internal audio capture) requires API 29 as a hard floor. Setting the whole app's min SDK to 29 removes the need for a version-gated fallback branch in the audio pipeline — internal audio capture is simply always available, and mic recording becomes a user *preference* toggle rather than a compatibility shim.

---

## 1. Project overview

Yaqeen Gulf is an Android app that detects AI-generated text, images, and spoofed audio, and — critically — explains why it reached that verdict rather than returning a bare score. All inference runs on-device; no user content is sent to a server.

**Current status:** all model checkpoints are trained and evaluated (see §3, paths in §3.1). No Android code exists yet. The agent is starting from Phase 0 of §10.

**Distribution:** side-loaded signed APK, not Google Play (decision detailed in §2).

---

## 2. Distribution: signed APK, not Play Store

**Decision:** ship as a signed release APK for direct download/sideload (e.g. GitHub Releases), not through Google Play Console.

**Why:** avoids the $25 developer fee, identity verification, the mandatory 14-day/12-tester closed test for new accounts, and — most relevant to this app — Play's extra review scrutiny on apps using Accessibility Service + screen/audio capture together (restricted-permission justification video, higher rejection risk). This can be revisited later; nothing in the architecture below is Play-Store-incompatible if that changes.

**Implications for the agent to build in:**
- Generate a signed release build via Gradle (`signingConfigs` block with a release keystore) — do not ship a debug-signed APK.
- No Play Billing, no Play Integrity API, no Play In-App Update API — skip any Play-specific dependency.
- Runtime permission requests still go through normal Android runtime permission flows — this doesn't change with sideloading, only the store review of them is skipped.
- Optionally wire in a self-hosted update check (e.g. compare a version string from a raw GitHub URL) since there's no Play auto-update — mark this as a "nice to have," not required for v1.

---

## 3. Finalized model inventory

Five models are the deployed default. **Swin-T is included as a user-selectable alternative for image detection**, not a hidden/excluded model — this is a deliberate change from earlier drafts, which excluded it entirely.

| # | Modality | Model | Trained on | Params | Deploy file name | Selectable? |
|---|---|---|---|---|---|---|
| 1 | Image (default) | EfficientNet-B3 | GenImage (5 generators) | ~12M | `img_det_effnet.onnx` | Default |
| 2 | Image (alternative) | Swin-T | GenImage (5 generators) | larger, transformer | `img_det_swin.onnx` | User-selectable in Settings |
| 3 | Audio — English | SpectrogramCNN | ASVspoof 2019 LA | ~422K | `voice_en.onnx` | Only option |
| 4 | Audio — Arabic | SpectrogramCNN | ArEnAV | ~422K | `voice_ar.onnx` | Only option |
| 5 | Text — English | BERT-base | ISOT (Reuters-leak cleaned) | ~110M | `text_en.onnx` | Only option |
| 6 | Text — Arabic | AraBERT | AFND | ~135M | `text_ar.onnx` | Only option |

**Still excluded from deployment** (thesis comparison models only, do not export/ship): XLSR-53 (both languages), mBERT (both languages). If the agent encounters references to these in older docs, ignore them — this table is authoritative.

### 3.1 Checkpoint source paths

Original checkpoints are on the development machine (Windows) at `D:\yakeen gulf\...`. The ML export pipeline itself lives at a **fixed absolute path: `D:\ml_export`** — this is not nested inside the Android app repo (see §8 note). The agent should copy checkpoints from their original location into `D:\ml_export\checkpoints\` rather than reference `D:\yakeen gulf\...` directly in code, so the export scripts don't depend on that folder still existing/being named the same later.

| Deploy target | Source file (original location) |
|---|---|
| `img_det_effnet.onnx` | `D:\yakeen gulf\Image\efficentnetb3.pt` |
| `img_det_swin.onnx` | `D:\yakeen gulf\Image\swint_T.pt` |
| `voice_en.onnx` | `D:\yakeen gulf\Audio English\spectogramcnn_asvspoof.pt` |
| `voice_ar.onnx` | `D:\yakeen gulf\Audio Arabic\spectogramcnn_arenav.pt` |
| `text_en.onnx` | `D:\yakeen gulf\Text English\bertbase_isot.pt` |
| `text_ar.onnx` | `D:\yakeen gulf\Text Arabic\arabert_afnd.pt` |

Target layout after copying, under `D:\ml_export\`:

```text
D:\ml_export\
└── checkpoints\
    ├── image\
    │   ├── efficentnetb3.pt
    │   └── swint_T.pt
    ├── audio_en\spectogramcnn_asvspoof.pt
    ├── audio_ar\spectogramcnn_arenav.pt
    ├── text_en\bertbase_isot.pt
    └── text_ar\arabert_afnd.pt
```

---

## 4. Finalized XAI approach (per modality)

| Modality | Method | Notes |
|---|---|---|
| Image — EfficientNet-B3 | Plain CAM (Class Activation Mapping), not Grad-CAM | Ends in global-average-pool → FC. CAM only needs the FC weights + last conv feature map — no gradient computation needed. Avoids requiring ONNX Runtime Training on-device. |
| Image — Swin-T (optional) | CAM-style, adapted for transformer feature maps | Swin-T's last stage produces a spatial grid of patch features, not a conv grid — same *concept* (weight the last feature map by classifier weights, no gradients) but requires its own export/upscale code in Phase 2. Do not reuse the EfficientNet-B3 CAM function unmodified for Swin-T. |
| Audio | Plain CAM | SpectrogramCNN has the same GAP → FC structure as EfficientNet-B3. Apply CAM to the 2D log-mel spectrogram, then map the resulting heatmap back onto the time axis for the waveform overlay. |
| Text | Attention weights from the final transformer layer | AraBERT/BERT-base — attention weights are a direct model output, no extra computation needed. |

**Required Python export changes (Phase 2):**
- Image/audio CNN models: `forward()` must return `(logits, final_conv_feature_map)`. FC weights are read directly from the state dict.
- Swin-T: `forward()` must return `(logits, final_stage_feature_map)` — export separately from the EfficientNet-B3 script; do not assume identical tensor shapes.
- Text models: `forward()` must return `(logits, attention_weights)` from the final encoder layer.
- Export all outputs with explicit, stable names (e.g. `logits`, `feature_map`, `attention`) so the Kotlin ONNX Runtime session can retrieve them by name via `session.getOutput(n)`.

---

## 5. Finalized tech stack

| Layer | Choice | License | Notes |
|---|---|---|---|
| Language | Kotlin | Apache-2.0 | |
| UI | Jetpack Compose (app + overlay) | Apache-2.0 | |
| Architecture | MVVM + Clean Architecture | — | |
| DI | Hilt | Apache-2.0 | |
| Local storage (scan history) | Room | Apache-2.0 | |
| Settings storage | DataStore | Apache-2.0 | Also stores the image-model selection (EfficientNet-B3 vs Swin-T) and overlay preference state. |
| ML runtime | ONNX Runtime Mobile (`com.microsoft.onnxruntime:onnxruntime-android`) | MIT | Inference-only is sufficient — see §4. |
| Model conversion | PyTorch → ONNX (`torch.onnx.export`) | BSD | Run once on desktop/Colab, not on-device. |
| Image capture | Gallery/file picker (manual) + MediaProjection screenshot (overlay mode, permission-gated) | AOSP | |
| Text capture | Manual paste / Share Sheet intent (always available) + Android AccessibilityService (overlay mode, permission-gated) | AOSP | |
| Audio capture | File picker / mic `AudioRecord` (manual) + `AudioPlaybackCaptureConfiguration` (overlay mode, permission-gated) | AOSP | Min SDK 29 makes internal capture always available when permission is granted. |
| Audio preprocessing (mel-spectrogram) | Custom Kotlin FFT/mel-filterbank | — | TarsosDSP is GPL-3 (copyleft risk). Port the validated Python log-mel logic exactly (80 mel bins, n_fft=512, hop=160, win=400). |
| Text tokenizer | WordPiece tokenizer, e.g. via DJL (Deep Java Library) Android artifact | Apache-2.0 | AraBERT/BERT-base use WordPiece, not SentencePiece/BPE. |
| Language detection (EN vs AR routing) | Unicode-script heuristic, no model | — | A dedicated ML model isn't needed for a binary script check. |
| Overlay permission | `SYSTEM_ALERT_WINDOW` + Accessibility Service | AOSP | See §6 — feature is fully built but gracefully optional. |
| Signing | Manual release keystore via Gradle `signingConfigs` | — | Required for sideload install (§2). |
| Codebase mapping for the coding agent | Graphify (`graphifyy` on PyPI) | — | See §9. |

---

## 6. Overlay feature: fully built, gracefully optional

The floating overlay (draggable shield icon, scroll-time capture via Accessibility Service + `SYSTEM_ALERT_WINDOW`) is a **real, fully implemented feature**, not a placeholder. It is not required for the rest of the app to function.

**Behavior when overlay permissions are denied or not yet granted:**
- The app does **not** block, degrade, or nag persistently. The main app screen works exactly the same: manual image picker, paste/share-in text, record-or-pick audio file — all using standard, non-restricted permissions independent of the overlay.
- In Settings, the overlay toggle shows its current permission state. If the user taps it without having granted `SYSTEM_ALERT_WINDOW`/Accessibility Service, show:
  > "Overlay permission not granted — you can still scan content directly from the app. Enable overlay in system settings to scan while browsing other apps."
  with a button that deep-links to the relevant system permission screen.
- If permission is later revoked (e.g. via system settings while the app is running), the overlay should stop gracefully and the same message should reappear next time the user tries to enable it — no crash, no forced restart.

**Practical effect on phases:** Phase 1 still sets up the overlay service and requests `SYSTEM_ALERT_WINDOW`, but the app's core scan flow (Phase 3–4) must work fully without it, since manual capture is the baseline and overlay is additive.

---

## 7. Architecture (confirmed final)

```text
[ User Interface Layer (Jetpack Compose) ]
   ├── Main Dashboard (manual scan entry: image/text/audio)
   ├── Settings (image-model selector, overlay toggle + permission state)
   ├── Floating Overlay Service (draggable shield icon, permission-gated, always-on-top)
   ├── Capture UI (crop tool, text selector, audio recorder)
   └── XAI Result UI (CAM heatmaps, highlighted text, audio visualizer)

[ Core Logic Layer (Kotlin Coroutines/Flow) ]
   ├── Accessibility Service (overlay-mode text extraction only)
   ├── MediaProjection API (overlay-mode screen + internal audio capture)
   ├── Language Router (Unicode-script heuristic → EN/AR model selection)
   ├── Image Model Router (EfficientNet-B3 default / Swin-T if user-selected)
   └── XAI Post-Processor (raw activations → UI-ready heatmaps/highlights)

[ Inference Engine Layer (ONNX Runtime Mobile) ]
   ├── Model Manager (lazy load/unload — one model in memory at a time)
   ├── Preprocessor (WordPiece tokenizer, bitmap→tensor, custom Kotlin mel-spectrogram)
   └── ONNX Sessions (6 models total, each exposing logits + XAI tensor)
```

---

## 8. Folder structure

**Important: the Python export pipeline and the Android app repo are two separate trees, not one nested project.** `ml_export` lives at the fixed absolute path `D:\ml_export`, independent of wherever the Android app repo is created/cloned.

**Python export side — fixed at `D:\ml_export`:**
```text
D:\ml_export\
├── export_image_effnet.py
├── export_image_swin.py
├── export_audio.py
├── export_text.py
├── verify_onnx.py                        ← produces the Phase 2 verification report
├── checkpoints\                          ← §3.1 layout
└── onnx_out\                             ← the 6 .onnx export outputs land here
```

**Android app repo — wherever the agent initializes it:**
```text
yaqeen-gulf/
├── deepfake_shield_build_plan.md         ← this doc, at repo root
├── .graphifyignore
├── app/
│   ├── build.gradle.kts                  ← applicationId com.yaqeengulf.deepfake, minSdk 29
│   └── src/main/
│       ├── java/com/yaqeengulf/deepfake/ ← Kotlin source, MVVM packages (ui/, domain/, data/)
│       ├── assets/models/                ← the 6 .onnx files, copied in from D:\ml_export\onnx_out\
│       └── assets/vocab/                 ← tokenizer vocab files for text_en/text_ar
└── graphify-out/                         ← generated, not hand-edited
```

**Bridging step (end of Phase 2):** copy the 6 `.onnx` files from `D:\ml_export\onnx_out\` into `app/src/main/assets/models/` in the Android repo. This is a manual/scripted copy step, not an automatic link between the two trees — the agent should not assume the Android build can read directly from `D:\ml_export`.

---

## 9. AI agent context & token management (Graphify)

To prevent the agent from burning through context by reading every file, this project uses **Graphify** (`graphifyy` on PyPI) to map the codebase into a queryable knowledge graph.

**Project setup status: NOT yet configured.** This must happen before any other work begins — treat it as Phase 0.

**Phase 0 — Graphify setup (agent performs this first):**
1. Install the CLI: `uv tool install graphifyy` (or `pipx install graphifyy`).
2. Register the skill with the AI assistant: `graphify install`.
3. Inside the AI assistant, run: `/graphify .`
4. This produces three files in `graphify-out/`:
   - `graph.html` — browsable graph (open in any browser, click/filter/search nodes)
   - `GRAPH_REPORT.md` — key concepts, notable connections, suggested questions
   - `graph.json` — the full graph, queryable without re-reading source files
5. Create a `.graphifyignore` file at the project root excluding at minimum: `*.onnx`, `*.pt`, `*.apk`, `.gradle/`, `build/`.
6. If setup fails at any step, fall back to normal file reading rather than guessing at graph output that doesn't exist, and flag the failure back to the project owner rather than silently proceeding.

**Agent rules for token efficiency, once Phase 0 succeeds:**
- Do not read raw source files unless actively writing or modifying them.
- Consult `GRAPH_REPORT.md` for a high-level overview before starting any new phase.
- Never attempt to read `.onnx`, `.pt`, `.apk`, `.gradle/`, or `build/` files directly — treat the 6 models as existing at the asset paths specified in §8 once Phase 2 completes.

---

## 10. Detailed implementation plan

**Pacing rule for the agent:** complete one phase, then stop and wait for explicit approval before starting the next. Do not merge phases.

### Phase 0 — Graphify setup
See §9. Must succeed (or be explicitly flagged as failed with a fallback in place) before Phase 1 begins.

### Phase 1 — Project setup & shell
- Initialize Android project: Kotlin, Jetpack Compose, Hilt, Room. `applicationId = com.yaqeengulf.deepfake`, `minSdk = 29`.
- Add Gradle release `signingConfigs` block (keystore path via local `gradle.properties`, not committed).
- Implement Floating Overlay Service; request `SYSTEM_ALERT_WINDOW` at runtime — but do not gate any other feature on it (§6).
- Basic navigation shell: Main Dashboard (manual scan entry points), Settings (overlay toggle, image-model selector — placeholder until Phase 3).

**Deliverable:** app installs via sideloaded signed APK, shows the overlay icon and manual-scan entry points, no ML yet.

### Phase 2 — ONNX export & XAI tensor export (Python, off-device)
- Copy checkpoints from their original locations (§3.1) into `D:\ml_export\checkpoints\` (§8).
- Load each of the 6 checkpoints.
- Modify `forward()` per §4 to return the appropriate XAI tensor per architecture (CNN feature map, Swin-T stage feature map, or attention weights).
- Export to ONNX with explicit output names into `D:\ml_export\onnx_out\`; verify each export's logits match the original PyTorch checkpoint on a held-out sample before proceeding.
- **Bridging step:** copy the resulting 6 `.onnx` files from `D:\ml_export\onnx_out\` into the Android repo's `app/src/main/assets/models/` (§8) — this does not happen automatically.

**Deliverable:** 6 `.onnx` files (in `D:\ml_export\onnx_out\`, then copied into the app repo) + a verification report (max diff between PyTorch and ONNX logits per model).

### Phase 3 — Inference engine & preprocessing (Android)
- Integrate `onnxruntime-android`.
- Preprocessing pipelines: WordPiece tokenizer (EN/AR), bitmap→tensor (image), custom Kotlin mel-spectrogram (audio, matching `spectrogram_dataset.py` exactly).
- Model Manager: lazy-load, only one ONNX session resident at a time.
- Settings: wire up the real image-model selector (EfficientNet-B3 default / Swin-T).

**Deliverable:** given a sample image/text/audio clip, the app produces a raw logit output matching the Phase 2 verification report, for both image model options.

### Phase 4 — XAI post-processing (Android)
- CAM for EfficientNet-B3 and SpectrogramCNN (image/audio): upscale feature map, weight by FC weights, overlay via Compose Canvas.
- CAM-style for Swin-T: separate implementation per §4, matching its patch-grid feature map shape.
- Attention-weight mapping for text: map final-layer attention to tokens, render via `AnnotatedString`.
- All XAI computation on `Dispatchers.Default`.

**Deliverable:** each modality (and both image-model options) produces a visual explanation artifact, not just a score.

### Phase 5 — Capture pipelines
- Manual capture (baseline, always available): gallery/file picker, paste/share-in text, record-or-pick audio.
- Overlay capture (permission-gated, additive per §6): Accessibility Service text extraction, MediaProjection screenshot, internal audio capture.
- Language Router: Unicode-script heuristic.

**Deliverable:** end-to-end capture → inference → XAI result via both manual entry and (if permitted) the overlay.

### Phase 6 — Result UI & polish
- Bottom sheet / overlay card: verdict + confidence %, modality-specific evidence view, action buttons (Dismiss / Save to History / Share Report).
- Room database for scan history.
- Overlay permission messaging per §6.
- Animations/transitions.

**Deliverable:** feature-complete v1, ready for a signed release build per §2.

---

## 11. Known model limitations to surface in the UI (not blockers, but disclose)

- **ADM-generated images** are the hardest case for the image models (~80% accuracy vs. 95–100% on other generators) — use a lower-confidence display band for this case.
- **English text detection (ISOT-trained)** reflects wire-service formatting differences more than genuine veracity detection — avoid over-claiming accuracy in-app copy for English text scans.
- **Audio spoof detection** has only been validated against attack types A01–A06 (ASVspoof) — newer/unseen synthesis techniques are untested.
- **Arabic audio model** has no enforced speaker-leakage control in its training split — a documented limitation, not a functional bug.

---

## 12. Instructions for the AI coding agent

1. This document is authoritative over any older drafts wherever they conflict (model count, XAI method, tech stack, distribution method, app identity).
2. Do not train any new models. All 6 model files in §3 are final; Phase 2 is export only.
3. Follow the phase order in §10 exactly; stop after each phase and wait for approval before continuing.
4. When extracting ONNX outputs in Kotlin, retrieve tensors by the explicit output names set in Phase 2 (not positional index).
5. Enforce one ONNX Runtime session in memory at a time (Model Manager, Phase 3).
6. Complete Phase 0 (Graphify setup, §9) before writing any code, and confirm it actually succeeded rather than assuming it's pre-configured.
7. Do not gate any core feature on overlay permissions (§6) — manual capture must work standalone.
8. First task: run Phase 0, then begin Phase 1 (project shell) and Phase 2 (Python export scripts) in parallel, since neither depends on the other. Produce the Phase 2 verification report before Phase 3 begins.
