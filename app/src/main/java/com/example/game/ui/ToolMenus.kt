package com.example.game.ui

import androidx.compose.foundation.BorderStroke
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
    stats: CityStats,
    onSelectTool: (ActiveTool) -> Unit,
    onSelectOverlay: (OverlayMode) -> Unit,
    onOpenCityOverview: () -> Unit,
    onOpenEconomy: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeCategory == null) return

    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        borderColor = PixelColors.PanelBorder,
        backgroundColor = PixelColors.PanelBg
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Header Row: Category Title & Close Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◆ ${activeCategory.uppercase()} MENU ◆",
                    color = PixelColors.AccentCyan,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close Menu",
                    tint = Color.LightGray,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onClose() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Submenu Items
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when (activeCategory) {
                    "Zones" -> ZoneItems(activeTool, onSelectTool)
                    "Roads" -> RoadItems(activeTool, onSelectTool)
                    "Services" -> ServiceItems(activeTool, onSelectTool)
                    "Utilities" -> UtilityItems(activeTool, stats, onSelectTool)
                    "Transport" -> TransportItems(activeTool, stats, onSelectTool)
                    "More" -> MoreItems(
                        activeOverlay = activeOverlay,
                        onSelectOverlay = onSelectOverlay,
                        onOpenCityOverview = onOpenCityOverview,
                        onOpenEconomy = onOpenEconomy,
                        onOpenStats = onOpenStats,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. ZONES SUBMENU
// -------------------------------------------------------------
@Composable
private fun ZoneItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    var selectedCategoryTab by remember { mutableStateOf(ZoneCategory.RESIDENTIAL) }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            CategoryTabChip("Residential", selectedCategoryTab == ZoneCategory.RESIDENTIAL, PixelColors.AccentGreen) {
                selectedCategoryTab = ZoneCategory.RESIDENTIAL
            }
            CategoryTabChip("Commercial", selectedCategoryTab == ZoneCategory.COMMERCIAL, PixelColors.AccentBlue) {
                selectedCategoryTab = ZoneCategory.COMMERCIAL
            }
            CategoryTabChip("Industrial", selectedCategoryTab == ZoneCategory.INDUSTRIAL, PixelColors.AccentOrange) {
                selectedCategoryTab = ZoneCategory.INDUSTRIAL
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when (selectedCategoryTab) {
                ZoneCategory.RESIDENTIAL -> {
                    PixelToolCard(
                        emoji = "🏠",
                        name = "LOW RES",
                        density = "Houses",
                        cost = "$20",
                        accentColor = Color(0xFF81C784),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_LOW,
                        testTag = "zone_res_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_LOW)) }
                    )
                    PixelToolCard(
                        emoji = "🏢",
                        name = "MED RES",
                        density = "Townhomes",
                        cost = "$60",
                        accentColor = PixelColors.AccentGreen,
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_MED,
                        testTag = "zone_res_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_MED)) }
                    )
                    PixelToolCard(
                        emoji = "🏙",
                        name = "HIGH RES",
                        density = "Skyscrapers",
                        cost = "$150",
                        accentColor = Color(0xFF2E7D32),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.RESIDENTIAL_HIGH,
                        testTag = "zone_res_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.RESIDENTIAL_HIGH)) }
                    )
                }
                ZoneCategory.COMMERCIAL -> {
                    PixelToolCard(
                        emoji = "🏪",
                        name = "LOW COM",
                        density = "Local Shops",
                        cost = "$30",
                        accentColor = Color(0xFF64B5F6),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_LOW,
                        testTag = "zone_com_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_LOW)) }
                    )
                    PixelToolCard(
                        emoji = "🏬",
                        name = "MED COM",
                        density = "Malls/Office",
                        cost = "$90",
                        accentColor = PixelColors.AccentBlue,
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_MED,
                        testTag = "zone_com_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_MED)) }
                    )
                    PixelToolCard(
                        emoji = "🏙",
                        name = "HIGH COM",
                        density = "Fin Towers",
                        cost = "$220",
                        accentColor = Color(0xFF1565C0),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.COMMERCIAL_HIGH,
                        testTag = "zone_com_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.COMMERCIAL_HIGH)) }
                    )
                }
                ZoneCategory.INDUSTRIAL -> {
                    PixelToolCard(
                        emoji = "🏭",
                        name = "LOW IND",
                        density = "Light Artisan",
                        cost = "$25",
                        accentColor = Color(0xFFFFD54F),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_LOW,
                        testTag = "zone_ind_low",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_LOW)) }
                    )
                    PixelToolCard(
                        emoji = "🏭",
                        name = "MED IND",
                        density = "Factory",
                        cost = "$75",
                        accentColor = PixelColors.AccentOrange,
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_MED,
                        testTag = "zone_ind_med",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_MED)) }
                    )
                    PixelToolCard(
                        emoji = "🏗",
                        name = "HIGH IND",
                        density = "Heavy Industry",
                        cost = "$180",
                        accentColor = Color(0xFFE65100),
                        isSelected = activeTool.mode == ToolMode.ZONE && activeTool.zoneType == ZoneType.INDUSTRIAL_HIGH,
                        testTag = "zone_ind_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.ZONE, zoneType = ZoneType.INDUSTRIAL_HIGH)) }
                    )
                }
                else -> {}
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ROADS SUBMENU
// -------------------------------------------------------------
@Composable
private fun RoadItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PixelToolCard(
            emoji = "🛣",
            name = "ROAD",
            density = "Standard 2-Lane",
            cost = "$25/tile",
            accentColor = Color(0xFF90A4AE),
            isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.SMALL_2L,
            testTag = "road_small",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.SMALL_2L)) }
        )
        PixelToolCard(
            emoji = "🛣",
            name = "AVENUE",
            density = "4-Lane High Capacity",
            cost = "$60/tile",
            accentColor = Color(0xFF78909C),
            isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.MEDIUM_4L,
            testTag = "road_avenue",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.MEDIUM_4L)) }
        )
        PixelToolCard(
            emoji = "🌴",
            name = "BOULEVARD",
            density = "Tree-Lined Land Value+",
            cost = "$120/tile",
            accentColor = Color(0xFF81C784),
            isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.LARGE_6L,
            testTag = "road_boulevard",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.LARGE_6L)) }
        )
        PixelToolCard(
            emoji = "🚗",
            name = "HIGHWAY",
            density = "Fast Express Lanes",
            cost = "$220/tile",
            accentColor = Color(0xFFFFB74D),
            isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.HIGHWAY,
            testTag = "road_highway",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.HIGHWAY)) }
        )
        PixelToolCard(
            emoji = "🌉",
            name = "BRIDGE",
            density = "Water Crossing",
            cost = "$90/tile",
            accentColor = Color(0xFF4DD0E1),
            isSelected = activeTool.mode == ToolMode.ROAD && activeTool.roadType == RoadType.BRIDGE,
            testTag = "road_bridge",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.ROAD, roadType = RoadType.BRIDGE)) }
        )
        PixelToolCard(
            emoji = "🚜",
            name = "BULLDOZE",
            density = "Clear Tile",
            cost = "$10",
            accentColor = PixelColors.AccentRed,
            isSelected = activeTool.mode == ToolMode.DEMOLISH,
            testTag = "tool_demolish",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.DEMOLISH)) }
        )
    }
}

