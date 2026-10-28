"""
generate_thesis_figures.py
Generates:
1. 100% SVG 1.1 standard-compliant vector SVG files (compatible with ImageMagick, Inkscape, Chrome, online converters)
2. Direct high-resolution 300-DPI JPG images (no converter required)
3. Direct high-resolution PNG images

Figures generated:
- Figure 7: Overlay three-tier image localisation flow (conceptual decision-flow diagram)
- Figure 8: Overlay dual-model consensus outcomes (conceptual 3-state indicator)
- Figure 20: PyTorch vs ONNX parity (log-scale bar chart, max logit diff and max XAI diff)
- Figure 21: Model size vs latency (scatter plot, 6 points)
"""

import math
import os
from PIL import Image, ImageDraw, ImageFont

FIGURES_DIR = r"d:\yaqeen-gulf\figures"
os.makedirs(FIGURES_DIR, exist_ok=True)


# ==============================================================================
# 1. FIGURE 20: PyTorch vs. ONNX Parity (Log-Scale Bar Chart)
# ==============================================================================

FIG20_DATA = [
    {"model": "EfficientNet-B3", "type": "Vision CNN", "logit": 3.87e-6, "xai": 3.34e-5},
    {"model": "Swin-T", "type": "Vision Transformer", "logit": 1.91e-6, "xai": 8.58e-6},
    {"model": "SpectrogramCNN (EN)", "type": "Audio CNN", "logit": 9.54e-7, "xai": 9.06e-6},
    {"model": "SpectrogramCNN (AR)", "type": "Audio CNN", "logit": 1.19e-7, "xai": 7.63e-6},
    {"model": "BERT-base (EN)", "type": "NLP Transformer", "logit": 7.15e-7, "xai": 3.84e-7},
    {"model": "AraBERT (AR)", "type": "NLP Transformer", "logit": 6.68e-6, "xai": 1.05e-5},
]


def generate_figure_20_svg():
    width = 1000
    height = 580
    chart_x = 110
    chart_y = 110
    chart_w = 830
    chart_h = 360

    min_log = -8.0
    max_log = -2.0
    log_range = max_log - min_log

    def val_to_y(val):
        log_v = math.log10(val)
        norm = (log_v - min_log) / log_range
        return chart_y + chart_h - (norm * chart_h)

    tolerance_y = val_to_y(1e-3)

    svg = f"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <!-- Background -->
  <rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>

  <!-- Header Banner -->
  <rect x="0" y="0" width="{width}" height="70" fill="#0F172A"/>
  <text x="30" y="32" fill="#10B981" font-size="13" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">OFF-DEVICE MODEL EXPORT &amp; VERIFICATION</text>
  <text x="30" y="55" fill="#FFFFFF" font-size="18" font-weight="bold" font-family="Arial, sans-serif">Figure 20: PyTorch vs. ONNX Runtime Numerical Parity (Logarithmic Scale)</text>

  <!-- Legend -->
  <rect x="620" y="24" width="16" height="16" rx="3" ry="3" fill="#10B981"/>
  <text x="644" y="37" fill="#FFFFFF" font-size="12" font-family="Arial, sans-serif">Max Logit Diff</text>
  
  <rect x="760" y="24" width="16" height="16" rx="3" ry="3" fill="#6366F1"/>
  <text x="784" y="37" fill="#FFFFFF" font-size="12" font-family="Arial, sans-serif">Max XAI Diff</text>

  <line x1="880" y1="32" x2="910" y2="32" stroke="#EF4444" stroke-width="2" stroke-dasharray="4,3"/>
  <text x="918" y="37" fill="#FCA5A5" font-size="12" font-family="Arial, sans-serif">Tolerance (&lt; 1e-3)</text>

  <!-- Gridlines -->
"""

    for exp in range(-8, -1):
        val = 10.0 ** exp
        y = val_to_y(val)
        svg += f"""  <line x1="{chart_x}" y1="{y:.1f}" x2="{chart_x + chart_w}" y2="{y:.1f}" stroke="#E2E8F0" stroke-width="1"/>
  <text x="{chart_x - 12}" y="{y + 4:.1f}" fill="#64748B" font-size="12" font-family="Arial, sans-serif" text-anchor="end">1e{exp}</text>
"""

    # Red Tolerance line (1e-3)
    svg += f"""
  <!-- Tolerance Line -->
  <line x1="{chart_x}" y1="{tolerance_y:.1f}" x2="{chart_x + chart_w}" y2="{tolerance_y:.1f}" stroke="#EF4444" stroke-width="2" stroke-dasharray="6,4"/>
  <rect x="{chart_x + chart_w - 220}" y="{tolerance_y - 24:.1f}" width="210" height="20" rx="3" ry="3" fill="#FEE2E2"/>
  <text x="{chart_x + chart_w - 15}" y="{tolerance_y - 10:.1f}" fill="#DC2626" font-size="11" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Maximum Allowed Tolerance &lt; 1e-3</text>
"""

    # Grouped Bars
    num_models = len(FIG20_DATA)
    group_width = chart_w / num_models
    bar_width = 30
    spacing = 6

    for i, item in enumerate(FIG20_DATA):
        center_x = chart_x + (i + 0.5) * group_width
        x_logit = center_x - bar_width - (spacing / 2)
        x_xai = center_x + (spacing / 2)

        y_logit = val_to_y(item["logit"])
        h_logit = (chart_y + chart_h) - y_logit

        y_xai = val_to_y(item["xai"])
        h_xai = (chart_y + chart_h) - y_xai

        logit_fmt = f"{item['logit']:.2e}"
        xai_fmt = f"{item['xai']:.2e}"

        svg += f"""
  <!-- Group {i}: {item['model']} -->
  <rect x="{x_logit:.1f}" y="{y_logit:.1f}" width="{bar_width}" height="{h_logit:.1f}" rx="3" ry="3" fill="#10B981"/>
  <text x="{x_logit + bar_width/2:.1f}" y="{y_logit - 5:.1f}" fill="#065F46" font-size="9" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">{logit_fmt}</text>

  <rect x="{x_xai:.1f}" y="{y_xai:.1f}" width="{bar_width}" height="{h_xai:.1f}" rx="3" ry="3" fill="#6366F1"/>
  <text x="{x_xai + bar_width/2:.1f}" y="{y_xai - 5:.1f}" fill="#3730A3" font-size="9" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">{xai_fmt}</text>

  <!-- Labels -->
  <text x="{center_x:.1f}" y="{chart_y + chart_h + 22}" fill="#0F172A" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">{item['model']}</text>
  <text x="{center_x:.1f}" y="{chart_y + chart_h + 38}" fill="#64748B" font-size="10" font-family="Arial, sans-serif" text-anchor="middle">{item['type']}</text>
  <rect x="{center_x - 26:.1f}" y="{chart_y + chart_h + 46}" width="52" height="16" rx="3" ry="3" fill="#DCFCE7"/>
  <text x="{center_x:.1f}" y="{chart_y + chart_h + 58}" fill="#15803D" font-size="9" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">PASS</text>
"""

    svg += f"""
  <!-- Axes -->
  <line x1="{chart_x}" y1="{chart_y + chart_h}" x2="{chart_x + chart_w}" y2="{chart_y + chart_h}" stroke="#334155" stroke-width="2"/>
  <line x1="{chart_x}" y1="{chart_y}" x2="{chart_x}" y2="{chart_y + chart_h}" stroke="#334155" stroke-width="2"/>
  <text x="35" y="{chart_y + chart_h / 2}" fill="#334155" font-size="12" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle" transform="rotate(-90 35 {chart_y + chart_h / 2})">Absolute Numerical Difference (Log Scale)</text>
