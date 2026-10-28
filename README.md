# Yaqeen Gulf (يقين الخليج)
### On-Device Multimodal Deepfake Verification & Explainable AI Shield

[![Android](https://img.shields.io/badge/Platform-Android_10+-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![ONNX Runtime](https://img.shields.io/badge/Inference-ONNX_Runtime_Mobile-005CED.svg?logo=onnx&logoColor=white)](https://onnxruntime.ai)
[![Models](https://img.shields.io/badge/%F0%9F%A4%97%20Hugging%20Face-Models-FFD21E.svg)](https://huggingface.co/daynephilip/yaqeen-deepfake-models)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

Yaqeen Gulf is an on-device multimodal deepfake detection system engineered specifically for Gulf and Arabic-region threat models. It combines edge neural networks (image, audio, text), Class Activation Mapping (CAM) explainability, C2PA digital provenance validation, and real-time screen overlay inspection.

---

## 🚀 Key Architectural Highlights

- **On-Device Edge Inference**: All deepfake detection runs 100% locally on Android via ONNX Runtime Mobile — no media bytes leave the user's device.
- **Multimodal Ensembles (Arabic & English)**:
  - **Image**: Dual-engine pipeline featuring EfficientNet-B3 (spatial artifacts) and Swin Transformer-T (patch-level synthetic patterns).
  - **Audio**: Spectrogram CNNs trained on ASVspoof (English) and ARENA-V (Gulf Arabic dialect).
  - **Text**: WordPiece Transformer models fine-tuned on ISOT (English) and AFND / AraBERT (Arabic fake news detection).
- **Explainable AI (XAI)**:
  - Grad-CAM heatmap overlays highlighting manipulated image pixels.
  - Frequency-time spectrogram activation maps identifying synthetic acoustic artifacts.
  - WordPiece token-level saliency highlights for deceptive text.
- **Hardware-Isolated Cloud Proxy**: Zero-data-retention proxy for external fact-checking and SSRF-hardened claim verification.

---

## 📦 Model Weights (Hugging Face)

All trained model weights and ONNX checkpoints are hosted on the Hugging Face Hub:
👉 **[daynephilip/yaqeen-deepfake-models](https://huggingface.co/daynephilip/yaqeen-deepfake-models)**

### Downloading Models
To download all required ONNX models into the Android assets directory (`app/src/main/assets/models/`), run:

```bash
python execution/download_models.py
```

---

## 🛠️ Project Setup & Building

### 1. Prerequisites
- **Android Studio** (Koala / Ladybug or newer)
- **JDK 17** (e.g. Microsoft OpenJDK 17 or Eclipse Temurin)
- **Android SDK Platform 36** (Min SDK 29 / Android 10+)
- **Python 3.10+** (for export & model download scripts)

### 2. Configure Local Properties
Copy the template configuration:
```bash
cp local.properties.example local.properties
```
*Android Studio will automatically populate `sdk.dir` upon opening.*

### 3. Build APK
- **Debug Build**:
  ```bash
  ./gradlew assembleDebug
  ```
  *Output APK: `app/build/outputs/apk/debug/app-debug.apk`*

- **Release Build (Signed)**:
  ```powershell
  powershell -ExecutionPolicy Bypass -File execution/build_signed_apk.ps1 -BuildType Release
  ```

---

## 🌐 Cloud Proxy (Optional Fact-Checking)

The cloud fact-checking proxy resides in `cloud_proxy/`:
```bash
cd cloud_proxy
npm install
cp .env.example .env
# Fill in GEMINI_API_KEY / GROQ_API_KEY in .env
npm run dev
```

---

## 📜 Repository Structure

```
yaqeen-gulf/
├── app/                  # Android application module (Jetpack Compose, Room, ONNX Runtime)
│   ├── src/main/assets/  # Model assets, FC weights, and vocabularies
│   └── src/main/java/    # Deepfake orchestrator, inference, overlay, and UI
├── cloud_proxy/          # Vercel serverless proxy for fact-checking & LLM debunking
├── directives/           # Standard Operating Procedures & phase specifications
├── execution/            # Layer 3 execution scripts (ONNX export, build, download)
├── figures/              # Thesis diagrams, confusion matrices, and ROC curves
├── ml_export/            # ML export pipelines and verification harnesses
└── xai_dumps/            # Sample CAM overlays and XAI verification artifacts
```

---

## 🛡️ License & Ethics

This software is developed for research, education, and defense against malicious synthetic media. Media processed on the device remains entirely private and never leaves the host device.
