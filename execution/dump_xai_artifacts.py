"""
dump_xai_artifacts.py

Uses PIL and onnxruntime directly without matplotlib to avoid numpy 2 binary incompatibility.
Generates:
1. sample_input_image.png
2. effnet_cam_overlay.png
3. swin_cam_overlay.png
4. voice_en_spectrogram_cam.png & voice_en_time_curve.csv
5. voice_ar_spectrogram_cam.png & voice_ar_time_curve.csv
6. text_en_highlights.json
7. text_ar_highlights.json
"""
import os
import json
import numpy as np
from PIL import Image
import onnxruntime as ort

OUTPUT_DIR = r"D:\yaqeen-gulf\xai_dumps"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def jet_colormap(intensity):
    # intensity in [0, 1] -> RGBA
    v = float(np.clip(intensity, 0.0, 1.0))
    four_x = 4.0 * v
    r = int(np.clip(min(four_x - 1.5, -four_x + 4.5), 0.0, 1.0) * 255)
    g = int(np.clip(min(four_x - 0.5, -four_x + 3.5), 0.0, 1.0) * 255)
    b = int(np.clip(min(four_x + 0.5, -four_x + 2.5), 0.0, 1.0) * 255)
    a = int(v * 180) # alpha
    return (r, g, b, a)

def magma_colormap(intensity):
    # Black/Purple -> Red/Orange -> Yellow
    v = float(np.clip(intensity, 0.0, 1.0))
    r = int(np.clip(v * 1.5, 0.0, 1.0) * 255)
    g = int(np.clip((v - 0.3) * 1.4, 0.0, 1.0) * 255)
    b = int(np.clip((v - 0.7) * 3.3, 0.0, 1.0) * 255)
    return (r, g, b)

# -------------------------------------------------------------
# 1. IMAGE CAM (EfficientNet-B3 vs Swin-T)
# -------------------------------------------------------------
print("--- 1. Image CAM (EffNet vs Swin-T) ---")

# Create a synthetic test image with a circular "anomaly" at (140, 140)
img_np = np.zeros((224, 224, 3), dtype=np.uint8)
img_np[:, :] = [180, 200, 220]
for y in range(224):
    for x in range(224):
        if (x - 140)**2 + (y - 140)**2 < 40**2:
            img_np[y, x] = [240, 60, 60]

orig_pil = Image.fromarray(img_np)
orig_pil.save(os.path.join(OUTPUT_DIR, "sample_input_image.png"))

mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
norm_img = ((img_np / 255.0).astype(np.float32) - mean) / std
norm_img = np.transpose(norm_img, (2, 0, 1))[None, :]

# EffNet
sess_eff = ort.InferenceSession(r"app\src\main\assets\models\img_det_effnet.onnx", providers=["CPUExecutionProvider"])
outs_eff = sess_eff.run(["logits", "feature_map"], {"input": norm_img})
logits_eff, feat_eff = outs_eff[0][0], outs_eff[1][0]

with open(r"app\src\main\assets\models\img_det_effnet_fc_weights.json") as f:
    w_eff_fake = np.array(json.load(f)["weight"][1], dtype=np.float32)

cam_eff = np.sum(w_eff_fake[:, None, None] * feat_eff, axis=0)
cam_eff = np.maximum(cam_eff, 0)
cam_eff = (cam_eff - cam_eff.min()) / (cam_eff.max() - cam_eff.min() + 1e-8)

cam_eff_pil = Image.fromarray((cam_eff * 255).astype(np.uint8)).resize((224, 224), Image.BILINEAR)
cam_eff_up = np.array(cam_eff_pil) / 255.0

overlay_eff = img_np.copy()
for y in range(224):
    for x in range(224):
        r, g, b, a = jet_colormap(cam_eff_up[y, x])
        alpha = a / 255.0
        overlay_eff[y, x, 0] = int(overlay_eff[y, x, 0] * (1 - alpha) + r * alpha)
        overlay_eff[y, x, 1] = int(overlay_eff[y, x, 1] * (1 - alpha) + g * alpha)
        overlay_eff[y, x, 2] = int(overlay_eff[y, x, 2] * (1 - alpha) + b * alpha)