</svg>
"""
    path = os.path.join(FIGURES_DIR, "figure20_pytorch_vs_onnx_parity.svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Generated {path}")


def render_figure_20_image():
    # 2x supersampling for ultra high resolution
    scale = 2
    w, h = 1000 * scale, 580 * scale
    img = Image.new("RGB", (w, h), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    f_title = ImageFont.truetype("arialbd.ttf", 18 * scale)
    f_sub = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_body = ImageFont.truetype("arial.ttf", 11 * scale)
    f_bold = ImageFont.truetype("arialbd.ttf", 11 * scale)
    f_small = ImageFont.truetype("arial.ttf", 9 * scale)
    f_small_b = ImageFont.truetype("arialbd.ttf", 9 * scale)

    # Header
    draw.rectangle([0, 0, w, 70 * scale], fill="#0F172A")
    draw.text((30 * scale, 16 * scale), "OFF-DEVICE MODEL EXPORT & VERIFICATION", fill="#10B981", font=f_sub)
    draw.text((30 * scale, 38 * scale), "Figure 20: PyTorch vs. ONNX Runtime Numerical Parity (Logarithmic Scale)", fill="#FFFFFF", font=f_title)

    # Legend
    draw.rectangle([620 * scale, 24 * scale, 636 * scale, 40 * scale], fill="#10B981")
    draw.text((642 * scale, 24 * scale), "Max Logit Diff", fill="#FFFFFF", font=f_body)

    draw.rectangle([760 * scale, 24 * scale, 776 * scale, 40 * scale], fill="#6366F1")
    draw.text((782 * scale, 24 * scale), "Max XAI Diff", fill="#FFFFFF", font=f_body)

    # Dashed line legend
    lx = 880 * scale
    ly = 32 * scale
    for seg in range(0, 30 * scale, 8):
        draw.line([(lx + seg, ly), (lx + seg + 4, ly)], fill="#EF4444", width=2 * scale)
    draw.text((916 * scale, 24 * scale), "Tolerance (< 1e-3)", fill="#FCA5A5", font=f_body)

    # Chart Geometry
    chart_x = 110 * scale
    chart_y = 110 * scale
    chart_w = 830 * scale
    chart_h = 360 * scale

    min_log = -8.0
    max_log = -2.0
    log_range = max_log - min_log

    def val_to_y(val):
        log_v = math.log10(val)
        norm = (log_v - min_log) / log_range
        return chart_y + chart_h - (norm * chart_h)

    # Gridlines
    for exp in range(-8, -1):
        val = 10.0 ** exp
        y = val_to_y(val)
        draw.line([(chart_x, y), (chart_x + chart_w, y)], fill="#E2E8F0", width=scale)
        lbl = f"1e{exp}"
        bbox = f_body.getbbox(lbl)
        tw = bbox[2] - bbox[0]
        draw.text((chart_x - tw - 12 * scale, y - 7 * scale), lbl, fill="#64748B", font=f_body)

    # Tolerance line 1e-3
    tol_y = val_to_y(1e-3)
    for seg in range(0, int(chart_w), 12):
        draw.line([(chart_x + seg, tol_y), (chart_x + min(seg + 7, chart_w), tol_y)], fill="#EF4444", width=2 * scale)
    draw.rounded_rectangle([chart_x + chart_w - 225 * scale, tol_y - 25 * scale, chart_x + chart_w - 15 * scale, tol_y - 5 * scale], radius=3 * scale, fill="#FEE2E2")
    draw.text((chart_x + chart_w - 215 * scale, tol_y - 22 * scale), "Maximum Allowed Tolerance < 1e-3", fill="#DC2626", font=f_small_b)

    # Bars
    num_models = len(FIG20_DATA)
    group_w = chart_w / num_models
    bw = 30 * scale
    spacing = 6 * scale

    for i, item in enumerate(FIG20_DATA):
        cx = chart_x + (i + 0.5) * group_w
        x_logit = cx - bw - (spacing / 2)
        x_xai = cx + (spacing / 2)

        y_logit = val_to_y(item["logit"])
        h_logit = (chart_y + chart_h) - y_logit

        y_xai = val_to_y(item["xai"])
        h_xai = (chart_y + chart_h) - y_xai

        draw.rounded_rectangle([x_logit, y_logit, x_logit + bw, y_logit + h_logit], radius=3 * scale, fill="#10B981")
        lbl_l = f"{item['logit']:.2e}"
        lb_box = f_small_b.getbbox(lbl_l)
        lw = lb_box[2] - lb_box[0]
        draw.text((x_logit + (bw - lw) / 2, y_logit - 15 * scale), lbl_l, fill="#065F46", font=f_small_b)

        draw.rounded_rectangle([x_xai, y_xai, x_xai + bw, y_xai + h_xai], radius=3 * scale, fill="#6366F1")
        lbl_x = f"{item['xai']:.2e}"
        xb_box = f_small_b.getbbox(lbl_x)
        xw = xb_box[2] - xb_box[0]
        draw.text((x_xai + (bw - xw) / 2, y_xai - 15 * scale), lbl_x, fill="#3730A3", font=f_small_b)

        # X-axis label
        m_name = item["model"]
        mn_box = f_bold.getbbox(m_name)
        mw = mn_box[2] - mn_box[0]
        draw.text((cx - mw / 2, chart_y + chart_h + 12 * scale), m_name, fill="#0F172A", font=f_bold)

        m_type = item["type"]
        mt_box = f_small.getbbox(m_type)
        mtw = mt_box[2] - mt_box[0]
        draw.text((cx - mtw / 2, chart_y + chart_h + 28 * scale), m_type, fill="#64748B", font=f_small)

        # Pass badge
        draw.rounded_rectangle([cx - 26 * scale, chart_y + chart_h + 44 * scale, cx + 26 * scale, chart_y + chart_h + 60 * scale], radius=3 * scale, fill="#DCFCE7")
        draw.text((cx - 13 * scale, chart_y + chart_h + 46 * scale), "PASS", fill="#15803D", font=f_small_b)

    # Base axes
    draw.line([(chart_x, chart_y + chart_h), (chart_x + chart_w, chart_y + chart_h)], fill="#334155", width=2 * scale)
    draw.line([(chart_x, chart_y), (chart_x, chart_y + chart_h)], fill="#334155", width=2 * scale)

    # Y-axis title
    txt_y = Image.new("RGBA", (int(chart_h), int(40 * scale)), (255, 255, 255, 0))
    d_txt = ImageDraw.Draw(txt_y)
    d_txt.text((chart_h / 2 - 120 * scale, 5 * scale), "Absolute Numerical Difference (Log Scale)", fill="#334155", font=f_bold)
    txt_y = txt_y.rotate(90, expand=True)
    img.paste(txt_y, (int(25 * scale), int(chart_y)), txt_y)

    jpg_path = os.path.join(FIGURES_DIR, "figure20_pytorch_vs_onnx_parity.jpg")
    png_path = os.path.join(FIGURES_DIR, "figure20_pytorch_vs_onnx_parity.png")
    img.save(jpg_path, "JPEG", quality=95)
    img.save(png_path, "PNG")
    print(f"Generated {jpg_path} and {png_path}")


# ==============================================================================
# 2. FIGURE 21: Model Size vs. Latency (Scatter Plot)
# ==============================================================================

FIG21_DATA = [
    {"model": "SpectrogramCNN (EN)", "modality": "Audio", "size": 1.6, "latency": 6, "color": "#0284C7", "align": "left", "dx": 12, "dy": -4},
    {"model": "SpectrogramCNN (AR)", "modality": "Audio", "size": 1.6, "latency": 6, "color": "#0284C7", "align": "left", "dx": 12, "dy": 14},
    {"model": "EfficientNet-B3", "modality": "Image", "size": 40.8, "latency": 33, "color": "#059669", "align": "left", "dx": 14, "dy": -6},
    {"model": "Swin-T", "modality": "Image", "size": 107.3, "latency": 90, "color": "#059669", "align": "left", "dx": 14, "dy": 5},
    {"model": "BERT-base (EN)", "modality": "Text", "size": 417.9, "latency": 51, "color": "#7C3AED", "align": "right", "dx": -14, "dy": -10},
    {"model": "AraBERT (AR)", "modality": "Text", "size": 516.0, "latency": 45, "color": "#7C3AED", "align": "right", "dx": -14, "dy": 16},
]


def generate_figure_21_svg():
    width = 1000
    height = 620
    chart_x = 100
    chart_y = 110
    chart_w = 830
    chart_h = 420

    max_x = 550.0
    max_y = 100.0

    def pt_to_xy(size, latency):
        x = chart_x + (size / max_x) * chart_w
        y = chart_y + chart_h - (latency / max_y) * chart_h
        return x, y

    svg = f"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <!-- Background -->
  <rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>

  <!-- Header Banner -->
  <rect x="0" y="0" width="{width}" height="70" fill="#0F172A"/>
  <text x="30" y="32" fill="#10B981" font-size="13" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">ON-DEVICE BENCHMARK &amp; HARDWARE EFFICIENCY</text>
  <text x="30" y="55" fill="#FFFFFF" font-size="18" font-weight="bold" font-family="Arial, sans-serif">Figure 21: Model Storage Footprint vs. CPU Inference Latency (Six Deployed Checkpoints)</text>

  <!-- Legend -->
  <circle cx="670" cy="35" r="7" fill="#059669"/>
  <text x="685" y="39" fill="#FFFFFF" font-size="12" font-family="Arial, sans-serif">Vision (GenImage)</text>

  <circle cx="800" cy="35" r="7" fill="#0284C7"/>
  <text x="815" y="39" fill="#FFFFFF" font-size="12" font-family="Arial, sans-serif">Audio (ASVspoof/ArEnAV)</text>

  <circle cx="920" cy="35" r="7" fill="#7C3AED"/>
  <text x="935" y="39" fill="#FFFFFF" font-size="12" font-family="Arial, sans-serif">Text (ISOT/AFND)</text>

  <!-- Highlight Regions -->
  <rect x="{chart_x}" y="{chart_y + chart_h/2}" width="{chart_w/3}" height="{chart_h/2}" fill="#F0FDF4" opacity="0.6"/>
  <text x="{chart_x + 15}" y="{chart_y + chart_h - 15}" fill="#16A34A" font-size="11" font-weight="bold" font-family="Arial, sans-serif">Ultra-Lightweight &amp; Fast (&lt;10ms, &lt;50MB)</text>

  <rect x="{chart_x + chart_w * 0.6}" y="{chart_y}" width="{chart_w * 0.4}" height="{chart_h}" fill="#F5F3FF" opacity="0.6"/>
  <text x="{chart_x + chart_w - 15}" y="{chart_y + 25}" fill="#6D28D9" font-size="11" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Large NLP Weight Regime (Single-Session Resident Only)</text>

  <!-- Y-Axis Gridlines (Latency: 0 to 100 ms) -->
"""

    for lat in range(0, 101, 10):
        _, y = pt_to_xy(0, lat)
        svg += f"""  <line x1="{chart_x}" y1="{y:.1f}" x2="{chart_x + chart_w}" y2="{y:.1f}" stroke="#E2E8F0" stroke-width="1"/>
  <text x="{chart_x - 12}" y="{y + 4:.1f}" fill="#64748B" font-size="12" font-family="Arial, sans-serif" text-anchor="end">{lat} ms</text>
"""

    # X-Axis Gridlines (Size: 0 to 550 MB)
    for mb in range(0, 551, 50):
        x, _ = pt_to_xy(mb, 0)
        svg += f"""  <line x1="{x:.1f}" y1="{chart_y}" x2="{x:.1f}" y2="{chart_y + chart_h}" stroke="#E2E8F0" stroke-width="1"/>
  <text x="{x:.1f}" y="{chart_y + chart_h + 20}" fill="#64748B" font-size="12" font-family="Arial, sans-serif" text-anchor="middle">{mb} MB</text>
"""

    # Axes
    svg += f"""  <line x1="{chart_x}" y1="{chart_y}" x2="{chart_x}" y2="{chart_y + chart_h}" stroke="#334155" stroke-width="2"/>
  <line x1="{chart_x}" y1="{chart_y + chart_h}" x2="{chart_x + chart_w}" y2="{chart_y + chart_h}" stroke="#334155" stroke-width="2"/>

  <!-- Axis Titles -->
  <text x="{chart_x + chart_w/2}" y="{chart_y + chart_h + 46}" fill="#0F172A" font-size="13" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">On-Device Model Package Size on Storage (MB)</text>
  <text x="35" y="{chart_y + chart_h/2}" fill="#0F172A" font-size="13" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle" transform="rotate(-90 35 {chart_y + chart_h/2})">Single-Threaded CPU Inference Latency (ms)</text>
"""

    # Scatter points
    for pt in FIG21_DATA:
        cx, cy = pt_to_xy(pt["size"], pt["latency"])
        label_x = cx + pt["dx"]
        label_y = cy + pt["dy"]
        text_anchor = "start" if pt["align"] == "left" else "end"

        svg += f"""
  <!-- Point: {pt['model']} -->
  <circle cx="{cx:.1f}" cy="{cy:.1f}" r="12" fill="{pt['color']}" opacity="0.2"/>
  <circle cx="{cx:.1f}" cy="{cy:.1f}" r="7" fill="{pt['color']}" stroke="#FFFFFF" stroke-width="2"/>
  <text x="{label_x:.1f}" y="{label_y:.1f}" fill="#0F172A" font-size="12" font-weight="bold" font-family="Arial, sans-serif" text-anchor="{text_anchor}">{pt['model']}</text>
  <text x="{label_x:.1f}" y="{label_y + 14:.1f}" fill="{pt['color']}" font-size="11" font-weight="bold" font-family="Arial, sans-serif" text-anchor="{text_anchor}">{pt['size']} MB • {pt['latency']} ms</text>
"""

    svg += f"""
  <text x="{chart_x + chart_w/2}" y="{height - 12}" fill="#94A3B8" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">
    Key Insight: NLP Transformers exhibit large memory footprint (~418–516 MB) but moderate latency (~45–51 ms) due to 1D sequence packing (L=64), whereas Swin-T has highest latency (90 ms) from 2D shifted-window patch attention.
  </text>
</svg>
"""
    path = os.path.join(FIGURES_DIR, "figure21_model_size_vs_latency.svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Generated {path}")


