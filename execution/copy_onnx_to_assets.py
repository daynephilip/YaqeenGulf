"""
copy_onnx_to_assets.py

Bridging step (end of Phase 2):
Copies the 6 verified .onnx models from D:\\ml_export\\onnx_out\\
to app\\src\\main\\assets\\models\\ in the Android repo.

Also copies the tokenizer vocabularies from D:\\ml_export\\vocab\\
to app\\src\\main\\assets\\vocab\\ in the Android repo.
"""

import os
import shutil
import sys

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_MODELS_DIR = r"D:\ml_export\onnx_out"
SRC_VOCAB_DIR = r"D:\ml_export\vocab"

DEST_MODELS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models")
DEST_VOCAB_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "vocab")

EXPECTED_MODELS = [
    "img_det_effnet.onnx",
    "img_det_swin.onnx",
    "voice_en.onnx",
    "voice_ar.onnx",
    "text_en.onnx",
    "text_ar.onnx",
]

EXPECTED_WEIGHTS = [
    "img_det_effnet_fc_weights.json",
    "img_det_swin_fc_weights.json",
    "voice_en_fc_weights.json",
    "voice_ar_fc_weights.json",
]

EXPECTED_VOCABS = [
    "vocab_en.txt",
    "vocab_ar.txt",
]



def bridge():
    print(f"Destination models directory: {DEST_MODELS_DIR}")
    print(f"Destination vocab directory: {DEST_VOCAB_DIR}")

    os.makedirs(DEST_MODELS_DIR, exist_ok=True)
    os.makedirs(DEST_VOCAB_DIR, exist_ok=True)

    print("\n--- Copying ONNX Models ---")
    for model_name in EXPECTED_MODELS:
        src = os.path.join(SRC_MODELS_DIR, model_name)
        dst = os.path.join(DEST_MODELS_DIR, model_name)
        if not os.path.exists(src):
            raise FileNotFoundError(f"Missing expected model file: {src}")
        size_mb = os.path.getsize(src) / (1024 * 1024)
        print(f"Copying {model_name} ({size_mb:.1f} MB) -> {dst} ...")
        shutil.copy2(src, dst)

    print("\n--- Copying Classifier FC Weights for CAM ---")
    for weight_name in EXPECTED_WEIGHTS:
        src = os.path.join(SRC_MODELS_DIR, weight_name)
        dst = os.path.join(DEST_MODELS_DIR, weight_name)
        if not os.path.exists(src):
            raise FileNotFoundError(f"Missing expected weight file: {src}")
        size_kb = os.path.getsize(src) / 1024
        print(f"Copying {weight_name} ({size_kb:.1f} KB) -> {dst} ...")
        shutil.copy2(src, dst)

    print("\n--- Copying Vocabularies ---")
    for vocab_name in EXPECTED_VOCABS:
        src = os.path.join(SRC_VOCAB_DIR, vocab_name)
        dst = os.path.join(DEST_VOCAB_DIR, vocab_name)
        if not os.path.exists(src):
            raise FileNotFoundError(f"Missing expected vocab file: {src}")
        size_kb = os.path.getsize(src) / 1024
        print(f"Copying {vocab_name} ({size_kb:.1f} KB) -> {dst} ...")
        shutil.copy2(src, dst)

    print("\nBridging completed successfully!")



if __name__ == "__main__":
    bridge()
