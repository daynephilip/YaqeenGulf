"""
verify_onnx.py

Verification suite for Phase 2:
Loads original PyTorch models from checkpoints and ONNX models via onnxruntime.
Runs inference on test/synthetic inputs and calculates:
- PyTorch output vs ONNX output max absolute diff
- PyTorch output vs ONNX output mean absolute diff
- Confirms tolerance < 1e-3
- Verifies output shapes and tensor names ('logits', 'feature_map' or 'attention')
- Generates markdown verification report D:\\ml_export\\PHASE2_VERIFICATION_REPORT.md
  and mirrors it to d:\\yaqeen-gulf\\PHASE2_VERIFICATION_REPORT.md.
"""

import os
import sys

os.environ["USE_TF"] = "0"
os.environ["TRANSFORMERS_NO_TF"] = "1"

if sys.platform == "win32" and getattr(sys.stdout, "encoding", "").lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

import numpy as np
import torch
import torch.nn as nn
from torchvision import models
from transformers import AutoModelForSequenceClassification
import onnxruntime as ort

from export_image_effnet import EfficientNetB3WithCAM
from export_image_swin import SwinTWithCAM
from export_audio import SpectrogramCNN, SpectrogramCNNWithCAM
from export_text import BertWithFinalAttention

TOLERANCE = 1e-3


def verify_image_effnet():
    print("Verifying EfficientNet-B3...")
    ckpt_path = r"D:\ml_export\checkpoints\Image\efficentnetb3.pt"
    onnx_path = r"D:\ml_export\onnx_out\img_det_effnet.onnx"

    ckpt = torch.load(ckpt_path, map_location="cpu")
    state = ckpt["model_state"] if "model_state" in ckpt else ckpt
    base = models.efficientnet_b3(weights=None)
    base.classifier[1] = nn.Linear(base.classifier[1].in_features, 2)
    base.load_state_dict(state)
    base.eval()
    pt_model = EfficientNetB3WithCAM(base)
    pt_model.eval()

    torch.manual_seed(42)
    dummy_input = torch.randn(2, 3, 224, 224, dtype=torch.float32)

    with torch.no_grad():
        pt_logits, pt_feat = pt_model(dummy_input)

    session = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    onnx_inputs = {session.get_inputs()[0].name: dummy_input.numpy()}
    onnx_outs = session.run(None, onnx_inputs)

    out_names = [o.name for o in session.get_outputs()]
    logits_idx = out_names.index("logits")
    feat_idx = out_names.index("feature_map")

    onnx_logits = onnx_outs[logits_idx]
    onnx_feat = onnx_outs[feat_idx]

    max_diff_logits = float(np.max(np.abs(pt_logits.numpy() - onnx_logits)))
    mean_diff_logits = float(np.mean(np.abs(pt_logits.numpy() - onnx_logits)))
    max_diff_feat = float(np.max(np.abs(pt_feat.numpy() - onnx_feat)))

    passed = max_diff_logits < TOLERANCE and max_diff_feat < TOLERANCE

    return {
        "model": "EfficientNet-B3 (Image Default)",
        "file": "img_det_effnet.onnx",
        "input_shape": list(dummy_input.shape),
        "logits_shape": list(onnx_logits.shape),
        "xai_tensor": "feature_map",
        "xai_shape": list(onnx_feat.shape),
        "max_diff_logits": max_diff_logits,
        "mean_diff_logits": mean_diff_logits,
        "max_diff_xai": max_diff_feat,
        "status": "PASS" if passed else "FAIL",
    }


