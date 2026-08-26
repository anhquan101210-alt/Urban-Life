package com.example.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.engine.TrafficEngine
import com.example.game.engine.WorldMap
import com.example.game.model.*
import kotlin.math.*

class Game3DRenderer(
    val camera: Camera3D
) {
    private var animTick = 0f

    fun render(
        drawScope: DrawScope,
        world: WorldMap,
        traffic: TrafficEngine,
        stats: CityStats,
        overlay: OverlayMode,
        graphicsQuality: GraphicsQuality,
        hoverTile: Pair<Int, Int>?,
        activeTool: ActiveTool
    ) {
        animTick += 0.05f

        val lighting = calculateDayNightLighting(stats.dayTime)

        // Clear background with sky color
        drawScope.drawRect(color = lighting.skyColor)

        // 1. Collect all drawable items with depth for Painter's Algorithm (Back-to-Front sorting)
        val renderItems = mutableListOf<RenderItem>()

        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val depth = camera.getDepth(x.toFloat() + 0.5f, y.toFloat() + 0.5f)
                renderItems.add(RenderItem(x, y, depth, tile))
            }
        }

        // Sort items so furthest tiles are drawn first
        renderItems.sortBy { it.depth }

        // 2. Render ground tiles & roads & base zoning
        for (item in renderItems) {
            val tile = item.tile
            val x = item.x
            val y = item.y

            drawGroundTile(drawScope, tile, x, y, lighting, overlay)
        }

        // 3. Render 3D structures, trees, buildings, services, utilities, and vehicles
        for (item in renderItems) {
            val tile = item.tile
            val x = item.x
            val y = item.y

            // Draw trees
            if (tile.treeType > 0 && tile.building == null && tile.road == RoadType.NONE && tile.service == null && tile.utility == null) {
                draw3DTree(drawScope, x, y, tile.treeType, tile.elevation, lighting)
            }

            // Draw Services
            tile.service?.let { s ->
                draw3DService(drawScope, x, y, s, tile.elevation, lighting)
            }

            // Draw Utilities
            tile.utility?.let { u ->
                draw3DUtility(drawScope, x, y, u, tile.elevation, lighting, animTick)
            }

            // Draw Buildings
            tile.building?.let { b ->
                draw3DBuilding(drawScope, x, y, b, tile.elevation, lighting, stats.dayTime, graphicsQuality)
            }
        }

        // 4. Draw Vehicles on top of roads
        if (graphicsQuality != GraphicsQuality.LOW || traffic.vehicles.size <= 25) {
            for (v in traffic.vehicles) {
                draw3DVehicle(drawScope, v, lighting, stats.dayTime)
            }
        }

        // 5. Draw active tool preview / hover indicator
        hoverTile?.let { (hx, hy) ->
            if (world.isInside(hx, hy)) {
                drawHoverCursor(drawScope, hx, hy, world.tiles[hx][hy].elevation, activeTool)
            }
        }
    }

    private fun drawGroundTile(
        drawScope: DrawScope,
        tile: GridTile,
        x: Int,
        y: Int,
        lighting: DayNightLighting,
        overlay: OverlayMode
    ) {
        val elev = tile.elevation
        val p00 = camera.project(x.toFloat(), y.toFloat(), elev)
        val p10 = camera.project(x + 1f, y.toFloat(), elev)
        val p11 = camera.project(x + 1f, y + 1f, elev)
        val p01 = camera.project(x.toFloat(), y + 1f, elev)

        val polyPath = Path().apply {
            moveTo(p00.x, p00.y)
            lineTo(p10.x, p10.y)
            lineTo(p11.x, p11.y)
            lineTo(p01.x, p01.y)
            close()
        }

        var baseColor = when (tile.terrain) {
            TerrainType.WATER -> {
                val wave = sin(animTick + x * 0.8f + y * 0.6f) * 0.05f
                Color(0xFF1E88E5).copy(
                    red = (0.12f + wave).coerceIn(0f, 1f),
                    green = (0.50f + wave).coerceIn(0f, 1f),
                    blue = 0.85f
                )
            }
            TerrainType.SHORE -> Color(0xFFE5D4A3)
            TerrainType.HILL -> Color(0xFF5B8C5A)
            TerrainType.PLAINS -> Color(0xFF6DA368)
            TerrainType.FOREST -> Color(0xFF3B6B38)
        }

        // Apply Overlays if active
        if (overlay != OverlayMode.NORMAL) {
            baseColor = when (overlay) {
                OverlayMode.ZONES -> when (tile.zone.category) {
                    ZoneCategory.RESIDENTIAL -> when (tile.zone.density) {
                        DensityLevel.LOW -> Color(0xFF81C784)
                        DensityLevel.MEDIUM -> Color(0xFF4CAF50)
                        DensityLevel.HIGH -> Color(0xFF2E7D32)
                        DensityLevel.NONE -> baseColor
                    }
                    ZoneCategory.COMMERCIAL -> when (tile.zone.density) {
                        DensityLevel.LOW -> Color(0xFF64B5F6)
                        DensityLevel.MEDIUM -> Color(0xFF2196F3)
                        DensityLevel.HIGH -> Color(0xFF1565C0)
                        DensityLevel.NONE -> baseColor
                    }
                    ZoneCategory.INDUSTRIAL -> when (tile.zone.density) {
                        DensityLevel.LOW -> Color(0xFFFFEE58)
                        DensityLevel.MEDIUM -> Color(0xFFFFCA28)
                        DensityLevel.HIGH -> Color(0xFFF57F17)
                        DensityLevel.NONE -> baseColor
                    }
                    ZoneCategory.NONE -> baseColor
                }
                OverlayMode.TRAFFIC -> if (tile.road != RoadType.NONE) {
                    when {
                        tile.trafficVolume > 0.7f -> Color(0xFFE53935)
                        tile.trafficVolume > 0.4f -> Color(0xFFFFB300)
                        else -> Color(0xFF43A047)
                    }
                } else baseColor
                OverlayMode.POLLUTION -> {
                    val pRatio = (tile.airPollution / 100f).coerceIn(0f, 1f)
                    if (pRatio > 0.05f) {
                        Color(0xFF8E24AA).copy(alpha = pRatio * 0.7f)
                    } else baseColor
                }
                OverlayMode.LAND_VALUE -> {
                    val lvRatio = (tile.landValue / 100f).coerceIn(0f, 1f)
                    Color(0xFFFFD54F).copy(alpha = 0.2f + lvRatio * 0.7f)
                }
                OverlayMode.POWER -> {
                    if (tile.building?.isPowered == true || tile.utility?.category == UtilityCategory.POWER) {
                        Color(0xFF00E5FF).copy(alpha = 0.6f)
                    } else if (tile.building != null && !tile.building!!.isPowered) {
                        Color(0xFFFF1744).copy(alpha = 0.7f)
                    } else baseColor
                }
                OverlayMode.WATER -> {
                    if (tile.building?.isWatered == true || tile.utility?.category == UtilityCategory.WATER) {
                        Color(0xFF00B0FF).copy(alpha = 0.6f)
                    } else if (tile.building != null && !tile.building!!.isWatered) {
                        Color(0xFFFF5252).copy(alpha = 0.7f)
                    } else baseColor
                }
                OverlayMode.SERVICES -> {
                    val totalCoverage = (tile.policeCoverage + tile.fireCoverage + tile.healthCoverage + tile.educationCoverage + tile.parkCoverage).coerceIn(0, 100) / 100f
                    Color(0xFF00E676).copy(alpha = 0.1f + totalCoverage * 0.7f)
                }
                OverlayMode.NORMAL -> baseColor
            }
        }

        // Blend with day/night light
        val finalColor = blendWithLighting(baseColor, lighting)
        drawScope.drawPath(polyPath, color = finalColor, style = Fill)

        // Subtle grid line
        drawScope.drawPath(polyPath, color = Color.Black.copy(alpha = 0.08f), style = Stroke(width = 0.8f))

        // Draw Road if present
        if (tile.road != RoadType.NONE) {
            drawRoadSegment(drawScope, tile, x, y, elev, lighting)
        }

        // Draw Zone Border Outlines in normal mode
        if (overlay == OverlayMode.NORMAL && tile.zone != ZoneType.NONE && tile.building == null) {
            val zoneColor = when (tile.zone.category) {
                ZoneCategory.RESIDENTIAL -> Color(0xFF4CAF50)
                ZoneCategory.COMMERCIAL -> Color(0xFF2196F3)
                ZoneCategory.INDUSTRIAL -> Color(0xFFFFC107)
                ZoneCategory.NONE -> Color.Transparent
            }
            drawScope.drawPath(polyPath, color = zoneColor.copy(alpha = 0.35f), style = Fill)
            drawScope.drawPath(polyPath, color = zoneColor, style = Stroke(width = 1.5f))
        }
    }

    private fun drawRoadSegment(
        drawScope: DrawScope,
        tile: GridTile,
        x: Int,
        y: Int,
        elevation: Float,
        lighting: DayNightLighting
    ) {
        val roadType = tile.road
        val roadColor = if (roadType == RoadType.BRIDGE) Color(0xFFB0BEC5) else Color(0xFF37474F)
        val blendedRoad = blendWithLighting(roadColor, lighting)

        val p00 = camera.project(x.toFloat(), y.toFloat(), elevation)
        val p10 = camera.project(x + 1f, y.toFloat(), elevation)
        val p11 = camera.project(x + 1f, y + 1f, elevation)
        val p01 = camera.project(x.toFloat(), y + 1f, elevation)

        val roadPath = Path().apply {
            moveTo(p00.x, p00.y)
            lineTo(p10.x, p10.y)
            lineTo(p11.x, p11.y)
            lineTo(p01.x, p01.y)
            close()
        }
        drawScope.drawPath(roadPath, color = blendedRoad, style = Fill)

        // Center markings
        val mid0 = camera.project(x + 0.5f, y.toFloat(), elevation)
        val mid1 = camera.project(x + 0.5f, y + 1f, elevation)
        val markColor = if (roadType == RoadType.MEDIUM_4L || roadType == RoadType.LARGE_6L) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.7f)

        drawScope.drawLine(
            color = markColor,
            start = mid0,
            end = mid1,
            strokeWidth = if (roadType == RoadType.LARGE_6L) 2.5f else 1.2f
        )
    }

    private fun draw3DBuilding(
        drawScope: DrawScope,
        x: Int,
        y: Int,
        b: Building,
        baseElevation: Float,
        lighting: DayNightLighting,
        dayTime: Float,
        graphicsQuality: GraphicsQuality
    ) {
        val isNight = dayTime !in 6.0f..18.5f

        // Height based on Density & Level
        val height3D = when (b.zoneType.density) {
            DensityLevel.LOW -> when (b.level) {
                1 -> 0.45f
                2 -> 0.75f
                else -> 1.05f
            }
            DensityLevel.MEDIUM -> when (b.level) {
                1 -> 1.10f
                2 -> 1.80f
                else -> 2.60f
            }
            DensityLevel.HIGH -> when (b.level) {
                1 -> 2.40f
                2 -> 4.20f
                else -> 6.80f
            }
            DensityLevel.NONE -> 0.2f
        }

        val actualHeight = if (b.stage == BuildingStage.FOUNDATION) 0.15f
        else if (b.stage == BuildingStage.CONSTRUCTION) height3D * 0.5f
        else height3D

        val inset = when (b.zoneType.density) {
            DensityLevel.LOW -> 0.18f
            DensityLevel.MEDIUM -> 0.10f
            DensityLevel.HIGH -> 0.05f
            DensityLevel.NONE -> 0.2f
        }

        val fx0 = x + inset
        val fy0 = y + inset
        val fx1 = x + 1f - inset
        val fy1 = y + 1f - inset

        val topZ = baseElevation + actualHeight
        val botZ = baseElevation

        // Base 3D colors
        val (wallTopColor, wallLeftColor, wallRightColor) = when (b.stage) {
            BuildingStage.FOUNDATION -> Triple(Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFF5D4037))
            BuildingStage.CONSTRUCTION -> Triple(Color(0xFFFFB300), Color(0xFFFFA000), Color(0xFFFF8F00))
            BuildingStage.ABANDONED -> Triple(Color(0xFF424242), Color(0xFF303030), Color(0xFF212121))
            BuildingStage.BUILT -> getBuildingPalette(b.zoneType, b.level, b.colorSeed)
            BuildingStage.EMPTY -> Triple(Color.Transparent, Color.Transparent, Color.Transparent)
        }

        // Draw 3D Box Prism
        draw3DPrism(
            drawScope = drawScope,
            x0 = fx0, y0 = fy0,
            x1 = fx1, y1 = fy1,
            z0 = botZ, z1 = topZ,
            topColor = blendWithLighting(wallTopColor, lighting),
            leftColor = blendWithLighting(wallLeftColor, lighting),
            rightColor = blendWithLighting(wallRightColor, lighting)
        )

        // Windows illuminated at night
        if (isNight && b.stage == BuildingStage.BUILT && b.isPowered && graphicsQuality != GraphicsQuality.LOW) {
            drawIlluminatedWindows(drawScope, fx0, fy0, fx1, fy1, botZ, topZ, b.zoneType.density)
        }

        // Fire & Smoke on active disaster
        if (b.onFire) {
            val centerTop = camera.project(x + 0.5f, y + 0.5f, topZ)
            drawScope.drawCircle(
                color = Color(0xFFFF3D00).copy(alpha = 0.85f),
                radius = 12f + sin(animTick * 3f) * 4f,
                center = centerTop
            )
            drawScope.drawCircle(
                color = Color(0xFFFFEA00).copy(alpha = 0.9f),
                radius = 7f + sin(animTick * 4f) * 2f,
                center = centerTop
            )
        }
    }

    private fun draw3DService(
        drawScope: DrawScope,
        x: Int,
        y: Int,
        s: ServiceType,
        baseElevation: Float,
        lighting: DayNightLighting
    ) {
        val fx0 = x + 0.12f
        val fy0 = y + 0.12f
        val fx1 = x + 0.88f
        val fy1 = y + 0.88f
        val height = when (s.category) {
            ServiceCategory.POLICE -> 1.4f
            ServiceCategory.FIRE -> 1.3f
            ServiceCategory.HEALTH -> 2.0f
            ServiceCategory.EDUCATION -> if (s == ServiceType.UNIVERSITY) 2.5f else 1.2f
            ServiceCategory.PARK -> 0.15f
            ServiceCategory.DEATH_CARE -> 0.3f
            ServiceCategory.GARBAGE -> 0.9f
        }

        val (topColor, leftColor, rightColor) = when (s.category) {
            ServiceCategory.POLICE -> Triple(Color(0xFF1976D2), Color(0xFF1565C0), Color(0xFF0D47A1))
            ServiceCategory.FIRE -> Triple(Color(0xFFD32F2F), Color(0xFFC62828), Color(0xFFB71C1C))
            ServiceCategory.HEALTH -> Triple(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFBDBDBD))
            ServiceCategory.EDUCATION -> Triple(Color(0xFFFF9800), Color(0xFFF57C00), Color(0xFFE65100))
            ServiceCategory.PARK -> Triple(Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF2E7D32))
            ServiceCategory.DEATH_CARE -> Triple(Color(0xFF78909C), Color(0xFF607D8B), Color(0xFF455A64))
            ServiceCategory.GARBAGE -> Triple(Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF4E342E))
        }

        draw3DPrism(
            drawScope = drawScope,
            x0 = fx0, y0 = fy0,
            x1 = fx1, y1 = fy1,
            z0 = baseElevation, z1 = baseElevation + height,
            topColor = blendWithLighting(topColor, lighting),
            leftColor = blendWithLighting(leftColor, lighting),
            rightColor = blendWithLighting(rightColor, lighting)
        )

        // Hospital Red Cross emblem
        if (s.category == ServiceCategory.HEALTH) {
            val center = camera.project(x + 0.5f, y + 0.5f, baseElevation + height)
            drawScope.drawLine(Color.Red, Offset(center.x - 8f, center.y), Offset(center.x + 8f, center.y), strokeWidth = 3f)
            drawScope.drawLine(Color.Red, Offset(center.x, center.y - 8f), Offset(center.x, center.y + 8f), strokeWidth = 3f)
        }
    }

    private fun draw3DUtility(
        drawScope: DrawScope,
        x: Int,
        y: Int,
        u: UtilityType,
        baseElevation: Float,
        lighting: DayNightLighting,
        anim: Float
    ) {
        val centerBase = camera.project(x + 0.5f, y + 0.5f, baseElevation)
        val centerTop = camera.project(x + 0.5f, y + 0.5f, baseElevation + 1.8f)

        when (u) {
            UtilityType.WIND_TURBINE -> {
                // Tower pole
                drawScope.drawLine(
                    color = blendWithLighting(Color.White, lighting),
                    start = centerBase,
                    end = centerTop,
                    strokeWidth = 3.5f
                )
                // 3 spinning blades
                val bladeLen = 16f
                for (i in 0..2) {
                    val angle = anim * 2.5f + (i * 2f * PI.toFloat() / 3f)
                    val bx = centerTop.x + cos(angle) * bladeLen
                    val by = centerTop.y + sin(angle) * bladeLen
                    drawScope.drawLine(Color.White, centerTop, Offset(bx, by), strokeWidth = 2.0f)
                }
            }
            UtilityType.SOLAR_PLANT -> {
                draw3DPrism(
                    drawScope, x + 0.1f, y + 0.1f, x + 0.9f, y + 0.9f,
                    baseElevation, baseElevation + 0.25f,
                    blendWithLighting(Color(0xFF1A237E), lighting),
                    blendWithLighting(Color(0xFF0D47A1), lighting),
                    blendWithLighting(Color(0xFF01579B), lighting)
                )
            }
            UtilityType.COAL_PLANT, UtilityType.GAS_PLANT -> {
                draw3DPrism(
                    drawScope, x + 0.15f, y + 0.15f, x + 0.85f, y + 0.85f,
                    baseElevation, baseElevation + 1.6f,
                    blendWithLighting(Color(0xFF424242), lighting),
                    blendWithLighting(Color(0xFF303030), lighting),
                    blendWithLighting(Color(0xFF212121), lighting)
                )
                // Smokestack smoke
                val smokeCenter = Offset(centerTop.x + sin(anim * 1.5f) * 4f, centerTop.y - 12f)
                drawScope.drawCircle(Color.Gray.copy(alpha = 0.4f), radius = 8f, center = smokeCenter)
            }
            UtilityType.WATER_PUMP, UtilityType.WATER_TOWER, UtilityType.SEWAGE_PLANT -> {
                draw3DPrism(
                    drawScope, x + 0.2f, y + 0.2f, x + 0.8f, y + 0.8f,
                    baseElevation, baseElevation + 1.2f,
                    blendWithLighting(Color(0xFF0288D1), lighting),
                    blendWithLighting(Color(0xFF0277BD), lighting),
                    blendWithLighting(Color(0xFF01579B), lighting)
                )
            }
        }
    }

    private fun draw3DTree(
        drawScope: DrawScope,
        x: Int,
        y: Int,
        treeType: Int,
        baseElevation: Float,
        lighting: DayNightLighting
    ) {
        val root = camera.project(x + 0.5f, y + 0.5f, baseElevation)
        val top = camera.project(x + 0.5f, y + 0.5f, baseElevation + 0.65f)

        // Trunk
        drawScope.drawLine(
            color = blendWithLighting(Color(0xFF5D4037), lighting),
            start = root,
            end = top,
            strokeWidth = 2.5f
        )

        // Foliage
        val leafColor = if (treeType == 1) Color(0xFF2E7D32) else if (treeType == 2) Color(0xFF388E3C) else Color(0xFF1B5E20)
        drawScope.drawCircle(
            color = blendWithLighting(leafColor, lighting),
            radius = 7.5f,
            center = top
        )
    }

    private fun draw3DVehicle(
        drawScope: DrawScope,
        v: Vehicle,
        lighting: DayNightLighting,
        dayTime: Float
    ) {
        val isNight = dayTime !in 6.0f..18.5f
        val pos = camera.project(v.x, v.y, 0.05f)

        val vColor = Color(v.type.colorHex)
        val size = if (v.type == VehicleType.BUS || v.type == VehicleType.TRUCK) 5.5f else 4.0f

        drawScope.drawCircle(
            color = blendWithLighting(vColor, lighting),
            radius = size,
            center = pos
        )

        // Headlights at night
        if (isNight) {
            val headingX = cos(v.angle) * 14f
            val headingY = sin(v.angle) * 14f
            val headlightPos = Offset(pos.x + headingX, pos.y + headingY)

            drawScope.drawLine(
                color = Color(0xFFFFF9C4).copy(alpha = 0.6f),
                start = pos,
                end = headlightPos,
                strokeWidth = 3.5f
            )

            // Flashing emergency sirens
            if (v.isEmergencyMission) {
                val sirenColor = if (sin(animTick * 8f) > 0) Color.Red else Color.Blue
                drawScope.drawCircle(color = sirenColor, radius = 6f, center = pos)
            }
        }
    }

    private fun drawHoverCursor(
        drawScope: DrawScope,
        x: Int,
        y: Int,
        elevation: Float,
        tool: ActiveTool
    ) {
        val p00 = camera.project(x.toFloat(), y.toFloat(), elevation)
        val p10 = camera.project(x + 1f, y.toFloat(), elevation)
        val p11 = camera.project(x + 1f, y + 1f, elevation)
        val p01 = camera.project(x.toFloat(), y + 1f, elevation)

        val cursorColor = when (tool.mode) {
            ToolMode.DEMOLISH -> Color(0xFFFF1744)
            ToolMode.ROAD -> Color(0xFF00E5FF)
            ToolMode.ZONE -> when (tool.zoneType.category) {
                ZoneCategory.RESIDENTIAL -> Color(0xFF00E676)
                ZoneCategory.COMMERCIAL -> Color(0xFF2979FF)
                ZoneCategory.INDUSTRIAL -> Color(0xFFFFD600)
                ZoneCategory.NONE -> Color.White
            }
            ToolMode.SERVICE, ToolMode.UTILITY, ToolMode.TRANSPORT -> Color(0xFF76FF03)
            ToolMode.INSPECT -> Color.White
        }

        val polyPath = Path().apply {
            moveTo(p00.x, p00.y)
            lineTo(p10.x, p10.y)
            lineTo(p11.x, p11.y)
            lineTo(p01.x, p01.y)
            close()
        }

        drawScope.drawPath(polyPath, color = cursorColor.copy(alpha = 0.35f), style = Fill)
        drawScope.drawPath(polyPath, color = cursorColor, style = Stroke(width = 2.5f))
    }

    private fun draw3DPrism(
        drawScope: DrawScope,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        z0: Float, z1: Float,
        topColor: Color,
        leftColor: Color,
        rightColor: Color
    ) {
        // 8 vertices in 3D
        val b00 = camera.project(x0, y0, z0)
        val b10 = camera.project(x1, y0, z0)
        val b11 = camera.project(x1, y1, z0)
        val b01 = camera.project(x0, y1, z0)

        val t00 = camera.project(x0, y0, z1)
        val t10 = camera.project(x1, y0, z1)
        val t11 = camera.project(x1, y1, z1)
        val t01 = camera.project(x0, y1, z1)

        // Right / South Wall
        val rightWall = Path().apply {
            moveTo(b10.x, b10.y)
            lineTo(b11.x, b11.y)
            lineTo(t11.x, t11.y)
            lineTo(t10.x, t10.y)
            close()
        }
        drawScope.drawPath(rightWall, color = rightColor, style = Fill)

        // Left / West Wall
        val leftWall = Path().apply {
            moveTo(b01.x, b01.y)
            lineTo(b11.x, b11.y)
            lineTo(t11.x, t11.y)
            lineTo(t01.x, t01.y)
            close()
        }
        drawScope.drawPath(leftWall, color = leftColor, style = Fill)

        // Top Roof
        val roof = Path().apply {
            moveTo(t00.x, t00.y)
            lineTo(t10.x, t10.y)
            lineTo(t11.x, t11.y)
            lineTo(t01.x, t01.y)
            close()
        }
        drawScope.drawPath(roof, color = topColor, style = Fill)
    }

    private fun drawIlluminatedWindows(
        drawScope: DrawScope,
        x0: Float, y0: Float,
        x1: Float, y1: Float,
        z0: Float, z1: Float,
        density: DensityLevel
    ) {
        val windowColor = Color(0xFFFFF176).copy(alpha = 0.85f)
        val stories = when (density) {
            DensityLevel.LOW -> 2
            DensityLevel.MEDIUM -> 5
            DensityLevel.HIGH -> 12
            DensityLevel.NONE -> 1
        }

        val stepZ = (z1 - z0) / stories
        for (i in 1 until stories) {
            val wz = z0 + i * stepZ
            val wp = camera.project((x0 + x1) / 2f, y1, wz)
            drawScope.drawCircle(color = windowColor, radius = 2.0f, center = wp)
        }
    }

    private fun getBuildingPalette(zone: ZoneType, level: Int, seed: Int): Triple<Color, Color, Color> {
        return when (zone.category) {
            ZoneCategory.RESIDENTIAL -> when (zone.density) {
                DensityLevel.LOW -> when (level) {
                    1 -> Triple(Color(0xFFE57373), Color(0xFFD32F2F), Color(0xFFC62828)) // Red cottage
                    2 -> Triple(Color(0xFF81C784), Color(0xFF388E3C), Color(0xFF2E7D32)) // Green villa
                    else -> Triple(Color(0xFFE0E0E0), Color(0xFF9E9E9E), Color(0xFF757575)) // Modern white estate
                }
                DensityLevel.MEDIUM -> when (level) {
                    1 -> Triple(Color(0xFFFFB74D), Color(0xFFF57C00), Color(0xFFE65100))
                    2 -> Triple(Color(0xFF90CAF9), Color(0xFF1976D2), Color(0xFF0D47A1))
                    else -> Triple(Color(0xFFBA68C8), Color(0xFF7B1FA2), Color(0xFF4A148C))
                }
                DensityLevel.HIGH -> when (level) {
                    1 -> Triple(Color(0xFF4DD0E1), Color(0xFF0097A7), Color(0xFF006064))
                    2 -> Triple(Color(0xFF80DEEA), Color(0xFF00ACC1), Color(0xFF00838F))
                    else -> Triple(Color(0xFFE0F7FA), Color(0xFF26C6DA), Color(0xFF00838F)) // Crystal tower
                }
                DensityLevel.NONE -> Triple(Color.Gray, Color.DarkGray, Color.Black)
            }
            ZoneCategory.COMMERCIAL -> when (zone.density) {
                DensityLevel.LOW -> Triple(Color(0xFF64B5F6), Color(0xFF1E88E5), Color(0xFF1565C0))
                DensityLevel.MEDIUM -> Triple(Color(0xFF42A5F5), Color(0xFF1565C0), Color(0xFF0D47A1))
                DensityLevel.HIGH -> Triple(Color(0xFF29B6F6), Color(0xFF0288D1), Color(0xFF01579B))
                DensityLevel.NONE -> Triple(Color.Gray, Color.DarkGray, Color.Black)
            }
            ZoneCategory.INDUSTRIAL -> when (zone.density) {
                DensityLevel.LOW -> Triple(Color(0xFFFFD54F), Color(0xFFFFA000), Color(0xFFFF8F00))
                DensityLevel.MEDIUM -> Triple(Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFFF6F00))
                DensityLevel.HIGH -> Triple(Color(0xFF78909C), Color(0xFF546E7A), Color(0xFF37474F))
                DensityLevel.NONE -> Triple(Color.Gray, Color.DarkGray, Color.Black)
            }
            ZoneCategory.NONE -> Triple(Color.Gray, Color.DarkGray, Color.Black)
        }
    }

    private fun calculateDayNightLighting(dayTime: Float): DayNightLighting {
        return when {
            // Dawn (05:00 - 08:00)
            dayTime in 5.0f..8.0f -> {
                val t = (dayTime - 5.0f) / 3.0f
                DayNightLighting(
                    skyColor = lerpColor(Color(0xFF1A1A2E), Color(0xFF87CEEB), t),
                    lightMultiplier = 0.5f + t * 0.5f,
                    tintColor = Color(0xFFFFCC80)
                )
            }
            // Daytime (08:00 - 17:00)
            dayTime in 8.0f..17.0f -> {
                DayNightLighting(
                    skyColor = Color(0xFF87CEEB),
                    lightMultiplier = 1.0f,
                    tintColor = Color.White
                )
            }
            // Dusk / Sunset (17:00 - 20:00)
            dayTime in 17.0f..20.0f -> {
                val t = (dayTime - 17.0f) / 3.0f
                DayNightLighting(
                    skyColor = lerpColor(Color(0xFF87CEEB), Color(0xFF0F172A), t),
                    lightMultiplier = 1.0f - t * 0.65f,
                    tintColor = Color(0xFFFF7043)
                )
            }
            // Night (20:00 - 05:00)
            else -> {
                DayNightLighting(
                    skyColor = Color(0xFF0A0F1D),
                    lightMultiplier = 0.35f,
                    tintColor = Color(0xFF90CAF9)
                )
            }
        }
    }

    private fun blendWithLighting(color: Color, lighting: DayNightLighting): Color {
        return Color(
            red = (color.red * lighting.lightMultiplier * lighting.tintColor.red).coerceIn(0f, 1f),
            green = (color.green * lighting.lightMultiplier * lighting.tintColor.green).coerceIn(0f, 1f),
            blue = (color.blue * lighting.lightMultiplier * lighting.tintColor.blue).coerceIn(0f, 1f),
            alpha = color.alpha
        )
    }

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        return Color(
            red = c1.red + (c2.red - c1.red) * t,
            green = c1.green + (c2.green - c1.green) * t,
            blue = c1.blue + (c2.blue - c1.blue) * t,
            alpha = 1f
        )
    }

    private data class RenderItem(
        val x: Int,
        val y: Int,
        val depth: Float,
        val tile: GridTile
    )

    private data class DayNightLighting(
        val skyColor: Color,
        val lightMultiplier: Float,
        val tintColor: Color
    )
}
