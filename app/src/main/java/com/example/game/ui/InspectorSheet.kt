package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.game.model.*

@Composable
fun InspectorSheet(
    tile: GridTile,
    onDemolish: (GridTile) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val b = tile.building

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("inspector_sheet"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xF0101E30),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        shadowElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val title = when {
                        b != null -> b.buildingName
                        tile.service != null -> tile.service!!.displayName
                        tile.utility != null -> tile.utility!!.displayName
                        tile.transport != null -> tile.transport!!.displayName
                        tile.road != RoadType.NONE -> tile.road.displayName
                        tile.zone != ZoneType.NONE -> "${tile.zone.displayName} Lot"
                        else -> "${tile.terrain.name.lowercase().capitalize()} Tile"
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Location: (${tile.x}, ${tile.y})  •  Elevation: ${tile.elevation}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Demolish button
                    IconButton(
                        onClick = { onDemolish(tile) },
                        modifier = Modifier.testTag("inspector_demolish_btn")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Demolish", tint = Color(0xFFFF5252))
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Building Details if present
            if (b != null) {
                // Zone & Density Badge
                val densityColor = when (b.zoneType.density) {
                    DensityLevel.LOW -> Color(0xFF81C784)
                    DensityLevel.MEDIUM -> Color(0xFF4CAF50)
                    DensityLevel.HIGH -> Color(0xFF2E7D32)
                    DensityLevel.NONE -> Color.Gray
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(
                        text = "${b.zoneType.category.name} (${b.zoneType.density.name} DENSITY)",
                        color = densityColor
                    )
                    StatusBadge(
                        text = "Level ${b.level}/3",
                        color = Color(0xFF90CAF9)
                    )
                    StatusBadge(
                        text = b.stage.name,
                        color = if (b.stage == BuildingStage.BUILT) Color(0xFF81C784) else Color(0xFFFFB74D)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (b.zoneType.category == ZoneCategory.RESIDENTIAL) {
                        InspectorMetricCard("Residents", "${b.population}", Icons.Default.People, Color(0xFF64B5F6), Modifier.weight(1f))
                    } else {
                        InspectorMetricCard("Jobs", "${b.jobs}", Icons.Default.Work, Color(0xFFFFB74D), Modifier.weight(1f))
                    }
                    InspectorMetricCard("Happiness", "${b.happinessScore}%", Icons.Default.SentimentSatisfied, Color(0xFF81C784), Modifier.weight(1f))
                    InspectorMetricCard("Land Value", "${tile.landValue}", Icons.Default.MonetizationOn, Color(0xFFFFD54F), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Utility Status Checks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UtilityStatusChip("Power", b.isPowered, Icons.Default.Bolt, Modifier.weight(1f))
                    UtilityStatusChip("Water", b.isWatered, Icons.Default.WaterDrop, Modifier.weight(1f))
                    UtilityStatusChip("Road", b.hasRoadAccess, Icons.Default.AddRoad, Modifier.weight(1f))
                }
            } else if (tile.road != RoadType.NONE) {
                Text(
                    text = "Lanes: ${tile.road.lanes}  •  Speed Limit: ${tile.road.speedLimit}x  •  Capacity: ${tile.road.capacity}",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Text(
                    text = "Traffic Congestion: ${(tile.trafficVolume * 100).toInt()}%",
                    color = if (tile.trafficVolume > 0.6f) Color(0xFFFF5252) else Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else if (tile.service != null) {
                val s = tile.service!!
                Text(
                    text = "Service: ${s.displayName}  •  Coverage Radius: ${s.radius} tiles  •  Maintenance: $${s.maintenance}/day",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            } else if (tile.utility != null) {
                val u = tile.utility!!
                val output = if (u.category == UtilityCategory.POWER) "${u.outputPowerMW} MW Electricity" else "${u.outputWaterMG} MG Water"
                Text(
                    text = "Utility: ${u.displayName}  •  Output: $output  •  Maintenance: $${u.maintenance}/day",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.25f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun InspectorMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0x22FFFFFF)
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(text = label, color = Color.Gray, fontSize = 8.sp)
                Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun UtilityStatusChip(
    label: String,
    isActive: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val color = if (isActive) Color(0xFF81C784) else Color(0xFFFF5252)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isActive) "$label: OK" else "$label: NO",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
