"""
export_image_swin.py

Exports fine-tuned Swin-T to ONNX with explicit output names:
- 'logits': [batch, 2]
- 'feature_map': [batch, 768, 7, 7] (last stage feature map permuted to B, C, H, W for spatial CAM)

Checkpoint: D:\\ml_export\\checkpoints\\Image\\swint_T.pt
Output: D:\\ml_export\\onnx_out\\img_det_swin.onnx
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

CKPT_PATH = r"D:\ml_export\checkpoints\Image\swint_T.pt"
OUTPUT_ONNX = r"D:\ml_export\onnx_out\img_det_swin.onnx"


class SwinTWithCAM(nn.Module):
    def __init__(self, base_model):
        super().__init__()
        self.features = base_model.features
        self.norm = base_model.norm
        self.permute = base_model.permute
        self.avgpool = base_model.avgpool
        self.flatten = base_model.flatten
        self.head = base_model.head

    def forward(self, x):
        x = self.features(x)  # (B, 7, 7, 768)
        x = self.norm(x)
        feat = self.permute(x)  # (B, 768, 7, 7)
        pooled = self.avgpool(feat)
        flat = self.flatten(pooled)
        logits = self.head(flat)  # (B, 2)
        return logits, feat


def export():
    print(f"Loading checkpoint: {CKPT_PATH}")
    ckpt = torch.load(CKPT_PATH, map_location="cpu")
    state_dict = ckpt["model_state"] if "model_state" in ckpt else ckpt

    base_model = models.swin_t(weights=None)
    in_features = base_model.head.in_features
    base_model.head = nn.Linear(in_features, 2)
    base_model.load_state_dict(state_dict)
    base_model.eval()

    wrapped_model = SwinTWithCAM(base_model)
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
