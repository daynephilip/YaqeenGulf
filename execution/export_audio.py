"""
export_audio.py

Exports fine-tuned SpectrogramCNN models to ONNX:
1. voice_en.onnx (from D:\\ml_export\\checkpoints\\audio_en\\spectogramcnn_asvspoof.pt)
2. voice_ar.onnx (from D:\\ml_export\\checkpoints\\audio_ar\\spectogramcnn_arenav.pt)

Explicit outputs:
- 'logits': [batch, 2]
- 'feature_map': [batch, 256, 10, 50] (last conv feature map after ReLU before AdaptiveAvgPool2d)

Input: [batch, 1, 80, 401]
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

MODELS = [
    {
        "name": "voice_en",
        "ckpt": r"D:\ml_export\checkpoints\audio_en\spectogramcnn_asvspoof.pt",
        "output": r"D:\ml_export\onnx_out\voice_en.onnx",
    },
    {
        "name": "voice_ar",
        "ckpt": r"D:\ml_export\checkpoints\audio_ar\spectogramcnn_arenav.pt",
        "output": r"D:\ml_export\onnx_out\voice_ar.onnx",
    },
]


class SpectrogramCNNWithCAM(nn.Module):
    def __init__(self, base_model):
        super().__init__()
        self.conv_features = base_model.features[:15]
        self.avgpool = base_model.features[15]
        self.classifier = base_model.classifier

    def forward(self, x):
        feat = self.conv_features(x)  # (B, 256, 10, 50)
        pooled = self.avgpool(feat)   # (B, 256, 1, 1)
        logits = self.classifier(pooled)  # (B, 2)
        return logits, feat


class SpectrogramCNN(nn.Module):
    def __init__(self, num_classes=2, dropout=0.3):
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(1, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),  # -> (32, 40, 200)

            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),  # -> (64, 20, 100)

            nn.Conv2d(64, 128, kernel_size=3, padding=1),
            nn.BatchNorm2d(128),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),  # -> (128, 10, 50)

            nn.Conv2d(128, 256, kernel_size=3, padding=1),
            nn.BatchNorm2d(256),
            nn.ReLU(inplace=True),
            nn.AdaptiveAvgPool2d(1),  # -> (256, 1, 1)
        )
        self.classifier = nn.Sequential(
            nn.Flatten(),
            nn.Dropout(dropout),
            nn.Linear(256, 128),
            nn.ReLU(inplace=True),
            nn.Dropout(dropout),
            nn.Linear(128, num_classes),
        )

    def forward(self, x):
        x = self.features(x)
        return self.classifier(x)


def export():
    os.makedirs(r"D:\ml_export\onnx_out", exist_ok=True)
    dummy_input = torch.randn(1, 1, 80, 401, dtype=torch.float32)

    for item in MODELS:
        print(f"\n--- Processing {item['name']} ---")
        print(f"Loading checkpoint: {item['ckpt']}")
        ckpt = torch.load(item["ckpt"], map_location="cpu")
        state_dict = ckpt["model_state"] if "model_state" in ckpt else ckpt

        base_model = SpectrogramCNN()
        base_model.load_state_dict(state_dict)
        base_model.eval()

        wrapped_model = SpectrogramCNNWithCAM(base_model)
        wrapped_model.eval()

        print(f"Exporting to ONNX: {item['output']}")
        torch.onnx.export(
            wrapped_model,
            dummy_input,
            item["output"],
            export_params=True,
            opset_version=14,
            do_constant_folding=True,
            input_names=["input"],
            output_names=["logits", "feature_map"],
            dynamic_axes={
                "input": {0: "batch_size", 3: "time_steps"},
                "logits": {0: "batch_size"},
                "feature_map": {0: "batch_size", 3: "feat_time_steps"},
            },
            dynamo=False,
        )
        print(f"Successfully exported {item['output']}")


if __name__ == "__main__":
    export()