// -------------------------------------------------------------
// 3. SERVICES SUBMENU
// -------------------------------------------------------------
@Composable
private fun ServiceItems(
    activeTool: ActiveTool,
    onSelectTool: (ActiveTool) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Safety") }

    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            CategoryTabChip("Safety", selectedCategory == "Safety", PixelColors.AccentRed) { selectedCategory = "Safety" }
            CategoryTabChip("Health", selectedCategory == "Health", Color(0xFF4DD0E1)) { selectedCategory = "Health" }
            CategoryTabChip("Education", selectedCategory == "Education", Color(0xFFBA68C8)) { selectedCategory = "Education" }
            CategoryTabChip("Civic/Parks", selectedCategory == "Civic", PixelColors.AccentGreen) { selectedCategory = "Civic" }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            when (selectedCategory) {
                "Safety" -> {
                    PixelToolCard(
                        emoji = "🚓",
                        name = "POLICE",
                        density = "Cuts Crime Rate",
                        cost = "$600",
                        accentColor = Color(0xFF29B6F6),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.POLICE_STATION,
                        testTag = "service_police",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.POLICE_STATION)) }
                    )
                    PixelToolCard(
                        emoji = "🚒",
                        name = "FIRE DEPT",
                        density = "Extinguishes Fires",
                        cost = "$600",
                        accentColor = PixelColors.AccentRed,
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.FIRE_STATION,
                        testTag = "service_fire",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.FIRE_STATION)) }
                    )
                }
                "Health" -> {
                    PixelToolCard(
                        emoji = "🏥",
                        name = "HOSPITAL",
                        density = "Boosts City Health",
                        cost = "$900",
                        accentColor = Color(0xFF4DD0E1),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.HOSPITAL,
                        testTag = "service_hospital",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.HOSPITAL)) }
                    )
                }
                "Education" -> {
                    PixelToolCard(
                        emoji = "🏫",
                        name = "ELEM SCHOOL",
                        density = "Basic Education",
                        cost = "$500",
                        accentColor = Color(0xFFBA68C8),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.ELEMENTARY_SCHOOL,
                        testTag = "service_school_elem",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.ELEMENTARY_SCHOOL)) }
                    )
                    PixelToolCard(
                        emoji = "🏫",
                        name = "HIGH SCHOOL",
                        density = "Secondary Education",
                        cost = "$1,100",
                        accentColor = Color(0xFFAB47BC),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.HIGH_SCHOOL,
                        testTag = "service_school_high",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.HIGH_SCHOOL)) }
                    )
                    PixelToolCard(
                        emoji = "🎓",
                        name = "UNIVERSITY",
                        density = "High-Tech Skills",
                        cost = "$2,800",
                        accentColor = Color(0xFF8E24AA),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.UNIVERSITY,
                        testTag = "service_university",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.UNIVERSITY)) }
                    )
                }
                "Civic" -> {
                    PixelToolCard(
                        emoji = "🌳",
                        name = "SMALL PARK",
                        density = "+Land Value/Joy",
                        cost = "$150",
                        accentColor = PixelColors.AccentGreen,
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.PARK_SMALL,
                        testTag = "service_park_small",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.PARK_SMALL)) }
                    )
                    PixelToolCard(
                        emoji = "⛲",
                        name = "CENTRAL PARK",
                        density = "Large Urban Oasis",
                        cost = "$650",
                        accentColor = Color(0xFF43A047),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.PARK_LARGE,
                        testTag = "service_park_large",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.PARK_LARGE)) }
                    )
                    PixelToolCard(
                        emoji = "⚰",
                        name = "CEMETERY",
                        density = "Civic Serenity",
                        cost = "$400",
                        accentColor = Color(0xFF78909C),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.CEMETERY,
                        testTag = "service_cemetery",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.CEMETERY)) }
                    )
                    PixelToolCard(
                        emoji = "🗑",
                        name = "GARBAGE PLANT",
                        density = "Waste Management",
                        cost = "$850",
                        accentColor = Color(0xFFFFB74D),
                        isSelected = activeTool.mode == ToolMode.SERVICE && activeTool.serviceType == ServiceType.GARBAGE_PLANT,
                        testTag = "service_garbage",
                        onClick = { onSelectTool(ActiveTool(mode = ToolMode.SERVICE, serviceType = ServiceType.GARBAGE_PLANT)) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. UTILITIES SUBMENU
// -------------------------------------------------------------
@Composable
private fun UtilityItems(
    activeTool: ActiveTool,
    stats: CityStats,
    onSelectTool: (ActiveTool) -> Unit
) {
    val powerDeficit = stats.powerCapacityMW < stats.powerDemandMW
    val waterDeficit = stats.waterCapacityMG < stats.waterDemandMG

    Column {
        // Live Grid Counters
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "⚡ Power: ${stats.powerDemandMW} / ${stats.powerCapacityMW} MW",
                color = if (powerDeficit) PixelColors.AccentRed else PixelColors.AccentGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "💧 Water: ${stats.waterDemandMG} / ${stats.waterCapacityMG} MG",
                color = if (waterDeficit) PixelColors.AccentRed else PixelColors.AccentCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelToolCard(
                emoji = "🌀",
                name = "WIND TURBINE",
                density = "+45 MW Clean",
                cost = "$450",
                accentColor = Color(0xFF81D4FA),
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.WIND_TURBINE,
                testTag = "util_wind",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.WIND_TURBINE)) }
            )
            PixelToolCard(
                emoji = "☀️",
                name = "SOLAR FARM",
                density = "+110 MW Clean",
                cost = "$950",
                accentColor = PixelColors.AccentGold,
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.SOLAR_PLANT,
                testTag = "util_solar",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.SOLAR_PLANT)) }
            )
            PixelToolCard(
                emoji = "⚡",
                name = "GAS PLANT",
                density = "+280 MW High",
                cost = "$2,000",
                accentColor = Color(0xFFFF9800),
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.GAS_PLANT,
                testTag = "util_gas",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.GAS_PLANT)) }
            )
            PixelToolCard(
                emoji = "🏭",
                name = "COAL PLANT",
                density = "+550 MW Mega",
                cost = "$3,500",
                accentColor = Color(0xFF5D4037),
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.COAL_PLANT,
                testTag = "util_coal",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.COAL_PLANT)) }
            )
            PixelToolCard(
                emoji = "💧",
                name = "WATER PUMP",
                density = "+180 MG (Shore)",
                cost = "$600",
                accentColor = PixelColors.AccentCyan,
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.WATER_PUMP,
                testTag = "util_water_pump",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.WATER_PUMP)) }
            )
            PixelToolCard(
                emoji = "🗼",
                name = "WATER TOWER",
                density = "+80 MG Storage",
                cost = "$350",
                accentColor = Color(0xFF42A5F5),
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.WATER_TOWER,
                testTag = "util_water_tower",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.WATER_TOWER)) }
            )
            PixelToolCard(
                emoji = "🚰",
                name = "SEWAGE PLANT",
                density = "Sanitation Facility",
                cost = "$800",
                accentColor = Color(0xFF26A69A),
                isSelected = activeTool.mode == ToolMode.UTILITY && activeTool.utilityType == UtilityType.SEWAGE_PLANT,
                testTag = "util_sewage",
                onClick = { onSelectTool(ActiveTool(mode = ToolMode.UTILITY, utilityType = UtilityType.SEWAGE_PLANT)) }
            )
        }
    }
}

