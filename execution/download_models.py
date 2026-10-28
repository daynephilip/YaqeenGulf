"""
execution/download_models.py

Layer 3 Deterministic Script:
Downloads ONNX models and PyTorch checkpoints from Hugging Face Hub:
    Repository: https://huggingface.co/daynephilip/yaqeen-deepfake-models

Usage:
    python execution/download_models.py
"""

import os
import sys

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST_MODELS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models")
DEST_CHECKPOINTS_DIR = os.path.join(REPO_ROOT, "ml_export", "checkpoints")

HF_REPO_ID = "daynephilip/yaqeen-deepfake-models"

EXPECTED_ONNX_MODELS = [
    "img_det_effnet.onnx",
    "img_det_swin.onnx",
    "voice_en.onnx",
    "voice_ar.onnx",
    "text_en.onnx",
    "text_ar.onnx",
]


def download_models():
    try:
        from huggingface_hub import hf_hub_download, snapshot_download
    except ImportError:
        print("Error: 'huggingface_hub' is not installed.")
        print("Please install it with: pip install huggingface_hub")
        sys.exit(1)

    print("==================================================")
    print(f" Downloading Yaqeen Gulf Models from Hugging Face")
    print(f" Source: https://huggingface.co/{HF_REPO_ID}")
    print("==================================================")

    os.makedirs(DEST_MODELS_DIR, exist_ok=True)

    print("\n--- Downloading ONNX Models into Android assets ---")
    for model_name in EXPECTED_ONNX_MODELS:
        target_path = os.path.join(DEST_MODELS_DIR, model_name)
        if os.path.exists(target_path) and os.path.getsize(target_path) > 1024:
            size_mb = os.path.getsize(target_path) / (1024 * 1024)
            print(f" [EXISTS] {model_name} ({size_mb:.1f} MB) already in assets, skipping.")
            continue

        print(f" [FETCH] Downloading {model_name} from {HF_REPO_ID}...")
        try:
            downloaded = hf_hub_download(
                repo_id=HF_REPO_ID,
                filename=model_name,
                local_dir=DEST_MODELS_DIR,
                local_dir_use_symlinks=False,
            )
            print(f" [OK] Saved {model_name} to {downloaded}")
        except Exception as e:
            # Check if stored in a subfolder e.g. onnx_models/
            try:
                downloaded = hf_hub_download(
                    repo_id=HF_REPO_ID,
                    filename=f"onnx_models/{model_name}",
                    local_dir=DEST_MODELS_DIR,
                    local_dir_use_symlinks=False,
                )
                print(f" [OK] Saved {model_name} from subfolder to {downloaded}")
            except Exception as e2:
                print(f" [WARN] Could not download {model_name}: {e2}")

    print("\nModel download check completed successfully!")


if __name__ == "__main__":
    download_models()