def verify_image_swin():
    print("Verifying Swin-T...")
    ckpt_path = r"D:\ml_export\checkpoints\Image\swint_T.pt"
    onnx_path = r"D:\ml_export\onnx_out\img_det_swin.onnx"

    ckpt = torch.load(ckpt_path, map_location="cpu")
    state = ckpt["model_state"] if "model_state" in ckpt else ckpt
    base = models.swin_t(weights=None)
    base.head = nn.Linear(base.head.in_features, 2)
    base.load_state_dict(state)
    base.eval()
    pt_model = SwinTWithCAM(base)
    pt_model.eval()

    torch.manual_seed(42)
    dummy_input = torch.randn(2, 3, 224, 224, dtype=torch.float32)

    with torch.no_grad():
        pt_logits, pt_feat = pt_model(dummy_input)

    session = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    onnx_inputs = {session.get_inputs()[0].name: dummy_input.numpy()}
    onnx_outs = session.run(None, onnx_inputs)

    out_names = [o.name for o in session.get_outputs()]
    logits_idx = out_names.index("logits")
    feat_idx = out_names.index("feature_map")

    onnx_logits = onnx_outs[logits_idx]
    onnx_feat = onnx_outs[feat_idx]

    max_diff_logits = float(np.max(np.abs(pt_logits.numpy() - onnx_logits)))
    mean_diff_logits = float(np.mean(np.abs(pt_logits.numpy() - onnx_logits)))
    max_diff_feat = float(np.max(np.abs(pt_feat.numpy() - onnx_feat)))

    passed = max_diff_logits < TOLERANCE and max_diff_feat < TOLERANCE

    return {
        "model": "Swin-T (Image Alternative)",
        "file": "img_det_swin.onnx",
        "input_shape": list(dummy_input.shape),
        "logits_shape": list(onnx_logits.shape),
        "xai_tensor": "feature_map",
        "xai_shape": list(onnx_feat.shape),
        "max_diff_logits": max_diff_logits,
        "mean_diff_logits": mean_diff_logits,
        "max_diff_xai": max_diff_feat,
        "status": "PASS" if passed else "FAIL",
    }


def verify_audio(name, ckpt_path, onnx_file):
    print(f"Verifying {name}...")
    onnx_path = os.path.join(r"D:\ml_export\onnx_out", onnx_file)

    ckpt = torch.load(ckpt_path, map_location="cpu")
    state = ckpt["model_state"] if "model_state" in ckpt else ckpt
    base = SpectrogramCNN()
    base.load_state_dict(state)
    base.eval()
    pt_model = SpectrogramCNNWithCAM(base)
    pt_model.eval()

    torch.manual_seed(42)
    dummy_input = torch.randn(2, 1, 80, 401, dtype=torch.float32)

    with torch.no_grad():
        pt_logits, pt_feat = pt_model(dummy_input)

    session = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    onnx_inputs = {session.get_inputs()[0].name: dummy_input.numpy()}
    onnx_outs = session.run(None, onnx_inputs)

    out_names = [o.name for o in session.get_outputs()]
    logits_idx = out_names.index("logits")
    feat_idx = out_names.index("feature_map")

    onnx_logits = onnx_outs[logits_idx]
    onnx_feat = onnx_outs[feat_idx]

    max_diff_logits = float(np.max(np.abs(pt_logits.numpy() - onnx_logits)))
    mean_diff_logits = float(np.mean(np.abs(pt_logits.numpy() - onnx_logits)))
    max_diff_feat = float(np.max(np.abs(pt_feat.numpy() - onnx_feat)))

    passed = max_diff_logits < TOLERANCE and max_diff_feat < TOLERANCE

    return {
        "model": name,
        "file": onnx_file,
        "input_shape": list(dummy_input.shape),
        "logits_shape": list(onnx_logits.shape),
        "xai_tensor": "feature_map",
        "xai_shape": list(onnx_feat.shape),
        "max_diff_logits": max_diff_logits,
        "mean_diff_logits": mean_diff_logits,
        "max_diff_xai": max_diff_feat,
        "status": "PASS" if passed else "FAIL",
    }