// -------------------------------------------------------------
// 5. TRANSPORT SUBMENU
// -------------------------------------------------------------
@Composable
private fun TransportItems(
    activeTool: ActiveTool,
    stats: CityStats,
    onSelectTool: (ActiveTool) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PixelToolCard(
            emoji = "🚏",
            name = "BUS STOP",
            density = "Local Neighborhood",
            cost = "$120",
            accentColor = Color(0xFFFFA726),
            isSelected = activeTool.mode == ToolMode.TRANSPORT && activeTool.transportType == TransportType.BUS_STOP,
            testTag = "trans_bus_stop",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.TRANSPORT, transportType = TransportType.BUS_STOP)) }
        )
        PixelToolCard(
            emoji = "🚌",
            name = "BUS DEPOT",
            density = "Transit Hub",
            cost = "$900",
            accentColor = Color(0xFFFF9800),
            isSelected = activeTool.mode == ToolMode.TRANSPORT && activeTool.transportType == TransportType.BUS_DEPOT,
            testTag = "trans_bus_depot",
            onClick = { onSelectTool(ActiveTool(mode = ToolMode.TRANSPORT, transportType = TransportType.BUS_DEPOT)) }
        )

        val metroUnlocked = stats.population >= 1000
        PixelToolCard(
            emoji = if (metroUnlocked) "🚇" else "🔒",
            name = "METRO STATION",
            density = if (metroUnlocked) "High-Speed Rail" else "Requires 1k Pop",
            cost = "$4,000",
            accentColor = if (metroUnlocked) Color(0xFFAB47BC) else Color.Gray,
            isEnabled = metroUnlocked,
            isSelected = activeTool.mode == ToolMode.TRANSPORT && activeTool.transportType == TransportType.METRO_STATION,
            testTag = "trans_metro",
            onClick = {
                if (metroUnlocked) {
                    onSelectTool(ActiveTool(mode = ToolMode.TRANSPORT, transportType = TransportType.METRO_STATION))
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 6. MORE SUBMENU (Overlays, Economy, Stats, Settings)
// -------------------------------------------------------------
@Composable
private fun MoreItems(
    activeOverlay: OverlayMode,
    onSelectOverlay: (OverlayMode) -> Unit,
    onOpenCityOverview: () -> Unit,
    onOpenEconomy: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showOverlaysSub by remember { mutableStateOf(false) }

    if (!showOverlaysSub) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelToolCard(
                emoji = "🏙",
                name = "CITY INFO",
                density = "Citizen Metrics",
                cost = "Overview",
                accentColor = PixelColors.AccentCyan,
                testTag = "more_city_info",
                onClick = onOpenCityOverview
            )
            PixelToolCard(
                emoji = "💰",
                name = "ECONOMY",
                density = "Taxes & Budget",
                cost = "Finance",
                accentColor = PixelColors.AccentGreen,
                testTag = "more_economy",
                onClick = onOpenEconomy
            )
            PixelToolCard(
                emoji = "📊",
                name = "STATISTICS",
                density = "Historical Charts",
                cost = "Data",
                accentColor = PixelColors.AccentBlue,
                testTag = "more_stats",
                onClick = onOpenStats
            )
            PixelToolCard(
                emoji = "🗺",
                name = "OVERLAYS",
                density = "Map Heatmaps",
                cost = "View",
                accentColor = Color(0xFF00ACC1),
                testTag = "more_overlays",
                onClick = { showOverlaysSub = true }
            )
            PixelToolCard(
                emoji = "⚙",
                name = "SETTINGS",
                density = "Save, Sound, FX",
                cost = "Options",
                accentColor = Color(0xFFB0BEC5),
                testTag = "more_settings",
                onClick = onOpenSettings
            )
        }
    } else {
        Column {
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← BACK TO MORE",
                    color = PixelColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { showOverlaysSub = false }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OverlayCard("Normal", "Standard View", activeOverlay == OverlayMode.NORMAL, Color(0xFF90A4AE)) { onSelectOverlay(OverlayMode.NORMAL) }
                OverlayCard("Traffic", "Congestion Map", activeOverlay == OverlayMode.TRAFFIC, Color(0xFFFFB74D)) { onSelectOverlay(OverlayMode.TRAFFIC) }
                OverlayCard("Pollution", "Air Quality", activeOverlay == OverlayMode.POLLUTION, Color(0xFFE57373)) { onSelectOverlay(OverlayMode.POLLUTION) }
                OverlayCard("Land Value", "Wealth Heatmap", activeOverlay == OverlayMode.LAND_VALUE, Color(0xFFFFD54F)) { onSelectOverlay(OverlayMode.LAND_VALUE) }
                OverlayCard("Power Grid", "Electric Coverage", activeOverlay == OverlayMode.POWER, Color(0xFFFFCA28)) { onSelectOverlay(OverlayMode.POWER) }
                OverlayCard("Water Pipes", "Water Coverage", activeOverlay == OverlayMode.WATER, Color(0xFF4DD0E1)) { onSelectOverlay(OverlayMode.WATER) }
                OverlayCard("Services", "Police/Fire/Hosp", activeOverlay == OverlayMode.SERVICES, Color(0xFFAB47BC)) { onSelectOverlay(OverlayMode.SERVICES) }
            }
        }
    }
}

@Composable
private fun OverlayCard(
    name: String,
    desc: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) color.copy(alpha = 0.3f) else Color(0xFF0F1E30))
            .border(BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFF1E3A5F)), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(name, color = if (isSelected) Color.White else color, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
            Text(desc, color = Color(0xFF90A4AE), fontSize = 8.5.sp)
        }
    }
}

@Composable
private fun CategoryTabChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) color.copy(alpha = 0.35f) else Color(0xFF0C1929))
            .border(BorderStroke(1.dp, if (isSelected) color else Color(0xFF1B324D)), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFFB0BEC5),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun PixelToolCard(
    emoji: String,
    name: String,
    density: String,
    cost: String,
    accentColor: Color,
    isSelected: Boolean = false,
    isEnabled: Boolean = true,
    testTag: String = "",
    onClick: () -> Unit
) {
    PixelButton(
        onClick = onClick,
        isSelected = isSelected,
        selectedColor = accentColor,
        enabled = isEnabled,
        modifier = Modifier
            .width(84.dp)
            .height(58.dp)
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(text = emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = name,
                color = if (isSelected) Color.White else Color(0xFFECEFF1),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = cost,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontSize = 8.5.sp
            )
        }
    }
}
