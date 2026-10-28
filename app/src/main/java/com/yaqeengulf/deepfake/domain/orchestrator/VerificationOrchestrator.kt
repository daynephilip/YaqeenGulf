package com.yaqeengulf.deepfake.domain.orchestrator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import com.yaqeengulf.deepfake.data.local.AppDatabase
import com.yaqeengulf.deepfake.data.local.ScanHistoryEntity
import com.yaqeengulf.deepfake.domain.model.ContentCredentialsResult
import com.yaqeengulf.deepfake.domain.model.CropResolutionTier
import com.yaqeengulf.deepfake.domain.model.DualImageScanResult
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import com.yaqeengulf.deepfake.domain.router.LanguageRouter
import com.yaqeengulf.deepfake.inference.ModelManager
import com.yaqeengulf.deepfake.inference.ModelType
import com.yaqeengulf.deepfake.xai.AudioCamPostProcessor
import com.yaqeengulf.deepfake.xai.AudioXaiResult
import com.yaqeengulf.deepfake.xai.FcWeightsLoader
import com.yaqeengulf.deepfake.xai.ImageCamPostProcessor
import com.yaqeengulf.deepfake.xai.ImageXaiResult
import com.yaqeengulf.deepfake.xai.TextAttentionPostProcessor
import com.yaqeengulf.deepfake.xai.TextXaiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp

/**
 * High-level domain result wrapping inference verdict, probability score, and XAI artifacts.
 */
data class UnifiedScanResult(
    val id: Long = 0,
    val modality: String, // "IMAGE", "TEXT", "AUDIO"
    val verdict: String,  // "REAL", "SYNTHETIC", "SUSPICIOUS"
    val confidence: Float, // 0.0 - 1.0 (probability of the predicted class)
    val fakeProbability: Float, // 0.0 - 1.0 (probability of fake class)
    val rawScore: Float = fakeProbability, // 0.000 to 1.000 raw prediction score
    val modelName: String,
    val summary: String,
    val imageXai: ImageXaiResult? = null,
    val textXai: TextXaiResult? = null,
    val audioXai: AudioXaiResult? = null,
    val originalBitmap: Bitmap? = null,
    val originalText: String? = null,
    val c2paResult: ContentCredentialsResult = ContentCredentialsResult.NotPresent
)

/**
 * Single entry point orchestrating content capture data into:
 * 1. ModelManager inference (Phase 3)
 * 2. Softmax / Threshold decision logic
 * 3. XAI Post-Processing (Phase 4)
 * 4. AppDatabase scan history persistence (Phase 1 / Phase 6)
 */
