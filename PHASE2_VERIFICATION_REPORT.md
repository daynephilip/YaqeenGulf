# Phase 2 — ONNX Export & XAI Tensor Verification Report

**Tolerance Threshold:** `< 1e-3` max absolute difference between PyTorch and ONNX Runtime.

| Model | Export File | Logits Shape | XAI Tensor & Shape | Max Logit Diff | Mean Logit Diff | Max XAI Diff | Status |
|---|---|---|---|---|---|---|---|
| EfficientNet-B3 (Image Default) | `img_det_effnet.onnx` | `[2, 2]` | `feature_map`: `[2, 1536, 7, 7]` | `3.87e-06` | `2.41e-06` | `3.34e-05` | **PASS** |
| Swin-T (Image Alternative) | `img_det_swin.onnx` | `[2, 2]` | `feature_map`: `[2, 768, 7, 7]` | `1.91e-06` | `1.19e-06` | `8.58e-06` | **PASS** |
| SpectrogramCNN (Audio English) | `voice_en.onnx` | `[2, 2]` | `feature_map`: `[2, 256, 10, 50]` | `9.54e-07` | `2.38e-07` | `9.06e-06` | **PASS** |
| SpectrogramCNN (Audio Arabic) | `voice_ar.onnx` | `[2, 2]` | `feature_map`: `[2, 256, 10, 50]` | `1.19e-07` | `8.94e-08` | `7.63e-06` | **PASS** |
| BERT-base (Text English) | `text_en.onnx` | `[2, 2]` | `attention`: `[2, 12, 32, 32]` | `7.15e-07` | `3.58e-07` | `3.84e-07` | **PASS** |
| AraBERT (Text Arabic) | `text_ar.onnx` | `[2, 2]` | `attention`: `[2, 12, 32, 32]` | `6.68e-06` | `4.26e-06` | `1.05e-05` | **PASS** |

## Overall Result: ALL 6 MODELS PASSED VERIFICATION (TOLERANCE < 1e-3)

---

## 1. Classifier Weights & Audio Head Architecture for CAM

Inspection of the original model code (`cnn_model.py`) revealed that SpectrogramCNN's classifier head has a non-linearity:
```python
self.classifier = nn.Sequential(
    nn.Flatten(),
    nn.Dropout(dropout),
    nn.Linear(256, 128),
    nn.ReLU(inplace=True),
    nn.Dropout(dropout),
    nn.Linear(128, num_classes),
)
```
Because of the `ReLU`, static collapse ($W = W_2 \times W_1$) loses the activation mask. To ensure exact mathematical correctness without requiring PyTorch/autograd on-device, the companion files provide full individual layer weights:

| Model | Companion File | Contained Weights | Channels | CAM Mathematical Formulation |
|---|---|---|---|---|
| **EfficientNet-B3** | `img_det_effnet_fc_weights.json` | `weight`: `[2, 1536]`, `bias`: `[2]` | 1536 | Standard CAM: $\alpha = W_{\text{pred}, :}$ |
| **Swin-T** | `img_det_swin_fc_weights.json` | `weight`: `[2, 768]`, `bias`: `[2]` | 768 | Spatial Patch CAM: $\alpha = W_{\text{pred}, :}$ |
| **Audio English** | `voice_en_fc_weights.json` | `layer1_weight` `[128, 256]`, `layer1_bias` `[128]`, `layer2_weight` `[2, 128]`, `layer2_bias` `[2]` | 256 | Analytical gradient through ReLU: $\alpha_k = \sum_j W_2[\text{pred}, j] \cdot \mathbb{I}(z_1[j] > 0) \cdot W_1[j, k]$ ($\text{diff vs autograd} = 0.0$) |
| **Audio Arabic** | `voice_ar_fc_weights.json` | `layer1_weight` `[128, 256]`, `layer1_bias` `[128]`, `layer2_weight` `[2, 128]`, `layer2_bias` `[2]` | 256 | Analytical gradient through ReLU: $\alpha_k = \sum_j W_2[\text{pred}, j] \cdot \mathbb{I}(z_1[j] > 0) \cdot W_1[j, k]$ ($\text{diff vs autograd} = 0.0$) |

---

## 2. Real ONNX Runtime (JVM / Android Runtime) Execution Verification

We configured `onnxruntime-android:1.20.0` inside `app/build.gradle.kts`, verified the APK packaging (all 4 ABIs present), and executed all 6 models through the official Java/JVM bindings (`ai.onnxruntime.*`) mirroring the Android execution environment:

| Model | Size | Session Creation Time | Inference Time (CPU) | Output Verification | Status |
|---|---|---|---|---|---|
| `img_det_effnet.onnx` | 40.8 MB | 712 ms | 33 ms | `logits`: `[1, 2]`, `feature_map`: `[1, 1536, 7, 7]` | **PASS** |
| `img_det_swin.onnx` | 107.3 MB | 2,261 ms | 90 ms | `logits`: `[1, 2]`, `feature_map`: `[1, 768, 7, 7]` | **PASS** |
| `voice_en.onnx` | 1.6 MB | 169 ms | 6 ms | `logits`: `[1, 2]`, `feature_map`: `[1, 256, 10, 50]` | **PASS** |
| `voice_ar.onnx` | 1.6 MB | 44 ms | 6 ms | `logits`: `[1, 2]`, `feature_map`: `[1, 256, 10, 50]` | **PASS** |
| `text_en.onnx` | 417.9 MB | 4,428 ms | 51 ms | `logits`: `[1, 2]`, `attention`: `[1, 12, 32, 32]` | **PASS** |
| `text_ar.onnx` | 516.0 MB | 5,190 ms | 45 ms | `logits`: `[1, 2]`, `attention`: `[1, 12, 32, 32]` | **PASS** |

### Key Observations for Phase 3 Model Manager:
1. **Memory & Lazy Loading:** The two large text models (`text_en` and `text_ar`) take ~4.5–5.2 seconds to parse and initialize their ONNX graph into memory. The single-session resident lazy-loading constraint in Model Manager (build plan §7 & §12) is vital to keep memory usage smooth and avoid out-of-memory crashes on device.
2. **Dynamic Shape Handling:** Real inference with dynamic batch and sequence lengths executes without tensor slicing errors or graph partitioning slowdowns.