Image.fromarray(overlay_eff).save(os.path.join(OUTPUT_DIR, "effnet_cam_overlay.png"))
print("Saved effnet_cam_overlay.png (Fake logit: {:.3f})".format(logits_eff[1]))

# Swin-T
sess_swin = ort.InferenceSession(r"app\src\main\assets\models\img_det_swin.onnx", providers=["CPUExecutionProvider"])
outs_swin = sess_swin.run(["logits", "feature_map"], {"input": norm_img})
logits_swin, feat_swin = outs_swin[0][0], outs_swin[1][0]

with open(r"app\src\main\assets\models\img_det_swin_fc_weights.json") as f:
    w_swin_fake = np.array(json.load(f)["weight"][1], dtype=np.float32)

cam_swin = np.sum(w_swin_fake[:, None, None] * feat_swin, axis=0)
cam_swin = np.maximum(cam_swin, 0)
cam_swin = (cam_swin - cam_swin.min()) / (cam_swin.max() - cam_swin.min() + 1e-8)

cam_swin_pil = Image.fromarray((cam_swin * 255).astype(np.uint8)).resize((224, 224), Image.BILINEAR)
cam_swin_up = np.array(cam_swin_pil) / 255.0

overlay_swin = img_np.copy()
for y in range(224):
    for x in range(224):
        r, g, b, a = jet_colormap(cam_swin_up[y, x])
        alpha = a / 255.0
        overlay_swin[y, x, 0] = int(overlay_swin[y, x, 0] * (1 - alpha) + r * alpha)
        overlay_swin[y, x, 1] = int(overlay_swin[y, x, 1] * (1 - alpha) + g * alpha)
        overlay_swin[y, x, 2] = int(overlay_swin[y, x, 2] * (1 - alpha) + b * alpha)

Image.fromarray(overlay_swin).save(os.path.join(OUTPUT_DIR, "swin_cam_overlay.png"))
print("Saved swin_cam_overlay.png (Fake logit: {:.3f})".format(logits_swin[1]))

# -------------------------------------------------------------
# 2. AUDIO CAM (Analytical Gradient)
# -------------------------------------------------------------
print("\n--- 2. Audio CAM (Voice EN & AR) ---")

def process_audio_sample(model_name, onnx_file, weights_file):
    spec = np.random.randn(1, 1, 80, 401).astype(np.float32) * 0.1
    spec[0, 0, 30:60, 150:250] += 2.0

    sess = ort.InferenceSession(os.path.join(r"app\src\main\assets\models", onnx_file), providers=["CPUExecutionProvider"])
    outs = sess.run(["logits", "feature_map"], {"input": spec})
    logits, feat = outs[0][0], outs[1][0]

    with open(os.path.join(r"app\src\main\assets\models", weights_file)) as f:
        w_dict = json.load(f)
    w1 = np.array(w_dict["layer1_weight"], dtype=np.float32)
    b1 = np.array(w_dict["layer1_bias"], dtype=np.float32)
    w2 = np.array(w_dict["layer2_weight"], dtype=np.float32)

    g = np.mean(feat, axis=(1, 2))
    z1 = w1 @ g + b1
    mask = (z1 > 0).astype(np.float32)
    alpha = np.sum(w2[1, :, None] * mask[:, None] * w1, axis=0)

    cam2d = np.sum(alpha[:, None, None] * feat, axis=0)
    cam2d = np.maximum(cam2d, 0)
    cam2d = (cam2d - cam2d.min()) / (cam2d.max() - cam2d.min() + 1e-8)

    time_curve = np.mean(cam2d, axis=0)
    time_curve = (time_curve - time_curve.min()) / (time_curve.max() - time_curve.min() + 1e-8)

    cam_upscaled = np.array(Image.fromarray((cam2d * 255).astype(np.uint8)).resize((401, 80), Image.BILINEAR)) / 255.0

    # Save heatmap image
    h_rgb = np.zeros((80, 401, 3), dtype=np.uint8)
    for y in range(80):
        for x in range(401):
            h_rgb[79 - y, x] = magma_colormap(cam_upscaled[y, x]) # invert Y for standard spectrogram orientation

    img_out = os.path.join(OUTPUT_DIR, f"{model_name.lower()}_spectrogram_cam.png")
    Image.fromarray(h_rgb).save(img_out)

    # Save temporal curve CSV
    csv_out = os.path.join(OUTPUT_DIR, f"{model_name.lower()}_time_curve.csv")
    time_secs = np.linspace(0, 4.0, len(time_curve))
    with open(csv_out, "w", encoding="utf-8") as f:
        f.write("time_seconds,attribution_score\n")
        for ts, sc in zip(time_secs, time_curve):
            f.write(f"{ts:.4f},{sc:.6f}\n")

    print(f"Saved {img_out} and {csv_out}")

