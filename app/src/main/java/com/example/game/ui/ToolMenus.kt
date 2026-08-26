package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*

@Composable
fun ToolFlyoutMenu(
    activeCategory: String?,
    activeTool: ActiveTool,
    activeOverlay: OverlayMode,
    onSelectTool: (ActiveTool) -> Unit,
    onSelectOverlay: (OverlayMode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeCategory == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF0101B2B),
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header Row with category title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeCategory.uppercase(),
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Menu",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submenu Items horizontally scrollable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (activeCategory) {
                    "Zones" -> ZoneItems(activeTool, onSelectTool)
                    "Roads" -> RoadItems(activeTool, onSelectTool)
                    "Services" -> ServiceItems(activeTool, onSelectTool)
                    "Utilities" -> UtilityItems(activeTool, onSelectTool)
                    "Transport" -> TransportItems(activeTool, onSelectTool)
                    "Overlays" -> OverlayItems(activeOverlay, onSelectOverlay)
                }
            }
        }
    }
}

@Composable
private fun ZoneItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf(ZoneCategory.RESIDENTIAL) }

    Column {
        // Tabs for Residential / Commercial / Industrial
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            CategoryTabChip("Residential", selectedCategoryTab == ZoneCategory.RESIDENTIAL, Color(0xFF4CAF50)) {
                selectedCategoryTab = ZoneCategory.RESIDENTIAL
            }
            CategoryTabChip("Commercial", selectedCategoryTab == ZoneCategory.COMMERCIAL, Color(0xFF2196F3)) {
                selectedCategoryTab = ZoneCategory.COMMERCIAL
            }
            CategoryTabChip("Industrial", selectedCategoryTab == ZoneCategory.INDUSTRIAL, Color(0xFFFFC107)) {
                selectedCategoryTab = ZoneCategory.INDUSTRIAL
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (selectedCategoryTab) {
                ZoneCategory.RESIDENTIAL -> {
                    ToolItemCard(
                        title = "Low Res",
                        subtitle = "Houses (2-6 pop)",
                        cost = "$20",
                        color = Color(0xFF81C784),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_LOW,
                        testTag = "zone_res_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_LOW)) }
                    )
                    ToolItemCard(
                        title = "Med Res",
                        subtitle = "Townhouse/Flats (20-80)",
                        cost = "$60",
                        color = Color(0xFF4CAF50),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_MED,
                        testTag = "zone_res_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_MED)) }
                    )
                    ToolItemCard(
                        title = "High Res",
                        subtitle = "Skyscrapers (100-500+)",
                        cost = "$150",
                        color = Color(0xFF2E7D32),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_HIGH,
                        testTag = "zone_res_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_HIGH)) }
                    )
                }
                ZoneCategory.COMMERCIAL -> {
                    ToolItemCard(
                        title = "Low Com",
                        subtitle = "Corner Shops/Cafes",
                        cost = "$30",
                        color = Color(0xFF64B5F6),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_LOW,
                        testTag = "zone_com_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_LOW)) }
                    )
                    ToolItemCard(
                        title = "Med Com",
                        subtitle = "Business Plazas",
                        cost = "$90",
                        color = Color(0xFF2196F3),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_MED,
                        testTag = "zone_com_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_MED)) }
                    )
                    ToolItemCard(
                        title = "High Com",
                        subtitle = "Mega Malls/Towers",
                        cost = "$220",
                        color = Color(0xFF1565C0),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_HIGH,
                        testTag = "zone_com_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_HIGH)) }
                    )
                }
                ZoneCategory.INDUSTRIAL -> {
                    ToolItemCard(
                        title = "Low Ind",
                        subtitle = "Workshops/Crafts",
                        cost = "$25",
                        color = Color(0xFFFFEE58),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_LOW,
                        testTag = "zone_ind_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_LOW)) }
                    )
                    ToolItemCard(
                        title = "Med Ind",
                        subtitle = "Factories/Hubs",
                        cost = "$75",
                        color = Color(0xFFFFCA28),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_MED,
                        testTag = "zone_ind_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_MED)) }
                    )
                    ToolItemCard(
                        title = "High Ind",
                        subtitle = "Heavy Industrial Complex",
                        cost = "$180",
                        color = Color(0xFFF57F17),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_HIGH,
                        testTag = "zone_ind_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_HIGH)) }
                    )
                }
                ZoneCategory.NONE -> {}
            }

            // De-zone tool
            ToolItemCard(
                title = "De-Zone",
                subtitle = "Clear Zoning",
                cost = "Free",
                color = Color.DarkGray,
                isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.NONE,
                testTag = "zone_dezone",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.NONE)) }
            )
        }
    }
}

