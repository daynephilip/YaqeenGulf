package com.yaqeengulf.deepfake.ui.navigation

sealed class Screen {
    object Dashboard : Screen()
    object CloudVerification : Screen()
    object Settings : Screen()
    object History : Screen()
}
