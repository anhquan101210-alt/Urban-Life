package com.example.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
fun InspectorSheet(
    tile: GridTile,
    onUpgrade: (GridTile) -> Unit,
    onDemolish: (GridTile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val b = tile.building

    PixelPanel(
        modifier = modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth(0.92f)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("inspector_sheet"),
        borderColor = PixelColors.AccentCyan,
        backgroundColor = PixelColors.PanelBgSolid
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconEmoji = when {
                        b != null -> when (b.zoneType.category) {
                            ZoneCategory.RESIDENTIAL -> "🏠"
                            ZoneCategory.COMMERCIAL -> "🏬"
                            ZoneCategory.INDUSTRIAL -> "🏭"
                            else -> "🏢"
                        }
                        tile.service != null -> "🛡"
                        tile.utility != null -> "⚡"
                        tile.transport != null -> "🚌"
                        tile.road != RoadType.NONE -> "🛣"
                        tile.zone != ZoneType.NONE -> "📐"
                        else -> "🌱"
                    }
                    Text(iconEmoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        val title = when {
                            b != null -> b.buildingName
                            tile.service != null -> tile.service!!.displayName
                            tile.utility != null -> tile.utility!!.displayName
                            tile.transport != null -> tile.transport!!.displayName
                            tile.road != RoadType.NONE -> tile.road.displayName
                            tile.zone != ZoneType.NONE -> "${tile.zone.displayName} Zone Lot"
                            else -> "${tile.terrain.name.lowercase().replaceFirstChar { it.uppercase() }} Tile"
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Grid (${tile.x}, ${tile.y}) • Land Value: $${tile.landValue}",
                            color = Color(0xFF90A4AE),
                            fontSize = 9.sp
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Building Details
            if (b != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PixelBadge("Level ${b.level}/3", PixelColors.AccentCyan)
                    PixelBadge(
                        if (b.zoneType.category == ZoneCategory.RESIDENTIAL) "Pop: ${b.population}" else "Jobs: ${b.jobs}",
                        PixelColors.AccentGreen
                    )
                    PixelBadge("Joy: ${b.happinessScore}%", PixelColors.AccentGold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Service Status Checks (Power, Water, Fire Safety, Road Access)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusCheckPill("⚡ Power", b.isPowered, Modifier.weight(1f))
                    StatusCheckPill("💧 Water", b.isWatered, Modifier.weight(1f))
                    StatusCheckPill("🛣 Road", b.hasRoadAccess, Modifier.weight(1f))
                }
            } else if (tile.road != RoadType.NONE) {
                Text(
                    text = "Capacity: ${tile.road.capacity} cars/h • Speed: ${tile.road.speedLimit}x",
                    color = Color(0xFFCFD8DC),
                    fontSize = 10.sp
                )
                Text(
                    text = "Traffic: ${(tile.trafficVolume * 100).toInt()}% ${if (tile.trafficVolume > 0.6f) "(Congested)" else "(Smooth)"}",
                    color = if (tile.trafficVolume > 0.6f) PixelColors.AccentRed else PixelColors.AccentGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (tile.service != null) {
                val s = tile.service!!
                Text(
                    text = "Radius: ${s.radius} tiles • Maintenance: $${s.maintenance}/day",
                    color = Color(0xFFCFD8DC),
                    fontSize = 10.sp
                )
            } else if (tile.utility != null) {
                val u = tile.utility!!
                val output = if (u.category == UtilityCategory.POWER) "${u.outputPowerMW} MW Electric" else "${u.outputWaterMG} MG Water"
                Text(
                    text = "Output: $output • Maintenance: $${u.maintenance}/day",
                    color = Color(0xFFCFD8DC),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Upgrade (if building), Demolish, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (b != null && b.level < 3) {
                    val upgradeCost = b.level * 150L
                    PixelButton(
                        onClick = { onUpgrade(tile) },
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF1B5E20),
                        borderColor = PixelColors.AccentGreen
                    ) {
                        Text("⬆ UPGRADE ($$upgradeCost)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                PixelButton(
                    onClick = { onDemolish(tile) },
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color(0xFF7F0000),
                    borderColor = PixelColors.AccentRed
                ) {
                    Text("🚜 DEMOLISH ($10)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                PixelButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.7f),
                    backgroundColor = Color(0xFF1E3A5F)
                ) {
                    Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun StatusCheckPill(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) PixelColors.AccentGreen else PixelColors.AccentRed
    val icon = if (isActive) "✓" else "✕"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label $icon",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
        )
    }
}
