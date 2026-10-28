"""
export_image_effnet.py

Exports fine-tuned EfficientNet-B3 to ONNX with explicit output names:
- 'logits': [batch, 2]
- 'feature_map': [batch, 1536, 7, 7] (last conv feature map before GAP, for CAM)

Checkpoint: D:\\ml_export\\checkpoints\\Image\\efficentnetb3.pt
Output: D:\\ml_export\\onnx_out\\img_det_effnet.onnx
"""

import os
import sys

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

import torch
import torch.nn as nn
from torchvision import models

CKPT_PATH = r"D:\ml_export\checkpoints\Image\efficentnetb3.pt"
OUTPUT_ONNX = r"D:\ml_export\onnx_out\img_det_effnet.onnx"


class EfficientNetB3WithCAM(nn.Module):
    def __init__(self, base_model):
        super().__init__()
        self.features = base_model.features
        self.avgpool = base_model.avgpool
        self.classifier = base_model.classifier

    def forward(self, x):
        feat = self.features(x)  # (B, 1536, 7, 7)
        pooled = self.avgpool(feat)
        flat = torch.flatten(pooled, 1)
        logits = self.classifier(flat)  # (B, 2)
        return logits, feat


def export():
    print(f"Loading checkpoint: {CKPT_PATH}")
    ckpt = torch.load(CKPT_PATH, map_location="cpu")
    state_dict = ckpt["model_state"] if "model_state" in ckpt else ckpt

    base_model = models.efficientnet_b3(weights=None)
    in_features = base_model.classifier[1].in_features
    base_model.classifier[1] = nn.Linear(in_features, 2)
    base_model.load_state_dict(state_dict)
    base_model.eval()

    wrapped_model = EfficientNetB3WithCAM(base_model)
    wrapped_model.eval()

    dummy_input = torch.randn(1, 3, 224, 224, dtype=torch.float32)

    os.makedirs(os.path.dirname(OUTPUT_ONNX), exist_ok=True)
    print(f"Exporting to ONNX: {OUTPUT_ONNX}")

    torch.onnx.export(
        wrapped_model,
        dummy_input,
        OUTPUT_ONNX,
        export_params=True,
        opset_version=14,
        do_constant_folding=True,
        input_names=["input"],
        output_names=["logits", "feature_map"],
        dynamic_axes={
            "input": {0: "batch_size"},
            "logits": {0: "batch_size"},
            "feature_map": {0: "batch_size"},
        },
        dynamo=False,
    )
    print(f"Successfully exported {OUTPUT_ONNX}")


if __name__ == "__main__":
    export()