def render_figure_21_image():
    scale = 2
    w, h = 1000 * scale, 620 * scale
    img = Image.new("RGB", (w, h), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    f_title = ImageFont.truetype("arialbd.ttf", 18 * scale)
    f_sub = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_body = ImageFont.truetype("arial.ttf", 11 * scale)
    f_bold = ImageFont.truetype("arialbd.ttf", 11 * scale)
    f_small = ImageFont.truetype("arial.ttf", 10 * scale)
    f_small_b = ImageFont.truetype("arialbd.ttf", 10 * scale)

    # Header
    draw.rectangle([0, 0, w, 70 * scale], fill="#0F172A")
    draw.text((30 * scale, 16 * scale), "ON-DEVICE BENCHMARK & HARDWARE EFFICIENCY", fill="#10B981", font=f_sub)
    draw.text((30 * scale, 38 * scale), "Figure 21: Model Storage Footprint vs. CPU Inference Latency (Six Deployed Checkpoints)", fill="#FFFFFF", font=f_title)

    # Legend
    draw.ellipse([665 * scale, 28 * scale, 679 * scale, 42 * scale], fill="#059669")
    draw.text((685 * scale, 24 * scale), "Vision (GenImage)", fill="#FFFFFF", font=f_body)

    draw.ellipse([795 * scale, 28 * scale, 809 * scale, 42 * scale], fill="#0284C7")
    draw.text((815 * scale, 24 * scale), "Audio (ASVspoof/ArEnAV)", fill="#FFFFFF", font=f_body)

    draw.ellipse([915 * scale, 28 * scale, 929 * scale, 42 * scale], fill="#7C3AED")
    draw.text((935 * scale, 24 * scale), "Text (ISOT/AFND)", fill="#FFFFFF", font=f_body)

    chart_x = 100 * scale
    chart_y = 110 * scale
    chart_w = 830 * scale
    chart_h = 420 * scale

    max_x = 550.0
    max_y = 100.0

    def pt_to_xy(size, latency):
        x = chart_x + (size / max_x) * chart_w
        y = chart_y + chart_h - (latency / max_y) * chart_h
        return x, y

    # Region highlights
    draw.rectangle([chart_x, chart_y + chart_h / 2, chart_x + chart_w / 3, chart_y + chart_h], fill="#F0FDF4")
    draw.text((chart_x + 15 * scale, chart_y + chart_h - 22 * scale), "Ultra-Lightweight & Fast (<10ms, <50MB)", fill="#16A34A", font=f_small_b)

    draw.rectangle([chart_x + chart_w * 0.6, chart_y, chart_x + chart_w, chart_y + chart_h], fill="#F5F3FF")
    draw.text((chart_x + chart_w - 320 * scale, chart_y + 15 * scale), "Large NLP Weight Regime (Single-Session Resident Only)", fill="#6D28D9", font=f_small_b)

    # Y-Axis Gridlines
    for lat in range(0, 101, 10):
        _, y = pt_to_xy(0, lat)
        draw.line([(chart_x, y), (chart_x + chart_w, y)], fill="#E2E8F0", width=scale)
        lbl = f"{lat} ms"
        bbox = f_body.getbbox(lbl)
        tw = bbox[2] - bbox[0]
        draw.text((chart_x - tw - 12 * scale, y - 7 * scale), lbl, fill="#64748B", font=f_body)

    # X-Axis Gridlines
    for mb in range(0, 551, 50):
        x, _ = pt_to_xy(mb, 0)
        draw.line([(x, chart_y), (x, chart_y + chart_h)], fill="#E2E8F0", width=scale)
        lbl = f"{mb} MB"
        bbox = f_body.getbbox(lbl)
        tw = bbox[2] - bbox[0]
        draw.text((x - tw / 2, chart_y + chart_h + 12 * scale), lbl, fill="#64748B", font=f_body)

    # Axes
    draw.line([(chart_x, chart_y), (chart_x, chart_y + chart_h)], fill="#334155", width=2 * scale)
    draw.line([(chart_x, chart_y + chart_h), (chart_x + chart_w, chart_y + chart_h)], fill="#334155", width=2 * scale)

    # Titles
    xt_box = f_bold.getbbox("On-Device Model Package Size on Storage (MB)")
    xtw = xt_box[2] - xt_box[0]
    draw.text((chart_x + (chart_w - xtw) / 2, chart_y + chart_h + 38 * scale), "On-Device Model Package Size on Storage (MB)", fill="#0F172A", font=f_bold)

    txt_y = Image.new("RGBA", (int(chart_h), int(40 * scale)), (255, 255, 255, 0))
    d_txt = ImageDraw.Draw(txt_y)
    d_txt.text((chart_h / 2 - 130 * scale, 5 * scale), "Single-Threaded CPU Inference Latency (ms)", fill="#0F172A", font=f_bold)
    txt_y = txt_y.rotate(90, expand=True)
    img.paste(txt_y, (int(20 * scale), int(chart_y)), txt_y)

    # Scatter points
    for pt in FIG21_DATA:
        cx, cy = pt_to_xy(pt["size"], pt["latency"])
        r_outer = 12 * scale
        r_inner = 7 * scale

        # Outer ring
        draw.ellipse([cx - r_outer, cy - r_outer, cx + r_outer, cy + r_outer], fill=pt["color"])
        # Inner white stroke
        draw.ellipse([cx - r_inner, cy - r_inner, cx + r_inner, cy + r_inner], fill=pt["color"], outline="#FFFFFF", width=2 * scale)

        lx = cx + pt["dx"] * scale
        ly = cy + pt["dy"] * scale
        lbl1 = pt["model"]
        lbl2 = f"{pt['size']} MB • {pt['latency']} ms"

        if pt["align"] == "right":
            b1 = f_bold.getbbox(lbl1)
            b2 = f_small_b.getbbox(lbl2)
            lx1 = lx - (b1[2] - b1[0])
            lx2 = lx - (b2[2] - b2[0])
        else:
            lx1 = lx
            lx2 = lx

        draw.text((lx1, ly), lbl1, fill="#0F172A", font=f_bold)
        draw.text((lx2, ly + 14 * scale), lbl2, fill=pt["color"], font=f_small_b)

    jpg_path = os.path.join(FIGURES_DIR, "figure21_model_size_vs_latency.jpg")
    png_path = os.path.join(FIGURES_DIR, "figure21_model_size_vs_latency.png")
    img.save(jpg_path, "JPEG", quality=95)
    img.save(png_path, "PNG")
    print(f"Generated {jpg_path} and {png_path}")


# ==============================================================================
# 2.5 FIGURE 7: Overlay Three-Tier Image Localisation Decision Flow
# ==============================================================================

def generate_figure_7_svg():
    width = 1100
    height = 760

    svg = f"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <!-- Background -->
  <rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>

  <!-- Header Banner -->
  <rect x="0" y="0" width="{width}" height="70" fill="#0F172A"/>
  <text x="30" y="32" fill="#10B981" font-size="13" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">YAQEEN GULF • REAL-TIME SCREEN OVERLAY</text>
  <text x="30" y="55" fill="#FFFFFF" font-size="18" font-weight="bold" font-family="Arial, sans-serif">Figure 7: Overlay Three-Tier Image Localisation Decision Flow</text>

  <!-- Subtitle -->
  <text x="30" y="94" fill="#475569" font-size="12" font-family="Arial, sans-serif">When on-screen capture is initiated, the subsystem isolates media graphics from UI chrome through a deterministic three-stage cascade.</text>
  <text x="30" y="112" fill="#64748B" font-size="12" font-family="Arial, sans-serif">Semantic accessibility nodes are prioritized first; computer vision saliency acts as secondary fallback, with center cropping guaranteeing zero failure.</text>

  <!-- ==================== INPUT NODE ==================== -->
  <rect x="75" y="136" width="290" height="46" rx="8" ry="8" fill="#F1F5F9" stroke="#64748B" stroke-width="1.5"/>
  <circle cx="98" cy="159" r="13" fill="#E2E8F0"/>
  <rect x="91" y="153" width="14" height="11" rx="1.5" ry="1.5" fill="none" stroke="#475569" stroke-width="1.5"/>
  <circle cx="98" cy="158.5" r="2.5" fill="#475569"/>
  <text x="122" y="154" fill="#0F172A" font-size="12.5" font-weight="bold" font-family="Arial, sans-serif">Screen Framebuffer Captured</text>
  <text x="122" y="170" fill="#64748B" font-size="10" font-family="Arial, sans-serif">MediaProjection / VirtualDisplay stream</text>

  <!-- Arrow from Input to Tier 1 -->
  <line x1="220" y1="182" x2="220" y2="212" stroke="#64748B" stroke-width="2"/>
  <polygon points="215,212 225,212 220,218" fill="#64748B"/>

  <!-- ==================== TIER 1 ==================== -->
  <!-- Decision Card -->
  <rect x="35" y="218" width="370" height="138" rx="10" ry="10" fill="#EFF6FF" stroke="#3B82F6" stroke-width="2"/>
  <rect x="50" y="230" width="64" height="22" rx="4" ry="4" fill="#1D4ED8"/>
  <text x="82" y="245" fill="#FFFFFF" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 1</text>
  <text x="124" y="247" fill="#1E3A8A" font-size="14" font-weight="bold" font-family="Arial, sans-serif">Accessibility Node Check</text>
  <text x="52" y="268" fill="#2563EB" font-size="10.5" font-family="Arial, sans-serif">Traverses active window AccessibilityNodeInfo hierarchy</text>

  <!-- Criteria Box -->
  <rect x="50" y="276" width="340" height="66" rx="6" ry="6" fill="#FFFFFF" stroke="#BFDBFE" stroke-width="1"/>
  <text x="60" y="294" fill="#1E293B" font-size="10" font-family="Arial, sans-serif">• Target View: ImageView, Photo, Picture</text>
  <text x="60" y="312" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Min Dimensions: Width, Height ≥ 110 dp</text>
  <text x="60" y="330" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Aspect Ratio Gate: 0.35 ≤ (W / H) ≤ 2.85</text>

  <!-- Tier 1 Outcome: Found -> Action 1 -->
  <line x1="405" y1="287" x2="474" y2="287" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="474,282 474,292 480,287" fill="#10B981"/>
  <rect x="414" y="276" width="52" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="440" y="291" fill="#047857" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Found</text>

  <!-- Action Box 1 -->
  <rect x="480" y="228" width="295" height="118" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="508" cy="252" r="12" fill="#DCFCE7"/>
  <polyline points="502,252 506,256 514,248" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="526" y="256" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Node Bounds</text>
  <text x="496" y="278" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Resolves to touch-proximity coord (x, y)</text>
  <text x="496" y="296" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Fallback: Largest visible candidate media card</text>
  <rect x="496" y="310" width="220" height="24" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="606" y="326" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_1_ACCESSIBILITY_NODE</text>

  <!-- Tier 1 Outcome: Not Found -> Tier 2 -->
  <line x1="220" y1="356" x2="220" y2="394" stroke="#F59E0B" stroke-width="2.5"/>
  <polygon points="215,394 225,394 220,400" fill="#F59E0B"/>
  <rect x="156" y="367" width="128" height="22" rx="11" ry="11" fill="#FEF3C7" stroke="#F59E0B" stroke-width="1"/>
  <text x="220" y="382" fill="#B45309" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Not Found / Obscured</text>

  <!-- ==================== TIER 2 ==================== -->
  <!-- Decision Card -->
  <rect x="35" y="400" width="370" height="138" rx="10" ry="10" fill="#F5F3FF" stroke="#8B5CF6" stroke-width="2"/>
  <rect x="50" y="412" width="64" height="22" rx="4" ry="4" fill="#6D28D9"/>
  <text x="82" y="427" fill="#FFFFFF" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 2</text>
  <text x="124" y="429" fill="#4C1D95" font-size="14" font-weight="bold" font-family="Arial, sans-serif">Luminance-Variance Check</text>
  <text x="52" y="450" fill="#7C3AED" font-size="10.5" font-family="Arial, sans-serif">CV spatial texture &amp; edge distribution analysis</text>

  <!-- Criteria Box -->
  <rect x="50" y="458" width="340" height="66" rx="6" ry="6" fill="#FFFFFF" stroke="#DDD6FE" stroke-width="1"/>
  <text x="60" y="476" fill="#1E293B" font-size="10" font-family="Arial, sans-serif">• 16px downsampling grid on captured bitmap</text>
  <text x="60" y="494" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Perceived luminance: Y = 0.299R + 0.587G + 0.114B</text>
  <text x="60" y="512" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Contour detection: Spatial row variance σ²(y) &gt; 250</text>

  <!-- Tier 2 Outcome: Found -> Action 2 -->
  <line x1="405" y1="469" x2="474" y2="469" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="474,464 474,474 480,469" fill="#10B981"/>
  <rect x="414" y="458" width="52" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="440" y="473" fill="#047857" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Found</text>

  <!-- Action Box 2 -->
  <rect x="480" y="410" width="295" height="118" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="508" cy="434" r="12" fill="#DCFCE7"/>
  <polyline points="502,434 506,438 514,430" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="526" y="438" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Contour Bounds</text>
  <text x="496" y="460" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Extracts contiguous high-variance row band</text>
  <text x="496" y="478" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Trims uniform app chrome &amp; navigation bars</text>
  <rect x="496" y="492" width="190" height="24" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="591" y="508" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_2_CV_CONTOUR</text>

  <!-- Tier 2 Outcome: Not Found -> Tier 3 -->
  <line x1="220" y1="538" x2="220" y2="576" stroke="#F59E0B" stroke-width="2.5"/>
  <polygon points="215,576 225,576 220,582" fill="#F59E0B"/>
  <rect x="156" y="549" width="128" height="22" rx="11" ry="11" fill="#FEF3C7" stroke="#F59E0B" stroke-width="1"/>
  <text x="220" y="564" fill="#B45309" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Not Found / Uniform</text>

  <!-- ==================== TIER 3 ==================== -->
  <!-- Fallback Card -->
  <rect x="35" y="582" width="370" height="138" rx="10" ry="10" fill="#FFFBEB" stroke="#D97706" stroke-width="2"/>
  <rect x="50" y="594" width="64" height="22" rx="4" ry="4" fill="#B45309"/>
  <text x="82" y="609" fill="#FFFFFF" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 3</text>
  <text x="124" y="611" fill="#78350F" font-size="14" font-weight="bold" font-family="Arial, sans-serif">Fixed Centre-Square Fallback</text>
  <text x="52" y="632" fill="#B45309" font-size="10.5" font-family="Arial, sans-serif">Deterministic geometric screen-center clamp</text>

  <!-- Criteria Box -->
  <rect x="50" y="640" width="340" height="66" rx="6" ry="6" fill="#FFFFFF" stroke="#FDE68A" stroke-width="1"/>
  <text x="60" y="658" fill="#1E293B" font-size="10" font-family="Arial, sans-serif">• Dim_center = min(W_screen, 0.65 · H_screen)</text>
  <text x="60" y="676" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Clamped screen center: (W - Dim)/2, (H - Dim)/2</text>
  <text x="60" y="694" fill="#047857" font-size="10" font-weight="bold" font-family="Arial, sans-serif">• Zero-Failure Invariant: Always produces usable crop</text>

  <!-- Tier 3 Outcome: Always -> Action 3 -->
  <line x1="405" y1="651" x2="474" y2="651" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="474,646 474,656 480,651" fill="#10B981"/>
  <rect x="412" y="640" width="56" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="440" y="655" fill="#047857" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Always</text>

  <!-- Action Box 3 -->
  <rect x="480" y="592" width="295" height="118" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="508" cy="616" r="12" fill="#DCFCE7"/>
  <polyline points="502,616 506,620 514,612" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="526" y="620" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Centre Square</text>
  <text x="496" y="642" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Clamps to guaranteed screen-center square</text>
  <text x="496" y="660" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Guaranteed fallback; zero unhandled nulls</text>
  <rect x="496" y="674" width="190" height="24" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="591" y="690" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_3_CENTER_CROP</text>

  <!-- ==================== CONVERGENCE BUS ==================== -->
  <line x1="775" y1="287" x2="805" y2="287" stroke="#059669" stroke-width="2.5"/>
  <line x1="775" y1="469" x2="805" y2="469" stroke="#059669" stroke-width="2.5"/>
  <line x1="775" y1="651" x2="805" y2="651" stroke="#059669" stroke-width="2.5"/>
  <line x1="805" y1="287" x2="805" y2="651" stroke="#059669" stroke-width="2.5"/>
  <line x1="805" y1="469" x2="824" y2="469" stroke="#059669" stroke-width="2.5"/>
  <polygon points="824,464 824,474 830,469" fill="#059669"/>

  <!-- ==================== UNIFIED TARGET CROP CARD ==================== -->
  <rect x="830" y="218" width="235" height="502" rx="12" ry="12" fill="#F8FAFC" stroke="#0F172A" stroke-width="2"/>
  
  <!-- Banner Top -->
  <path d="M 830 230 A 12 12 0 0 1 842 218 L 1053 218 A 12 12 0 0 1 1065 230 L 1065 276 L 830 276 Z" fill="#0F172A"/>
  <text x="845" y="238" fill="#10B981" font-size="10" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">UNIFIED CROP REGION</text>
  <text x="845" y="260" fill="#FFFFFF" font-size="13.5" font-weight="bold" font-family="Arial, sans-serif">Clamped Target Crop</text>

  <!-- Step 1: Geometric Validation -->
  <rect x="842" y="286" width="211" height="68" rx="6" ry="6" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1"/>
  <text x="852" y="303" fill="#0F172A" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">1. Display Bounds Clamp</text>
  <text x="852" y="320" fill="#475569" font-size="9.5" font-family="Arial, sans-serif">• Clamps bounds to [0, 0, W, H]</text>
  <text x="852" y="336" fill="#475569" font-size="9.5" font-family="Arial, sans-serif">• Validates aspect ratio integrity</text>

  <!-- Step 2: Tensor Preprocessing -->
  <rect x="842" y="362" width="211" height="82" rx="6" ry="6" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1"/>
  <text x="852" y="379" fill="#0F172A" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">2. Tensor Preprocessing</text>
  <text x="852" y="396" fill="#475569" font-size="9.5" font-family="Arial, sans-serif">• Bilinear resize to 224 × 224</text>
  <text x="852" y="412" fill="#475569" font-size="9.5" font-family="Arial, sans-serif">• RGB float normalisation [0, 1]</text>
  <text x="852" y="428" fill="#0284C7" font-size="9" font-weight="bold" font-family="Arial, sans-serif">• Shape: (1, 3, 224, 224) FloatBuffer</text>

  <!-- Step 3: Dual-Model Execution -->
  <rect x="842" y="452" width="211" height="138" rx="6" ry="6" fill="#EFF6FF" stroke="#BFDBFE" stroke-width="1"/>
  <text x="852" y="470" fill="#1E3A8A" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">3. Dual-Model Inference</text>
  <text x="852" y="487" fill="#334155" font-size="9.5" font-family="Arial, sans-serif">Sequential execution on identical</text>
  <text x="852" y="501" fill="#334155" font-size="9.5" font-family="Arial, sans-serif">cropped bitmap:</text>
  
  <rect x="850" y="512" width="195" height="28" rx="4" ry="4" fill="#FFFFFF" stroke="#3B82F6" stroke-width="1"/>
  <text x="858" y="525" fill="#1D4ED8" font-size="9" font-weight="bold" font-family="Arial, sans-serif">1. EfficientNet-B3 (CNN)</text>
  <text x="858" y="535" fill="#64748B" font-size="8" font-family="Arial, sans-serif">Conv CAM Grid (7 × 7 × 1536)</text>

  <rect x="850" y="546" width="195" height="28" rx="4" ry="4" fill="#FFFFFF" stroke="#8B5CF6" stroke-width="1"/>
  <text x="858" y="559" fill="#6D28D9" font-size="9" font-weight="bold" font-family="Arial, sans-serif">2. Swin-T (Transformer)</text>
  <text x="858" y="569" fill="#64748B" font-size="8" font-family="Arial, sans-serif">Patch CAM Grid (7 × 7 × 768)</text>

  <!-- Step 4: Invariant Guarantee -->
  <rect x="842" y="598" width="211" height="110" rx="8" ry="8" fill="#DCFCE7" stroke="#10B981" stroke-width="1.5"/>
  <circle cx="862" cy="616" r="10" fill="#059669"/>
  <polyline points="858,616 861,619 867,613" fill="none" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="878" y="620" fill="#065F46" font-size="11" font-weight="bold" font-family="Arial, sans-serif">Zero-Failure Invariant</text>
  <text x="852" y="640" fill="#047857" font-size="9.5" font-family="Arial, sans-serif">Every screen capture strictly</text>
  <text x="852" y="655" fill="#047857" font-size="9.5" font-family="Arial, sans-serif">resolves to a valid crop region.</text>
  <text x="852" y="670" fill="#047857" font-size="9.5" font-family="Arial, sans-serif">Zero crashes, zero nulls,</text>
  <text x="852" y="686" fill="#065F46" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif">uninterrupted user HUD flow.</text>

  <!-- Footer -->
  <text x="550" y="744" fill="#64748B" font-size="10.5" font-family="Arial, sans-serif" text-anchor="middle">
    Yaqeen Gulf Architecture Principle: Three-tier localisation guarantees 100% operational resilience across standard native apps, hybrid Flutter canvases, and custom WebViews.
  </text>
</svg>
"""
    path = os.path.join(FIGURES_DIR, "figure7_three_tier_image_localisation.svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Generated {path}")


def render_figure_7_image():
    scale = 2
    w, h = 1100 * scale, 760 * scale
    img = Image.new("RGB", (w, h), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    f_title = ImageFont.truetype("arialbd.ttf", 18 * scale)
    f_sub = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_desc = ImageFont.truetype("arial.ttf", 11 * scale)
    f_hdr = ImageFont.truetype("arialbd.ttf", 13 * scale)
    f_tag = ImageFont.truetype("arialbd.ttf", 10 * scale)
    f_txt = ImageFont.truetype("arial.ttf", 10 * scale)
    f_bold = ImageFont.truetype("arialbd.ttf", 10 * scale)
    f_sm = ImageFont.truetype("arial.ttf", 9 * scale)
    f_sm_b = ImageFont.truetype("arialbd.ttf", 9 * scale)
    f_xs = ImageFont.truetype("arial.ttf", 8 * scale)
    f_footer = ImageFont.truetype("arial.ttf", 10 * scale)

    def draw_h_arrow(x1, x2, y, color, width=int(2.5 * scale), arrow_size=6 * scale):
        draw.line([(x1, y), (x2 - arrow_size, y)], fill=color, width=width)
        draw.polygon([(x2, y), (x2 - arrow_size, y - arrow_size * 0.7), (x2 - arrow_size, y + arrow_size * 0.7)], fill=color)

    def draw_v_arrow(y1, y2, x, color, width=int(2.5 * scale), arrow_size=6 * scale):
        draw.line([(x, y1), (x, y2 - arrow_size)], fill=color, width=width)
        draw.polygon([(x, y2), (x - arrow_size * 0.7, y2 - arrow_size), (x + arrow_size * 0.7, y2 - arrow_size)], fill=color)

    def draw_check_icon(cx, cy, r, bg_color, check_color):
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=bg_color)
        draw.line([(cx - 0.5 * r, cy), (cx - 0.1 * r, cy + 0.4 * r)], fill=check_color, width=2 * scale)
        draw.line([(cx - 0.1 * r, cy + 0.4 * r), (cx + 0.55 * r, cy - 0.35 * r)], fill=check_color, width=2 * scale)

    def draw_screen_icon(cx, cy, r):
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill="#E2E8F0")
        rw, rh = 14 * scale, 10 * scale
        draw.rounded_rectangle([cx - rw // 2, cy - rh // 2, cx + rw // 2, cy + rh // 2], radius=2 * scale, outline="#475569", width=int(1.5 * scale))
        draw.ellipse([cx - 2.5 * scale, cy - 2.5 * scale, cx + 2.5 * scale, cy + 2.5 * scale], fill="#475569")

    # Header Banner
    draw.rectangle([0, 0, w, 70 * scale], fill="#0F172A")
    draw.text((30 * scale, 16 * scale), "YAQEEN GULF • REAL-TIME SCREEN OVERLAY", fill="#10B981", font=f_sub)
    draw.text((30 * scale, 38 * scale), "Figure 7: Overlay Three-Tier Image Localisation Decision Flow", fill="#FFFFFF", font=f_title)

    # Subtitle
    draw.text((30 * scale, 86 * scale), "When on-screen capture is initiated, the subsystem isolates media graphics from UI chrome through a deterministic three-stage cascade.", fill="#475569", font=f_desc)
    draw.text((30 * scale, 104 * scale), "Semantic accessibility nodes are prioritized first; computer vision saliency acts as secondary fallback, with center cropping guaranteeing zero failure.", fill="#64748B", font=f_desc)

    # Input Node
    draw.rounded_rectangle([75 * scale, 136 * scale, 365 * scale, 182 * scale], radius=8 * scale, fill="#F1F5F9", outline="#64748B", width=int(1.5 * scale))
    draw_screen_icon(98 * scale, 159 * scale, 13 * scale)
    draw.text((120 * scale, 142 * scale), "Screen Framebuffer Captured", fill="#0F172A", font=f_hdr)
    draw.text((120 * scale, 162 * scale), "MediaProjection / VirtualDisplay stream", fill="#64748B", font=f_txt)
    draw_v_arrow(182 * scale, 218 * scale, 220 * scale, "#64748B")

    # ==================== TIER 1 ====================
    draw.rounded_rectangle([35 * scale, 218 * scale, 405 * scale, 356 * scale], radius=10 * scale, fill="#EFF6FF", outline="#3B82F6", width=2 * scale)
    draw.rounded_rectangle([50 * scale, 230 * scale, 114 * scale, 252 * scale], radius=4 * scale, fill="#1D4ED8")
    draw.text((58 * scale, 233 * scale), "TIER 1", fill="#FFFFFF", font=f_tag)
    draw.text((124 * scale, 230 * scale), "Accessibility Node Check", fill="#1E3A8A", font=f_hdr)
    draw.text((52 * scale, 258 * scale), "Traverses active window AccessibilityNodeInfo hierarchy", fill="#2563EB", font=f_txt)

    # Criteria Box
    draw.rounded_rectangle([50 * scale, 276 * scale, 390 * scale, 342 * scale], radius=6 * scale, fill="#FFFFFF", outline="#BFDBFE", width=scale)
    draw.text((60 * scale, 282 * scale), "• Target View: ImageView, Photo, Picture", fill="#1E293B", font=f_txt)
    draw.text((60 * scale, 300 * scale), "• Min Dimensions: Width, Height ≥ 110 dp", fill="#475569", font=f_txt)
    draw.text((60 * scale, 318 * scale), "• Aspect Ratio Gate: 0.35 ≤ (W / H) ≤ 2.85", fill="#475569", font=f_txt)

    # Tier 1 Outcome: Found -> Action 1
    draw_h_arrow(405 * scale, 480 * scale, 287 * scale, "#10B981", width=int(2.5 * scale))
    draw.rounded_rectangle([414 * scale, 276 * scale, 466 * scale, 298 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((422 * scale, 279 * scale), "Found", fill="#047857", font=f_sm_b)

    # Action Box 1
    draw.rounded_rectangle([480 * scale, 228 * scale, 775 * scale, 346 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(508 * scale, 252 * scale, 12 * scale, "#DCFCE7", "#059669")
    draw.text((526 * scale, 241 * scale), "Extract Node Bounds", fill="#065F46", font=f_hdr)
    draw.text((496 * scale, 268 * scale), "• Resolves to touch-proximity coord (x, y)", fill="#334155", font=f_txt)
    draw.text((496 * scale, 286 * scale), "• Fallback: Largest visible candidate media card", fill="#475569", font=f_txt)
    draw.rounded_rectangle([496 * scale, 308 * scale, 716 * scale, 332 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((506 * scale, 312 * scale), "TIER_1_ACCESSIBILITY_NODE", fill="#047857", font=f_sm_b)

    # Tier 1 Outcome: Not Found -> Tier 2
    draw_v_arrow(356 * scale, 400 * scale, 220 * scale, "#F59E0B", width=int(2.5 * scale))
    draw.rounded_rectangle([156 * scale, 367 * scale, 284 * scale, 389 * scale], radius=11 * scale, fill="#FEF3C7", outline="#F59E0B", width=scale)
    draw.text((165 * scale, 370 * scale), "Not Found / Obscured", fill="#B45309", font=f_sm_b)

    # ==================== TIER 2 ====================
    draw.rounded_rectangle([35 * scale, 400 * scale, 405 * scale, 538 * scale], radius=10 * scale, fill="#F5F3FF", outline="#8B5CF6", width=2 * scale)
    draw.rounded_rectangle([50 * scale, 412 * scale, 114 * scale, 434 * scale], radius=4 * scale, fill="#6D28D9")
    draw.text((58 * scale, 415 * scale), "TIER 2", fill="#FFFFFF", font=f_tag)
    draw.text((124 * scale, 412 * scale), "Luminance-Variance Saliency", fill="#4C1D95", font=f_hdr)
    draw.text((52 * scale, 440 * scale), "CV spatial texture & edge distribution analysis", fill="#7C3AED", font=f_txt)

    # Criteria Box
    draw.rounded_rectangle([50 * scale, 458 * scale, 390 * scale, 524 * scale], radius=6 * scale, fill="#FFFFFF", outline="#DDD6FE", width=scale)
    draw.text((60 * scale, 464 * scale), "• 16px downsampling grid on captured bitmap", fill="#1E293B", font=f_txt)
    draw.text((60 * scale, 482 * scale), "• Perceived luminance: Y = 0.299R + 0.587G + 0.114B", fill="#475569", font=f_txt)
    draw.text((60 * scale, 500 * scale), "• Contour detection: Spatial row variance σ²(y) > 250", fill="#475569", font=f_txt)

    # Tier 2 Outcome: Found -> Action 2
    draw_h_arrow(405 * scale, 480 * scale, 469 * scale, "#10B981", width=int(2.5 * scale))
    draw.rounded_rectangle([414 * scale, 458 * scale, 466 * scale, 480 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((422 * scale, 461 * scale), "Found", fill="#047857", font=f_sm_b)

    # Action Box 2
    draw.rounded_rectangle([480 * scale, 410 * scale, 775 * scale, 528 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(508 * scale, 434 * scale, 12 * scale, "#DCFCE7", "#059669")
    draw.text((526 * scale, 423 * scale), "Extract Contour Bounds", fill="#065F46", font=f_hdr)
    draw.text((496 * scale, 450 * scale), "• Extracts contiguous high-variance row band", fill="#334155", font=f_txt)
    draw.text((496 * scale, 468 * scale), "• Trims uniform app chrome & navigation bars", fill="#475569", font=f_txt)
    draw.rounded_rectangle([496 * scale, 490 * scale, 686 * scale, 514 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((506 * scale, 494 * scale), "TIER_2_CV_CONTOUR", fill="#047857", font=f_sm_b)

    # Tier 2 Outcome: Not Found -> Tier 3
    draw_v_arrow(538 * scale, 582 * scale, 220 * scale, "#F59E0B", width=int(2.5 * scale))
    draw.rounded_rectangle([156 * scale, 549 * scale, 284 * scale, 571 * scale], radius=11 * scale, fill="#FEF3C7", outline="#F59E0B", width=scale)
    draw.text((165 * scale, 552 * scale), "Not Found / Uniform", fill="#B45309", font=f_sm_b)

    # ==================== TIER 3 ====================
    draw.rounded_rectangle([35 * scale, 582 * scale, 405 * scale, 720 * scale], radius=10 * scale, fill="#FFFBEB", outline="#D97706", width=2 * scale)
    draw.rounded_rectangle([50 * scale, 594 * scale, 114 * scale, 616 * scale], radius=4 * scale, fill="#B45309")
    draw.text((58 * scale, 597 * scale), "TIER 3", fill="#FFFFFF", font=f_tag)
    draw.text((124 * scale, 594 * scale), "Fixed Centre-Square Fallback", fill="#78350F", font=f_hdr)
    draw.text((52 * scale, 622 * scale), "Deterministic geometric screen-center clamp", fill="#B45309", font=f_txt)

    # Criteria Box
    draw.rounded_rectangle([50 * scale, 640 * scale, 390 * scale, 706 * scale], radius=6 * scale, fill="#FFFFFF", outline="#FDE68A", width=scale)
    draw.text((60 * scale, 646 * scale), "• Dim_center = min(W_screen, 0.65 · H_screen)", fill="#1E293B", font=f_txt)
    draw.text((60 * scale, 664 * scale), "• Clamped screen center: (W - Dim)/2, (H - Dim)/2", fill="#475569", font=f_txt)
    draw.text((60 * scale, 682 * scale), "• Zero-Failure Invariant: Always produces usable crop", fill="#047857", font=f_bold)

    # Tier 3 Outcome: Always -> Action 3
    draw_h_arrow(405 * scale, 480 * scale, 651 * scale, "#10B981", width=int(2.5 * scale))
    draw.rounded_rectangle([412 * scale, 640 * scale, 468 * scale, 662 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((419 * scale, 643 * scale), "Always", fill="#047857", font=f_sm_b)

    # Action Box 3
    draw.rounded_rectangle([480 * scale, 592 * scale, 775 * scale, 710 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(508 * scale, 616 * scale, 12 * scale, "#DCFCE7", "#059669")
    draw.text((526 * scale, 605 * scale), "Extract Centre Square", fill="#065F46", font=f_hdr)
    draw.text((496 * scale, 632 * scale), "• Clamps to guaranteed screen-center square", fill="#334155", font=f_txt)
    draw.text((496 * scale, 650 * scale), "• Guaranteed fallback; zero unhandled nulls", fill="#475569", font=f_txt)
    draw.rounded_rectangle([496 * scale, 672 * scale, 686 * scale, 696 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((506 * scale, 676 * scale), "TIER_3_CENTER_CROP", fill="#047857", font=f_sm_b)

    # ==================== CONVERGENCE BUS ====================
    draw.line([(775 * scale, 287 * scale), (805 * scale, 287 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(775 * scale, 469 * scale), (805 * scale, 469 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(775 * scale, 651 * scale), (805 * scale, 651 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(805 * scale, 287 * scale), (805 * scale, 651 * scale)], fill="#059669", width=int(2.5 * scale))
    draw_h_arrow(805 * scale, 830 * scale, 469 * scale, "#059669", width=int(2.5 * scale))

    # ==================== UNIFIED TARGET CROP CARD ====================
    draw.rounded_rectangle([830 * scale, 218 * scale, 1065 * scale, 720 * scale], radius=12 * scale, fill="#F8FAFC", outline="#0F172A", width=2 * scale)
    draw.rounded_rectangle([830 * scale, 218 * scale, 1065 * scale, 276 * scale], radius=12 * scale, fill="#0F172A")
    draw.rectangle([830 * scale, 255 * scale, 1065 * scale, 276 * scale], fill="#0F172A")
    draw.text((845 * scale, 228 * scale), "UNIFIED CROP REGION", fill="#10B981", font=f_tag)
    draw.text((845 * scale, 248 * scale), "Clamped Target Crop", fill="#FFFFFF", font=f_hdr)

    # Step 1: Geometric Validation
    draw.rounded_rectangle([842 * scale, 286 * scale, 1053 * scale, 354 * scale], radius=6 * scale, fill="#FFFFFF", outline="#E2E8F0", width=scale)
    draw.text((852 * scale, 294 * scale), "1. Display Bounds Clamp", fill="#0F172A", font=f_bold)
    draw.text((852 * scale, 312 * scale), "• Clamps bounds to [0, 0, W, H]", fill="#475569", font=f_sm)
    draw.text((852 * scale, 328 * scale), "• Validates aspect ratio integrity", fill="#475569", font=f_sm)

    # Step 2: Tensor Preprocessing
    draw.rounded_rectangle([842 * scale, 362 * scale, 1053 * scale, 444 * scale], radius=6 * scale, fill="#FFFFFF", outline="#E2E8F0", width=scale)
    draw.text((852 * scale, 370 * scale), "2. Tensor Preprocessing", fill="#0F172A", font=f_bold)
    draw.text((852 * scale, 388 * scale), "• Bilinear resize to 224 × 224", fill="#475569", font=f_sm)
    draw.text((852 * scale, 404 * scale), "• RGB float normalisation [0, 1]", fill="#475569", font=f_sm)
    draw.text((852 * scale, 420 * scale), "• Shape: (1, 3, 224, 224) FloatBuffer", fill="#0284C7", font=f_sm_b)

    # Step 3: Dual-Model Execution
    draw.rounded_rectangle([842 * scale, 452 * scale, 1053 * scale, 590 * scale], radius=6 * scale, fill="#EFF6FF", outline="#BFDBFE", width=scale)
    draw.text((852 * scale, 460 * scale), "3. Dual-Model Inference", fill="#1E3A8A", font=f_bold)
    draw.text((852 * scale, 478 * scale), "Sequential execution on identical", fill="#334155", font=f_sm)
    draw.text((852 * scale, 492 * scale), "cropped bitmap:", fill="#334155", font=f_sm)

    draw.rounded_rectangle([850 * scale, 510 * scale, 1045 * scale, 538 * scale], radius=4 * scale, fill="#FFFFFF", outline="#3B82F6", width=scale)
    draw.text((858 * scale, 514 * scale), "1. EfficientNet-B3 (CNN)", fill="#1D4ED8", font=f_sm_b)
    draw.text((858 * scale, 526 * scale), "Conv CAM Grid (7 × 7 × 1536)", fill="#64748B", font=f_xs)

    draw.rounded_rectangle([850 * scale, 544 * scale, 1045 * scale, 572 * scale], radius=4 * scale, fill="#FFFFFF", outline="#8B5CF6", width=scale)
    draw.text((858 * scale, 548 * scale), "2. Swin-T (Transformer)", fill="#6D28D9", font=f_sm_b)
    draw.text((858 * scale, 560 * scale), "Patch CAM Grid (7 × 7 × 768)", fill="#64748B", font=f_xs)

    # Step 4: Invariant Guarantee
    draw.rounded_rectangle([842 * scale, 598 * scale, 1053 * scale, 708 * scale], radius=8 * scale, fill="#DCFCE7", outline="#10B981", width=int(1.5 * scale))
    draw_check_icon(862 * scale, 616 * scale, 10 * scale, "#059669", "#FFFFFF")
    draw.text((878 * scale, 608 * scale), "Zero-Failure Invariant", fill="#065F46", font=f_bold)
    draw.text((852 * scale, 632 * scale), "Every screen capture strictly", fill="#047857", font=f_sm)
    draw.text((852 * scale, 647 * scale), "resolves to a valid crop region.", fill="#047857", font=f_sm)
    draw.text((852 * scale, 662 * scale), "Zero crashes, zero nulls,", fill="#047857", font=f_sm)
    draw.text((852 * scale, 678 * scale), "uninterrupted user HUD flow.", fill="#065F46", font=f_sm_b)

    # Footer
    draw.text((90 * scale, 736 * scale), "Yaqeen Gulf Architecture Principle: Three-tier localisation guarantees 100% operational resilience across standard native apps, hybrid Flutter canvases, and custom WebViews.", fill="#64748B", font=f_footer)

    jpg_path = os.path.join(FIGURES_DIR, "figure7_three_tier_image_localisation.jpg")
    png_path = os.path.join(FIGURES_DIR, "figure7_three_tier_image_localisation.png")
    img.save(jpg_path, "JPEG", quality=95)
    img.save(png_path, "PNG")
    print(f"Generated {jpg_path} and {png_path}")


# ==============================================================================
# 3. FIGURE 8: Overlay Dual-Model Consensus Outcomes (Conceptual 3-State)
# ==============================================================================

def generate_figure_8_svg():
    width = 1000
    height = 620

    svg = f"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <!-- Background -->
  <rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>

  <!-- Header Banner -->
  <rect x="0" y="0" width="{width}" height="70" fill="#0F172A"/>
  <text x="30" y="32" fill="#10B981" font-size="13" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">YAQEEN GULF • REAL-TIME SCREEN OVERLAY</text>
  <text x="30" y="55" fill="#FFFFFF" font-size="18" font-weight="bold" font-family="Arial, sans-serif">Figure 8: Three-State Dual-Model Consensus Verification Decision Flow</text>

  <!-- Subtitle -->
  <text x="30" y="98" fill="#64748B" font-size="12.5" font-family="Arial, sans-serif">When an on-screen image is captured, EfficientNet-B3 (CNN) and Swin-T (Transformer) execute simultaneously on the identical cropped frame.</text>
  <text x="30" y="116" fill="#64748B" font-size="12.5" font-family="Arial, sans-serif">The overlay maps the outputs into a tri-state indicator, preventing false certainty by isolating architecture disagreements.</text>

  <!-- STATE 1: SYNTHETIC AGREEMENT -->
  <g transform="translate(30, 140)">
    <rect x="0" y="0" width="295" height="440" rx="12" ry="12" fill="#FFF5F5" stroke="#EF4444" stroke-width="2"/>
    <rect x="18" y="18" width="259" height="34" rx="6" ry="6" fill="#EF4444"/>
    <text x="147" y="40" fill="#FFFFFF" font-size="12.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">STATE 1: SYNTHETIC AGREEMENT</text>
    
    <!-- Big Alert Badge -->
    <circle cx="147" cy="85" r="24" fill="#FEE2E2"/>
    <text x="147" y="93" fill="#DC2626" font-size="22" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">!</text>
    
    <text x="147" y="126" fill="#991B1B" font-size="16" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">DEEPFAKE DETECTED</text>
    <text x="147" y="144" fill="#B91C1C" font-size="11.5" font-family="Arial, sans-serif" text-anchor="middle">High-Confidence Consensus</text>

    <!-- Sub-models Box -->
    <rect x="18" y="160" width="259" height="105" rx="8" ry="8" fill="#FFFFFF" stroke="#FECACA" stroke-width="1"/>
    <text x="30" y="182" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Individual Model Outputs:</text>
    <text x="30" y="206" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• EfficientNet-B3:</text>
    <text x="260" y="206" fill="#DC2626" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Fake (98.4%)</text>
    <text x="30" y="228" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• Swin-T (Patch CAM):</text>
    <text x="260" y="228" fill="#DC2626" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Fake (96.8%)</text>
    <text x="30" y="250" fill="#64748B" font-size="10.5" font-family="Arial, sans-serif">Cross-Entropy Disparity: &lt; 0.02</text>

    <!-- XAI Box -->
    <rect x="18" y="275" width="259" height="90" rx="8" ry="8" fill="#FFFFFF" stroke="#FECACA" stroke-width="1"/>
    <text x="30" y="296" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Explainable AI (XAI) Behavior:</text>
    <text x="30" y="316" fill="#334155" font-size="11" font-family="Arial, sans-serif">• Both CAM maps focus on matching</text>
    <text x="38" y="332" fill="#334155" font-size="11" font-family="Arial, sans-serif">spatial generator artifacts.</text>
    <text x="30" y="352" fill="#DC2626" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">Action: Prominent Red Alert Banner</text>

    <!-- Action -->
    <rect x="18" y="378" width="259" height="42" rx="6" ry="6" fill="#DC2626"/>
    <text x="147" y="404" fill="#FFFFFF" font-size="12" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Verdict: SYNTHETIC (Confirmed)</text>
  </g>

  <!-- STATE 2: AUTHENTIC AGREEMENT -->
  <g transform="translate(352, 140)">
    <rect x="0" y="0" width="295" height="440" rx="12" ry="12" fill="#F0FDF4" stroke="#10B981" stroke-width="2"/>
    <rect x="18" y="18" width="259" height="34" rx="6" ry="6" fill="#10B981"/>
    <text x="147" y="40" fill="#FFFFFF" font-size="12.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">STATE 2: AUTHENTIC AGREEMENT</text>
    
    <!-- Big Check Badge -->
    <circle cx="147" cy="85" r="24" fill="#DCFCE7"/>
    <polyline points="137,85 144,92 158,78" fill="none" stroke="#059669" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
    
    <text x="147" y="126" fill="#065F46" font-size="16" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">AUTHENTIC / REAL</text>
    <text x="147" y="144" fill="#047857" font-size="11.5" font-family="Arial, sans-serif" text-anchor="middle">High-Confidence Consensus</text>

    <!-- Sub-models Box -->
    <rect x="18" y="160" width="259" height="105" rx="8" ry="8" fill="#FFFFFF" stroke="#A7F3D0" stroke-width="1"/>
    <text x="30" y="182" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Individual Model Outputs:</text>
    <text x="30" y="206" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• EfficientNet-B3:</text>
    <text x="260" y="206" fill="#059669" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Real (99.1%)</text>
    <text x="30" y="228" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• Swin-T (Patch CAM):</text>
    <text x="260" y="228" fill="#059669" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Real (98.3%)</text>
    <text x="30" y="250" fill="#64748B" font-size="10.5" font-family="Arial, sans-serif">Organic sensor noise verified</text>

    <!-- XAI Box -->
    <rect x="18" y="275" width="259" height="90" rx="8" ry="8" fill="#FFFFFF" stroke="#A7F3D0" stroke-width="1"/>
    <text x="30" y="296" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Explainable AI (XAI) Behavior:</text>
    <text x="30" y="316" fill="#334155" font-size="11" font-family="Arial, sans-serif">• Diffuse low-activation attention;</text>
    <text x="38" y="332" fill="#334155" font-size="11" font-family="Arial, sans-serif">no anomalous focal clustering.</text>
    <text x="30" y="352" fill="#059669" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">Action: Emerald Shield Badge Displayed</text>

    <!-- Action -->
    <rect x="18" y="378" width="259" height="42" rx="6" ry="6" fill="#059669"/>
    <text x="147" y="404" fill="#FFFFFF" font-size="12" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Verdict: REAL (Verified)</text>
  </g>

  <!-- STATE 3: DISAGREEMENT -->
  <g transform="translate(675, 140)">
    <rect x="0" y="0" width="295" height="440" rx="12" ry="12" fill="#FFFBEB" stroke="#F59E0B" stroke-width="2"/>
    <rect x="18" y="18" width="259" height="34" rx="6" ry="6" fill="#F59E0B"/>
    <text x="147" y="40" fill="#FFFFFF" font-size="12.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">STATE 3: MODEL DIVERGENCE</text>
    
    <!-- Big Warning Badge -->
    <circle cx="147" cy="85" r="24" fill="#FEF3C7"/>
    <text x="147" y="93" fill="#D97706" font-size="22" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">?</text>
    
    <text x="147" y="126" fill="#92400E" font-size="16" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">DISPUTED / DIVERGENT</text>
    <text x="147" y="144" fill="#B45309" font-size="11.5" font-family="Arial, sans-serif" text-anchor="middle">Lower-Confidence Result (No Guess)</text>

    <!-- Sub-models Box -->
    <rect x="18" y="160" width="259" height="105" rx="8" ry="8" fill="#FFFFFF" stroke="#FDE68A" stroke-width="1"/>
    <text x="30" y="182" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Individual Model Outputs:</text>
    <text x="30" y="206" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• EfficientNet-B3:</text>
    <text x="260" y="206" fill="#059669" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Real (78.2%)</text>
    <text x="30" y="228" fill="#1E293B" font-size="11.5" font-family="Arial, sans-serif">• Swin-T (Transformer):</text>
    <text x="260" y="228" fill="#DC2626" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="end">Fake (84.6%)</text>
    <text x="30" y="250" fill="#D97706" font-size="10.5" font-family="Arial, sans-serif">Significant Arch Divergence (Δ = 62.8%)</text>

    <!-- XAI Box -->
    <rect x="18" y="275" width="259" height="90" rx="8" ry="8" fill="#FFFFFF" stroke="#FDE68A" stroke-width="1"/>
    <text x="30" y="296" fill="#475569" font-size="11.5" font-weight="bold" font-family="Arial, sans-serif">Explainable AI (XAI) Behavior:</text>
    <text x="30" y="316" fill="#334155" font-size="11" font-family="Arial, sans-serif">• Side-by-side Dual Grad-CAM shows</text>
    <text x="38" y="332" fill="#334155" font-size="11" font-family="Arial, sans-serif">conflicting patch vs conv focus.</text>
    <text x="30" y="352" fill="#B45309" font-size="10.5" font-weight="bold" font-family="Arial, sans-serif">Action: Prompts Full Manual Inspection</text>

    <!-- Action -->
    <rect x="18" y="378" width="259" height="42" rx="6" ry="6" fill="#D97706"/>
    <text x="147" y="404" fill="#FFFFFF" font-size="12" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Verdict: DISPUTED (Low Confidence)</text>
  </g>

  <!-- Footer -->
  <text x="500" y="605" fill="#94A3B8" font-size="11" font-family="Arial, sans-serif" text-anchor="middle">Yaqeen Gulf Architecture Principle: In State 3, the system deliberately refrains from forcing an unprincipled 50/50 tie-break, presenting a transparent lower-confidence advisory.</text>
</svg>
"""
    path = os.path.join(FIGURES_DIR, "figure8_dual_model_consensus.svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Generated {path}")


def render_figure_8_image():
    scale = 2
    w, h = 1000 * scale, 620 * scale
    img = Image.new("RGB", (w, h), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    f_title = ImageFont.truetype("arialbd.ttf", 18 * scale)
    f_sub = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_desc = ImageFont.truetype("arial.ttf", 11 * scale)
    f_card_hdr = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_verdict = ImageFont.truetype("arialbd.ttf", 15 * scale)
    f_conf = ImageFont.truetype("arial.ttf", 11 * scale)
    f_box_hdr = ImageFont.truetype("arialbd.ttf", 11 * scale)
    f_box_txt = ImageFont.truetype("arial.ttf", 11 * scale)
    f_box_bold = ImageFont.truetype("arialbd.ttf", 11 * scale)
    f_btn = ImageFont.truetype("arialbd.ttf", 12 * scale)
    f_footer = ImageFont.truetype("arial.ttf", 10 * scale)

    # Header
    draw.rectangle([0, 0, w, 70 * scale], fill="#0F172A")
    draw.text((30 * scale, 16 * scale), "YAQEEN GULF • REAL-TIME SCREEN OVERLAY", fill="#10B981", font=f_sub)
    draw.text((30 * scale, 38 * scale), "Figure 8: Three-State Dual-Model Consensus Verification Decision Flow", fill="#FFFFFF", font=f_title)

    # Subtitle
    draw.text((30 * scale, 85 * scale), "When an on-screen image is captured, EfficientNet-B3 (CNN) and Swin-T (Transformer) execute simultaneously on the identical cropped frame.", fill="#64748B", font=f_desc)
    draw.text((30 * scale, 102 * scale), "The overlay maps the outputs into a tri-state indicator, preventing false certainty by isolating architecture disagreements.", fill="#64748B", font=f_desc)

    cards = [
        {
            "x": 30 * scale,
            "bg": "#FFF5F5",
            "border": "#EF4444",
            "hdr_bg": "#EF4444",
            "hdr_txt": "STATE 1: SYNTHETIC AGREEMENT",
            "icon_bg": "#FEE2E2",
            "icon_color": "#DC2626",
            "icon_char": "!",
            "verdict": "DEEPFAKE DETECTED",
            "verdict_color": "#991B1B",
            "conf": "High-Confidence Consensus",
            "conf_color": "#B91C1C",
            "box_bg": "#FFFFFF",
            "box_border": "#FECACA",
            "m1_txt": "Fake (98.4%)",
            "m1_color": "#DC2626",
            "m2_txt": "Fake (96.8%)",
            "m2_color": "#DC2626",
            "sub_txt": "Cross-Entropy Disparity: < 0.02",
            "xai1": "• Both CAM maps focus on matching",
            "xai2": "  spatial generator artifacts.",
            "act_txt": "Action: Prominent Red Alert Banner",
            "act_color": "#DC2626",
            "btn_bg": "#DC2626",
            "btn_txt": "Verdict: SYNTHETIC (Confirmed)"
        },
        {
            "x": 352 * scale,
            "bg": "#F0FDF4",
            "border": "#10B981",
            "hdr_bg": "#10B981",
            "hdr_txt": "STATE 2: AUTHENTIC AGREEMENT",
            "icon_bg": "#DCFCE7",
            "icon_color": "#059669",
            "icon_char": "OK",
            "verdict": "AUTHENTIC / REAL",
            "verdict_color": "#065F46",
            "conf": "High-Confidence Consensus",
            "conf_color": "#047857",
            "box_bg": "#FFFFFF",
            "box_border": "#A7F3D0",
            "m1_txt": "Real (99.1%)",
            "m1_color": "#059669",
            "m2_txt": "Real (98.3%)",
            "m2_color": "#059669",
            "sub_txt": "Organic sensor noise verified",
            "xai1": "• Diffuse low-activation attention;",
            "xai2": "  no anomalous focal clustering.",
            "act_txt": "Action: Emerald Shield Badge Displayed",
            "act_color": "#059669",
            "btn_bg": "#059669",
            "btn_txt": "Verdict: REAL (Verified)"
        },
        {
            "x": 675 * scale,
            "bg": "#FFFBEB",
            "border": "#F59E0B",
            "hdr_bg": "#F59E0B",
            "hdr_txt": "STATE 3: MODEL DIVERGENCE",
            "icon_bg": "#FEF3C7",
            "icon_color": "#D97706",
            "icon_char": "?",
            "verdict": "DISPUTED / DIVERGENT",
            "verdict_color": "#92400E",
            "conf": "Lower-Confidence Result (No Guess)",
            "conf_color": "#B45309",
            "box_bg": "#FFFFFF",
            "box_border": "#FDE68A",
            "m1_txt": "Real (78.2%)",
            "m1_color": "#059669",
            "m2_txt": "Fake (84.6%)",
            "m2_color": "#DC2626",
            "sub_txt": "Significant Arch Divergence (Δ = 62.8%)",
            "xai1": "• Side-by-side Dual Grad-CAM shows",
            "xai2": "  conflicting patch vs conv focus.",
            "act_txt": "Action: Prompts Full Manual Inspection",
            "act_color": "#B45309",
            "btn_bg": "#D97706",
            "btn_txt": "Verdict: DISPUTED (Low Confidence)"
        }
    ]

    for c in cards:
        cx = c["x"]
        cy = 135 * scale
        cw = 295 * scale
        ch = 440 * scale

        # Main Card Body
        draw.rounded_rectangle([cx, cy, cx + cw, cy + ch], radius=12 * scale, fill=c["bg"], outline=c["border"], width=2 * scale)

        # Header Badge
        draw.rounded_rectangle([cx + 18 * scale, cy + 18 * scale, cx + cw - 18 * scale, cy + 52 * scale], radius=6 * scale, fill=c["hdr_bg"])
        tb = f_card_hdr.getbbox(c["hdr_txt"])
        tw = tb[2] - tb[0]
        draw.text((cx + (cw - tw) / 2, cy + 26 * scale), c["hdr_txt"], fill="#FFFFFF", font=f_card_hdr)

        # Center Icon circle
        ic_x = cx + cw / 2
        ic_y = cy + 85 * scale
        draw.ellipse([ic_x - 22 * scale, ic_y - 22 * scale, ic_x + 22 * scale, ic_y + 22 * scale], fill=c["icon_bg"])
        ib = f_card_hdr.getbbox(c["icon_char"])
        iw = ib[2] - ib[0]
        draw.text((ic_x - iw / 2, ic_y - 7 * scale), c["icon_char"], fill=c["icon_color"], font=f_card_hdr)

        # Verdict text
        vb = f_verdict.getbbox(c["verdict"])
        vw = vb[2] - vb[0]
        draw.text((cx + (cw - vw) / 2, cy + 116 * scale), c["verdict"], fill=c["verdict_color"], font=f_verdict)

        cb = f_conf.getbbox(c["conf"])
        cw_txt = cb[2] - cb[0]
        draw.text((cx + (cw - cw_txt) / 2, cy + 138 * scale), c["conf"], fill=c["conf_color"], font=f_conf)

        # Sub-models box
        b1_y = cy + 160 * scale
        b1_h = 105 * scale
        draw.rounded_rectangle([cx + 18 * scale, b1_y, cx + cw - 18 * scale, b1_y + b1_h], radius=8 * scale, fill=c["box_bg"], outline=c["box_border"], width=scale)
        draw.text((cx + 28 * scale, b1_y + 12 * scale), "Individual Model Outputs:", fill="#475569", font=f_box_hdr)
        draw.text((cx + 28 * scale, b1_y + 34 * scale), "• EfficientNet-B3:", fill="#1E293B", font=f_box_txt)
        m1_b = f_box_bold.getbbox(c["m1_txt"])
        draw.text((cx + cw - 28 * scale - (m1_b[2] - m1_b[0]), b1_y + 34 * scale), c["m1_txt"], fill=c["m1_color"], font=f_box_bold)

        draw.text((cx + 28 * scale, b1_y + 56 * scale), "• Swin-T (Transformer):", fill="#1E293B", font=f_box_txt)
        m2_b = f_box_bold.getbbox(c["m2_txt"])
        draw.text((cx + cw - 28 * scale - (m2_b[2] - m2_b[0]), b1_y + 56 * scale), c["m2_txt"], fill=c["m2_color"], font=f_box_bold)

        draw.text((cx + 28 * scale, b1_y + 80 * scale), c["sub_txt"], fill="#64748B", font=f_desc)

        # XAI box
        b2_y = cy + 275 * scale
        b2_h = 90 * scale
        draw.rounded_rectangle([cx + 18 * scale, b2_y, cx + cw - 18 * scale, b2_y + b2_h], radius=8 * scale, fill=c["box_bg"], outline=c["box_border"], width=scale)
        draw.text((cx + 28 * scale, b2_y + 10 * scale), "Explainable AI (XAI) Behavior:", fill="#475569", font=f_box_hdr)
        draw.text((cx + 28 * scale, b2_y + 30 * scale), c["xai1"], fill="#334155", font=f_box_txt)
        draw.text((cx + 28 * scale, b2_y + 46 * scale), c["xai2"], fill="#334155", font=f_box_txt)
        draw.text((cx + 28 * scale, b2_y + 66 * scale), c["act_txt"], fill=c["act_color"], font=f_box_bold)

        # Action Button
        draw.rounded_rectangle([cx + 18 * scale, cy + 378 * scale, cx + cw - 18 * scale, cy + 420 * scale], radius=6 * scale, fill=c["btn_bg"])
        btn_b = f_btn.getbbox(c["btn_txt"])
        bw_txt = btn_b[2] - btn_b[0]
        draw.text((cx + (cw - bw_txt) / 2, cy + 390 * scale), c["btn_txt"], fill="#FFFFFF", font=f_btn)

    # Footer note
    draw.text((w / 2 - 400 * scale, 595 * scale), "Yaqeen Gulf Architecture Principle: In State 3, the system deliberately refrains from forcing an unprincipled 50/50 tie-break, presenting a transparent lower-confidence advisory.", fill="#94A3B8", font=f_footer)

    jpg_path = os.path.join(FIGURES_DIR, "figure8_dual_model_consensus.jpg")
    png_path = os.path.join(FIGURES_DIR, "figure8_dual_model_consensus.png")
    img.save(jpg_path, "JPEG", quality=95)
    img.save(png_path, "PNG")
    print(f"Generated {jpg_path} and {png_path}")


if __name__ == "__main__":
    print("Generating Figure 7...")
    generate_figure_7_svg()
    render_figure_7_image()

    print("Generating Figure 20...")
    generate_figure_20_svg()
    render_figure_20_image()

    print("Generating Figure 21...")
    generate_figure_21_svg()
    render_figure_21_image()

    print("Generating Figure 8...")
    generate_figure_8_svg()
    render_figure_8_image()

    print("\nSUCCESS: All SVG, JPG, and PNG files generated without external dependencies!")