process_audio_sample("voice_en", "voice_en.onnx", "voice_en_fc_weights.json")
process_audio_sample("voice_ar", "voice_ar.onnx", "voice_ar_fc_weights.json")

# -------------------------------------------------------------
# 3. TEXT ATTENTION HIGHLIGHTS
# -------------------------------------------------------------
print("\n--- 3. Text Attention Highlights ---")

def process_text_sample(name, onnx_file, tokens, ids, original_text):
    sess = ort.InferenceSession(os.path.join(r"app\src\main\assets\models", onnx_file), providers=["CPUExecutionProvider"])
    mask = np.ones((1, len(ids)), dtype=np.int64)
    outs = sess.run(["logits", "attention"], {"input_ids": np.array([ids], dtype=np.int64), "attention_mask": mask})
    logits, attn = outs[0][0], outs[1][0]

    mean_attn = np.mean(attn, axis=0)
    cls_scores = mean_attn[0]

    sub_scores = cls_scores[1:-1]
    norm_scores = (cls_scores - sub_scores.min()) / (sub_scores.max() - sub_scores.min() + 1e-8)
    norm_scores = np.clip(norm_scores, 0.0, 1.0)

    words = []
    i = 0
    search_idx = 0
    while i < len(tokens):
        tok = tokens[i]
        if tok in ["[CLS]", "[SEP]", "[PAD]"]:
            i += 1
            continue
        group_toks = [tok]
        group_scs = [norm_scores[i]]
        j = i + 1
        while j < len(tokens) and tokens[j].startswith("##"):
            group_toks.append(tokens[j])
            group_scs.append(norm_scores[j])
            j += 1
        
        reconstructed = "".join([t[2:] if t.startswith("##") else t for t in group_toks])
        score = float(np.max(group_scs))
        
        start = original_text.find(reconstructed, search_idx)
        if start == -1:
            start = search_idx
            end = start + len(reconstructed)
        else:
            end = start + len(reconstructed)
        search_idx = end

        words.append({
            "word": reconstructed,
            "score": float(f"{score:.4f}"),
            "start_index": start,
            "end_index": end
        })
        i = j

    out_json = {
        "text": original_text,
        "tokens": tokens,
        "token_scores": [float(f"{s:.4f}") for s in norm_scores],
        "word_highlights": words
    }
    
    out_path = os.path.join(OUTPUT_DIR, f"{name}_highlights.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out_json, f, indent=2, ensure_ascii=False)
    
    print(f"\n{name} Whole-Word Highlight Results:")
    for item in out_json["word_highlights"]:
        bar = "#" * int(item["score"] * 20)
        print(f"  [{item['start_index']}:{item['end_index']}] {item['word']:<15} | {item['score']:.3f} | {bar}")

tokens_en = ["[CLS]", "the", "deep", "##fa", "##ke", "video", "went", "viral", "[SEP]"]
ids_en = [101, 1996, 2784, 7011, 3489, 2678, 2253, 6201, 102]
process_text_sample("text_en", "text_en.onnx", tokens_en, ids_en, "the deepfake video went viral")

tokens_ar = ["[CLS]", "انتشر", "مقطع", "الفيديو", "المزيف", "بسرعة", "[SEP]"]
ids_ar = [2, 17822, 10243, 6902, 28190, 8312, 3]
process_text_sample("text_ar", "text_ar.onnx", tokens_ar, ids_ar, "انتشر مقطع الفيديو المزيف بسرعة")
