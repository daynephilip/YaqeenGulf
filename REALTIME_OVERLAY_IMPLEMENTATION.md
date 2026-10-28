# Yaqeen Gulf: Real-Time On-Screen Detection & Floating HUD Overlay Subsystem
## Comprehensive Technical Architecture, Implementation Mechanics, and Academic Thesis Reference

---

### Executive Overview

The **Real-Time On-Screen Detection and Floating Overlay Subsystem** in Yaqeen Gulf (`com.yaqeengulf.deepfake`) delivers non-intrusive, zero-latency media authenticity analysis directly over third-party applications (e.g., social media platforms such as TikTok, Instagram, X/Twitter, WhatsApp, and YouTube). Rather than requiring users to manually capture, save, and import suspicious media files into the main application, the overlay provides an always-on-top, draggable Heads-Up Display (HUD). 

Operating as an Android Foreground Service (`FloatingOverlayService`), the subsystem integrates:
1. **Hardware-Accelerated Framebuffer Projection:** Zero-copy screen capture via Android `MediaProjection` and `VirtualDisplay`.
2. **Autonomous Three-Tier Image Localization & Cropping:** Traversal of Android accessibility node hierarchies (`YaqeenAccessibilityService`) paired with Computer Vision luminance-variance saliency detection to isolate photographic media from surrounding UI chrome.
3. **Dual-Model Cross-Validation Live-Scan:** Sequential on-device execution of both EfficientNet-B3 (CNN Conv CAM) and Swin Transformer (Swin-T Patch-Grid CAM) on the exact same cropped frame, producing real-time consensus telemetry.
4. **In-Place Live Audio Capture:** Internal audio stream recording via `AudioPlaybackCaptureConfiguration` (API 29+) and live microphone loopback, paired with floating acoustic spectral analysis.
5. **Non-Blocking Window Management:** Custom touch-physics handling, edge-snapping coordinates, and floating card overlays rendered through the Android `WindowManager` without intercepting background app touches.

---

## 1. System Architecture & Component Interactions

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                THIRD-PARTY FOREGROUND APP                              │
│                    (Social Media Feed, Web Browser, or Messaging App)                   │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Visual & Acoustic Output
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              YAQEEN GULF OVERLAY SUBSYSTEM                             │
│                                                                                        │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │                 FloatingOverlayService (Foreground Android Service)            │   │
│   │   - WindowManager attachment (TYPE_APPLICATION_OVERLAY)                        │   │
│   │   - Touch drag listener with inertia & edge snapping                           │   │
│   │   - Expandable Quick Shield Radial Menu                                        │   │
│   │   - Live Scanning Pills & Floating In-Place Result Cards                       │   │
│   └───────────────────────┬───────────────────────────────┬────────────────────────┘   │
│                           │ Trigger Action                │ Trigger Action             │
│                           ▼                               ▼                            │
│   ┌───────────────────────────────────┐   ┌────────────────────────────────────────┐   │
│   │       IMAGE LIVE-SCAN PIPELINE    │   │        AUDIO LIVE-SCAN PIPELINE        │   │
│   │                                   │   │                                        │   │
│   │ 1. Screen Framebuffer Capture     │   │ 1. AudioPlaybackCaptureConfiguration   │   │
│   │    (VirtualDisplay / ImageReader) │   │    (API 29+ USAGE_MEDIA loopback)      │   │
│   │                                   │   │    OR AudioRecord 16kHz PCM mono       │   │
│   │ 2. 3-Tier Autonomous Crop:        │   │                                        │   │
│   │    - Tier 1: Accessibility Node   │   │ 2. Real-Time Listening Pill HUD        │   │
│   │    - Tier 2: CV Saliency Contour  │   │    (Progressive 4.0s sample buffer)    │   │
│   │    - Tier 3: Central Square Crop  │   │                                        │   │
│   │                                   │   │ 3. AudioMelPreprocessor (Pure Kotlin)  │   │
│   │ 3. Dual-Model Execution:          │   │    (STFT 512, 80 Mel bins -> 401 time) │   │
│   │    - Model 1: EfficientNet-B3 CAM │   │                                        │   │
│   │    - Model 2: Swin-T Patch CAM    │   │ 4. SpectrogramCNN Inference (ArEnAV)   │   │
│   │    - Cross-Model Consensus Metric │   │    & Analytical ReLU Gradient CAM      │   │
│   │                                   │   │                                        │   │
│   │ 4. Floating Dual Grad-CAM Card    │   │ 5. Floating In-Place Audio Result Card │   │
│   │    (Side-by-side composite views) │   │    (Score, Risk, Full Report Trigger)  │   │
│   └───────────────────────────────────┘   └────────────────────────────────────────┘   │
│                                           │                                            │
│                                           ▼                                            │
│   ┌────────────────────────────────────────────────────────────────────────────────┐   │
│   │                     Local Persistence & Data Management                        │   │
│   │   - Room Database (ScanHistoryEntity with sourceType = "overlay")              │   │
│   │   - Telemetry Logging (Crop resolution tier, latency, model agreement)        │   │
│   └────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Window Manager Parameters & Lifecycle Mechanics

