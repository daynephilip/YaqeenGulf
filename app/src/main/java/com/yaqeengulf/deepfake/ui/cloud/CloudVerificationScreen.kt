package com.yaqeengulf.deepfake.ui.cloud

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaqeengulf.deepfake.ui.dashboard.DashboardViewModel
import com.yaqeengulf.deepfake.ui.theme.CardLabelDark
import com.yaqeengulf.deepfake.ui.theme.ElectricBlueCard
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkSubtext
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkText
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface

@Composable
fun CloudVerificationScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isScanning by viewModel.isScanning.collectAsState()
    val cloudResult by viewModel.cloudScanResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var headlineInput by remember { mutableStateOf("") }

    LaunchedEffect(cloudResult) {
        cloudResult?.let { res ->
            if (urlInput.isBlank() && res.url.isNotBlank()) {
                urlInput = res.url
            }
            if (headlineInput.isBlank() && !res.headline.isNullOrBlank()) {
                headlineInput = res.headline
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MintTealHeader)
    ) {
        // --- Top Header Section (Matches Mockup Mint Teal Aesthetic) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HeaderDarkText
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Verify News",
                            tint = HeaderDarkText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verify News",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HeaderDarkText
                    )
                }

                // Placeholder for symmetry
                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Verify News & Links",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = HeaderDarkText
            )
            Text(
                text = "فحص الروابط ومصداقية المصادر الصحفية",
                style = MaterialTheme.typography.bodySmall,
                color = HeaderDarkSubtext
            )
        }

        // --- Main Content Container (Rounded Top Mint Surface) ---
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            color = MintTealSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Enter News Article URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CardLabelDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paste a URL from any news publication, website, or social platform to analyze publisher credibility and fact-checking records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // URL Input Field
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("https://example.com/article...", color = Color.LightGray) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = ElectricBlueCard
                                )
                            },
                            trailingIcon = {
                                if (urlInput.isNotEmpty()) {
                                    IconButton(onClick = { urlInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = Color.Gray
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = clipboard?.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                            val extracted = DashboardViewModel.extractUrl(pasted) ?: pasted
                                            urlInput = extracted
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste",
                                            tint = ElectricBlueCard
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlueCard,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = CardLabelDark,
                                unfocusedTextColor = CardLabelDark
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Optional Headline / Claim Field
                        OutlinedTextField(
                            value = headlineInput,
                            onValueChange = { headlineInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Article Headline / Claim (Optional)", color = Color.LightGray) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlueCard,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = CardLabelDark,
                                unfocusedTextColor = CardLabelDark
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                if (urlInput.isNotBlank()) {
                                    viewModel.checkCloudUrl(urlInput, headlineInput)
                                } else {
                                    Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isScanning && urlInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlueCard,
                                disabledContainerColor = ElectricBlueCard.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying Source...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verify Source & Trustworthiness",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Error Display
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "Verification error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Findings / Results Card
                if (cloudResult != null) {
                    val result = cloudResult!!
                    val isTrusted = result.confidence >= 0.80f
                    val isUncertain = result.confidence >= 0.60f && result.confidence < 0.80f

                    val bannerColor = when {
                        isTrusted -> Color(0xFF10B981)
                        isUncertain -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    val bannerBg = when {
                        isTrusted -> Color(0xFFECFDF5)
                        isUncertain -> Color(0xFFFFFBEB)
                        else -> Color(0xFFFEF2F2)
                    }
                    val bannerIcon = when {
                        isTrusted -> Icons.Default.CheckCircle
                        isUncertain -> Icons.Default.Warning
                        else -> Icons.Default.Cancel
                    }
                    val bannerTitle = when {
                        isTrusted -> "Trusted"
                        isUncertain -> "Uncertain"
                        else -> "Not Trusted"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Trustworthiness Verdict Banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = bannerBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = bannerIcon,
                                        contentDescription = bannerTitle,
                                        tint = bannerColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bannerTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = bannerColor
                                        )
                                        val evaluatorDisplay = when {
                                            result.providerUsed.contains("qwen", ignoreCase = true) -> "Groq Qwen 3.8 (27B)"
                                            result.providerUsed.contains("gemini", ignoreCase = true) -> "Google Gemini"
                                            result.providerUsed.contains("gpt-oss", ignoreCase = true) || result.providerUsed.contains("gptoss", ignoreCase = true) -> "GPT-OSS (120B)"
                                            else -> "GPT-OSS (120B)"
                                        }
                                        Text(
                                            text = "Confidence: ${(result.confidence * 100).toInt()}% • Evaluated by $evaluatorDisplay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bannerColor.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Article Headline & Author
                            if (!result.headline.isNullOrBlank()) {
                                Text(
                                    text = "Article Headline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = result.headline ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CardLabelDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (!result.author.isNullOrBlank() || !result.publisher.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (!result.publisher.isNullOrBlank()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Publisher / Source",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = result.publisher ?: result.url,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CardLabelDark
                                            )
                                        }
                                    }
                                    if (!result.author.isNullOrBlank()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Author / Byline",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = result.author ?: "Unspecified",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = CardLabelDark
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Analysis Findings
                            Text(
                                text = "Verification Findings & Reasoning",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Text(
                                    text = result.reasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    viewModel.clearCloudResult()
                                    urlInput = ""
                                    headlineInput = ""
                                }) {
                                    Text("Verify Another Link", color = ElectricBlueCard)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
            }
        }
    }
}