@Composable
private fun CategoryTabChip(label: String, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accentColor.copy(alpha = 0.25f) else Color(0x33FFFFFF))
            .border(1.dp, if (selected) accentColor else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoadItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    ToolItemCard(
        title = "Small (2L)",
        subtitle = "Local Street",
        cost = "$25",
        color = Color(0xFF78909C),
        isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.SMALL_2L,
        testTag = "road_small",
        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.SMALL_2L)) }
    )
    ToolItemCard(
        title = "Medium (4L)",
        subtitle = "Avenue",
        cost = "$60",
        color = Color(0xFF607D8B),
        isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.MEDIUM_4L,
        testTag = "road_medium",
        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.MEDIUM_4L)) }
    )
    ToolItemCard(
        title = "Boulevard (6L)",
        subtitle = "Main Arterial",
        cost = "$120",
        color = Color(0xFF455A64),
        isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.LARGE_6L,
        testTag = "road_boulevard",
        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.LARGE_6L)) }
    )
    ToolItemCard(
        title = "Highway",
        subtitle = "Expressway",
        cost = "$220",
        color = Color(0xFF37474F),
        isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.HIGHWAY,
        testTag = "road_highway",
        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.HIGHWAY)) }
    )
    ToolItemCard(
        title = "Bridge",
        subtitle = "Over Water",
        cost = "$90",
        color = Color(0xFF90A4AE),
        isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.BRIDGE,
        testTag = "road_bridge",
        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.BRIDGE)) }
    )
}

@Composable
private fun ServiceItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    ServiceType.values().forEach { s ->
        ToolItemCard(
            title = s.displayName,
            subtitle = "Radius: ${s.radius} tiles",
            cost = "$${s.cost}",
            color = when (s.category) {
                ServiceCategory.POLICE -> Color(0xFF1E88E5)
                ServiceCategory.FIRE -> Color(0xFFE53935)
                ServiceCategory.HEALTH -> Color(0xFFE0E0E0)
                ServiceCategory.EDUCATION -> Color(0xFFFB8C00)
                ServiceCategory.PARK -> Color(0xFF43A047)
                ServiceCategory.DEATH_CARE -> Color(0xFF78909C)
                ServiceCategory.GARBAGE -> Color(0xFF6D4C41)
            },
            isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == s,
            testTag = "service_${s.name.lowercase()}",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = s)) }
        )
    }
}

@Composable
private fun UtilityItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    UtilityType.values().forEach { u ->
        val desc = if (u.category == UtilityCategory.POWER) "+${u.outputPowerMW} MW" else "+${u.outputWaterMG} MG"
        ToolItemCard(
            title = u.displayName,
            subtitle = desc,
            cost = "$${u.cost}",
            color = if (u.category == UtilityCategory.POWER) Color(0xFFFFB300) else Color(0xFF00B0FF),
            isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == u,
            testTag = "utility_${u.name.lowercase()}",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = u)) }
        )
    }
}

@Composable
private fun TransportItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    TransportType.values().forEach { tr ->
        ToolItemCard(
            title = tr.displayName,
            subtitle = "Relief: ${tr.trafficReliefRadius} tiles",
            cost = "$${tr.cost}",
            color = Color(0xFFAB47BC),
            isSelected = activeTool.mode == ToolMode.TRANSPORT && activeTool.transportType == tr,
            testTag = "transport_${tr.name.lowercase()}",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.TRANSPORT, transportType = tr)) }
        )
    }
}

@Composable
private fun OverlayItems(
    activeOverlay: OverlayMode,
    onSelectOverlay: (OverlayMode) -> Unit
) {
    OverlayMode.values().forEach { ov ->
        ToolItemCard(
            title = ov.label,
            subtitle = "Data Heatmap",
            cost = "",
            color = Color(0xFF00ACC1),
            isSelected = activeOverlay == ov,
            testTag = "overlay_${ov.name.lowercase()}",
            onClick = { onSelectOverlay(ov) }
        )
    }
}

@Composable
private fun ToolItemCard(
    title: String,
    subtitle: String,
    cost: String,
    color: Color,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(115.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isSelected) color.copy(alpha = 0.35f) else Color(0x33FFFFFF),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) color else Color(0x22FFFFFF)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = Color.LightGray,
                fontSize = 9.sp,
                maxLines = 1
            )
            if (cost.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cost,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}
