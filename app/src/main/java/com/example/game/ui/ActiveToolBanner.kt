package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*

@Composable
fun ActiveToolBanner(
    activeTool: ActiveTool,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeTool.mode == ToolMode.INSPECT) return

    val toolName = when (activeTool.mode) {
        ToolMode.ZONE -> when (activeTool.zoneType) {
            ZoneType.RESIDENTIAL_LOW -> "Low Residential ($20)"
            ZoneType.RESIDENTIAL_MED -> "Med Residential ($60)"
            ZoneType.RESIDENTIAL_HIGH -> "High Residential ($150)"
            ZoneType.COMMERCIAL_LOW -> "Low Commercial ($30)"
            ZoneType.COMMERCIAL_MED -> "Med Commercial ($90)"
            ZoneType.COMMERCIAL_HIGH -> "High Commercial ($220)"
            ZoneType.INDUSTRIAL_LOW -> "Low Industrial ($25)"
            ZoneType.INDUSTRIAL_MED -> "Med Industrial ($75)"
            ZoneType.INDUSTRIAL_HIGH -> "High Industrial ($180)"
            else -> "Zone Tool"
        }
        ToolMode.ROAD -> "${activeTool.roadType.displayName} ($${activeTool.roadType.cost}/tile)"
        ToolMode.SERVICE -> "${activeTool.serviceType?.displayName} ($${activeTool.serviceType?.cost})"
        ToolMode.UTILITY -> "${activeTool.utilityType?.displayName} ($${activeTool.utilityType?.cost})"
        ToolMode.TRANSPORT -> "${activeTool.transportType?.displayName} ($${activeTool.transportType?.cost})"
        ToolMode.DEMOLISH -> "Bulldozer Demolish ($10/tile)"
        ToolMode.INSPECT -> ""
    }

    val accentColor = when (activeTool.mode) {
        ToolMode.ZONE -> when {
            activeTool.zoneType.name.startsWith("RESIDENTIAL") -> PixelColors.AccentGreen
            activeTool.zoneType.name.startsWith("COMMERCIAL") -> PixelColors.AccentBlue
            else -> PixelColors.AccentOrange
        }
        ToolMode.ROAD -> Color(0xFF90A4AE)
        ToolMode.SERVICE -> PixelColors.AccentRed
        ToolMode.UTILITY -> PixelColors.AccentGold
        ToolMode.TRANSPORT -> Color(0xFFAB47BC)
        ToolMode.DEMOLISH -> Color(0xFFFF1744)
        ToolMode.INSPECT -> Color.White
    }

    PixelPanel(
        modifier = modifier
            .wrapContentSize()
            .testTag("active_tool_banner"),
        borderColor = accentColor,
        backgroundColor = PixelColors.PanelBgSolid
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Text(
                text = toolName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Cancel Button
            PixelButton(
                onClick = onCancel,
                backgroundColor = Color(0xFF2C3E50),
                borderColor = Color(0xFF7F8C8D),
                modifier = Modifier.height(26.dp)
            ) {
                Text(
                    text = "✕ CANCEL",
                    color = Color(0xFFFF8A80),
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}
