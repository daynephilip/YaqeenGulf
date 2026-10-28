# Directive: Phase 2 — ONNX Export & XAI Tensor Export

## Goal
Export all 6 finalized checkpoints to ONNX, each exposing the extra tensor needed for that modality's XAI method, and verify each export's logits match the original PyTorch checkpoint before Phase 3 begins. This is an export task, not a training task — no checkpoint should be retrained or fine-tuned here.

## Inputs
Checkpoints copied into `D:\ml_export\checkpoints\` (see build plan §3.1 for original source paths):
- `image\efficentnetb3.pt`
- `image\swint_T.pt`
- `audio_en\spectogramcnn_asvspoof.pt`
- `audio_ar\spectogramcnn_arenav.pt`
- `text_en\bertbase_isot.pt`
- `text_ar\arabert_afnd.pt`

## Tools / scripts to use
Check `execution/` for these before writing new ones:
- `execution/export_image_effnet.py`
- `execution/export_image_swin.py` — **separate script, do not merge with the EfficientNet-B3 one**
- `execution/export_audio.py` (handles both `voice_en` and `voice_ar` — same architecture, different weights)
- `execution/export_text.py` (handles both `text_en` and `text_ar` — same architecture family, different weights/vocab)
- `execution/verify_onnx.py`
- `execution/copy_onnx_to_assets.py` — bridging step, run last

## Outputs
- `D:\ml_export\onnx_out\img_det_effnet.onnx`
- `D:\ml_export\onnx_out\img_det_swin.onnx`
- `D:\ml_export\onnx_out\voice_en.onnx`
- `D:\ml_export\onnx_out\voice_ar.onnx`
- `D:\ml_export\onnx_out\text_en.onnx`
- `D:\ml_export\onnx_out\text_ar.onnx`
- A verification report: max logit diff (PyTorch vs. ONNX) per model, on a held-out sample
- After verification passes: the same 6 files copied into `app/src/main/assets/models/` in the Android repo

## Per-model export requirements
- **EfficientNet-B3 / SpectrogramCNN (audio, both languages):** `forward()` returns `(logits, final_conv_feature_map)`. FC weights are read from the state dict directly — do not route them through the forward pass.
- **Swin-T:** `forward()` returns `(logits, final_stage_feature_map)`. This is a patch-grid feature map, not a conv grid — do not assume the same tensor shape or upscaling logic as the CNN models.
- **BERT-base / AraBERT:** `forward()` returns `(logits, attention_weights)` from the final encoder layer.
- All models: export outputs with explicit, stable names (`logits`, `feature_map`, `attention`) so Kotlin can retrieve by name via `session.getOutput(n)`, not by positional index.

## Edge cases & Learnings
- **Large checkpoints (arabert_afnd ~1.6GB, bertbase_isot ~1.3GB):** export on CPU if GPU memory is constrained; no gradient computation is needed anywhere in this pipeline (CAM and attention extraction are both gradient-free), so `torch.no_grad()` throughout is safe and reduces memory pressure.
- **PyTorch 2.x exporter default:** In PyTorch >=2.9 / 2.14, `torch.onnx.export` defaults to `dynamo=True` which requires `onnxscript` and may fail on Windows with emoji/character encoding in console outputs. Using `dynamo=False` with `opset_version=14` reliably uses the TorchScript exporter and completes cleanly.
- **Windows UTF-8 console output:** Always use `sys.stdout.reconfigure(encoding="utf-8", errors="replace")` rather than re-wrapping `sys.stdout = TextIOWrapper(...)`, to avoid "I/O operation on closed file" when modules are imported multiple times.
- **Transformers backend conflicts:** On environments where TensorFlow Intel is installed alongside NumPy 2, Transformers attempts to import TensorFlow by default and throws `AttributeError: _ARRAY_API not found`. Set `USE_TF=0` and `TRANSFORMERS_NO_TF=1` in the environment before importing `transformers`.
- **An op in Swin-T's export isn't supported by ONNX Runtime Mobile's operator set:** don't silently swap architectures — look for an opset version bump or an equivalent supported op first, and log the workaround in this directive once found.
- **Verification shows a logit mismatch beyond a sane tolerance (e.g. >1e-3 max diff):** stop and investigate before copying to assets — a silent mismatch here surfaces as wrong verdicts in the app with no obvious cause.
- **Vocab file mismatch (text models):** verify the vocab file being paired with each ONNX text model matches the tokenizer the checkpoint was actually trained with — `bert-base-uncased` produces `vocab_en.txt` (30,522 tokens) and `aubmindlab/bert-base-arabertv02` produces `vocab_ar.txt` (64,000 tokens). Both are bridged to `app/src/main/assets/vocab/`.
- **Classifier / FC weights for CAM & Audio Head Architecture:**
  - `img_det_effnet_fc_weights.json`: shape `[2, 1536]`, bias `[2]`.
  - `img_det_swin_fc_weights.json`: shape `[2, 768]`, bias `[2]`.
  - `voice_en_fc_weights.json` & `voice_ar_fc_weights.json`: Inspection of `cnn_model.py` confirms the classifier head has a non-linearity: `Linear(256, 128) -> ReLU -> Linear(128, 2)`. Because of the `ReLU`, collapsing to static `W2 @ W1` ignores the activation mask. The companion JSON files provide:
    1. Full raw layers: `layer1_weight` (`[128, 256]`), `layer1_bias` (`[128]`), `layer2_weight` (`[2, 128]`), and `layer2_bias` (`[2]`).
    2. Exact analytical gradient formula for Phase 4 CAM: for target class $c$ and pooled feature $g = \text{GAP}(\text{feat})$, let $z_1 = W_1 g + b_1$. The exact gradient of logit $c$ with respect to feature channel $k$ is given by:
       $$\alpha_k^{(c)} = \sum_{j} W_{2}[c, j] \cdot \mathbb{I}(z_{1}[j] > 0) \cdot W_{1}[j, k]$$
       This analytical formulation was mathematically verified against PyTorch autograd ($\text{diff} = 0.0$), requiring zero gradient runtime overhead on Android while retaining exact mathematical fidelity through the ReLU.
- **Android ONNX Runtime Package & Real JVM Runtime Verification:**
  - The build plan specifies `com.microsoft.onnxruntime:onnxruntime-android`. The dependency was added to `app/build.gradle.kts` and packaged into `app-debug.apk` with all 4 ABI native shared libraries (`libonnxruntime.so`, `libonnxruntime4j_jni.so`).
  - To verify real runtime behavior beyond static symbol checks, all 6 models were executed through the official ONNX Runtime Java/JVM API (`ai.onnxruntime.*`) using the exact session options and tensor structures used on Android.
  - Results on actual inputs:
    - `img_det_effnet.onnx` (40 MB): session load 712 ms, inference 33 ms -> **PASS**
    - `img_det_swin.onnx` (107 MB): session load 2261 ms, inference 90 ms -> **PASS**
    - `voice_en.onnx` (1 MB): session load 169 ms, inference 6 ms -> **PASS**
    - `voice_ar.onnx` (1 MB): session load 44 ms, inference 6 ms -> **PASS**
    - `text_en.onnx` (417 MB): session load 4428 ms, inference 51 ms -> **PASS**
    - `text_ar.onnx` (515 MB): session load 5190 ms, inference 45 ms -> **PASS**
  - Session creation time for the large text models (~4.5–5.2s on first load) underscores the importance of the Model Manager's lazy-load and single-session resident design in Phase 3.



