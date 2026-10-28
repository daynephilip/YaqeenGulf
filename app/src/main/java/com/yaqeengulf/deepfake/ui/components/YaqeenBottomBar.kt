package com.yaqeengulf.deepfake.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yaqeengulf.deepfake.ui.navigation.Screen
import com.yaqeengulf.deepfake.ui.theme.BottomNavBg
import com.yaqeengulf.deepfake.ui.theme.BottomNavIconSelected
import com.yaqeengulf.deepfake.ui.theme.BottomNavIconUnselected
import com.yaqeengulf.deepfake.ui.theme.MintTealHeader

@Composable
fun YaqeenBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        color = BottomNavBg,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                iconSelected = Icons.Filled.Home,
                iconUnselected = Icons.Outlined.Home,
                contentDescription = "Home",
                isSelected = currentScreen == Screen.Dashboard || currentScreen == Screen.CloudVerification,
                onClick = { onNavigate(Screen.Dashboard) }
            )

            BottomNavItem(
                iconSelected = Icons.Filled.History,
                iconUnselected = Icons.Outlined.History,
                contentDescription = "Recent Scans",
                isSelected = currentScreen == Screen.History,
                onClick = { onNavigate(Screen.History) }
            )

            BottomNavItem(
                iconSelected = Icons.Filled.Settings,
                iconUnselected = Icons.Outlined.Settings,
                contentDescription = "Settings",
                isSelected = currentScreen == Screen.Settings,
                onClick = { onNavigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(MintTealHeader.copy(alpha = 0.18f), CircleShape)
            )
        }

        Icon(
            imageVector = if (isSelected) iconSelected else iconUnselected,
            contentDescription = contentDescription,
            tint = if (isSelected) BottomNavIconSelected else BottomNavIconUnselected,
            modifier = Modifier.size(28.dp)
        )
    }
}
