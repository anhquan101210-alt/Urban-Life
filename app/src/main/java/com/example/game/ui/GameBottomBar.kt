package com.example.game.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier
) {
    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
        borderColor = PixelColors.PanelBorder,
        backgroundColor = PixelColors.PanelBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockButton(
                title = "ZONES",
                iconEmoji = "🏠",
                isSelected = activeCategory == "Zones" || (activeCategory == null && activeTool.mode == ToolMode.ZONE),
                testTag = "bottom_btn_zones",
                accentColor = PixelColors.AccentGreen,
                onClick = { onCategoryClick("Zones") }
            )

            DockButton(
                title = "ROADS",
                iconEmoji = "🛣",
                isSelected = activeCategory == "Roads" || (activeCategory == null && (activeTool.mode == ToolMode.ROAD || activeTool.mode == ToolMode.DEMOLISH)),
                testTag = "bottom_btn_roads",
                accentColor = Color(0xFF90A4AE),
                onClick = { onCategoryClick("Roads") }
            )

            DockButton(
                title = "SERVICES",
                iconEmoji = "🛡",
                isSelected = activeCategory == "Services" || (activeCategory == null && activeTool.mode == ToolMode.SERVICE),
                testTag = "bottom_btn_services",
                accentColor = PixelColors.AccentRed,
                onClick = { onCategoryClick("Services") }
            )

            DockButton(
                title = "UTILITIES",
                iconEmoji = "⚡",
                isSelected = activeCategory == "Utilities" || (activeCategory == null && activeTool.mode == ToolMode.UTILITY),
                testTag = "bottom_btn_utilities",
                accentColor = PixelColors.AccentGold,
                onClick = { onCategoryClick("Utilities") }
            )

            DockButton(
                title = "TRANSPORT",
                iconEmoji = "🚌",
                isSelected = activeCategory == "Transport" || (activeCategory == null && activeTool.mode == ToolMode.TRANSPORT),
                testTag = "bottom_btn_transport",
                accentColor = Color(0xFFAB47BC),
                onClick = { onCategoryClick("Transport") }
            )

            DockButton(
                title = "MORE",
                iconEmoji = "☰",
                isSelected = activeCategory == "More",
                testTag = "bottom_btn_more",
                accentColor = PixelColors.AccentCyan,
                onClick = { onCategoryClick("More") }
            )
        }
    }
}

@Composable
private fun DockButton(
    title: String,
    iconEmoji: String,
    isSelected: Boolean,
    testTag: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    PixelButton(
        onClick = onClick,
        isSelected = isSelected,
        selectedColor = accentColor,
        modifier = Modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Text(text = iconEmoji, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFFCFD8DC),
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            fontSize = 10.5.sp,
            letterSpacing = 0.5.sp
        )
    }
}
