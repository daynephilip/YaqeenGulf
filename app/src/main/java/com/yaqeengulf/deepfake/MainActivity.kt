package com.yaqeengulf.deepfake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.yaqeengulf.deepfake.ui.cloud.CloudVerificationScreen
import com.yaqeengulf.deepfake.ui.components.YaqeenBottomBar
import com.yaqeengulf.deepfake.ui.dashboard.DashboardScreen
import com.yaqeengulf.deepfake.ui.dashboard.DashboardViewModel
import com.yaqeengulf.deepfake.ui.navigation.Screen
import com.yaqeengulf.deepfake.ui.settings.SettingsScreen
import com.yaqeengulf.deepfake.ui.settings.SettingsViewModel
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader
import com.yaqeengulf.deepfake.ui.theme.MintTealSurface
import com.yaqeengulf.deepfake.ui.theme.YaqeenTheme

class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(application as YaqeenApp)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(application as YaqeenApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            YaqeenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MintTealHeader
                ) {
                    YaqeenNavHost(
                        dashboardViewModel = dashboardViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return
        if (intent.action == android.content.Intent.ACTION_SEND) {
            when {
                intent.type?.startsWith("text/") == true -> {
                    val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                        ?: intent.getCharSequenceExtra(android.content.Intent.EXTRA_TEXT)?.toString()
                    if (!sharedText.isNullOrBlank()) {
                        val extracted = DashboardViewModel.extractUrl(sharedText)
                        if (extracted != null) {
                            dashboardViewModel.navigateTo(Screen.CloudVerification)
                        }
                        dashboardViewModel.scanText(sharedText, sourceTag = "Incoming Share Sheet")
                    }
                }
                intent.type?.startsWith("image/") == true -> {
                    val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
                    }
                    if (imageUri != null) {
                        lifecycleScope.launch {
                            val bmpRes = com.yaqeengulf.deepfake.capture.image.ImageCaptureManager.loadBitmapFromUri(this@MainActivity, imageUri)
                            bmpRes.onSuccess { bmp ->
                                dashboardViewModel.scanImage(bmp, sourceTag = "Shared Photo", imageUri = imageUri)
                            }
                        }
                    }
                }
            }
        }

        if (intent.getBooleanExtra("EXTRA_SCAN_AUDIO", false)) {
            dashboardViewModel.showAudioDialog.value = true
        }
        if (intent.getBooleanExtra("EXTRA_SCAN_TEXT", false)) {
            dashboardViewModel.showTextDialog.value = true
        }
    }
}

@Composable
fun YaqeenNavHost(
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val navigationTarget by dashboardViewModel.navigationTarget.collectAsState()

    androidx.compose.runtime.LaunchedEffect(navigationTarget) {
        navigationTarget?.let { target ->
            currentScreen = target
            dashboardViewModel.clearNavigationTarget()
        }
    }

    BackHandler(enabled = currentScreen != Screen.Dashboard) {
        currentScreen = Screen.Dashboard
    }

    Scaffold(
        bottomBar = {
            YaqeenBottomBar(
                currentScreen = currentScreen,
                onNavigate = { screen -> currentScreen = screen }
            )
        },
        containerColor = MintTealHeader
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    is Screen.Dashboard -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            onNavigateToHistory = { currentScreen = Screen.History },
                            onNavigateToCloud = { currentScreen = Screen.CloudVerification },
                            onScanClicked = { /* Handled in Phase 5 */ }
                        )
                    }
                    is Screen.CloudVerification -> {
                        CloudVerificationScreen(
                            viewModel = dashboardViewModel,
                            onNavigateBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                    is Screen.History -> {
                        com.yaqeengulf.deepfake.ui.history.HistoryScreen(
                            viewModel = dashboardViewModel,
                            onNavigateBack = { currentScreen = Screen.Dashboard }
                        )
                    }
                }
            }
        }
    }
}
