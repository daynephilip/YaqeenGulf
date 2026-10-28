package com.yaqeengulf.deepfake.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yaqeengulf.deepfake.data.local.ScanHistoryEntity
import com.yaqeengulf.deepfake.ui.dashboard.DashboardViewModel
import com.yaqeengulf.deepfake.ui.theme.CardLabelDark
import com.yaqeengulf.deepfake.ui.theme.ElectricBlueCard
import com.yaqeengulf.deepfake.ui.theme.EmeraldLight
import com.yaqeengulf.deepfake.ui.theme.EmeraldPrimary
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkSubtext
import com.yaqeengulf.deepfake.ui.theme.HeaderDarkText
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface
import com.yaqeengulf.deepfake.ui.theme.TrustBlue
import com.yaqeengulf.deepfake.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit
) {
    val allHistory by viewModel.recentScans.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Image", "Text", "Audio", "Cloud"
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredList = remember(allHistory, selectedFilter) {
        when (selectedFilter) {
            "Image" -> allHistory.filter { it.modality.equals("IMAGE", ignoreCase = true) }
            "Text" -> allHistory.filter { it.modality.equals("TEXT", ignoreCase = true) }
            "Audio" -> allHistory.filter { it.modality.equals("AUDIO", ignoreCase = true) }
            "Cloud" -> allHistory.filter { it.sourceType.equals("cloud", ignoreCase = true) }
            else -> allHistory
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault()) }

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
                        text = "Scan History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HeaderDarkText
                    )
                    Text(
                        text = "سجل الفحص والتحقق",
                        style = MaterialTheme.typography.labelSmall,
                        color = HeaderDarkSubtext
                    )
                }

                if (allHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear All History",
                            tint = Color(0xFFDC2626)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
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
                    .padding(top = 16.dp)
            ) {
                // Filter Chips Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Image", "Text", "Audio", "Cloud")
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = CardLabelDark,
                                selectedContainerColor = if (filter == "Cloud") TrustBlue else ElectricBlueCard,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = Color(0xFFE2E8F0),
                                selectedBorderColor = Color.Transparent,
                                enabled = true,
                                selected = selectedFilter == filter
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                shadowElevation = 2.dp,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No records found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CardLabelDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedFilter == "All") "Completed scan results will appear here." else "No scans matching category '$selectedFilter'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { scan ->
                            HistoryItemCard(
                                scan = scan,
                                formattedDate = dateFormat.format(Date(scan.timestamp)),
                                onDelete = {
                                    viewModel.deleteScan(scan.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Clear Verification History?",
                    fontWeight = FontWeight.Bold,
                    color = CardLabelDark
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all stored scan records and attribution summaries from your device. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun HistoryItemCard(
    scan: ScanHistoryEntity,
    formattedDate: String,
    onDelete: () -> Unit
) {
    val isCloud = scan.sourceType.equals("cloud", ignoreCase = true)
    val isFake = scan.verdict.equals("SYNTHETIC", ignoreCase = true) ||
            scan.verdict.equals("UNRELIABLE", ignoreCase = true) ||
            scan.verdict.equals("AI GENERATED", ignoreCase = true) ||
            scan.verdict.equals("FAKE", ignoreCase = true)
    val isPotentiallyFake = scan.verdict.equals("POTENTIALLY FAKE", ignoreCase = true) || scan.verdict.equals("SUSPICIOUS", ignoreCase = true) || scan.verdict.equals("UNCERTAIN", ignoreCase = true)

    val verdictColor = when {
        isFake -> Color(0xFFEF4444)
        isPotentiallyFake -> WarningAmber
        else -> Color(0xFF10B981)
    }

    val icon = when {
        isCloud -> Icons.Default.Cloud
        scan.modality.equals("IMAGE", ignoreCase = true) -> Icons.Default.Image
        scan.modality.equals("TEXT", ignoreCase = true) -> Icons.Default.Description
        scan.modality.equals("AUDIO", ignoreCase = true) -> Icons.Default.Audiotrack
        else -> Icons.Default.Security
    }

    val originTag = if (isCloud) "Cloud Check" else "On-Device"
    val originColor = if (isCloud) TrustBlue else EmeraldLight

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Origin Badge, Modality, Timestamp, and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = originColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, originColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isCloud) Icons.Default.Cloud else Icons.Default.Security,
                                contentDescription = null,
                                tint = originColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = originTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = originColor,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scan.modelUsed,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Record",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Modality Icon, Verdict, and Confidence
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
                            .size(38.dp)
                            .background(verdictColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = scan.modality,
                            tint = verdictColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${scan.modality}: ${scan.verdict}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = verdictColor
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "${(scan.confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = verdictColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Text
            Text(
                text = scan.summary,
                style = MaterialTheme.typography.bodySmall,
                color = CardLabelDark,
                lineHeight = 18.sp
            )

            if (!scan.sourceIdentifier.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Source: ${scan.sourceIdentifier}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