The overlay HUD is implemented in `FloatingOverlayService.kt` as an Android `Service` declared with foreground service type `mediaProjection` and `microphone` in `AndroidManifest.xml`.

### 2.1 WindowManager Layout Configurations

To render on top of all system windows while remaining non-intrusive and battery-efficient, `WindowManager.LayoutParams` are customized depending on the active overlay widget:

#### The Floating Shield Bubble (Resting State)
```kotlin
val params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
        @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.TOP or Gravity.START
    x = 60
    y = 300
}
```
- **`TYPE_APPLICATION_OVERLAY`:** Required on Android 8.0+ (API 26+) for system-level overlays.
- **`FLAG_NOT_FOCUSABLE`:** Ensures key events and IME soft keyboards pass through unimpeded to the underlying app (e.g., text inputs in chat applications remain fully functional).
- **`FLAG_LAYOUT_NO_LIMITS`:** Allows the bubble to be dragged smoothly along display cutouts and screen borders without clipping.

### 2.2 Touch Event Processing & Edge Snapping Physics

The floating bubble manages motion events via an explicit `View.OnTouchListener`:
- **Delta Threshold Filtering:** Differentiates between intentional dragging and user taps:
  $$\Delta d = \sqrt{(x_{\text{raw}} - x_{\text{initial}})^2 + (y_{\text{raw}} - y_{\text{initial}})^2}$$
  If $\Delta d \le 10\text{ px}$, the interaction is classified as an intentional tap (`isClick = true`), triggering the action menu.
- **Real-Time Translation:** During `MotionEvent.ACTION_MOVE`, coordinates are updated dynamically via `windowManager?.updateViewLayout(container, params)`.
- **Spring Edge Snapping:** On `MotionEvent.ACTION_UP`, if dragged, the widget computes the nearest display boundary:
  $$x_{\text{target}} = \begin{cases} 16\text{ dp} & \text{if } x_{\text{current}} < \frac{W_{\text{screen}}}{2} \\ W_{\text{screen}} - W_{\text{bubble}} - 16\text{ dp} & \text{otherwise} \end{cases}$$
  The bubble snaps smoothly to the nearest edge to minimize visual obstruction.

---

## 3. Autonomous Three-Tier Image Localization & Cropping

When the user initiates an on-screen image verification, the system must extract the target media graphic from surrounding screen content (system status bars, navigation buttons, captions, like/comment buttons). `YaqeenAccessibilityService.kt` executes an autonomous three-tier localization algorithm:

```
Screen Framebuffer Captured
            │
            ▼
    [ Tier 1 Check ] ─── Valid Accessibility Node Found? ───► YES ──► Extract Node Bounds
            │                                                                │
           NO                                                                │
            ▼                                                                │
    [ Tier 2 Check ] ─── Saliency Row/Col Variance Found? ──► YES ──► Extract Contour Bounds
            │                                                                │
           NO                                                                │
            ▼                                                                │
    [ Tier 3 Fallback ] ────────────────────────────────────────────► Extract Center Square
                                                                             │
                                                                             ▼
                                                                Clamped Target Crop Rect
```