class VerificationOrchestrator(
    private val context: Context,
    private val modelManager: ModelManager,
    private val database: AppDatabase
) {

    /**
     * Executes end-to-end verification for an Image bitmap.
     */
    suspend fun verifyImage(
        bitmap: Bitmap,
        imageModelType: ImageModelType = ImageModelType.EFFICIENTNET_B3,
        sourceTag: String? = "Manual Image Pick",
        c2paResult: ContentCredentialsResult = ContentCredentialsResult.NotPresent
    ): UnifiedScanResult = withContext(Dispatchers.Default) {
        val infResult = modelManager.runImageInference(bitmap, imageModelType)
        val probs = softmax(infResult.logits)
        // Image checkpoints (GenImage): classes = ['fake', 'real'] -> index 0 is fake, index 1 is real
        val isC2paAiGenerated = c2paResult is ContentCredentialsResult.Verified && c2paResult.isAiGenerated
        val rawFakeProb = probs[0]
        val rawRealProb = probs[1]

        val fakeProb = if (isC2paAiGenerated) 1.0f else rawFakeProb
        val realProb = if (isC2paAiGenerated) 0.0f else rawRealProb
        val isFake = isC2paAiGenerated || rawFakeProb >= 0.05f
        val isPotentiallyFake = !isC2paAiGenerated && (rawFakeProb > 0.00099f && rawFakeProb < 0.05f)
        val verdict = when {
            isC2paAiGenerated -> "AI GENERATED"
            rawFakeProb >= 0.05f -> "SYNTHETIC"
            rawFakeProb > 0.00099f -> "POTENTIALLY FAKE"
            else -> "REAL"
        }
        val confidence = if (isC2paAiGenerated) 1.0f else (if (isFake || isPotentiallyFake) rawFakeProb else rawRealProb)
        val modelName = when (imageModelType) {
            ImageModelType.EFFICIENTNET_B3 -> "EfficientNet-B3"
            ImageModelType.SWIN_T -> "Swin-T"
        }

        // CAM target class: 0 for fake, 1 for real
        val targetClass = if (isC2paAiGenerated || isFake || isPotentiallyFake) 0 else 1
        val imageXai = if (imageModelType == ImageModelType.EFFICIENTNET_B3) {
            val weights = FcWeightsLoader.loadEffnetWeights(context)
            ImageCamPostProcessor.processEffnetCam(
                featureMap = infResult.xaiData,
                weights = weights,
                targetClass = targetClass,
                createBitmap = true
            )
        } else {
            val weights = FcWeightsLoader.loadSwinWeights(context)
            ImageCamPostProcessor.processSwinCam(
                featureMap = infResult.xaiData,
                weights = weights,
                targetClass = targetClass,
                createBitmap = true
            )
        }

        val rawScoreFormatted = String.format(java.util.Locale.US, "%.5f", fakeProb)
        val fakePercentFormatted = String.format(java.util.Locale.US, "%.3f", fakeProb * 100f)
        val realPercentFormatted = String.format(java.util.Locale.US, "%.3f", realProb * 100f)
        val baseSummary = when {
            isFake -> "Synthetic generator footprints detected by $modelName (Score: $rawScoreFormatted, $fakePercentFormatted%)."
            isPotentiallyFake -> "Potentially synthetic generator artifacts detected by $modelName (Score: $rawScoreFormatted, $fakePercentFormatted%)."
            else -> "Verified authentic photo patterns by $modelName (Score: $rawScoreFormatted, $realPercentFormatted%)."
        }
        val summary = when {
            isC2paAiGenerated -> {
                val producer = (c2paResult as? ContentCredentialsResult.Verified)?.producer ?: "Generative AI"
                "C2PA Content Credentials cryptographically verified this image as AI-generated ($producer). Manifest confirms synthetic origin."
            }
            c2paResult is ContentCredentialsResult.Verified ->
                "$baseSummary • C2PA Manifest: Cryptographically verified origin (${c2paResult.producer ?: "Authoritative Tool"})."
            else -> baseSummary
        }

        val entity = ScanHistoryEntity(
            modality = "IMAGE",
            verdict = verdict,
            confidenceScore = confidence,
            modelUsed = if (isC2paAiGenerated) "C2PA Manifest (${(c2paResult as? ContentCredentialsResult.Verified)?.producer ?: "AI Generator"})" else modelName,
            timestamp = System.currentTimeMillis(),
            summary = summary,
            sourceIdentifier = sourceTag,
            sourceType = "on_device"
        )
        val insertedId = database.insertScan(entity)

        UnifiedScanResult(
            id = insertedId,
            modality = "IMAGE",
            verdict = verdict,
            confidence = confidence,
            fakeProbability = fakeProb,
            rawScore = fakeProb,
            modelName = if (isC2paAiGenerated) "C2PA Provenance Manifest" else modelName,
            summary = summary,
            imageXai = imageXai,
            originalBitmap = bitmap,
            c2paResult = c2paResult
        )
    }

    /**
     * Executes sequential dual-model verification comparing EfficientNet-B3
     * and Swin-T on the exact same cropped bitmap, generating both Grad-CAM heatmaps
     * and cross-model agreement metrics per Feature A.
     */
    suspend fun verifyImageDual(
        croppedBitmap: Bitmap,
        cropBounds: Rect = Rect(0, 0, croppedBitmap.width, croppedBitmap.height),
        resolutionTier: CropResolutionTier = CropResolutionTier.TIER_1_ACCESSIBILITY_NODE,
        sourceTag: String = "Overlay Live-Scan"
    ): DualImageScanResult = withContext(Dispatchers.Default) {
        // --- Model 1: EfficientNet-B3 (CNN Conv-Grid Grad-CAM) ---
        val effnetInf = modelManager.runImageInference(croppedBitmap, ImageModelType.EFFICIENTNET_B3)
        val effnetProbs = softmax(effnetInf.logits)
        val effnetFakeProb = effnetProbs[0]
        val effnetRealProb = effnetProbs[1]
        val effnetVerdict = when {
            effnetFakeProb >= 0.05f -> "SYNTHETIC"
            effnetFakeProb > 0.00099f -> "POTENTIALLY FAKE"
            else -> "REAL"
        }
        val effnetConfidence = if (effnetFakeProb >= 0.00099f) effnetFakeProb else effnetRealProb
        val effnetTargetClass = if (effnetFakeProb >= 0.00099f) 0 else 1
        val effnetWeights = FcWeightsLoader.loadEffnetWeights(context)
        val effnetCam = ImageCamPostProcessor.processEffnetCam(
            featureMap = effnetInf.xaiData,
            weights = effnetWeights,
            targetClass = effnetTargetClass,
            createBitmap = true
        )
        val effnetComposite = compositeHeatmap(croppedBitmap, effnetCam.heatmapBitmap)

        // --- Model 2: Swin-T (Transformer Patch-Grid CAM) ---
        val swinInf = modelManager.runImageInference(croppedBitmap, ImageModelType.SWIN_T)
        val swinProbs = softmax(swinInf.logits)
        val swinFakeProb = swinProbs[0]
        val swinRealProb = swinProbs[1]
        val swinVerdict = when {
            swinFakeProb >= 0.05f -> "SYNTHETIC"
            swinFakeProb > 0.00099f -> "POTENTIALLY FAKE"
            else -> "REAL"
        }
        val swinConfidence = if (swinFakeProb >= 0.00099f) swinFakeProb else swinRealProb
        val swinTargetClass = if (swinFakeProb >= 0.00099f) 0 else 1
        val swinWeights = FcWeightsLoader.loadSwinWeights(context)
        val swinCam = ImageCamPostProcessor.processSwinCam(
            featureMap = swinInf.xaiData,
            weights = swinWeights,
            targetClass = swinTargetClass,
            createBitmap = true
        )
        val swinComposite = compositeHeatmap(croppedBitmap, swinCam.heatmapBitmap)

        // --- Cross-Model Agreement ---
        val effnetIsSynthetic = effnetVerdict == "SYNTHETIC"
        val swinIsSynthetic = swinVerdict == "SYNTHETIC"
        val effnetIsAuthentic = effnetVerdict == "REAL"
        val swinIsAuthentic = swinVerdict == "REAL"

        val agree = (effnetIsSynthetic && swinIsSynthetic) ||
                (effnetIsAuthentic && swinIsAuthentic) ||
                (effnetVerdict == swinVerdict)

        val agreementSummary = when {
            effnetIsSynthetic && swinIsSynthetic -> "Both models agree: Synthetic / AI-Generated"
            effnetIsAuthentic && swinIsAuthentic -> "Both models agree: Authentic Photo"
            agree -> "Both models agree: $effnetVerdict"
            else -> "Models Disagree — EffNet-B3: $effnetVerdict vs Swin-T: $swinVerdict"
        }

        // Record scan into history
        val historyEntity = ScanHistoryEntity(
            modality = "IMAGE_DUAL",
            verdict = if (agree) effnetVerdict else "DISPUTED",
            confidenceScore = maxOf(effnetConfidence, swinConfidence),
            modelUsed = "Dual: EffNet-B3 + Swin-T",
            timestamp = System.currentTimeMillis(),
            summary = "$agreementSummary [${resolutionTier.displayName}]",
            sourceIdentifier = sourceTag,
            sourceType = "overlay"
        )
        database.insertScan(historyEntity)

        DualImageScanResult(
            croppedBitmap = croppedBitmap,
            cropBounds = cropBounds,
            resolutionTier = resolutionTier,
            effnetVerdict = effnetVerdict,
            effnetFakeProbability = effnetFakeProb,
            effnetConfidence = effnetConfidence,
            effnetCam = effnetCam,
            effnetComposite = effnetComposite,
            swinVerdict = swinVerdict,
            swinFakeProbability = swinFakeProb,
            swinConfidence = swinConfidence,
            swinCam = swinCam,
            swinComposite = swinComposite,
            modelsAgree = agree,
            agreementSummary = agreementSummary
        )
    }

    private fun compositeHeatmap(baseBitmap: Bitmap, heatmapBitmap: Bitmap?): Bitmap {
        val result = Bitmap.createBitmap(baseBitmap.width, baseBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(baseBitmap, 0f, 0f, null)
        if (heatmapBitmap != null) {
            val scaledHeatmap = Bitmap.createScaledBitmap(heatmapBitmap, baseBitmap.width, baseBitmap.height, true)
            canvas.drawBitmap(scaledHeatmap, 0f, 0f, null)
            scaledHeatmap.recycle()
        }
        return result
    }

    /**
     * Executes end-to-end verification for Text (Arabic or English).
     */
    suspend fun verifyText(
        text: String,
        isArabic: Boolean? = null,
        sourceTag: String? = "Manual Text Input"
    ): UnifiedScanResult = withContext(Dispatchers.Default) {
        val useArabic = isArabic ?: LanguageRouter.isArabic(text)
        val infResult = modelManager.runTextInference(text, useArabic)
        val probs = softmax(infResult.logits)
        // Text models (AFND / ISOT): index 0 = real, index 1 = fake
        val fakeProb = probs[1]
        val realProb = probs[0]
        val isFake = fakeProb >= 0.50f
        val verdict = when {
            fakeProb >= 0.70f -> "SYNTHETIC"
            fakeProb >= 0.40f -> "SUSPICIOUS"
            else -> "REAL"
        }
        val confidence = if (isFake) fakeProb else realProb
        val modelName = if (useArabic) "AraBERT" else "BERT-base"

        val tokenizer = modelManager.getTokenizer(useArabic)
        val tokenized = tokenizer.tokenize(text, maxSeqLen = 64)
        val textXai = TextAttentionPostProcessor.processAttention(
            attentionData = infResult.xaiData,
            numHeads = 12,
            seqLen = tokenized.inputIds.size,
            tokens = tokenized.tokens,
            originalText = text
        )

        val summary = if (isFake) {
            "Synthetic LLM markers identified by $modelName (${(fakeProb * 100).toInt()}% confidence)."
        } else {
            "Natural linguistic coherence confirmed (${(probs[0] * 100).toInt()}% confidence)."
        }

        val entity = ScanHistoryEntity(
            modality = "TEXT",
            verdict = verdict,
            confidenceScore = confidence,
            modelUsed = modelName,
            timestamp = System.currentTimeMillis(),
            summary = summary,
            sourceIdentifier = sourceTag ?: text.take(30),
            sourceType = "on_device"
        )
        val insertedId = database.insertScan(entity)

        UnifiedScanResult(
            id = insertedId,
            modality = "TEXT",
            verdict = verdict,
            confidence = confidence,
            fakeProbability = fakeProb,
            modelName = modelName,
            summary = summary,
            textXai = textXai,
            originalText = text
        )
    }

    /**
     * Executes end-to-end verification for Audio (16kHz mono PCM FloatArray).
     */
    suspend fun verifyAudio(
        samples: FloatArray,
        isArabic: Boolean = true,
        sourceTag: String? = "Microphone / Audio Pick"
    ): UnifiedScanResult = withContext(Dispatchers.Default) {
        val infResult = modelManager.runAudioInference(samples, isArabic)
        val probs = softmax(infResult.logits)
        val fakeProb = probs[1]
        val isFake = fakeProb >= 0.50f
        val verdict = when {
            fakeProb >= 0.70f -> "SYNTHETIC"
            fakeProb >= 0.40f -> "SUSPICIOUS"
            else -> "REAL"
        }
        val confidence = if (isFake) fakeProb else probs[0]
        val modelName = "SpectrogramCNN (ArEnAV)"

        val weights = FcWeightsLoader.loadAudioWeights(context)
        val audioXai = AudioCamPostProcessor.processAudioCam(
            featureMap = infResult.xaiData,
            weights = weights,
            targetClass = if (isFake) 1 else 0,
            createBitmap = true
        )

        val rawScoreFormatted = String.format(java.util.Locale.US, "%.3f", fakeProb)
        val summary = if (isFake) {
            "Synthetic voice artifacts or vocoder footprints detected (Score: $rawScoreFormatted, ${(fakeProb * 100).toInt()}%)."
        } else {
            "Natural acoustic characteristics verified (Score: $rawScoreFormatted, ${(probs[0] * 100).toInt()}%)."
        }

        val entity = ScanHistoryEntity(
            modality = "AUDIO",
            verdict = verdict,
            confidenceScore = confidence,
            modelUsed = modelName,
            timestamp = System.currentTimeMillis(),
            summary = summary,
            sourceIdentifier = sourceTag,
            sourceType = "on_device"
        )
        val insertedId = database.insertScan(entity)

        UnifiedScanResult(
            id = insertedId,
            modality = "AUDIO",
            verdict = verdict,
            confidence = confidence,
            fakeProbability = fakeProb,
            rawScore = fakeProb,
            modelName = modelName,
            summary = summary,
            audioXai = audioXai
        )
    }

    private fun softmax(logits: FloatArray): FloatArray {
        var maxLogit = Float.NEGATIVE_INFINITY
        for (v in logits) {
            if (v > maxLogit) maxLogit = v
        }
        var sum = 0.0f
        val exps = FloatArray(logits.size)
        for (i in logits.indices) {
            val e = exp(logits[i] - maxLogit)
            exps[i] = e
            sum += e
        }
        val probs = FloatArray(logits.size)
        for (i in logits.indices) {
            probs[i] = exps[i] / sum
        }
        return probs
    }
}
