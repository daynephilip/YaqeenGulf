"""
generate_decision_flow_chart.py
Generates a focused, publication-grade standalone flowchart image for:
"Decision Flow Architecture & Mechanics - Overlay Three-Tier Image Localisation"
Outputs:
- figures/figure7_decision_flow_architecture.svg
- figures/figure7_decision_flow_architecture.png
- figures/figure7_decision_flow_architecture.jpg
"""

import os
from PIL import Image, ImageDraw, ImageFont

FIGURES_DIR = r"d:\yaqeen-gulf\figures"
os.makedirs(FIGURES_DIR, exist_ok=True)


def generate_svg():
    width = 1000
    height = 920

    svg = f"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <!-- Background -->
  <rect x="0" y="0" width="{width}" height="{height}" fill="#FFFFFF"/>

  <!-- Header Banner -->
  <rect x="0" y="0" width="{width}" height="70" fill="#0F172A"/>
  <text x="30" y="32" fill="#10B981" font-size="12" font-weight="bold" font-family="Arial, sans-serif" letter-spacing="1">YAQEEN GULF • SUBSYSTEM DECISION FLOW</text>
  <text x="30" y="55" fill="#FFFFFF" font-size="17" font-weight="bold" font-family="Arial, sans-serif">Decision Flow Architecture &amp; Mechanics: Three-Tier Image Localisation</text>

  <!-- ==================== INPUT NODE ==================== -->
  <rect x="90" y="96" width="360" height="50" rx="25" ry="25" fill="#F1F5F9" stroke="#475569" stroke-width="2"/>
  <circle cx="120" cy="121" r="14" fill="#E2E8F0"/>
  <rect x="113" y="115" width="14" height="11" rx="1.5" ry="1.5" fill="none" stroke="#475569" stroke-width="1.5"/>
  <circle cx="120" cy="120.5" r="2.5" fill="#475569"/>
  <text x="146" y="116" fill="#0F172A" font-size="12.5" font-weight="bold" font-family="Arial, sans-serif">Screen Framebuffer Captured</text>
  <text x="146" y="133" fill="#64748B" font-size="10.5" font-family="Arial, sans-serif">(MediaProjection / VirtualDisplay stream)</text>

  <!-- Arrow from Input to Tier 1 -->
  <line x1="270" y1="146" x2="270" y2="182" stroke="#475569" stroke-width="2.5"/>
  <polygon points="265,182 275,182 270,188" fill="#475569"/>

  <!-- ==================== TIER 1 ROW ==================== -->
  <!-- Decision Box: Tier 1 -->
  <rect x="70" y="188" width="400" height="96" rx="10" ry="10" fill="#EFF6FF" stroke="#3B82F6" stroke-width="2"/>
  <rect x="86" y="202" width="60" height="20" rx="4" ry="4" fill="#1D4ED8"/>
  <text x="116" y="216" fill="#FFFFFF" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 1</text>
  <text x="156" y="217" fill="#1E3A8A" font-size="13.5" font-weight="bold" font-family="Arial, sans-serif">Accessibility Node Traversal</text>
  <text x="86" y="238" fill="#2563EB" font-size="10.5" font-family="Arial, sans-serif">YaqeenAccessibilityService traverses active window</text>
  <text x="86" y="256" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Valid node: ImageView/Photo, Area ≥ 110dp, Aspect 0.35–2.85</text>

  <!-- Branch: Tier 1 Found (Right) -->
  <line x1="470" y1="236" x2="554" y2="236" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="554,231 554,241 560,236" fill="#10B981"/>
  <rect x="478" y="225" width="70" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="513" y="240" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Found</text>

  <!-- Action Box: Tier 1 -->
  <rect x="560" y="188" width="340" height="96" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="585" cy="214" r="11" fill="#DCFCE7"/>
  <polyline points="580,214 583,217 590,210" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="604" y="218" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Node Bounds</text>
  <text x="576" y="240" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Resolves touch coord (x, y) or dominant card</text>
  <rect x="576" y="252" width="200" height="22" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="676" y="267" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_1_ACCESSIBILITY_NODE</text>

  <!-- Branch: Tier 1 Not Found (Down) -->
  <line x1="270" y1="284" x2="270" y2="346" stroke="#F59E0B" stroke-width="2.5"/>
  <polygon points="265,346 275,346 270,352" fill="#F59E0B"/>
  <rect x="180" y="303" width="180" height="22" rx="11" ry="11" fill="#FEF3C7" stroke="#F59E0B" stroke-width="1"/>
  <text x="270" y="318" fill="#B45309" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Not Found / Obscured (Flutter/WebView)</text>

  <!-- ==================== TIER 2 ROW ==================== -->
  <!-- Decision Box: Tier 2 -->
  <rect x="70" y="352" width="400" height="96" rx="10" ry="10" fill="#F5F3FF" stroke="#8B5CF6" stroke-width="2"/>
  <rect x="86" y="366" width="60" height="20" rx="4" ry="4" fill="#6D28D9"/>
  <text x="116" y="380" fill="#FFFFFF" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 2</text>
  <text x="156" y="381" fill="#4C1D95" font-size="13.5" font-weight="bold" font-family="Arial, sans-serif">Luminance-Variance Saliency</text>
  <text x="86" y="402" fill="#7C3AED" font-size="10.5" font-family="Arial, sans-serif">16px spatial grid luminance texture profiling</text>
  <text x="86" y="420" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Y = 0.299R+0.587G+0.114B; contiguous rows σ²(y) &gt; 250</text>

  <!-- Branch: Tier 2 Found (Right) -->
  <line x1="470" y1="400" x2="554" y2="400" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="554,395 554,405 560,400" fill="#10B981"/>
  <rect x="478" y="389" width="70" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="513" y="404" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Found</text>

  <!-- Action Box: Tier 2 -->
  <rect x="560" y="352" width="340" height="96" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="585" cy="378" r="11" fill="#DCFCE7"/>
  <polyline points="580,378 583,381 590,374" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="604" y="382" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Contour Bounds</text>
  <text x="576" y="404" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Isolates contiguous high-variance detail band</text>
  <rect x="576" y="416" width="170" height="22" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="661" y="431" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_2_CV_CONTOUR</text>

  <!-- Branch: Tier 2 Not Found (Down) -->
  <line x1="270" y1="448" x2="270" y2="510" stroke="#F59E0B" stroke-width="2.5"/>
  <polygon points="265,510 275,510 270,516" fill="#F59E0B"/>
  <rect x="180" y="467" width="180" height="22" rx="11" ry="11" fill="#FEF3C7" stroke="#F59E0B" stroke-width="1"/>
  <text x="270" y="482" fill="#B45309" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Not Found / Uniform (Low Contrast)</text>

  <!-- ==================== TIER 3 ROW ==================== -->
  <!-- Fallback Box: Tier 3 -->
  <rect x="70" y="516" width="400" height="96" rx="10" ry="10" fill="#FFFBEB" stroke="#D97706" stroke-width="2"/>
  <rect x="86" y="530" width="60" height="20" rx="4" ry="4" fill="#B45309"/>
  <text x="116" y="544" fill="#FFFFFF" font-size="10" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER 3</text>
  <text x="156" y="545" fill="#78350F" font-size="13.5" font-weight="bold" font-family="Arial, sans-serif">Fixed Centre-Square Fallback</text>
  <text x="86" y="566" fill="#B45309" font-size="10.5" font-family="Arial, sans-serif">Deterministic geometric screen-center clamp</text>
  <text x="86" y="584" fill="#475569" font-size="10" font-family="Arial, sans-serif">• Dim = min(W_screen, 0.65 · H_screen) centered at screen</text>

  <!-- Branch: Tier 3 Always (Right) -->
  <line x1="470" y1="564" x2="554" y2="564" stroke="#10B981" stroke-width="2.5"/>
  <polygon points="554,559 554,569 560,564" fill="#10B981"/>
  <rect x="478" y="553" width="70" height="22" rx="11" ry="11" fill="#DCFCE7" stroke="#10B981" stroke-width="1"/>
  <text x="513" y="568" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Always</text>

  <!-- Action Box: Tier 3 -->
  <rect x="560" y="516" width="340" height="96" rx="10" ry="10" fill="#FFFFFF" stroke="#10B981" stroke-width="2"/>
  <circle cx="585" cy="542" r="11" fill="#DCFCE7"/>
  <polyline points="580,542 583,545 590,538" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="604" y="546" fill="#065F46" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Extract Centre Square</text>
  <text x="576" y="568" fill="#334155" font-size="10.5" font-family="Arial, sans-serif">• Guaranteed fallback; zero-failure baseline crop</text>
  <rect x="576" y="580" width="170" height="22" rx="4" ry="4" fill="#DCFCE7"/>
  <text x="661" y="595" fill="#047857" font-size="9.5" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">TIER_3_CENTER_CROP</text>

  <!-- ==================== CONVERGENCE BUS ==================== -->
  <line x1="900" y1="236" x2="930" y2="236" stroke="#059669" stroke-width="2.5"/>
  <line x1="900" y1="400" x2="930" y2="400" stroke="#059669" stroke-width="2.5"/>
  <line x1="900" y1="564" x2="930" y2="564" stroke="#059669" stroke-width="2.5"/>
  <line x1="930" y1="236" x2="930" y2="690" stroke="#059669" stroke-width="2.5"/>
  <line x1="930" y1="690" x2="846" y2="690" stroke="#059669" stroke-width="2.5"/>
  <polygon points="846,685 846,695 840,690" fill="#059669"/>

  <!-- ==================== CLAMPED TARGET CROP REGION ==================== -->
  <rect x="160" y="650" width="680" height="80" rx="12" ry="12" fill="#F8FAFC" stroke="#0F172A" stroke-width="2"/>
  <rect x="175" y="662" width="28" height="28" rx="6" ry="6" fill="#0F172A"/>
  <polyline points="183,676 187,680 195,672" fill="none" stroke="#10B981" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
  <text x="214" y="677" fill="#0F172A" font-size="13" font-weight="bold" font-family="Arial, sans-serif">Clamped Target Crop Region (Zero-Failure Guarantee)</text>
  <text x="214" y="696" fill="#475569" font-size="11" font-family="Arial, sans-serif">Display boundary clamping • Bilinear resize to 224 × 224 • RGB normalized (1, 3, 224, 224) FloatBuffer</text>
  <rect x="214" y="706" width="190" height="18" rx="3" ry="3" fill="#DCFCE7"/>
  <text x="309" y="719" fill="#047857" font-size="9" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">✔ GUARANTEED USABLE CROP</text>

  <!-- Arrow down to Dual Engine -->
  <line x1="500" y1="730" x2="500" y2="764" stroke="#0F172A" stroke-width="2.5"/>
  <polygon points="495,764 505,764 500,770" fill="#0F172A"/>

  <!-- ==================== DUAL-MODEL INFERENCE ENGINE ==================== -->
  <rect x="160" y="770" width="680" height="86" rx="12" ry="12" fill="#EFF6FF" stroke="#3B82F6" stroke-width="2"/>
  <text x="500" y="792" fill="#1E3A8A" font-size="13" font-weight="bold" font-family="Arial, sans-serif" text-anchor="middle">Dual-Model Cross-Validation Live-Scan Engine</text>
  
  <rect x="180" y="802" width="310" height="42" rx="6" ry="6" fill="#FFFFFF" stroke="#3B82F6" stroke-width="1"/>
  <text x="195" y="820" fill="#1D4ED8" font-size="11" font-weight="bold" font-family="Arial, sans-serif">Model 1: EfficientNet-B3 (CNN)</text>
  <text x="195" y="835" fill="#64748B" font-size="9.5" font-family="Arial, sans-serif">Convolutional CAM Grid (7 × 7 × 1536)</text>

  <rect x="510" y="802" width="310" height="42" rx="6" ry="6" fill="#FFFFFF" stroke="#8B5CF6" stroke-width="1"/>
  <text x="525" y="820" fill="#6D28D9" font-size="11" font-weight="bold" font-family="Arial, sans-serif">Model 2: Swin-T (Vision Transformer)</text>
  <text x="525" y="835" fill="#64748B" font-size="9.5" font-family="Arial, sans-serif">Spatial Patch CAM Grid (7 × 7 × 768)</text>

  <!-- Footer -->
  <text x="500" y="895" fill="#64748B" font-size="10.5" font-family="Arial, sans-serif" text-anchor="middle">
    Yaqeen Gulf Subsystem Invariant: Tier 1 extracts semantic structure; Tier 2 extracts visual saliency; Tier 3 guarantees zero crash or unhandled null.
  </text>