### 3.1 Tier 1: Accessibility Node Tree Traversal
The service queries `rootInActiveWindow` and recursively traverses the visible `AccessibilityNodeInfo` hierarchy:
- **Class Filtering:** Searches for view classes matching `ImageView`, `Photo`, or `Picture`.
- **Visibility & Dimension Constraints:** Nodes must satisfy:
  $$W_{\text{node}} \ge 110\text{ dp}, \quad H_{\text{node}} \ge 110\text{ dp}, \quad 0.35 \le \frac{W_{\text{node}}}{H_{\text{node}}} \le 2.85$$
  This immediately filters out notification badges, avatar circles, and UI navigation icons.
- **Touch-Proximity Resolution:** If touch coordinates $(x_{\text{tap}}, y_{\text{tap}})$ are supplied, the algorithm selects the tightest candidate rectangle enclosing the tap:
  $$R^* = \arg\min_{R \in \{R_i \mid (x_{\text{tap}}, y_{\text{tap}}) \in R_i\}} (\text{Area}(R))$$
  If no touch coordinates are provided, the algorithm selects the largest visible media card.

### 3.2 Tier 2: Computer Vision Saliency & Luminance Variance Fallback
If the underlying application renders inside a hardware canvas, web view, or Flutter engine where accessibility nodes are obscured, Tier 2 analyzes the captured bitmap using spatial luminance variance:
1. Downsamples the bitmap into a 16-pixel grid ($S_R = \frac{H}{16}$, $S_C = \frac{W}{16}$).
2. Calculates perceived luminance for each grid sample:
   $$Y(x, y) = 0.299 \cdot R(x, y) + 0.587 \cdot G(x, y) + 0.114 \cdot B(x, y)$$
3. Computes the spatial variance across rows:
   $$\sigma^2(y) = \frac{1}{S_C} \sum_{c=1}^{S_C} Y(c \cdot 16, y)^2 - \left(\frac{1}{S_C} \sum_{c=1}^{S_C} Y(c \cdot 16, y)\right)^2$$
4. Identifies the contiguous band of rows exhibiting high spatial detail ($\sigma^2(y) > 250$), isolating photographic media from uniform application backgrounds.

### 3.3 Tier 3: Central Square Fallback
If both node extraction and variance filtering fail, Tier 3 clamps to a central bounding square:
$$\text{Dim}_{\text{center}} = \min(W_{\text{screen}}, 0.65 \cdot H_{\text{screen}})$$
$$x_{\text{left}} = \frac{W_{\text{screen}} - \text{Dim}_{\text{center}}}{2}, \quad y_{\text{top}} = \frac{H_{\text{screen}} - \text{Dim}_{\text{center}}}{2}$$

---

## 4. Dual-Model Cross-Validation Live-Scan (Feature A)

A core innovation in Yaqeen Gulf's real-time detection is **Dual-Model Cross-Validation**. Relying on a single model can produce false positives on unseen generation architectures. When the overlay scans an image, it executes both **EfficientNet-B3** and **Swin-T** on the identical cropped frame.

### 4.1 Orchestration & Inference Flow
In `VerificationOrchestrator.kt`, `verifyImageDual()` coordinates execution on `Dispatchers.Default`:

