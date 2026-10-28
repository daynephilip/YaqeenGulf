package com.yaqeengulf.deepfake.ui.result

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaqeengulf.deepfake.domain.model.ContentCredentialsResult
import com.yaqeengulf.deepfake.domain.orchestrator.UnifiedScanResult
import com.yaqeengulf.deepfake.ui.theme.CardLabelDark
import com.yaqeengulf.deepfake.ui.theme.EmeraldLight
import com.yaqeengulf.deepfake.ui.theme.EmeraldPrimary
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface
import com.yaqeengulf.deepfake.ui.theme.WarningAmber
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultBottomSheet(
    result: UnifiedScanResult,
    onDismiss: () -> Unit,
    onSaveConfirmed: () -> Unit = {}
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isC2paAiGenerated = (result.c2paResult is ContentCredentialsResult.Verified && result.c2paResult.isAiGenerated) ||
            result.verdict.equals("AI GENERATED", ignoreCase = true)

    val isFake = isC2paAiGenerated || result.verdict == "SYNTHETIC" || result.verdict == "AI GENERATED" || result.verdict == "FAKE"
    val isPotentiallyFake = !isC2paAiGenerated && (result.verdict == "POTENTIALLY FAKE" || result.verdict == "SUSPICIOUS")

    val verdictColor = when {
        isFake -> Color(0xFFEF4444)
        isPotentiallyFake -> WarningAmber
        else -> Color(0xFF10B981)
    }

    val verdictLabel = when {
        isC2paAiGenerated || result.verdict.equals("AI GENERATED", ignoreCase = true) -> "❌ AI GENERATED"
        isFake -> "❌ SYNTHETIC / DEEPFAKE"
        isPotentiallyFake -> "⚠️ POTENTIALLY FAKE"
        else -> "✅ AUTHENTIC / REAL"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MintTealSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Modality & Dismiss
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "ON-DEVICE VERIFICATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result.modality,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Verdict Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, verdictColor.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = verdictLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = verdictColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CardLabelDark,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isC2paAiGenerated) "Detection Source" else "Model Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isC2paAiGenerated) "FAKE" else String.format(java.util.Locale.US, "%.5f", result.rawScore),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                            Text(
                                text = if (isC2paAiGenerated) "C2PA Manifest" else "range 0.0 - 1.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Confidence",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isC2paAiGenerated) "100%" else if (result.confidence < 0.01f) {
                                    String.format(java.util.Locale.US, "%.3f%%", result.confidence * 100f)
                                } else {
                                    "${(result.confidence * 100).toInt()}%"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                            Text(
                                text = if (isC2paAiGenerated) "AI Generated" else if (result.fakeProbability >= 0.5f) "Fake class" else "Real class",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 9.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Synthetic Prob",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isC2paAiGenerated) "100%" else if (result.fakeProbability < 0.05f) {
                                    String.format(java.util.Locale.US, "%.3f%%", result.fakeProbability * 100f)
                                } else {
                                    "${(result.fakeProbability * 100).toInt()}%"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isC2paAiGenerated || result.fakeProbability >= 0.05f) Color(0xFFEF4444) else if (result.fakeProbability >= 0.01f) Color(0xFFF59E0B) else EmeraldLight
                            )
                            Text(
                                text = if (isC2paAiGenerated || result.fakeProbability >= 0.05f) "High risk" else if (result.fakeProbability >= 0.01f) "Caution" else "Low risk",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isC2paAiGenerated || result.fakeProbability >= 0.05f) Color(0xFFEF4444) else if (result.fakeProbability >= 0.01f) Color(0xFFF59E0B) else EmeraldLight,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { if (isC2paAiGenerated) 1.0f else result.fakeProbability },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isC2paAiGenerated || result.fakeProbability >= 0.5f) Color(0xFFEF4444) else EmeraldLight,
                        trackColor = Color(0xFFF1F5F9)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isC2paAiGenerated) "Origin: Cryptographic Manifest" else "Model: ${result.modelName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = if (isC2paAiGenerated) "AI Manifest Verified" else "Local Hardware (Private)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isC2paAiGenerated) Color(0xFFEF4444) else EmeraldLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modality-Specific Evidence View
            when (result.modality) {
                "IMAGE" -> {
                    ContentCredentialsCard(result.c2paResult)
                    Spacer(modifier = Modifier.height(16.dp))
                    ImageEvidenceSection(result)
                }
                "TEXT" -> TextEvidenceSection(result)
                "AUDIO" -> AudioEvidenceSection(result)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Disclosures & Model Limitations (Build Plan §11)
            LimitationsSection(result)

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        shareVerificationReport(context, result)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CardLabelDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Share Report", color = CardLabelDark, style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Scan record saved in local database", Toast.LENGTH_SHORT).show()
                        onSaveConfirmed()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Saved", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ImageEvidenceSection(result: UnifiedScanResult) {
    val imageXai = result.imageXai
    var overlayAlpha by remember { mutableFloatStateOf(0.65f) }
    var viewMode by remember { mutableStateOf("Overlay") } // "Overlay" vs "SideBySide"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "XAI Class Activation Map (CAM)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CardLabelDark
                )

                val isC2paAi = (result.c2paResult is ContentCredentialsResult.Verified && result.c2paResult.isAiGenerated) ||
                        result.verdict.equals("AI GENERATED", ignoreCase = true)
                Surface(
                    color = if (isC2paAi) Color(0xFFEF4444).copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isC2paAi) Color(0xFFEF4444).copy(alpha = 0.4f) else EmeraldLight.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isC2paAi) "Verdict: FAKE (C2PA)" else "Score: ${String.format(java.util.Locale.US, "%.3f", result.rawScore)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isC2paAi) Color(0xFFEF4444) else EmeraldLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Spatial visualization of features that influenced the model. Warm regions (red/yellow) indicate generator artifacts or synthesis boundaries.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (imageXai?.heatmapBitmap != null) {
                val camBitmap = imageXai.heatmapBitmap
                val original = result.originalBitmap

                if (viewMode == "Overlay" && original != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = original.asImageBitmap(),
                            contentDescription = "Original Scanned Image",
                            modifier = Modifier.matchParentSize()
                        )
                        Image(
                            bitmap = camBitmap.asImageBitmap(),
                            contentDescription = "CAM Heatmap Overlay",
                            alpha = overlayAlpha,
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Heatmap Opacity: ${(overlayAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.width(130.dp)
                        )
                        Slider(
                            value = overlayAlpha,
                            onValueChange = { overlayAlpha = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldLight,
                                activeTrackColor = EmeraldPrimary,
                                inactiveTrackColor = Color(0xFFF1F5F9)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Side by side or pure CAM view
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (original != null) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Original Image",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Image(
                                    bitmap = original.asImageBitmap(),
                                    contentDescription = "Original",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Attribution Heatmap",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldLight,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Image(
                                bitmap = camBitmap.asImageBitmap(),
                                contentDescription = "CAM Heatmap",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            viewMode = if (viewMode == "Overlay") "SideBySide" else "Overlay"
                        },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (viewMode == "Overlay") "Switch to Side-by-Side" else "Switch to Overlay",
                            style = MaterialTheme.typography.labelSmall,
                            color = CardLabelDark
                        )
                    }
                }
            } else {
                Text(
                    text = "CAM attribution computed (${imageXai?.width ?: 224}x${imageXai?.height ?: 224} feature activation grid).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun TextEvidenceSection(result: UnifiedScanResult) {
    val textXai = result.textXai

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Token Attention Attribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CardLabelDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Highlighted words represent key tokens whose cross-attention weights strongly directed the language model's verdict.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (textXai != null && textXai.wordHighlights.isNotEmpty()) {
                val annotatedString = buildAnnotatedString {
                    textXai.wordHighlights.forEach { wordSpan ->
                        val weight = wordSpan.score
                        val isHigh = weight >= 0.5f
                        val bg = if (isHigh) {
                            Color(0xFFEF4444).copy(alpha = min(0.65f, max(0.2f, weight * 2.5f)))
                        } else {
                            WarningAmber.copy(alpha = min(0.45f, weight * 1.8f))
                        }

                        withStyle(
                            style = SpanStyle(
                                background = bg,
                                color = CardLabelDark,
                                fontWeight = if (isHigh) FontWeight.Bold else FontWeight.Normal
                            )
                        ) {
                            append(wordSpan.word)
                        }
                        append(" ")
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = Color(0xFFF1F5F9).copy(alpha = 0.6f)
                ) {
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Top influential tokens: ${textXai.rawTokenHighlights.take(5).map { it.first }.joinToString(", ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            } else if (!result.originalText.isNullOrBlank()) {
                Text(
                    text = result.originalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardLabelDark
                )
            }
        }
    }
}

@Composable
private fun AudioEvidenceSection(result: UnifiedScanResult) {
    val audioXai = result.audioXai

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Spectrogram Attribution & Temporal Curve",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CardLabelDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Frequency bins across time. Peaks indicate acoustic vocoder signatures or unnatural pitch inflection.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (audioXai?.spectrogramHeatmapBitmap != null) {
                Column {
                    Text(
                        text = "Mel-Spectrogram CAM Heatmap (0 - 8 kHz, 4.0s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Image(
                        bitmap = audioXai.spectrogramHeatmapBitmap.asImageBitmap(),
                        contentDescription = "Audio CAM Spectrogram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    )
                }
            }

            if (audioXai?.timeCurve != null && audioXai.timeCurve.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Temporal Synthesis Activation Profile",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Render temporal activation line curve
                val curve: FloatArray = audioXai.timeCurve
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF1F5F9).copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val maxVal = curve.maxOrNull()?.coerceAtLeast(0.001f) ?: 1.0f
                    val stepX = size.width / (curve.size - 1).coerceAtLeast(1)

                    val path = Path()
                    curve.forEachIndexed { index: Int, value: Float ->
                        val x = index * stepX
                        val normalizedY = (1f - (value / maxVal)) * size.height
                        if (index == 0) {
                            path.moveTo(x, normalizedY)
                        } else {
                            path.lineTo(x, normalizedY)
                        }
                    }

                    drawPath(
                        path = path,
                        color = EmeraldLight,
                        style = Stroke(width = 3f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.0s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text("2.0s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                    Text("4.0s", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun LimitationsSection(result: UnifiedScanResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. Accurate Intermediate Confidence Advisory (Correction 3)
        if (result.modality == "IMAGE" && (result.confidence in 0.70f..0.85f || result.verdict == "SUSPICIOUS")) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WarningAmber.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Confidence Advisory: This result falls within an intermediate confidence band (70–85%). Certain image generation architectures (particularly diffusion-based synthesis) exhibit lower detection separation than earlier GAN models. Ambiguous texture patterns or compression artifacts may affect classification certainty.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardLabelDark,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 2. English Text (ISOT) Wire-Service Note
        if (result.modality == "TEXT" && result.modelName.contains("BERT-base", ignoreCase = true)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Model Performance Note: English text detection is trained on ISOT wire-service and web articles. It measures stylistic formatting and token distribution patterns rather than ground-truth factual accuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 3. Audio Spoof Scope Note (ArEnAV)
        if (result.modality == "AUDIO") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Scope Note: Validated against ArEnAV bilingual speech synthesis and voice cloning benchmarks. Unseen zero-shot neural synthesis architectures may yield reduced sensitivity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentCredentialsCard(c2paResult: ContentCredentialsResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = when (c2paResult) {
                            is ContentCredentialsResult.Verified ->
                                if (c2paResult.isAiGenerated) Color(0xFFEF4444) else EmeraldPrimary
                            is ContentCredentialsResult.TamperedOrUntrusted ->
                                Color(0xFFEF4444)
                            is ContentCredentialsResult.NotPresent ->
                                Color(0xFF64748B)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "C2PA Provenance Manifest",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CardLabelDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (c2paResult) {
                        is ContentCredentialsResult.Verified ->
                            if (c2paResult.isAiGenerated) Color(0xFFEF4444).copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                        is ContentCredentialsResult.TamperedOrUntrusted ->
                            Color(0xFFEF4444).copy(alpha = 0.15f)
                        is ContentCredentialsResult.NotPresent ->
                            Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = when (c2paResult) {
                            is ContentCredentialsResult.Verified ->
                                if (c2paResult.isAiGenerated) "AI GENERATED" else "AUTHENTIC"
                            is ContentCredentialsResult.TamperedOrUntrusted ->
                                "TAMPERED / UNVERIFIED"
                            is ContentCredentialsResult.NotPresent ->
                                "NO MANIFEST"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = when (c2paResult) {
                            is ContentCredentialsResult.Verified ->
                                if (c2paResult.isAiGenerated) Color(0xFFEF4444) else EmeraldLight
                            is ContentCredentialsResult.TamperedOrUntrusted ->
                                Color(0xFFEF4444)
                            is ContentCredentialsResult.NotPresent ->
                                Color(0xFF64748B)
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (c2paResult) {
                is ContentCredentialsResult.Verified -> {
                    Text(
                        text = if (c2paResult.isAiGenerated)
                            "Cryptographically signed C2PA manifest contains digital source assertion: trainedAlgorithmicMedia (AI-generated)."
                        else
                            "Cryptographically signed C2PA manifest confirms camera capture / authentic software workflow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardLabelDark,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        c2paResult.producer?.let {
                            Text(
                                text = "Producer / Tool: $it",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CardLabelDark
                            )
                        }
                        Text(
                            text = "Actions: ${c2paResult.actions.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Format: ${c2paResult.format}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                is ContentCredentialsResult.TamperedOrUntrusted -> {
                    Text(
                        text = "Manifest Warning: ${c2paResult.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444),
                        lineHeight = 16.sp
                    )
                }
                is ContentCredentialsResult.NotPresent -> {
                    Text(
                        text = "No embedded C2PA/JUMBF provenance manifest found. This is standard for camera snapshots, social media downloads, and messaging apps which strip file metadata. Primary verification relies on on-device neural deepfake detection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

private fun shareVerificationReport(context: Context, result: UnifiedScanResult) {
    val report = buildString {
        appendLine("--- Yaqeen Gulf Verification Report ---")
        appendLine("Modality: ${result.modality}")
        appendLine("Verdict: ${result.verdict}")
        appendLine("Raw Score: ${String.format(java.util.Locale.US, "%.5f", result.rawScore)}")
        appendLine("Confidence: ${(result.confidence * 100).toInt()}%")
        appendLine("Synthetic Probability: ${String.format(java.util.Locale.US, "%.5f (%.3f%%)", result.fakeProbability, result.fakeProbability * 100f)}")
        appendLine("Model Used: ${result.modelName} (On-Device)")
        if (result.modality == "IMAGE") {
            when (val c2pa = result.c2paResult) {
                is ContentCredentialsResult.Verified -> {
                    appendLine("C2PA Provenance: Verified (${c2pa.producer ?: "Authoritative Tool"})")
                    if (c2pa.isAiGenerated) appendLine("C2PA Assertion: AI Generated (trainedAlgorithmicMedia)")
                }
                is ContentCredentialsResult.TamperedOrUntrusted -> {
                    appendLine("C2PA Provenance: Tampered / Untrusted (${c2pa.reason})")
                }
                is ContentCredentialsResult.NotPresent -> {
                    appendLine("C2PA Provenance: Not Present (Unasserted / standard upload)")
                }
            }
        }
        appendLine("Summary: ${result.summary}")
        appendLine()
        appendLine("Verification ran 100% locally on-device. No data was transmitted to external servers.")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Yaqeen Gulf Verification: ${result.verdict}")
        putExtra(Intent.EXTRA_TEXT, report)
    }

    context.startActivity(Intent.createChooser(intent, "Share Verification Report"))
}
