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

### Summary of Outputs:
- **img_det_effnet.onnx**: Verified output nodes `['logits', 'feature_map']`
- **img_det_swin.onnx**: Verified output nodes `['logits', 'feature_map']`
- **voice_en.onnx**: Verified output nodes `['logits', 'feature_map']`
- **voice_ar.onnx**: Verified output nodes `['logits', 'feature_map']`
- **text_en.onnx**: Verified output nodes `['logits', 'attention']`
- **text_ar.onnx**: Verified output nodes `['logits', 'attention']`