```kotlin
suspend fun verifyImageDual(
    croppedBitmap: Bitmap,
    cropBounds: Rect = Rect(0, 0, croppedBitmap.width, croppedBitmap.height),
    resolutionTier: CropResolutionTier = CropResolutionTier.TIER_1_ACCESSIBILITY_NODE,
    sourceTag: String = "Overlay Live-Scan"
): DualImageScanResult = withContext(Dispatchers.Default) {
    // 1. EfficientNet-B3 (CNN Conv-Grid CAM)
    val effnetInf = modelManager.runImageInference(croppedBitmap, ImageModelType.EFFICIENTNET_B3)
    val effnetProbs = softmax(effnetInf.logits)
    val effnetFakeProb = effnetProbs[0]
    val effnetVerdict = if (effnetFakeProb >= 0.05f) "SYNTHETIC" else if (effnetFakeProb > 0.00099f) "POTENTIALLY FAKE" else "REAL"
    val effnetWeights = FcWeightsLoader.loadEffnetWeights(context)
    val effnetCam = ImageCamPostProcessor.processEffnetCam(
        featureMap = effnetInf.xaiData,
        weights = effnetWeights,
        targetClass = if (effnetFakeProb >= 0.00099f) 0 else 1,
        createBitmap = true
    )
    val effnetComposite = compositeHeatmap(croppedBitmap, effnetCam.heatmapBitmap)

    // 2. Swin-T (Vision Transformer Spatial Patch CAM)
    val swinInf = modelManager.runImageInference(croppedBitmap, ImageModelType.SWIN_T)
    val swinProbs = softmax(swinInf.logits)
    val swinFakeProb = swinProbs[0]
    val swinVerdict = if (swinFakeProb >= 0.05f) "SYNTHETIC" else if (swinFakeProb > 0.00099f) "POTENTIALLY FAKE" else "REAL"
    val swinWeights = FcWeightsLoader.loadSwinWeights(context)
    val swinCam = ImageCamPostProcessor.processSwinCam(
        featureMap = swinInf.xaiData,
        weights = swinWeights,
        targetClass = if (swinFakeProb >= 0.00099f) 0 else 1,
        createBitmap = true
    )
    val swinComposite = compositeHeatmap(croppedBitmap, swinCam.heatmapBitmap)

    // 3. Cross-Model Consensus Metrics
    val agree = (effnetVerdict == swinVerdict)
    val agreementSummary = when {
        effnetVerdict == "SYNTHETIC" && swinVerdict == "SYNTHETIC" -> "Both models agree: Synthetic / AI-Generated"
        effnetVerdict == "REAL" && swinVerdict == "REAL" -> "Both models agree: Authentic Photo"
        agree -> "Both models agree: $effnetVerdict"
        else -> "Models Disagree — EffNet-B3: $effnetVerdict vs Swin-T: $swinVerdict"
    }

    // Return structured DualImageScanResult
    ...
}
```

### 4.2 Floating Dual Grad-CAM Presentation Card
Once inference finishes, `FloatingOverlayService` mounts an interactive floating card directly above the scanned content:
- **Agreement Header:**
  - `🚨 Models Agree: SYNTHETIC` (Crimson border `#EF4444`, high-priority alert).
  - `✅ Models Agree: REAL` (Emerald border `#10B981`, authentic confirmation).
  - `⚠️ Models Disagree` (Amber border `#F59E0B`, warning indicating architecture divergence).
- **Telemetry Tag:** Displays resolution tier and pixel bounds (e.g., `Tier 1: Accessibility Node Tree • 1080x1350 px`).
- **Side-by-Side Grad-CAM Heatmaps:**
  - **Left View:** EfficientNet-B3 heatmap composite showing convolutional frequency artifacts.
  - **Right View:** Swin-T heatmap composite showing self-attention patch boundary anomalies.
- **Deep-Link Trigger:** Provides an `Open Full Report in Yaqeen` button that launches `MainActivity` to view full token statistics, share reports, or inspect C2PA metadata.

---

## 5. In-Place Acoustic Live-Scan (Voice & Audio)

For audio shared in videos, voice notes, or podcasts, the overlay implements an in-place acoustic capture pipeline:

### 5.1 Floating Recording Pill HUD
When the user taps `🎙️ Quick Voice Check (Listen)` from the action menu:
1. The overlay mounts a compact floating pill at the top of the display (`#0C1B2E` surface with amber border `#F59E0B`).
2. A high-priority coroutine runs `AudioCaptureManager.recordAudio(durationSeconds = 4)` with a progress listener.
3. The pill provides real-time feedback:
   $$\text{"🎙️ Listening (0/4s)..."} \longrightarrow \text{"🎙️ Listening (2/4s)..."} \longrightarrow \text{"Analyzing acoustic spectrum..."}$$

### 5.2 Signal Flow & Model Evaluation
- Captured PCM 16-bit 16,000 Hz mono audio is passed directly to `AudioMelPreprocessor`.
- Evaluated by `SpectrogramCNN` (trained on ASVspoof 2019 LA and ArEnAV benchmarks).
- Computes analytical-gradient CAM across the $256 \times 10 \times 50$ feature map without running autograd on-device.

