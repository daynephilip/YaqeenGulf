package com.yaqeengulf.deepfake.ui.dashboard

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaqeengulf.deepfake.domain.model.ImageModelType
import com.yaqeengulf.deepfake.ui.theme.CardLabelDark
import com.yaqeengulf.deepfake.ui.theme.ElectricBlueCard
import com.yaqeengulf.deepfake.ui.theme.EmeraldLight
import com.yaqeengulf.deepfake.ui.theme.EmeraldPrimary
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkSubtext
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkText
import com.yaqeengulf.deepfake.ui.theme.HeaderPillFill
import com.yaqeengulf.deepfake.ui.theme.HeaderStatPillBg
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface
import com.yaqeengulf.deepfake.ui.theme.SoftBlueCard
import com.yaqeengulf.deepfake.ui.theme.TrustBlue
import com.yaqeengulf.deepfake.ui.theme.WarningAmber
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToCloud: () -> Unit = {},
    onScanClicked: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val selectedImageModel by viewModel.selectedImageModel.collectAsState()
    val selectedAudioModel by viewModel.selectedAudioModel.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
    val recentScans by viewModel.recentScans.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanResult by viewModel.currentScanResult.collectAsState()
    val cloudScanResult by viewModel.cloudScanResult.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Dialog & Interaction States
    val externalShowAudio by viewModel.showAudioDialog.collectAsState()
    val externalShowText by viewModel.showTextDialog.collectAsState()
    var showImageOptionsDialog by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showAudioRecordDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(externalShowAudio) {
        if (externalShowAudio) {
            showAudioRecordDialog = true
            viewModel.showAudioDialog.value = false
        }
    }
    androidx.compose.runtime.LaunchedEffect(externalShowText) {
        if (externalShowText) {
            showTextInputDialog = true
            viewModel.showTextDialog.value = false
        }
    }

    var textInputVal by remember { mutableStateOf("") }
    var selectedTextLang by remember { mutableStateOf<Boolean?>(null) } // null = Auto, true = AR, false = EN
    var selectedAudioLangArabic by remember { mutableStateOf(true) } // true = AR, false = EN
    var isRecordingMic by remember { mutableStateOf(false) }
    var micProgress by remember { mutableFloatStateOf(0f) }

    // 1. Photo Picker Launcher (Baseline image capture)
    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val bitmapResult = com.yaqeengulf.deepfake.capture.image.ImageCaptureManager.loadBitmapFromUri(context, uri)
                bitmapResult.onSuccess { bmp ->
                    viewModel.scanImage(bmp, sourceTag = "Gallery Photo", imageUri = uri)
                }.onFailure { err ->
                    android.widget.Toast.makeText(context, "Failed to load image: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2. Audio File Picker Launcher (Baseline audio file capture)
    val audioFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val audioResult = com.yaqeengulf.deepfake.capture.audio.AudioCaptureManager.loadAudioFromUri(context, uri)
                audioResult.onSuccess { samples ->
                    viewModel.scanAudio(samples, isArabic = selectedAudioLangArabic, sourceTag = "Audio File Pick")
                }.onFailure { err ->
                    android.widget.Toast.makeText(context, "Failed to parse audio: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3. Microphone Permission Launcher
    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showAudioRecordDialog = true
        } else {
            android.widget.Toast.makeText(context, "Microphone permission required for voice recording", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MintTealHeader)
    ) {
        // --- Top Header Section (Matches Mockup) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // App Bar Row with Title and Floating Shield Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Yaqeen Gulf",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = HeaderDarkText
                    )
                    Text(
                        text = "يقين الخليج • On-Device Neural Shield",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = HeaderDarkSubtext
                    )
                }

                // Top-Right Floating Shield Toggle Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isOverlayEnabled) Color(0xFF052B1E) else Color.White.copy(alpha = 0.25f))
                        .clickable {
                            viewModel.toggleOverlay(context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOverlayEnabled) Icons.Filled.Security else Icons.Outlined.Shield,
                        contentDescription = "Toggle Floating Shield",
                        tint = if (isOverlayEnabled) MintTealHeader else HeaderDarkText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stat & Status Card Widget (Matches "$7,783.00 Balance" Widget from reference image)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = HeaderDarkSubtext,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shield Protection Status",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HeaderDarkSubtext
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isOverlayEnabled) "Active & Protected" else "Ready to Inspect",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = HeaderDarkText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Progress / Privacy Capsule Pill (Matches the dark capsule "30% $20,000.00" in mockup)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dark filled portion
                        Surface(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxSize(),
                            shape = RoundedCornerShape(18.dp),
                            color = HeaderPillFill
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "100% ON-DEVICE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MintTealHeader,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Light transparent remainder
                        Box(
                            modifier = Modifier
                                .weight(0.65f)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "Zero Cloud Leaks • Privacy First",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = HeaderDarkText,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Main Content Container (Rounded Mint Surface Matching Reference Image) ---
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)),
            color = MintTealSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2x2 Grid of Verification Modalities (Matches Mockup Layout & Box Shape)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Box 1: Image Verification (Deep Electric Blue)
                    ModalityBox(
                        title = "Image Verification",
                        icon = Icons.Outlined.Image,
                        containerColor = ElectricBlueCard,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showImageOptionsDialog = true
                        }
                    )

                    // Box 2: Audio Verification (Soft Sky Blue)
                    ModalityBox(
                        title = "Audio Verification",
                        icon = Icons.Outlined.GraphicEq,
                        containerColor = SoftBlueCard,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val isAudioArabic = selectedAudioModel.equals("arabic", ignoreCase = true)
                            selectedAudioLangArabic = isAudioArabic
                            showAudioRecordDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Box 3: Text Verification (Soft Sky Blue)
                    ModalityBox(
                        title = "Text Verification",
                        icon = Icons.Outlined.Article,
                        containerColor = SoftBlueCard,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showTextInputDialog = true
                        }
                    )

                    // Box 4: Verify News / Link Verification (Soft Sky Blue)
                    ModalityBox(
                        title = "Verify News",
                        icon = Icons.Outlined.Language,
                        containerColor = SoftBlueCard,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCloud
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Quick Recent Scans Row
                if (recentScans.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Latest Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CardLabelDark
                        )
                        TextButton(onClick = onNavigateToHistory) {
                            Text(
                                text = "View All (${recentScans.size})",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    recentScans.take(2).forEach { scan ->
                        val isCloud = scan.sourceType.equals("cloud", ignoreCase = true)
                        val isFake = scan.verdict.equals("SYNTHETIC", ignoreCase = true) || scan.verdict.equals("UNRELIABLE", ignoreCase = true)
                        val isSuspicious = scan.verdict.equals("SUSPICIOUS", ignoreCase = true) || scan.verdict.equals("UNCERTAIN", ignoreCase = true)
                        val verdictColor = when {
                            isFake -> Color(0xFFEF4444)
                            isSuspicious -> WarningAmber
                            else -> EmeraldPrimary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigateToHistory() },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${scan.modality}: ${scan.verdict}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = verdictColor
                                    )
                                    Text(
                                        text = scan.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${(scan.confidenceScore * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = verdictColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // Clearance for persistent bottom bar
            }
        }
    }

    // --- Dialogs & Modals ---

    // 1. Image Model Options Dialog
    if (showImageOptionsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImageOptionsDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Image Deepfake Verification",
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Choose detection model architecture:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedImageModel == ImageModelType.EFFICIENTNET_B3,
                            onClick = { viewModel.selectImageModel(ImageModelType.EFFICIENTNET_B3) },
                            label = { Text("EfficientNet-B3 (Default)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlueCard,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedImageModel == ImageModelType.SWIN_T,
                            onClick = { viewModel.selectImageModel(ImageModelType.SWIN_T) },
                            label = { Text("Swin-T", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlueCard,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Inspect photos for GAN/diffusion artifacts, frequency footprints, and spatial blending anomalies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "C2PA Content Credentials Provision",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Every selected image is automatically inspected for ISO 19566-5 JUMBF cryptographic manifests and IPTC digital source assertions (Adobe Firefly, DALL-E, Midjourney, camera origin) directly from raw file bytes alongside neural model inference.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CardLabelDark,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showImageOptionsDialog = false
                        photoPickerLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueCard)
                ) {
                    Text("Select Photo from Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageOptionsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // 2. Manual Text Input Dialog
    if (showTextInputDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTextInputDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Scan Text for Synthetic Markers",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter or paste Arabic or English text to inspect via AraBERT / BERT-base:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Model / Language Selection:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedTextLang == null,
                            onClick = { selectedTextLang = null },
                            label = { Text("Auto", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftBlueCard,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedTextLang == true,
                            onClick = { selectedTextLang = true },
                            label = { Text("Arabic (AraBERT)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedTextLang == false,
                            onClick = { selectedTextLang = false },
                            label = { Text("English (BERT)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftBlueCard,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInputVal,
                        onValueChange = { textInputVal = it },
                        placeholder = { Text("Paste text or news article here...", color = Color.LightGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CardLabelDark,
                            unfocusedTextColor = CardLabelDark,
                            focusedBorderColor = SoftBlueCard,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToScan = textInputVal.trim()
                        if (textToScan.isNotBlank()) {
                            showTextInputDialog = false
                            textInputVal = ""
                            viewModel.scanText(textToScan, isArabic = selectedTextLang, sourceTag = "Manual Text Paste")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlueCard)
                ) {
                    Text("Analyze", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // 3. Live Audio Recording / File Pick Dialog
    if (showAudioRecordDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                if (!isRecordingMic) showAudioRecordDialog = false
            },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Audio Spoof & Voice Verification",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Record 4 seconds from microphone or pick a .wav file to verify acoustic features:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acoustic Model: ArEnAV SpectrogramCNN (Arabic & English)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CardLabelDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isRecordingMic) {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { micProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = SoftBlueCard
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Recording acoustic spectrogram... ${(micProgress * 4.0).toInt()}s / 4s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldPrimary
                        )
                    } else {
                        Button(
                            onClick = {
                                val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (!hasMic) {
                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    return@Button
                                }

                                isRecordingMic = true
                                micProgress = 0f
                                coroutineScope.launch {
                                    val recordResult = com.yaqeengulf.deepfake.capture.audio.AudioCaptureManager.recordAudio(
                                        durationSeconds = 4,
                                        onProgress = { p -> micProgress = p }
                                    )
                                    isRecordingMic = false
                                    showAudioRecordDialog = false
                                    recordResult.onSuccess { samples ->
                                        viewModel.scanAudio(samples, isArabic = selectedAudioLangArabic, sourceTag = "Live Microphone Record")
                                    }.onFailure { err ->
                                        android.widget.Toast.makeText(context, "Recording failed: ${err.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SoftBlueCard)
                        ) {
                            Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record 4.0s from Mic", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                showAudioRecordDialog = false
                                audioFilePickerLauncher.launch("audio/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pick Audio File (.wav)", color = CardLabelDark)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!isRecordingMic) {
                    TextButton(onClick = { showAudioRecordDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        )
    }

    // Result Bottom Sheet (Modality evidence, CAM heatmaps, attention text, raw score 0.0-1.0)
    scanResult?.let { res ->
        com.yaqeengulf.deepfake.ui.result.ResultBottomSheet(
            result = res,
            onDismiss = { viewModel.clearScanResult() }
        )
    }

    // Loading overlay
    if (isScanning) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MintTealHeader, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing with neural model...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CardLabelDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Computing XAI attention & spatial heatmaps",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Error alert
    errorMessage?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearScanResult() },
            containerColor = Color.White,
            title = { Text("Verification Notice", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)) },
            text = { Text(msg, color = CardLabelDark) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearScanResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = MintTealHeader)
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

/**
 * High-aesthetic Modality Box matching the rounded rectangular blue cards in the reference mockup.
 */
@Composable
private fun ModalityBox(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            color = containerColor,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = CardLabelDark,
            maxLines = 1
        )
    }
}