def verify_text(name, base_model_id, ckpt_path, onnx_file):
    print(f"Verifying {name}...")
    onnx_path = os.path.join(r"D:\ml_export\onnx_out", onnx_file)

    base = AutoModelForSequenceClassification.from_pretrained(
        base_model_id,
        num_labels=2,
        output_attentions=True,
    )
    ckpt = torch.load(ckpt_path, map_location="cpu")
    state = ckpt["model_state_dict"] if "model_state_dict" in ckpt else (
        ckpt["model_state"] if "model_state" in ckpt else ckpt
    )
    base.load_state_dict(state)
    base.eval()
    pt_model = BertWithFinalAttention(base)
    pt_model.eval()

    torch.manual_seed(42)
    seq_len = 32
    dummy_ids = torch.randint(low=100, high=1000, size=(2, seq_len), dtype=torch.long)
    dummy_mask = torch.ones(2, seq_len, dtype=torch.long)

    with torch.no_grad():
        pt_logits, pt_attn = pt_model(dummy_ids, dummy_mask)

    session = ort.InferenceSession(onnx_path, providers=["CPUExecutionProvider"])
    input_names = [i.name for i in session.get_inputs()]
    onnx_inputs = {
        input_names[0]: dummy_ids.numpy(),
        input_names[1]: dummy_mask.numpy(),
    }
    onnx_outs = session.run(None, onnx_inputs)

    out_names = [o.name for o in session.get_outputs()]
    logits_idx = out_names.index("logits")
    attn_idx = out_names.index("attention")

    onnx_logits = onnx_outs[logits_idx]
    onnx_attn = onnx_outs[attn_idx]

    max_diff_logits = float(np.max(np.abs(pt_logits.numpy() - onnx_logits)))
    mean_diff_logits = float(np.mean(np.abs(pt_logits.numpy() - onnx_logits)))
    max_diff_attn = float(np.max(np.abs(pt_attn.numpy() - onnx_attn)))

    passed = max_diff_logits < TOLERANCE and max_diff_attn < TOLERANCE

    return {
        "model": name,
        "file": onnx_file,
        "input_shape": [list(dummy_ids.shape), list(dummy_mask.shape)],
        "logits_shape": list(onnx_logits.shape),
        "xai_tensor": "attention",
        "xai_shape": list(onnx_attn.shape),
        "max_diff_logits": max_diff_logits,
        "mean_diff_logits": mean_diff_logits,
        "max_diff_xai": max_diff_attn,
        "status": "PASS" if passed else "FAIL",
    }


def main():
    results = []

    # 1. EfficientNet-B3
    results.append(verify_image_effnet())

    # 2. Swin-T
    results.append(verify_image_swin())

    # 3. Audio EN
    results.append(verify_audio(
        "SpectrogramCNN (Audio English)",
        r"D:\ml_export\checkpoints\audio_en\spectogramcnn_asvspoof.pt",
        "voice_en.onnx",
    ))

    # 4. Audio AR
    results.append(verify_audio(
        "SpectrogramCNN (Audio Arabic)",
        r"D:\ml_export\checkpoints\audio_ar\spectogramcnn_arenav.pt",
        "voice_ar.onnx",
    ))

    # 5. Text EN
    results.append(verify_text(
        "BERT-base (Text English)",
        "bert-base-uncased",
        r"D:\ml_export\checkpoints\text_en\bertbase_isot.pt",
        "text_en.onnx",
    ))

    # 6. Text AR
    results.append(verify_text(
        "AraBERT (Text Arabic)",
        "aubmindlab/bert-base-arabertv02",
        r"D:\ml_export\checkpoints\text_ar\arabert_afnd.pt",
        "text_ar.onnx",
    ))

    # Build Markdown Report
    report_lines = [
        "# Phase 2 — ONNX Export & XAI Tensor Verification Report",
        "",
        "**Tolerance Threshold:** `< 1e-3` max absolute difference between PyTorch and ONNX Runtime.",
        "",
        "| Model | Export File | Logits Shape | XAI Tensor & Shape | Max Logit Diff | Mean Logit Diff | Max XAI Diff | Status |",
        "|---|---|---|---|---|---|---|---|",
    ]

    all_passed = True
    for r in results:
        status_badge = f"**{r['status']}**"
        if r["status"] != "PASS":
            all_passed = False
        report_lines.append(
            f"| {r['model']} | `{r['file']}` | `{r['logits_shape']}` | `{r['xai_tensor']}`: `{r['xai_shape']}` | `{r['max_diff_logits']:.2e}` | `{r['mean_diff_logits']:.2e}` | `{r['max_diff_xai']:.2e}` | {status_badge} |"
        )

    report_lines.append("")
    if all_passed:
        report_lines.append("## Overall Result: ALL 6 MODELS PASSED VERIFICATION (TOLERANCE < 1e-3)")
    else:
        report_lines.append("## Overall Result: ONE OR MORE MODELS FAILED VERIFICATION")

    report_lines.append("")
    report_lines.append("### Summary of Outputs:")
    for r in results:
        report_lines.append(f"- **{r['file']}**: Verified output nodes `['logits', '{r['xai_tensor']}']`")

    report_content = "\n".join(report_lines) + "\n"

    report_path_ml = r"D:\ml_export\PHASE2_VERIFICATION_REPORT.md"
    report_path_repo = r"d:\yaqeen-gulf\PHASE2_VERIFICATION_REPORT.md"

    with open(report_path_ml, "w", encoding="utf-8") as f:
        f.write(report_content)
    with open(report_path_repo, "w", encoding="utf-8") as f:
        f.write(report_content)

    print("\n" + report_content)


if __name__ == "__main__":
    main()