### 5.3 Floating Audio Result Card
Within ~300 ms of recording completion, the pill transitions into an acoustic verdict card:
- Displays verdict (`❌ SYNTHETIC`, `⚠️ SUSPICIOUS`, or `✅ REAL`), confidence percentage, and raw model score (e.g., `0.98412`).
- Summarizes detected synthetic vocoder footprints or confirms natural vocal tract resonance.
- Allows closing or deep-linking directly into the main application.

---

## 6. Permissions, Security, and Graceful Degradation

The overlay subsystem interacts with sensitive Android permissions. It is architected to remain **fully functional when permissions are granted, and gracefully non-blocking when permissions are denied**.

| Permission | System API Level | Subsystem Usage | Degradation Behavior if Denied |
|---|---|---|---|
| `SYSTEM_ALERT_WINDOW` | API 23+ (Marshmallow) | Mounting floating HUD container, radial menu, and in-place result cards via `WindowManager`. | The floating overlay does not start; all scan functionalities remain 100% accessible via manual file pickers in the main dashboard. |
| `BIND_ACCESSIBILITY_SERVICE` | API 16+ | On-screen text scraping and Tier-1 image bounding box detection. | The app falls back automatically to Tier-2 CV saliency and Tier-3 center cropping. |
| `MEDIA_PROJECTION` | API 21+ | Capturing framebuffer screenshots and internal system audio loopback. | The user can record audio via the physical microphone (`RECORD_AUDIO`) and import screenshots manually from the gallery. |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | API 34+ (Android 14) | Declaring foreground service media projection type for background recording compliance. | Build-level requirement; enforces system foreground notification visibility (`Yaqeen Gulf Shield Active`). |

---

## 7. Data Models & Verification Schemas

### 7.1 `DualImageScanResult` Schema
```kotlin
data class DualImageScanResult(
    val croppedBitmap: Bitmap? = null,
    val cropBounds: Rect? = null,
    val resolutionTier: CropResolutionTier = CropResolutionTier.TIER_1_ACCESSIBILITY_NODE,

    // Model 1: EfficientNet-B3 (CNN Conv-CAM)
    val effnetVerdict: String,
    val effnetFakeProbability: Float,
    val effnetConfidence: Float,
    val effnetCam: ImageXaiResult,
    val effnetComposite: Bitmap? = null,

    // Model 2: Swin-T (Vision Transformer Patch-CAM)
    val swinVerdict: String,
    val swinFakeProbability: Float,
    val swinConfidence: Float,
    val swinCam: ImageXaiResult,
    val swinComposite: Bitmap? = null,

    // Consensus Metrics
    val modelsAgree: Boolean,
    val agreementSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 7.2 Unit Testing & Validation Suite
The subsystem's mathematical and operational logic is verified in `DualImageScanUnitTest.kt` and `AudioCaptureUnitTest.kt`:
- **Bounding Box Validation:** Verifies coordinate intersection clamping against varied screen geometries (1080p, 1440p, foldable aspect ratios).
- **Consensus Logic Testing:** Tests all permutation matrices (both agree synthetic, both agree real, conflicting verdicts) to ensure consistent badge rendering.
- **Service Stability Testing:** Validates rapid attach/detach sequences across configuration changes (screen rotations, multi-window splits).

---

## 8. Thesis Integration Guide

When detailing this subsystem in an academic thesis:
- **Chapter 3 (System Architecture):** Cite Section 1 (Interaction Architecture) and Section 2 (Window Management and Touch Physics). Highlight how mobile OS window hierarchies can host real-time inference without interrupting user workflows.
- **Chapter 5 (Edge Implementation & Algorithms):** Cite Section 3 (Autonomous Three-Tier Localization) and Section 4 (Dual-Model Cross-Validation). Use the mathematical formulations for luminance variance filtering and the side-by-side CAM rendering approach.
- **Chapter 6 (Security & Privacy):** Cite Section 6 (Permission Gating & Graceful Degradation) to demonstrate zero-privilege escalation and privacy protection against background screen scraping.
