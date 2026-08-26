package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.ActiveTool
import com.example.game.model.ToolMode

@Composable
fun GameBottomBar(
    activeCategory: String?,
    activeTool: ActiveTool,
    onCategoryClick: (String) -> Unit,
    onEconomyClick: () -> Unit,
    onStatsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDemolishClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF00D1B2A),
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainDockButton(
                title = "Zones",
                icon = Icons.Default.GridOn,
                isSelected = activeCategory == "Zones",
                testTag = "bottom_btn_zones",
                accentColor = Color(0xFF4CAF50),
                onClick = { onCategoryClick("Zones") }
            )
            MainDockButton(
                title = "Roads",
                icon = Icons.Default.AltRoute,
                isSelected = activeCategory == "Roads",
                testTag = "bottom_btn_roads",
                accentColor = Color(0xFF78909C),
                onClick = { onCategoryClick("Roads") }
            )
            MainDockButton(
                title = "Services",
                icon = Icons.Default.LocalPolice,
                isSelected = activeCategory == "Services",
                testTag = "bottom_btn_services",
                accentColor = Color(0xFFE53935),
                onClick = { onCategoryClick("Services") }
            )
            MainDockButton(
                title = "Utilities",
                icon = Icons.Default.Bolt,
                isSelected = activeCategory == "Utilities",
                testTag = "bottom_btn_utilities",
                accentColor = Color(0xFFFFB300),
                onClick = { onCategoryClick("Utilities") }
            )
            MainDockButton(
                title = "Transit",
                icon = Icons.Default.DirectionsBus,
                isSelected = activeCategory == "Transport",
                testTag = "bottom_btn_transport",
                accentColor = Color(0xFFAB47BC),
                onClick = { onCategoryClick("Transport") }
            )
            MainDockButton(
                title = "Overlays",
                icon = Icons.Default.Layers,
                isSelected = activeCategory == "Overlays",
                testTag = "bottom_btn_overlays",
                accentColor = Color(0xFF00ACC1),
                onClick = { onCategoryClick("Overlays") }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(30.dp)
                    .padding(horizontal = 2.dp),
                color = Color(0x33FFFFFF)
            )

            // Bulldoze Tool
            MainDockButton(
                title = "Bulldoze",
                icon = Icons.Default.DeleteForever,
                isSelected = activeTool.mode == ToolMode.DEMOLISH,
                testTag = "bottom_btn_demolish",
                accentColor = Color(0xFFFF1744),
                onClick = onDemolishClick
            )

            // Economy Dialog
            MainDockButton(
                title = "Economy",
                icon = Icons.Default.AccountBalance,
                isSelected = false,
                testTag = "bottom_btn_economy",
                accentColor = Color(0xFF81C784),
                onClick = onEconomyClick
            )

            // Statistics Dialog
            MainDockButton(
                title = "Stats",
                icon = Icons.Default.InsertChart,
                isSelected = false,
                testTag = "bottom_btn_stats",
                accentColor = Color(0xFF64B5F6),
                onClick = onStatsClick
            )

            // Settings Dialog
            MainDockButton(
                title = "Settings",
                icon = Icons.Default.Settings,
                isSelected = false,
                testTag = "bottom_btn_settings",
                accentColor = Color(0xFFB0BEC5),
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
fun MainDockButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isSelected) accentColor.copy(alpha = 0.35f) else Color(0x22FFFFFF),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) accentColor else Color(0x22FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) accentColor else Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else Color(0xFFE0E0E0),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}
