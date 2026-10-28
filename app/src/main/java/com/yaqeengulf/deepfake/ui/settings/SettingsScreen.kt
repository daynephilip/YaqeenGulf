package com.yaqeengulf.deepfake.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import com.yaqeengulf.deepfake.ui.theme.CardBorder
import com.yaqeengulf.deepfake.ui.theme.DarkNavyBackground
import com.yaqeengulf.deepfake.ui.theme.DarkNavySurface
import com.yaqeengulf.deepfake.ui.theme.DarkNavySurfaceVariant
import com.yaqeengulf.deepfake.ui.theme.EmeraldLight
import com.yaqeengulf.deepfake.ui.theme.EmeraldPrimary
import com.yaqeengulf.deepfake.ui.theme.TextPrimaryDark
import com.yaqeengulf.deepfake.ui.theme.TextSecondaryDark
import com.yaqeengulf.deepfake.ui.theme.TrustBlue
import com.yaqeengulf.deepfake.ui.theme.WarningAmber

import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.statusBarsPadding
import com.yaqeengulf.deepfake.ui.theme.CardLabelDark
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkSubtext
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkText
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedImageModel by viewModel.selectedImageModel.collectAsState()
    val selectedAudioModel by viewModel.selectedAudioModel.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val isCloudCheckEnabled by viewModel.isCloudCheckEnabled.collectAsState()
    val cloudProvider by viewModel.cloudProvider.collectAsState()

    // Refresh overlay permission state when resuming the screen (e.g. returning from system settings)
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.updatePermissionStatus(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintTealHeader)
    ) {
        // --- Top Header Section (Mint Teal Theme) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HeaderDarkText
                    )
                    Text(
                        text = "إعدادات النماذج والخصوصية",
                        style = MaterialTheme.typography.labelSmall,
                        color = HeaderDarkSubtext
                    )
                }

                Spacer(modifier = Modifier.size(40.dp))
            }
        }

        // --- Main Content Container (Rounded Top Mint Surface) ---
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
            color = MintTealSurface
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Floating Overlay Settings Section (Build plan §6)
            item {
                Text(
                    text = "Floating Shield Overlay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Overlay",
                                        tint = if (hasOverlayPermission && isOverlayEnabled) EmeraldLight else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Enable Floating Shield",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CardLabelDark
                                    )
                                    Text(
                                        text = if (hasOverlayPermission) "Permission Granted" else "Permission Required",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasOverlayPermission) EmeraldLight else WarningAmber
                                    )
                                }
                            }

                            Switch(
                                checked = isOverlayEnabled && hasOverlayPermission,
                                onCheckedChange = { checked ->
                                    if (hasOverlayPermission) {
                                        viewModel.toggleOverlay(context, checked)
                                    } else {
                                        // Trigger system settings navigation
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFFF1F5F9)
                                )
                            )
                        }

                        // Required message per Build Plan §6 when overlay permission is not granted
                        if (!hasOverlayPermission) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = WarningAmber.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = WarningAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Overlay permission not granted — you can still scan content directly from the app. Enable overlay in system settings to scan while browsing other apps.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CardLabelDark,
                                            lineHeight = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                                    ) {
                                        Text(
                                            text = "Open Overlay Settings",
                                            color = Color.Black,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Image Detection Model Selector (Build plan §3 & §10)
            item {
                Text(
                    text = "Image Detection Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Neural Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CardLabelDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose between the default lightweight CNN or the hierarchical Vision Transformer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Model Option 1: EfficientNet-B3 (Default)
                        ModelOptionRow(
                            model = ImageModelType.EFFICIENTNET_B3,
                            isSelected = selectedImageModel == ImageModelType.EFFICIENTNET_B3,
                            onSelect = { viewModel.selectImageModel(ImageModelType.EFFICIENTNET_B3) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Option 2: Swin-T (Alternative)
                        ModelOptionRow(
                            model = ImageModelType.SWIN_T,
                            isSelected = selectedImageModel == ImageModelType.SWIN_T,
                            onSelect = { viewModel.selectImageModel(ImageModelType.SWIN_T) }
                        )
                    }
                }
            }

            // Audio Spoof Detection Model Selector
            item {
                Text(
                    text = "Audio Spoof Detection Model",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Acoustic Verification Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CardLabelDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unified neural acoustic model evaluating voice scans for both English and Arabic audio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "ArEnAV SpectrogramCNN",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CardLabelDark
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = EmeraldPrimary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Unified Active",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Trained on ArEnAV bilingual deepfake speech synthesis and voice cloning benchmarks.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Source-Check Section (Build plan Phase 6)
            item {
                Text(
                    text = "Cloud Source-Check (Opt-In)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cloud,
                                        contentDescription = "Cloud Source-Check",
                                        tint = if (isCloudCheckEnabled) EmeraldLight else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Enable Cloud Source-Check",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CardLabelDark
                                    )
                                    Text(
                                        text = if (isCloudCheckEnabled) "Active" else "Disabled (Default)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isCloudCheckEnabled) EmeraldLight else Color(0xFF64748B)
                                    )
                                }
                            }
                            Switch(
                                checked = isCloudCheckEnabled,
                                onCheckedChange = { viewModel.toggleCloudCheck(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = Color(0xFF64748B),
                                    uncheckedTrackColor = Color(0xFFF1F5F9)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enabling this sends shared article URLs and their text to Google (Gemini) or Groq's servers over the internet to evaluate source reliability. All on-device scans (images, audio, pasted text) remain 100% offline and unaffected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )

                        if (isCloudCheckEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select Cloud LLM Provider",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = CardLabelDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Provider Option 1: GPT-OSS (Recommended)
                            val isGptOss = cloudProvider == "groq" || cloudProvider == "gptoss" || cloudProvider == "compound"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCloudProvider("gptoss") },
                                color = if (isGptOss) Color(0xFFF1F5F9) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGptOss) EmeraldLight else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isGptOss,
                                        onClick = { viewModel.selectCloudProvider("gptoss") },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = EmeraldPrimary,
                                            unselectedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "GPT-OSS (120B) ★ Recommended",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CardLabelDark
                                        )
                                        Text(
                                            text = "OpenAI GPT-OSS 120B on Groq — High capacity, multi-lingual Arabic/English news and source evaluation (8k TPM, 30 RPM).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Provider Option 2: Groq Qwen 3.8
                            val isGroqQwen = cloudProvider == "groq_qwen" || cloudProvider.contains("qwen")
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCloudProvider("groq_qwen") },
                                color = if (isGroqQwen) Color(0xFFF1F5F9) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGroqQwen) EmeraldLight else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isGroqQwen,
                                        onClick = { viewModel.selectCloudProvider("groq_qwen") },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = EmeraldPrimary,
                                            unselectedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Groq Qwen 3.8 (27B)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CardLabelDark
                                        )
                                        Text(
                                            text = "Qwen 3.8 27B model on Groq — Deep reasoning and multi-lingual Arabic/English source verification.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Provider Option 3: Gemini
                            val isGemini = cloudProvider == "gemini"
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectCloudProvider("gemini") },
                                color = if (isGemini) Color(0xFFF1F5F9) else Color.White,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGemini) EmeraldLight else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isGemini,
                                        onClick = { viewModel.selectCloudProvider("gemini") },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = EmeraldPrimary,
                                            unselectedColor = Color(0xFF64748B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Google Gemini",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CardLabelDark
                                        )
                                        Text(
                                            text = "Gemini Flash with Search Grounding. Note: Free tier may encounter Google API quota limits.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Model Limitations & Scope Disclosures (Build plan §11)
            item {
                Text(
                    text = "Known Model Limitations & Scope",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LimitationItem(
                            title = "ADM Diffusion Images",
                            detail = "Images generated via Autoregressive Diffusion Models (ADM) represent the hardest detection boundary (~80% accuracy vs 95-100% on GANs and latent diffusion)."
                        )
                        LimitationItem(
                            title = "English Text (ISOT Dataset)",
                            detail = "English classification reflects wire-service formatting patterns more than direct ground-truth fact-checking. Avoid treating detection as absolute veracity."
                        )
                        LimitationItem(
                            title = "Audio Verification Scope (ArEnAV)",
                            detail = "Trained and validated on ArEnAV bilingual speech synthesis and voice cloning benchmarks. Unseen or novel zero-shot synthesis architectures may yield reduced sensitivity."
                        )
                    }
                }
            }

            // About & Architecture Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = EmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About Yaqeen Gulf",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CardLabelDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Package: com.yaqeengulf.deepfake\nMin SDK: 29 (Android 10+)\nDistribution: Sideload Signed Release APK\nInference: ONNX Runtime Mobile (Inference Only)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun ModelOptionRow(
    model: ImageModelType,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        color = if (isSelected) Color(0xFFF1F5F9) else Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) EmeraldLight else Color(0xFFE2E8F0)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = EmeraldPrimary,
                    unselectedColor = Color(0xFF64748B)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = CardLabelDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = model.parameterCount,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldLight,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun LimitationItem(title: String, detail: String) {
    Column {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.titleMedium,
            color = WarningAmber
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}
