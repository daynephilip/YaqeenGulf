# Directive: Phase 3 — Inference Engine & Preprocessing (Android)

## Goal
Get all 6 ONNX models running on-device via ONNX Runtime Mobile, with correct preprocessing per modality, producing raw logits that match the Phase 2 verification report on the same sample inputs.

## Inputs
- The 6 `.onnx` files in `app/src/main/assets/models/` (copied at the end of Phase 2)
- Vocab files for `text_en`/`text_ar` in `app/src/main/assets/vocab/`
- Phase 2's verification report (expected logit values for the same sample inputs, used as the pass/fail bar here)

## Tools / scripts to use
- `com.microsoft.onnxruntime:onnxruntime-android` (Gradle dependency)
- WordPiece tokenizer (DJL Android artifact, or equivalent) — **not** SentencePiece/BPE
- Custom Kotlin FFT/mel-filterbank (ported from the validated Python `spectrogram_dataset.py` logic): 80 mel bins, n_fft=512, hop=160, win=400, per-chunk zero-mean/unit-std normalization

## Outputs
- Given a sample image/text/audio input, the app produces raw logits matching Phase 2's verification report (within the same tolerance used there)
- A working image-model selector in Settings (EfficientNet-B3 default, Swin-T alternative) wired to actually load the corresponding model
- A Model Manager that lazy-loads models and keeps only one ONNX Runtime session resident at a time

## Edge cases
- **Loading more than one model at once will OOM on typical devices** — this is a hard constraint, not a performance suggestion. The Model Manager must fully unload the previous session before loading the next, even when switching quickly (e.g. user flips the image-model selector then immediately runs a scan).
- **Text model checkpoints are large (bertbase_isot ~1.3GB, arabert_afnd ~1.6GB pre-export)** — the ONNX-exported versions should be substantially smaller, but if load times or memory pressure are still an issue on target devices, flag ONNX quantization as a possible follow-up rather than silently degrading accuracy to fix it without asking.
- **Tokenizer/vocab mismatch:** verify against Phase 2's vocab pairing before assuming a bug in the tokenizer implementation itself — garbage attributions downstream are more likely a mismatched vocab than a broken WordPiece implementation.
- **Mel-spectrogram output doesn't match Phase 2/training-time values:** check normalization order and FFT windowing parameters against `spectrogram_dataset.py` line-by-line before assuming the ONNX export is at fault — this preprocessing step is a common silent-mismatch point between training and on-device inference.
