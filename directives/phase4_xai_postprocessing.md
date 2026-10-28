# Directive: Phase 4 — XAI Post-Processing (Android)

## Goal
Turn each model's raw XAI tensor (feature map or attention weights) into a visual explanation artifact — not just a score — for every modality and both image-model options.

## Inputs
- `feature_map` output from EfficientNet-B3, Swin-T, and both SpectrogramCNN models
- `attention` output from BERT-base and AraBERT
- FC layer weights (read from the exported model or bundled alongside it — confirm which approach Phase 2 used before assuming)

## Tools / scripts to use
- Kotlin, Compose Canvas (heatmap overlay rendering)
- `AnnotatedString` (highlighted text rendering)
- `Dispatchers.Default` for all XAI computation — this must not run on the main thread

## Outputs
- **Image (EfficientNet-B3 or Swin-T):** CAM heatmap overlaid on the original image via Compose Canvas
- **Audio (either language):** CAM heatmap on the 2D log-mel spectrogram, mapped back onto the waveform's time axis
- **Text (either language):** attention weights mapped back to original tokens, rendered via `AnnotatedString` with attribution-scaled background color

## Edge cases
- **Swin-T's feature map is a patch grid, not a conv grid** — do not reuse the EfficientNet-B3 CAM function unmodified. Write a separate CAM-style function that matches Swin-T's actual output shape; verify visually (does the highlighted region correspond to a sensible image area) since patch-grid upscaling is easier to get subtly wrong than conv-grid upscaling.
- **CAM requires no gradients for either image model** — if an implementation attempt starts requiring backprop or ONNX Runtime Training, that's a sign the wrong CAM variant (Grad-CAM) is being implemented by mistake. Re-check against build plan §4 before proceeding.
- **Attention-weight-to-token mapping across tokenizer subword splits:** make sure multi-subword words get a single coherent highlight rather than fragmented per-subword coloring that reads as noise to the user.
- **Heavy XAI computation causing UI jank:** if `Dispatchers.Default` work still causes visible frame drops on the result screen, profile before optimizing blindly — CAM upscaling and Canvas drawing are the two most likely culprits, not the model inference itself (which already happened in Phase 3).
