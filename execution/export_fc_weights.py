"""
export_fc_weights.py

Exports classifier weights for CAM post-processing:
1. img_det_effnet_fc_weights.json
   - weight: [2, 1536]
   - bias: [2]
2. img_det_swin_fc_weights.json
   - weight: [2, 768]
   - bias: [2]
3. voice_en_fc_weights.json
   - weight: [2, 256] (effective linear projection from feature channels to classes)
   - layer1_weight: [128, 256], layer1_bias: [128]
   - layer2_weight: [2, 128], layer2_bias: [2]
4. voice_ar_fc_weights.json
   - weight: [2, 256]
   - layer1_weight: [128, 256], layer1_bias: [128]
   - layer2_weight: [2, 128], layer2_bias: [2]

Saves to both D:\\ml_export\\onnx_out\\ and app\\src\\main\\assets\\models\\.
"""

import json
import os
import sys

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

import torch

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ML_EXPORT_DIR = r"D:\ml_export\onnx_out"
ASSETS_MODELS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models")


def save_json(data, filename):
    p1 = os.path.join(ML_EXPORT_DIR, filename)
    p2 = os.path.join(ASSETS_MODELS_DIR, filename)
    with open(p1, "w", encoding="utf-8") as f:
        json.dump(data, f)
    with open(p2, "w", encoding="utf-8") as f:
        json.dump(data, f)
    print(f"Saved {filename} to:\n  {p1}\n  {p2}")


def export_weights():
    os.makedirs(ML_EXPORT_DIR, exist_ok=True)
    os.makedirs(ASSETS_MODELS_DIR, exist_ok=True)

    # 1. EfficientNet-B3
    print("\n--- EfficientNet-B3 FC Weights ---")
    ckpt_eff = torch.load(r"D:\ml_export\checkpoints\Image\efficentnetb3.pt", map_location="cpu")
    sd_eff = ckpt_eff["model_state"] if "model_state" in ckpt_eff else ckpt_eff
    w_eff = sd_eff["classifier.1.weight"].tolist()
    b_eff = sd_eff["classifier.1.bias"].tolist()
    print(f"EffNet weight shape: [{len(w_eff)}, {len(w_eff[0])}], bias shape: [{len(b_eff)}]")
    save_json(
        {
            "model": "EfficientNet-B3",
            "weight": w_eff,  # [2, 1536]
            "bias": b_eff,    # [2]
            "num_classes": len(w_eff),
            "channels": len(w_eff[0]),
        },
        "img_det_effnet_fc_weights.json",
    )

    # 2. Swin-T
    print("\n--- Swin-T FC Weights ---")
    ckpt_swin = torch.load(r"D:\ml_export\checkpoints\Image\swint_T.pt", map_location="cpu")
    sd_swin = ckpt_swin["model_state"] if "model_state" in ckpt_swin else ckpt_swin
    w_swin = sd_swin["head.weight"].tolist()
    b_swin = sd_swin["head.bias"].tolist()
    print(f"Swin-T weight shape: [{len(w_swin)}, {len(w_swin[0])}], bias shape: [{len(b_swin)}]")
    save_json(
        {
            "model": "Swin-T",
            "weight": w_swin,  # [2, 768]
            "bias": b_swin,    # [2]
            "num_classes": len(w_swin),
            "channels": len(w_swin[0]),
        },
        "img_det_swin_fc_weights.json",
    )

    # 3. Audio EN
    print("\n--- Audio EN Classifier Weights ---")
    ckpt_aen = torch.load(r"D:\ml_export\checkpoints\audio_en\spectogramcnn_asvspoof.pt", map_location="cpu")
    sd_aen = ckpt_aen["model_state"] if "model_state" in ckpt_aen else ckpt_aen
    w1_aen = sd_aen["classifier.2.weight"]  # [128, 256]
    b1_aen = sd_aen["classifier.2.bias"]    # [128]
    w2_aen = sd_aen["classifier.5.weight"]  # [2, 128]
    b2_aen = sd_aen["classifier.5.bias"]    # [2]
    # Effective linear projection matrix W = W2 @ W1: shape [2, 256]
    w_eff_aen = (w2_aen @ w1_aen).tolist()
    b_eff_aen = (w2_aen @ b1_aen + b2_aen).tolist()
    print(f"Audio EN effective CAM weight shape: [{len(w_eff_aen)}, {len(w_eff_aen[0])}]")
    save_json(
        {
            "model": "SpectrogramCNN (Audio English)",
            "weight": w_eff_aen,  # [2, 256]
            "bias": b_eff_aen,    # [2]
            "num_classes": 2,
            "channels": 256,
            "layer1_weight": w1_aen.tolist(),
            "layer1_bias": b1_aen.tolist(),
            "layer2_weight": w2_aen.tolist(),
            "layer2_bias": b2_aen.tolist(),
        },
        "voice_en_fc_weights.json",
    )

    # 4. Audio AR
    print("\n--- Audio AR Classifier Weights ---")
    ckpt_aar = torch.load(r"D:\ml_export\checkpoints\audio_ar\spectogramcnn_arenav.pt", map_location="cpu")
    sd_aar = ckpt_aar["model_state"] if "model_state" in ckpt_aar else ckpt_aar
    w1_aar = sd_aar["classifier.2.weight"]  # [128, 256]
    b1_aar = sd_aar["classifier.2.bias"]    # [128]
    w2_aar = sd_aar["classifier.5.weight"]  # [2, 128]
    b2_aar = sd_aar["classifier.5.bias"]    # [2]
    w_eff_aar = (w2_aar @ w1_aar).tolist()
    b_eff_aar = (w2_aar @ b1_aar + b2_aar).tolist()
    print(f"Audio AR effective CAM weight shape: [{len(w_eff_aar)}, {len(w_eff_aar[0])}]")
    save_json(
        {
            "model": "SpectrogramCNN (Audio Arabic)",
            "weight": w_eff_aar,  # [2, 256]
            "bias": b_eff_aar,    # [2]
            "num_classes": 2,
            "channels": 256,
            "layer1_weight": w1_aar.tolist(),
            "layer1_bias": b1_aar.tolist(),
            "layer2_weight": w2_aar.tolist(),
            "layer2_bias": b2_aar.tolist(),
        },
        "voice_ar_fc_weights.json",
    )


if __name__ == "__main__":
    export_weights()