</svg>
"""
    path = os.path.join(FIGURES_DIR, "figure7_decision_flow_architecture.svg")
    with open(path, "w", encoding="utf-8") as f:
        f.write(svg)
    print(f"Generated {path}")


def render_image():
    scale = 2
    w, h = 1000 * scale, 920 * scale
    img = Image.new("RGB", (w, h), "#FFFFFF")
    draw = ImageDraw.Draw(img)

    f_title = ImageFont.truetype("arialbd.ttf", 17 * scale)
    f_sub = ImageFont.truetype("arialbd.ttf", 12 * scale)
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
    draw.text((30 * scale, 16 * scale), "YAQEEN GULF • SUBSYSTEM DECISION FLOW", fill="#10B981", font=f_sub)
    draw.text((30 * scale, 38 * scale), "Decision Flow Architecture & Mechanics: Three-Tier Image Localisation", fill="#FFFFFF", font=f_title)

    # Input Node
    draw.rounded_rectangle([90 * scale, 96 * scale, 450 * scale, 146 * scale], radius=25 * scale, fill="#F1F5F9", outline="#475569", width=2 * scale)
    draw_screen_icon(120 * scale, 121 * scale, 14 * scale)
    draw.text((146 * scale, 105 * scale), "Screen Framebuffer Captured", fill="#0F172A", font=f_hdr)
    draw.text((146 * scale, 124 * scale), "(MediaProjection / VirtualDisplay stream)", fill="#64748B", font=f_txt)
    draw_v_arrow(146 * scale, 188 * scale, 270 * scale, "#475569")

    # ==================== TIER 1 ROW ====================
    # Decision Card
    draw.rounded_rectangle([70 * scale, 188 * scale, 470 * scale, 284 * scale], radius=10 * scale, fill="#EFF6FF", outline="#3B82F6", width=2 * scale)
    draw.rounded_rectangle([86 * scale, 202 * scale, 146 * scale, 222 * scale], radius=4 * scale, fill="#1D4ED8")
    draw.text((94 * scale, 205 * scale), "TIER 1", fill="#FFFFFF", font=f_tag)
    draw.text((156 * scale, 203 * scale), "Accessibility Node Traversal", fill="#1E3A8A", font=f_hdr)
    draw.text((86 * scale, 228 * scale), "YaqeenAccessibilityService traverses active window", fill="#2563EB", font=f_txt)
    draw.text((86 * scale, 248 * scale), "• Valid node: ImageView/Photo, Area ≥ 110dp, Aspect 0.35–2.85", fill="#475569", font=f_txt)

    # Branch: Found (Right)
    draw_h_arrow(470 * scale, 560 * scale, 236 * scale, "#10B981")
    draw.rounded_rectangle([478 * scale, 225 * scale, 548 * scale, 247 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((495 * scale, 228 * scale), "Found", fill="#047857", font=f_sm_b)

    # Action Card 1
    draw.rounded_rectangle([560 * scale, 188 * scale, 900 * scale, 284 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(585 * scale, 214 * scale, 11 * scale, "#DCFCE7", "#059669")
    draw.text((604 * scale, 205 * scale), "Extract Node Bounds", fill="#065F46", font=f_hdr)
    draw.text((576 * scale, 230 * scale), "• Resolves touch coord (x, y) or dominant card", fill="#334155", font=f_txt)
    draw.rounded_rectangle([576 * scale, 252 * scale, 776 * scale, 274 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((586 * scale, 256 * scale), "TIER_1_ACCESSIBILITY_NODE", fill="#047857", font=f_sm_b)

    # Branch: Not Found (Down)
    draw_v_arrow(284 * scale, 352 * scale, 270 * scale, "#F59E0B")
    draw.rounded_rectangle([180 * scale, 303 * scale, 360 * scale, 325 * scale], radius=11 * scale, fill="#FEF3C7", outline="#F59E0B", width=scale)
    draw.text((192 * scale, 306 * scale), "Not Found / Obscured (Flutter/WebView)", fill="#B45309", font=f_sm_b)

    # ==================== TIER 2 ROW ====================
    # Decision Card
    draw.rounded_rectangle([70 * scale, 352 * scale, 470 * scale, 448 * scale], radius=10 * scale, fill="#F5F3FF", outline="#8B5CF6", width=2 * scale)
    draw.rounded_rectangle([86 * scale, 366 * scale, 146 * scale, 386 * scale], radius=4 * scale, fill="#6D28D9")
    draw.text((94 * scale, 369 * scale), "TIER 2", fill="#FFFFFF", font=f_tag)
    draw.text((156 * scale, 367 * scale), "Luminance-Variance Saliency", fill="#4C1D95", font=f_hdr)
    draw.text((86 * scale, 392 * scale), "16px spatial grid luminance texture profiling", fill="#7C3AED", font=f_txt)
    draw.text((86 * scale, 412 * scale), "• Y = 0.299R+0.587G+0.114B; contiguous rows σ²(y) > 250", fill="#475569", font=f_txt)

    # Branch: Found (Right)
    draw_h_arrow(470 * scale, 560 * scale, 400 * scale, "#10B981")
    draw.rounded_rectangle([478 * scale, 389 * scale, 548 * scale, 411 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((495 * scale, 392 * scale), "Found", fill="#047857", font=f_sm_b)

    # Action Card 2
    draw.rounded_rectangle([560 * scale, 352 * scale, 900 * scale, 448 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(585 * scale, 378 * scale, 11 * scale, "#DCFCE7", "#059669")
    draw.text((604 * scale, 369 * scale), "Extract Contour Bounds", fill="#065F46", font=f_hdr)
    draw.text((576 * scale, 394 * scale), "• Isolates contiguous high-variance detail band", fill="#334155", font=f_txt)
    draw.rounded_rectangle([576 * scale, 416 * scale, 746 * scale, 438 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((586 * scale, 420 * scale), "TIER_2_CV_CONTOUR", fill="#047857", font=f_sm_b)

    # Branch: Not Found (Down)
    draw_v_arrow(448 * scale, 516 * scale, 270 * scale, "#F59E0B")
    draw.rounded_rectangle([180 * scale, 467 * scale, 360 * scale, 489 * scale], radius=11 * scale, fill="#FEF3C7", outline="#F59E0B", width=scale)
    draw.text((195 * scale, 470 * scale), "Not Found / Uniform (Low Contrast)", fill="#B45309", font=f_sm_b)

    # ==================== TIER 3 ROW ====================
    # Fallback Card
    draw.rounded_rectangle([70 * scale, 516 * scale, 470 * scale, 612 * scale], radius=10 * scale, fill="#FFFBEB", outline="#D97706", width=2 * scale)
    draw.rounded_rectangle([86 * scale, 530 * scale, 146 * scale, 550 * scale], radius=4 * scale, fill="#B45309")
    draw.text((94 * scale, 533 * scale), "TIER 3", fill="#FFFFFF", font=f_tag)
    draw.text((156 * scale, 531 * scale), "Fixed Centre-Square Fallback", fill="#78350F", font=f_hdr)
    draw.text((86 * scale, 556 * scale), "Deterministic geometric screen-center clamp", fill="#B45309", font=f_txt)
    draw.text((86 * scale, 576 * scale), "• Dim = min(W_screen, 0.65 · H_screen) centered at screen", fill="#475569", font=f_txt)

    # Branch: Always (Right)
    draw_h_arrow(470 * scale, 560 * scale, 564 * scale, "#10B981")
    draw.rounded_rectangle([478 * scale, 553 * scale, 548 * scale, 575 * scale], radius=11 * scale, fill="#DCFCE7", outline="#10B981", width=scale)
    draw.text((492 * scale, 556 * scale), "Always", fill="#047857", font=f_sm_b)

    # Action Card 3
    draw.rounded_rectangle([560 * scale, 516 * scale, 900 * scale, 612 * scale], radius=10 * scale, fill="#FFFFFF", outline="#10B981", width=2 * scale)
    draw_check_icon(585 * scale, 542 * scale, 11 * scale, "#DCFCE7", "#059669")
    draw.text((604 * scale, 533 * scale), "Extract Centre Square", fill="#065F46", font=f_hdr)
    draw.text((576 * scale, 558 * scale), "• Guaranteed fallback; zero-failure baseline crop", fill="#334155", font=f_txt)
    draw.rounded_rectangle([576 * scale, 580 * scale, 746 * scale, 602 * scale], radius=4 * scale, fill="#DCFCE7")
    draw.text((586 * scale, 584 * scale), "TIER_3_CENTER_CROP", fill="#047857", font=f_sm_b)

    # ==================== CONVERGENCE BUS ====================
    draw.line([(900 * scale, 236 * scale), (930 * scale, 236 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(900 * scale, 400 * scale), (930 * scale, 400 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(900 * scale, 564 * scale), (930 * scale, 564 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.line([(930 * scale, 236 * scale), (930 * scale, 690 * scale)], fill="#059669", width=int(2.5 * scale))
    
    # Arrow into Clamped Target Crop Region
    draw.line([(930 * scale, 690 * scale), (846 * scale, 690 * scale)], fill="#059669", width=int(2.5 * scale))
    draw.polygon([(840 * scale, 690 * scale), (846 * scale, 685 * scale), (846 * scale, 695 * scale)], fill="#059669")

    # ==================== CLAMPED TARGET CROP REGION ====================
    draw.rounded_rectangle([160 * scale, 650 * scale, 840 * scale, 730 * scale], radius=12 * scale, fill="#F8FAFC", outline="#0F172A", width=2 * scale)
    draw.rounded_rectangle([175 * scale, 662 * scale, 203 * scale, 690 * scale], radius=6 * scale, fill="#0F172A")
    draw_check_icon(189 * scale, 676 * scale, 8 * scale, "#0F172A", "#10B981")
    draw.text((214 * scale, 664 * scale), "Clamped Target Crop Region (Zero-Failure Guarantee)", fill="#0F172A", font=f_hdr)
    draw.rounded_rectangle([214 * scale, 704 * scale, 414 * scale, 722 * scale], radius=3 * scale, fill="#DCFCE7")
    draw_check_icon(226 * scale, 713 * scale, 4 * scale, "#DCFCE7", "#047857")
    draw.text((236 * scale, 706 * scale), "GUARANTEED USABLE CROP", fill="#047857", font=f_sm_b)

    # Arrow down to Dual Engine
    draw_v_arrow(730 * scale, 770 * scale, 500 * scale, "#0F172A")

    # ==================== DUAL-MODEL INFERENCE ENGINE ====================
    draw.rounded_rectangle([160 * scale, 770 * scale, 840 * scale, 856 * scale], radius=12 * scale, fill="#EFF6FF", outline="#3B82F6", width=2 * scale)
    draw.text((320 * scale, 778 * scale), "Dual-Model Cross-Validation Live-Scan Engine", fill="#1E3A8A", font=f_hdr)

    draw.rounded_rectangle([180 * scale, 802 * scale, 490 * scale, 844 * scale], radius=6 * scale, fill="#FFFFFF", outline="#3B82F6", width=scale)
    draw.text((195 * scale, 810 * scale), "Model 1: EfficientNet-B3 (CNN)", fill="#1D4ED8", font=f_bold)
    draw.text((195 * scale, 826 * scale), "Convolutional CAM Grid (7 × 7 × 1536)", fill="#64748B", font=f_xs)

    draw.rounded_rectangle([510 * scale, 802 * scale, 820 * scale, 844 * scale], radius=6 * scale, fill="#FFFFFF", outline="#8B5CF6", width=scale)
    draw.text((525 * scale, 810 * scale), "Model 2: Swin-T (Vision Transformer)", fill="#6D28D9", font=f_bold)
    draw.text((525 * scale, 826 * scale), "Spatial Patch CAM Grid (7 × 7 × 768)", fill="#64748B", font=f_xs)

    # Footer
    draw.text((80 * scale, 890 * scale), "Yaqeen Gulf Subsystem Invariant: Tier 1 extracts semantic structure; Tier 2 extracts visual saliency; Tier 3 guarantees zero crash or unhandled null.", fill="#64748B", font=f_footer)

    jpg_path = os.path.join(FIGURES_DIR, "figure7_decision_flow_architecture.jpg")
    png_path = os.path.join(FIGURES_DIR, "figure7_decision_flow_architecture.png")
    img.save(jpg_path, "JPEG", quality=95)
    img.save(png_path, "PNG")
    print(f"Generated {jpg_path} and {png_path}")


if __name__ == "__main__":
    print("Generating Figure 7 Decision Flow Architecture Chart...")
    generate_svg()
    render_image()
    print("SUCCESS: Decision flow chart generated!")
