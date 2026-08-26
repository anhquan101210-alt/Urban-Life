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

/**
 * High-Performance 2D Pixel-Art Isometric City Builder Renderer.
 * Inspired by classic isometric pixel city builders (e.g. TheoTown style).
 * Features sharp pixel aesthetics, auto-tiling roads, smooth shoreline transitions,
 * animated water & smoke, 3-tier building densities, animated vehicles & pedestrians,
 * pixel weather & day/night lighting overlays.
 */
class Game3DRenderer(
    val camera: Camera3D
) {
    private var animTick = 0f
    private var weatherTick = 0f

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
        animTick += 0.08f
        weatherTick += 0.15f

        val lighting = calculateDayNightLighting(stats.dayTime)

        // Clear background with sky / void color
        drawScope.drawRect(color = lighting.skyColor)

        val tileW = camera.baseTileWidth * camera.zoom
        val tileH = camera.baseTileHeight * camera.zoom
        val halfW = tileW / 2f
        val halfH = tileH / 2f

        // 1. Viewport Culling & Sorting (Isometric Painter's Algorithm: sort by x + y)
        val renderTiles = mutableListOf<GridTile>()
        for (x in 0 until world.width) {
            for (y in 0 until world.height) {
                val tile = world.tiles[x][y]
                val screenPos = camera.project(x.toFloat(), y.toFloat())
                // Cull tiles outside screen boundaries
                if (screenPos.x >= -tileW * 2 && screenPos.x <= camera.viewportWidth + tileW * 2 &&
                    screenPos.y >= -tileH * 4 && screenPos.y <= camera.viewportHeight + tileH * 4
                ) {
                    renderTiles.add(tile)
                }
            }
        }

        // Sort back-to-front by isometric depth
        renderTiles.sortBy { it.x + it.y }

        // 2. Pass 1: Render Terrain, Smooth Shorelines, Water & Ground Overlays
        for (tile in renderTiles) {
            drawPixelTerrainTile(drawScope, world, tile, tileW, tileH, lighting, overlay)
        }

        // 3. Pass 2: Render Roads with Auto-Tiling & Markings
        for (tile in renderTiles) {
            if (tile.road != RoadType.NONE) {
                drawPixelRoadTile(drawScope, world, tile, tileW, tileH, lighting, overlay)
            }
        }

        // 4. Pass 3: Render Nature Trees, Buildings, Services, Utilities
        for (tile in renderTiles) {
            val tx = tile.x
            val ty = tile.y

            // Draw Trees & Nature
            if (tile.treeType > 0 && tile.building == null && tile.road == RoadType.NONE && tile.service == null && tile.utility == null) {
                drawPixelTree(drawScope, tile, tile.treeType, tileW, tileH, lighting)
            }

            // Draw Services
            tile.service?.let { s ->
                drawPixelService(drawScope, tile, s, tileW, tileH, lighting, stats.dayTime)
            }

            // Draw Utilities
            tile.utility?.let { u ->
                drawPixelUtility(drawScope, tile, u, tileW, tileH, lighting, animTick)
            }

            // Draw Buildings
            tile.building?.let { b ->
                drawPixelBuilding(drawScope, tile, b, tileW, tileH, lighting, stats.dayTime, graphicsQuality)
            }
        }

        // 5. Pass 4: Draw Animated 2D Pixel Vehicles
        if (graphicsQuality != GraphicsQuality.LOW || traffic.vehicles.size <= 30) {
            for (v in traffic.vehicles) {
                drawPixelVehicle(drawScope, v, tileW, tileH, lighting, stats.dayTime)
            }
        }

        // 6. Pass 5: Draw Animated 2D Pixel Pedestrians
        if (graphicsQuality == GraphicsQuality.HIGH) {
            for (p in traffic.pedestrians) {
                drawPixelPedestrian(drawScope, p, tileW, tileH, lighting, stats.weather)
            }
        }

        // 7. Pass 6: Hover & Placement Preview Indicator
        hoverTile?.let { (hx, hy) ->
            if (world.isInside(hx, hy)) {
                drawPixelHoverCursor(drawScope, hx, hy, tileW, tileH, activeTool)
            }
        }

        // 8. Pass 7: Dynamic Weather Overlays (Rain streaks, Storm lightning, Cloud shadows)
        drawWeatherAndAtmosphere(drawScope, stats.weather, lighting, stats.dayTime)
    }

    // ==========================================
    // TERRAIN & WATER SYSTEM
    // ==========================================

    private fun drawPixelTerrainTile(
        drawScope: DrawScope,
        world: WorldMap,
        tile: GridTile,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        overlay: OverlayMode
    ) {
        val halfW = tileW / 2f
        val halfH = tileH / 2f
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)

        val top = Offset(p.x, p.y - halfH)
        val right = Offset(p.x + halfW, p.y)
        val bottom = Offset(p.x, p.y + halfH)
        val left = Offset(p.x - halfW, p.y)

        val diamondPath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(right.x, right.y)
            lineTo(bottom.x, bottom.y)
            lineTo(left.x, left.y)
            close()
        }

        var baseColor = when (tile.terrain) {
            TerrainType.WATER -> {
                // Pixel animated water wave color
                val wavePhase = sin(animTick * 1.5f + tile.x * 0.9f + tile.y * 0.7f)
                if (wavePhase > 0.4f) Color(0xFF1E88E5) else Color(0xFF1976D2)
            }
            TerrainType.SHORE -> Color(0xFFE5D08F)
            TerrainType.SAND -> Color(0xFFF0E0A6)
            TerrainType.DIRT -> Color(0xFF8D6E63)
            TerrainType.ROCK, TerrainType.HILL -> Color(0xFF78909C)
            TerrainType.DARK_GRASS, TerrainType.FOREST -> Color(0xFF2E7D32)
            TerrainType.GRASS, TerrainType.PLAINS -> {
                // Alternating checkerboard pixel grass texture
                if ((tile.x + tile.y) % 2 == 0) Color(0xFF558B2F) else Color(0xFF689F38)
            }
        }

        // Apply Data Overlays if active
        if (overlay != OverlayMode.NORMAL) {
            baseColor = getOverlayColor(tile, overlay, baseColor)
        }

        val blended = blendWithLighting(baseColor, lighting)
        drawScope.drawPath(diamondPath, color = blended, style = Fill)

        // Pixel water ripples and shoreline foam
        if (tile.terrain == TerrainType.WATER && overlay == OverlayMode.NORMAL) {
            val rippleY = p.y + sin(animTick * 2f + tile.x + tile.y) * (halfH * 0.3f)
            drawScope.drawLine(
                color = blendWithLighting(Color(0xFFBBDEFB).copy(alpha = 0.65f), lighting),
                start = Offset(p.x - halfW * 0.35f, rippleY),
                end = Offset(p.x + halfW * 0.35f, rippleY),
                strokeWidth = max(1f, 1.5f * camera.zoom)
            )

            // Shoreline transition check (Grass/Sand -> Water foam)
            val hasLandNeighbor = world.getNeighbors(tile.x, tile.y).any { it.terrain != TerrainType.WATER }
            if (hasLandNeighbor) {
                // Foaming shore outline
                drawScope.drawPath(
                    diamondPath,
                    color = blendWithLighting(Color(0xFFE0F7FA).copy(alpha = 0.55f), lighting),
                    style = Stroke(width = max(1f, 2f * camera.zoom))
                )
            }
        }

        // Pixel Grass blade specks
        if ((tile.terrain == TerrainType.GRASS || tile.terrain == TerrainType.PLAINS) && overlay == OverlayMode.NORMAL) {
            val speckColor = blendWithLighting(Color(0xFF7CB342), lighting)
            val sX = p.x + (if (tile.x % 2 == 0) -halfW * 0.2f else halfW * 0.2f)
            val sY = p.y + (if (tile.y % 2 == 0) -halfH * 0.2f else halfH * 0.2f)
            drawScope.drawRect(
                color = speckColor,
                topLeft = Offset(sX, sY),
                size = Size(max(1f, 2f * camera.zoom), max(1f, 3f * camera.zoom))
            )
        }

        // Subtle pixel grid stroke
        drawScope.drawPath(diamondPath, color = Color.Black.copy(alpha = 0.07f), style = Stroke(width = 1f))

        // In normal mode, draw designated zoning boundaries
        if (overlay == OverlayMode.NORMAL && tile.zone != ZoneType.NONE && tile.building == null && tile.road == RoadType.NONE) {
            val zColor = when (tile.zone.category) {
                ZoneCategory.RESIDENTIAL -> Color(0xFF4CAF50)
                ZoneCategory.COMMERCIAL -> Color(0xFF2196F3)
                ZoneCategory.INDUSTRIAL -> Color(0xFFFFC107)
                ZoneCategory.NONE -> Color.Transparent
            }
            drawScope.drawPath(diamondPath, color = zColor.copy(alpha = 0.35f), style = Fill)
            drawScope.drawPath(diamondPath, color = zColor, style = Stroke(width = 1.5f))
        }
    }

    // ==========================================
    // AUTO-TILING ROAD SYSTEM
    // ==========================================

    private fun drawPixelRoadTile(
        drawScope: DrawScope,
        world: WorldMap,
        tile: GridTile,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        overlay: OverlayMode
    ) {
        val halfW = tileW / 2f
        val halfH = tileH / 2f
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)

        val top = Offset(p.x, p.y - halfH)
        val right = Offset(p.x + halfW, p.y)
        val bottom = Offset(p.x, p.y + halfH)
        val left = Offset(p.x - halfW, p.y)

        // Check 4-directional road connections for smart auto-tiling
        val hasN = world.getTile(tile.x, tile.y - 1)?.road != null && world.getTile(tile.x, tile.y - 1)?.road != RoadType.NONE
        val hasS = world.getTile(tile.x, tile.y + 1)?.road != null && world.getTile(tile.x, tile.y + 1)?.road != RoadType.NONE
        val hasW = world.getTile(tile.x - 1, tile.y)?.road != null && world.getTile(tile.x - 1, tile.y)?.road != RoadType.NONE
        val hasE = world.getTile(tile.x + 1, tile.y)?.road != null && world.getTile(tile.x + 1, tile.y)?.road != RoadType.NONE

        val isBridge = tile.road == RoadType.BRIDGE || tile.terrain == TerrainType.WATER
        val roadAsphalt = if (isBridge) Color(0xFF78909C) else Color(0xFF37474F)
        val blendedAsphalt = blendWithLighting(roadAsphalt, lighting)

        val diamondPath = Path().apply {
            moveTo(top.x, top.y)
            lineTo(right.x, right.y)
            lineTo(bottom.x, bottom.y)
            lineTo(left.x, left.y)
            close()
        }

        // Bridge wooden / concrete pilings over water
        if (isBridge) {
            val pylonColor = blendWithLighting(Color(0xFF455A64), lighting)
            drawScope.drawRect(
                color = pylonColor,
                topLeft = Offset(p.x - 4f * camera.zoom, p.y + halfH * 0.6f),
                size = Size(8f * camera.zoom, 10f * camera.zoom)
            )
        }

        // Fill Asphalt Surface
        drawScope.drawPath(diamondPath, color = blendedAsphalt, style = Fill)

        // Draw Concrete Sidewalk Kerb Outlines
        val curbColor = blendWithLighting(Color(0xFFB0BEC5), lighting)
        drawScope.drawPath(diamondPath, color = curbColor, style = Stroke(width = max(1f, 1.2f * camera.zoom)))

        // Road Lane Markings
        val markColor = if (tile.road == RoadType.LARGE_6L || tile.road == RoadType.MEDIUM_4L) {
            blendWithLighting(Color(0xFFFFD54F), lighting)
        } else {
            blendWithLighting(Color.White.copy(alpha = 0.85f), lighting)
        }
        val strokeW = if (tile.road == RoadType.LARGE_6L) 2.5f * camera.zoom else 1.5f * camera.zoom

        // Auto-tiling line segments
        val isHorizontal = (hasW || hasE) && !hasN && !hasS
        val isVertical = (hasN || hasS) && !hasW && !hasE
        val isCrossroad = (hasN && hasS && hasW && hasE)

        if (isHorizontal || (!hasN && !hasS && !hasW && !hasE)) {
            // West to East diagonal centerline
            val midW = Offset(p.x - halfW * 0.5f, p.y - halfH * 0.5f)
            val midE = Offset(p.x + halfW * 0.5f, p.y + halfH * 0.5f)
            drawScope.drawLine(markColor, midW, midE, strokeWidth = strokeW)
        } else if (isVertical) {
            // North to South diagonal centerline
            val midN = Offset(p.x + halfW * 0.5f, p.y - halfH * 0.5f)
            val midS = Offset(p.x - halfW * 0.5f, p.y + halfH * 0.5f)
            drawScope.drawLine(markColor, midN, midS, strokeWidth = strokeW)
        } else {
            // Intersection or Corner: draw connector lines from center to active neighbors
            val center = Offset(p.x, p.y)
            if (hasN) drawScope.drawLine(markColor, center, Offset(p.x + halfW * 0.5f, p.y - halfH * 0.5f), strokeWidth = strokeW)
            if (hasS) drawScope.drawLine(markColor, center, Offset(p.x - halfW * 0.5f, p.y + halfH * 0.5f), strokeWidth = strokeW)
            if (hasW) drawScope.drawLine(markColor, center, Offset(p.x - halfW * 0.5f, p.y - halfH * 0.5f), strokeWidth = strokeW)
            if (hasE) drawScope.drawLine(markColor, center, Offset(p.x + halfW * 0.5f, p.y + halfH * 0.5f), strokeWidth = strokeW)

            // Zebra crosswalk stripes for intersections
            if (isCrossroad) {
                drawScope.drawCircle(color = markColor, radius = 2.5f * camera.zoom, center = center)
            }
        }

        // Bridge side safety railings
        if (isBridge) {
            val railColor = blendWithLighting(Color(0xFFCFD8DC), lighting)
            drawScope.drawLine(railColor, top, left, strokeWidth = 2f * camera.zoom)
            drawScope.drawLine(railColor, right, bottom, strokeWidth = 2f * camera.zoom)
        }
    }

    // ==========================================
    // PIXEL ART NATURE & TREES
    // ==========================================

    private fun drawPixelTree(
        drawScope: DrawScope,
        tile: GridTile,
        treeType: Int,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting
    ) {
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)
        val z = camera.zoom

        // Ground Drop Shadow
        val shadowColor = Color.Black.copy(alpha = 0.25f)
        drawScope.drawOval(
            color = shadowColor,
            topLeft = Offset(p.x - 7f * z, p.y - 2f * z),
            size = Size(14f * z, 6f * z)
        )

        val trunkColor = blendWithLighting(Color(0xFF5D4037), lighting)
        val trunkX = p.x - 1.5f * z
        val trunkY = p.y - 12f * z

        when (treeType) {
            1 -> {
                // Small Oak Tree (Round lush canopy with leaf highlights)
                val folColor = blendWithLighting(Color(0xFF43A047), lighting)
                val hiColor = blendWithLighting(Color(0xFF66BB6A), lighting)
                // Trunk
                drawScope.drawRect(trunkColor, Offset(trunkX, trunkY + 4f * z), Size(3f * z, 8f * z))
                // Canopy
                drawScope.drawCircle(folColor, 8f * z, Offset(p.x, p.y - 12f * z))
                drawScope.drawCircle(hiColor, 4.5f * z, Offset(p.x - 2f * z, p.y - 14f * z))
            }
            2 -> {
                // Apple / Fruit Tree (Green canopy with red pixel apples)
                val folColor = blendWithLighting(Color(0xFF2E7D32), lighting)
                val appleColor = blendWithLighting(Color(0xFFE53935), lighting)
                drawScope.drawRect(trunkColor, Offset(trunkX, trunkY + 3f * z), Size(3f * z, 9f * z))
                drawScope.drawCircle(folColor, 9f * z, Offset(p.x, p.y - 13f * z))
                // Red apples
                drawScope.drawRect(appleColor, Offset(p.x - 4f * z, p.y - 14f * z), Size(2f * z, 2f * z))
                drawScope.drawRect(appleColor, Offset(p.x + 3f * z, p.y - 11f * z), Size(2f * z, 2f * z))
                drawScope.drawRect(appleColor, Offset(p.x - 1f * z, p.y - 8f * z), Size(2f * z, 2f * z))
            }
            3 -> {
                // Pine Tree (Layered dark green triangular needles)
                val pine1 = blendWithLighting(Color(0xFF1B5E20), lighting)
                val pine2 = blendWithLighting(Color(0xFF2E7D32), lighting)
                drawScope.drawRect(trunkColor, Offset(trunkX, trunkY + 6f * z), Size(3f * z, 7f * z))
                // Tier 1 (bottom)
                drawTriangle(drawScope, p.x, p.y - 8f * z, 14f * z, 7f * z, pine1)
                // Tier 2 (middle)
                drawTriangle(drawScope, p.x, p.y - 13f * z, 11f * z, 6f * z, pine2)
                // Tier 3 (top)
                drawTriangle(drawScope, p.x, p.y - 17f * z, 7f * z, 5f * z, pine2)
            }
            4 -> {
                // Flowering Shrub / Bush (Pink/White flowers)
                val bushColor = blendWithLighting(Color(0xFF388E3C), lighting)
                val flowerColor = blendWithLighting(Color(0xFFF48FB1), lighting)
                drawScope.drawOval(bushColor, Offset(p.x - 8f * z, p.y - 8f * z), Size(16f * z, 10f * z))
                drawScope.drawRect(flowerColor, Offset(p.x - 4f * z, p.y - 6f * z), Size(2f * z, 2f * z))
                drawScope.drawRect(flowerColor, Offset(p.x + 3f * z, p.y - 5f * z), Size(2f * z, 2f * z))
            }
            5 -> {
                // Tall Birch Tree (White bark with yellow-green canopy)
                val birchBark = blendWithLighting(Color(0xFFECEFF1), lighting)
                val birchCanopy = blendWithLighting(Color(0xFF81C784), lighting)
                drawScope.drawRect(birchBark, Offset(trunkX, trunkY), Size(2.5f * z, 13f * z))
                drawScope.drawCircle(birchCanopy, 7f * z, Offset(p.x, p.y - 16f * z))
            }
            else -> {
                // Rocky Boulder
                val rockColor = blendWithLighting(Color(0xFF78909C), lighting)
                val rockShade = blendWithLighting(Color(0xFF546E7A), lighting)
                drawScope.drawOval(rockColor, Offset(p.x - 7f * z, p.y - 5f * z), Size(14f * z, 8f * z))
                drawScope.drawOval(rockShade, Offset(p.x - 4f * z, p.y - 3f * z), Size(8f * z, 4f * z))
            }
        }
    }

    // ==========================================
    // 3-TIER ZONING & PIXEL BUILDING SPRITES
    // ==========================================

    private fun drawPixelBuilding(
        drawScope: DrawScope,
        tile: GridTile,
        b: Building,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        dayTime: Float,
        graphicsQuality: GraphicsQuality
    ) {
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)
        val z = camera.zoom
        val isNight = dayTime !in 6.0f..18.5f

        // Ground Drop Shadow
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(p.x - 18f * z, p.y - 4f * z),
            size = Size(36f * z, 14f * z)
        )

        // Check Building Lifecycle Stages
        when (b.stage) {
            BuildingStage.EMPTY -> {
                // Empty lot: survey stakes and caution string
                val stakeColor = blendWithLighting(Color(0xFFFFA000), lighting)
                drawScope.drawRect(stakeColor, Offset(p.x - 8f * z, p.y - 6f * z), Size(2f * z, 6f * z))
                drawScope.drawRect(stakeColor, Offset(p.x + 8f * z, p.y - 6f * z), Size(2f * z, 6f * z))
                return
            }
            BuildingStage.FOUNDATION -> {
                // Foundation excavation pit with concrete footing grid
                val pitColor = blendWithLighting(Color(0xFF5D4037), lighting)
                val concreteColor = blendWithLighting(Color(0xFF9E9E9E), lighting)
                drawScope.drawOval(pitColor, Offset(p.x - 14f * z, p.y - 8f * z), Size(28f * z, 14f * z))
                drawScope.drawRect(concreteColor, Offset(p.x - 8f * z, p.y - 5f * z), Size(16f * z, 8f * z))
                return
            }
            BuildingStage.CONSTRUCTION -> {
                // Wooden / Steel Scaffolding with Crane
                drawConstructionSite(drawScope, p.x, p.y, z, b.zoneType.density, lighting, animTick)
                return
            }
            BuildingStage.ABANDONED -> {
                // Dilapidated abandoned structure
                drawPixelIsometricBox(
                    drawScope, p.x, p.y, z,
                    width = 24f, height = 20f,
                    roofColor = blendWithLighting(Color(0xFF424242), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF303030), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF212121), lighting)
                )
                // Boarded up X on windows
                val boardColor = blendWithLighting(Color(0xFF795548), lighting)
                drawScope.drawLine(boardColor, Offset(p.x - 6f * z, p.y - 12f * z), Offset(p.x - 2f * z, p.y - 8f * z), strokeWidth = 2f * z)
                return
            }
            BuildingStage.BUILT -> {
                // Proceed to full pixel sprite rendering!
            }
        }

        // Render Distinct Pixel Art Buildings based on Category, Density & Level
        when (b.zoneType.category) {
            ZoneCategory.RESIDENTIAL -> drawResidentialPixelBuilding(drawScope, p.x, p.y, z, b, lighting, isNight, animTick)
            ZoneCategory.COMMERCIAL -> drawCommercialPixelBuilding(drawScope, p.x, p.y, z, b, lighting, isNight)
            ZoneCategory.INDUSTRIAL -> drawIndustrialPixelBuilding(drawScope, p.x, p.y, z, b, lighting, isNight, animTick)
            ZoneCategory.NONE -> {}
        }

        // Disaster: Active Fire & Smoke
        if (b.onFire) {
            drawPixelFireAndSmoke(drawScope, p.x, p.y - 20f * z, z, animTick)
        }
    }

    // ==========================================
    // RESIDENTIAL SPRITES (3 DENSITIES)
    // ==========================================

    private fun drawResidentialPixelBuilding(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        z: Float,
        b: Building,
        lighting: DayNightLighting,
        isNight: Boolean,
        anim: Float
    ) {
        when (b.zoneType.density) {
            DensityLevel.LOW -> {
                // Low Density: 1-2 Story Cottages, Suburban Villas, Gardens
                val roofColor = when (b.level) {
                    1 -> Color(0xFFD32F2F) // Terracotta red cottage
                    2 -> Color(0xFF1976D2) // Blue shingle suburban villa
                    else -> Color(0xFF388E3C) // Emerald luxury estate
                }
                val wallColor = when (b.level) {
                    1 -> Color(0xFFFFF8E1) // Cream stucco
                    2 -> Color(0xFFE0E0E0) // Light grey siding
                    else -> Color(0xFFFFFFFF) // White modern villa
                }
                val bH = 14f + (b.level * 4f)

                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 22f, height = bH,
                    roofColor = blendWithLighting(roofColor, lighting),
                    leftWallColor = blendWithLighting(wallColor, lighting),
                    rightWallColor = blendWithLighting(wallColor.copy(red = wallColor.red * 0.85f), lighting)
                )

                // Chimney with animated pixel smoke
                val chimneyX = cx - 5f * z
                val chimneyY = cy - (bH + 4f) * z
                drawScope.drawRect(blendWithLighting(Color(0xFF8D6E63), lighting), Offset(chimneyX, chimneyY), Size(3f * z, 5f * z))
                val smokeY = chimneyY - (3f + sin(anim * 2f) * 2f) * z
                drawScope.drawCircle(Color.White.copy(alpha = 0.5f), 2.5f * z, Offset(chimneyX + 1.5f * z, smokeY))

                // Windows (Glowing at night)
                val winColor = if (isNight && b.isPowered) Color(0xFFFFF59D) else Color(0xFF81D4FA)
                drawScope.drawRect(winColor, Offset(cx - 7f * z, cy - (bH - 6f) * z), Size(3f * z, 3f * z))
                drawScope.drawRect(winColor, Offset(cx + 4f * z, cy - (bH - 6f) * z), Size(3f * z, 3f * z))
            }
            DensityLevel.MEDIUM -> {
                // Medium Density: 3-6 Story Townhouses, Brick Walkup Flats, European Mansard Roofs
                val bH = 28f + (b.level * 10f)
                val brickColor = when (b.level) {
                    1 -> Color(0xFFBF360C) // Red brick brownstone
                    2 -> Color(0xFFE65100) // Terracotta modern flats
                    else -> Color(0xFF5D4037) // Dark vintage brownstone
                }
                val roofColor = when (b.level) {
                    1 -> Color(0xFF37474F)
                    2 -> Color(0xFF263238)
                    else -> Color(0xFF006064)
                }

                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 26f, height = bH,
                    roofColor = blendWithLighting(roofColor, lighting),
                    leftWallColor = blendWithLighting(brickColor, lighting),
                    rightWallColor = blendWithLighting(brickColor.copy(red = brickColor.red * 0.8f), lighting)
                )

                // Rooftop AC Unit & Water Tank
                val acColor = blendWithLighting(Color(0xFF78909C), lighting)
                drawScope.drawRect(acColor, Offset(cx - 3f * z, cy - (bH + 5f) * z), Size(6f * z, 4f * z))

                // Rows of Pixel Windows
                val winColor = if (isNight && b.isPowered) Color(0xFFFFF59D) else Color(0xFFB3E5FC)
                val floors = 2 + b.level
                val floorStep = (bH - 6f) / floors
                for (f in 1..floors) {
                    val wy = cy - (f * floorStep) * z
                    drawScope.drawRect(winColor, Offset(cx - 9f * z, wy), Size(3f * z, 3.5f * z))
                    drawScope.drawRect(winColor, Offset(cx - 4f * z, wy), Size(3f * z, 3.5f * z))
                    drawScope.drawRect(winColor, Offset(cx + 3f * z, wy), Size(3f * z, 3.5f * z))
                    drawScope.drawRect(winColor, Offset(cx + 7f * z, wy), Size(3f * z, 3.5f * z))
                }
            }
            DensityLevel.HIGH -> {
                // High Density: 8-18 Story Skyscraper Towers, High-Rise Complexes
                val bH = 50f + (b.level * 22f)
                val glassColor = when (b.level) {
                    1 -> Color(0xFF0288D1) // Blue glass tower
                    2 -> Color(0xFF00ACC1) // Cyan crystalline tower
                    else -> Color(0xFF1E88E5) // Luxury sapphire megatower
                }
                val facadeColor = blendWithLighting(glassColor, lighting)

                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 30f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF263238), lighting),
                    leftWallColor = facadeColor,
                    rightWallColor = blendWithLighting(glassColor.copy(blue = glassColor.blue * 0.8f), lighting)
                )

                // Rooftop Helipad & Aviation Beacon
                val topY = cy - bH * z
                drawScope.drawCircle(Color.White.copy(alpha = 0.7f), 6f * z, Offset(cx, topY))
                drawScope.drawRect(
                    blendWithLighting(Color(0xFFE53935), lighting),
                    Offset(cx - 1f * z, topY - 8f * z),
                    Size(2f * z, 8f * z)
                )
                // Blinking red beacon light at night
                if (sin(anim * 4f) > 0) {
                    drawScope.drawCircle(Color(0xFFFF1744), 3f * z, Offset(cx, topY - 8f * z))
                }

                // Grid of Illuminating Glass Facade Panels
                val winColor = if (isNight && b.isPowered) Color(0xFFFFF9C4) else Color(0xFFE1F5FE)
                val rows = 6 + b.level * 3
                val rowStep = (bH - 8f) / rows
                for (r in 1..rows) {
                    val ry = cy - (r * rowStep) * z
                    drawScope.drawRect(winColor, Offset(cx - 11f * z, ry), Size(4f * z, 2f * z))
                    drawScope.drawRect(winColor, Offset(cx - 5f * z, ry), Size(4f * z, 2f * z))
                    drawScope.drawRect(winColor, Offset(cx + 3f * z, ry), Size(4f * z, 2f * z))
                    drawScope.drawRect(winColor, Offset(cx + 8f * z, ry), Size(4f * z, 2f * z))
                }
            }
            DensityLevel.NONE -> {}
        }
    }

    // ==========================================
    // COMMERCIAL SPRITES (3 DENSITIES)
    // ==========================================

    private fun drawCommercialPixelBuilding(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        z: Float,
        b: Building,
        lighting: DayNightLighting,
        isNight: Boolean
    ) {
        when (b.zoneType.density) {
            DensityLevel.LOW -> {
                // Low: Corner Cafe / Boutique Bakery with striped awning
                val bH = 16f + b.level * 3f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 24f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF546E7A), lighting),
                    leftWallColor = blendWithLighting(Color(0xFFFFF3E0), lighting),
                    rightWallColor = blendWithLighting(Color(0xFFFFE0B2), lighting)
                )
                // Red & White striped cafe awning
                val awningY = cy - (bH - 8f) * z
                drawScope.drawRect(Color(0xFFD32F2F), Offset(cx - 10f * z, awningY), Size(4f * z, 3f * z))
                drawScope.drawRect(Color.White, Offset(cx - 6f * z, awningY), Size(4f * z, 3f * z))
                drawScope.drawRect(Color(0xFFD32F2F), Offset(cx - 2f * z, awningY), Size(4f * z, 3f * z))
                // Storefront Glass
                val glassColor = if (isNight && b.isPowered) Color(0xFFFFF59D) else Color(0xFF81D4FA)
                drawScope.drawRect(glassColor, Offset(cx - 9f * z, cy - 5f * z), Size(9f * z, 4f * z))
            }
            DensityLevel.MEDIUM -> {
                // Medium: Corporate Plaza / Shopping Center
                val bH = 34f + b.level * 10f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 28f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF37474F), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF29B6F6), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF0288D1), lighting)
                )
                // Billboard sign on rooftop
                val signColor = if (isNight && b.isPowered) Color(0xFFFFD600) else Color(0xFFECEFF1)
                drawScope.drawRect(signColor, Offset(cx - 6f * z, cy - (bH + 6f) * z), Size(12f * z, 5f * z))
            }
            DensityLevel.HIGH -> {
                // High: World Financial Center / Commercial Megatower with Spire
                val bH = 58f + b.level * 24f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 30f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF1A237E), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF3949AB), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF283593), lighting)
                )
                // Majestic observation Spire
                val spireTop = cy - (bH + 16f) * z
                drawScope.drawLine(
                    color = blendWithLighting(Color(0xFFB0BEC5), lighting),
                    start = Offset(cx, cy - bH * z),
                    end = Offset(cx, spireTop),
                    strokeWidth = 2.5f * z
                )
                if (isNight && b.isPowered) {
                    drawScope.drawCircle(Color(0xFF00E5FF), 3.5f * z, Offset(cx, spireTop))
                }
            }
            DensityLevel.NONE -> {}
        }
    }

    // ==========================================
    // INDUSTRIAL SPRITES (3 DENSITIES)
    // ==========================================

    private fun drawIndustrialPixelBuilding(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        z: Float,
        b: Building,
        lighting: DayNightLighting,
        isNight: Boolean,
        anim: Float
    ) {
        when (b.zoneType.density) {
            DensityLevel.LOW -> {
                // Small Workshop / Garage with corrugated roof
                val bH = 14f + b.level * 3f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 24f, height = bH,
                    roofColor = blendWithLighting(Color(0xFFFFB300), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF78909C), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF546E7A), lighting)
                )
                // Roll-up warehouse garage door
                drawScope.drawRect(blendWithLighting(Color(0xFF37474F), lighting), Offset(cx - 8f * z, cy - 6f * z), Size(7f * z, 5f * z))
            }
            DensityLevel.MEDIUM -> {
                // Factory / Chemical Processing with Storage Tanks
                val bH = 26f + b.level * 8f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 28f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF424242), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF616161), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF424242), lighting)
                )
                // Industrial Storage Tanks
                val tankColor = blendWithLighting(Color(0xFFB0BEC5), lighting)
                drawScope.drawOval(tankColor, Offset(cx + 6f * z, cy - 8f * z), Size(8f * z, 8f * z))
                // Chimney smokestack
                val stackX = cx - 9f * z
                val stackY = cy - (bH + 10f) * z
                drawScope.drawRect(blendWithLighting(Color(0xFFB71C1C), lighting), Offset(stackX, stackY), Size(4f * z, 12f * z))
                // Billowing Animated Smoke
                drawPixelSmokePuff(drawScope, stackX + 2f * z, stackY, z, anim)
            }
            DensityLevel.HIGH -> {
                // Heavy Industrial Refinery Complex with Dual Smokestacks
                val bH = 40f + b.level * 14f
                drawPixelIsometricBox(
                    drawScope, cx, cy, z,
                    width = 30f, height = bH,
                    roofColor = blendWithLighting(Color(0xFF212121), lighting),
                    leftWallColor = blendWithLighting(Color(0xFF37474F), lighting),
                    rightWallColor = blendWithLighting(Color(0xFF263238), lighting)
                )
                // Dual Chimneys
                val s1X = cx - 8f * z
                val s2X = cx + 5f * z
                val sY = cy - (bH + 16f) * z
                drawScope.drawRect(blendWithLighting(Color(0xFFD32F2F), lighting), Offset(s1X, sY), Size(4f * z, 18f * z))
                drawScope.drawRect(blendWithLighting(Color(0xFFD32F2F), lighting), Offset(s2X, sY), Size(4f * z, 18f * z))
                // Animated Smoke Plumes
                drawPixelSmokePuff(drawScope, s1X + 2f * z, sY, z, anim)
                drawPixelSmokePuff(drawScope, s2X + 2f * z, sY, z, anim + 1.2f)
            }
            DensityLevel.NONE -> {}
        }
    }

    // ==========================================
    // SERVICES & UTILITIES SPRITES
    // ==========================================

    private fun drawPixelService(
        drawScope: DrawScope,
        tile: GridTile,
        s: ServiceType,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        dayTime: Float
    ) {
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)
        val z = camera.zoom

        when (s.category) {
            ServiceCategory.POLICE -> {
                // Police Station (Blue brick + Police Badge Crest)
                drawPixelIsometricBox(
                    drawScope, p.x, p.y, z, 26f, 24f,
                    blendWithLighting(Color(0xFF1565C0), lighting),
                    blendWithLighting(Color(0xFF1976D2), lighting),
                    blendWithLighting(Color(0xFF0D47A1), lighting)
                )
                // Blue flashing beacon
                drawScope.drawCircle(Color(0xFF2979FF), 3f * z, Offset(p.x, p.y - 28f * z))
            }
            ServiceCategory.FIRE -> {
                // Fire Station (Red garage + Hose tower)
                drawPixelIsometricBox(
                    drawScope, p.x, p.y, z, 26f, 22f,
                    blendWithLighting(Color(0xFFC62828), lighting),
                    blendWithLighting(Color(0xFFD32F2F), lighting),
                    blendWithLighting(Color(0xFFB71C1C), lighting)
                )
                // Hose drying tower
                drawScope.drawRect(blendWithLighting(Color(0xFF8E0000), lighting), Offset(p.x + 6f * z, p.y - 32f * z), Size(5f * z, 14f * z))
            }
            ServiceCategory.HEALTH -> {
                // Hospital (Clean white facade with Red Cross)
                drawPixelIsometricBox(
                    drawScope, p.x, p.y, z, 28f, 28f,
                    blendWithLighting(Color(0xFFECEFF1), lighting),
                    blendWithLighting(Color(0xFFFFFFFF), lighting),
                    blendWithLighting(Color(0xFFCFD8DC), lighting)
                )
                // Red Cross Emblem
                val cy = p.y - 18f * z
                drawScope.drawRect(Color.Red, Offset(p.x - 5f * z, cy - 1.5f * z), Size(10f * z, 3f * z))
                drawScope.drawRect(Color.Red, Offset(p.x - 1.5f * z, cy - 5f * z), Size(3f * z, 10f * z))
            }
            ServiceCategory.EDUCATION -> {
                // School / University
                val isUni = s == ServiceType.UNIVERSITY
                val h = if (isUni) 38f else 20f
                drawPixelIsometricBox(
                    drawScope, p.x, p.y, z, 28f, h,
                    blendWithLighting(Color(0xFFF57C00), lighting),
                    blendWithLighting(Color(0xFFFF9800), lighting),
                    blendWithLighting(Color(0xFFE65100), lighting)
                )
                // Clock / Bell tower
                drawScope.drawRect(blendWithLighting(Color(0xFFFFB74D), lighting), Offset(p.x - 3f * z, p.y - (h + 8f) * z), Size(6f * z, 8f * z))
            }
            ServiceCategory.PARK -> {
                // Park with Fountain, paths, flowerbeds
                val pathColor = blendWithLighting(Color(0xFFE0E0E0), lighting)
                drawScope.drawOval(pathColor, Offset(p.x - 10f * z, p.y - 6f * z), Size(20f * z, 12f * z))
                // Central Fountain with splashing water
                val fColor = blendWithLighting(Color(0xFF03A9F4), lighting)
                drawScope.drawCircle(fColor, 4.5f * z, Offset(p.x, p.y))
                drawScope.drawCircle(Color.White, 2f * z, Offset(p.x, p.y - (2f + sin(animTick * 3f) * 1f) * z))
            }
            ServiceCategory.DEATH_CARE -> {
                // Cemetery (Mausoleum + Gravestones)
                drawPixelIsometricBox(drawScope, p.x, p.y, z, 18f, 12f, Color(0xFF607D8B), Color(0xFF78909C), Color(0xFF455A64))
                drawScope.drawRect(Color(0xFFCFD8DC), Offset(p.x + 6f * z, p.y - 2f * z), Size(2.5f * z, 4f * z))
            }
            ServiceCategory.GARBAGE -> {
                // Garbage recycling facility
                drawPixelIsometricBox(drawScope, p.x, p.y, z, 24f, 16f, Color(0xFF4E342E), Color(0xFF6D4C41), Color(0xFF3E2723))
            }
        }
    }

    private fun drawPixelUtility(
        drawScope: DrawScope,
        tile: GridTile,
        u: UtilityType,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        anim: Float
    ) {
        val p = camera.project(tile.x.toFloat(), tile.y.toFloat(), tile.elevation)
        val z = camera.zoom

        when (u) {
            UtilityType.WIND_TURBINE -> {
                // Tower mast
                val mastColor = blendWithLighting(Color(0xFFECEFF1), lighting)
                val topY = p.y - 28f * z
                drawScope.drawLine(mastColor, Offset(p.x, p.y), Offset(p.x, topY), strokeWidth = 3f * z)
                // 3 Animated Spinning Blades
                val bladeLen = 14f * z
                for (i in 0..2) {
                    val angle = anim * 2.8f + (i * 2f * PI.toFloat() / 3f)
                    val bx = p.x + cos(angle) * bladeLen
                    val by = topY + sin(angle) * bladeLen
                    drawScope.drawLine(Color.White, Offset(p.x, topY), Offset(bx, by), strokeWidth = 2.0f * z)
                }
            }
            UtilityType.SOLAR_PLANT -> {
                // Tilted blue photovoltaic solar panels
                val solarColor = blendWithLighting(Color(0xFF0D47A1), lighting)
                drawScope.drawRect(solarColor, Offset(p.x - 10f * z, p.y - 6f * z), Size(20f * z, 10f * z))
                drawScope.drawLine(Color.Cyan.copy(alpha = 0.5f), Offset(p.x - 10f * z, p.y - 1f * z), Offset(p.x + 10f * z, p.y - 1f * z), strokeWidth = 1f * z)
            }
            UtilityType.COAL_PLANT, UtilityType.GAS_PLANT -> {
                // Power plant building + Smoking stack
                drawPixelIsometricBox(drawScope, p.x, p.y, z, 28f, 24f, Color(0xFF212121), Color(0xFF424242), Color(0xFF303030))
                val stackX = p.x - 8f * z
                val stackY = p.y - 36f * z
                drawScope.drawRect(Color(0xFF757575), Offset(stackX, stackY), Size(5f * z, 16f * z))
                drawPixelSmokePuff(drawScope, stackX + 2.5f * z, stackY, z, anim)
            }
            UtilityType.WATER_PUMP, UtilityType.WATER_TOWER, UtilityType.SEWAGE_PLANT -> {
                // Water infrastructure
                drawPixelIsometricBox(drawScope, p.x, p.y, z, 22f, 20f, Color(0xFF01579B), Color(0xFF0288D1), Color(0xFF0277BD))
            }
        }
    }

    // ==========================================
    // ANIMATED 2D PIXEL VEHICLES & PEDESTRIANS
    // ==========================================

    private fun drawPixelVehicle(
        drawScope: DrawScope,
        v: Vehicle,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        dayTime: Float
    ) {
        val p = camera.project(v.x, v.y, 0f)
        val z = camera.zoom
        val isNight = dayTime !in 6.0f..18.5f

        // Shadow under car
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(p.x - 5f * z, p.y - 2f * z),
            size = Size(10f * z, 4.5f * z)
        )

        val vColor = blendWithLighting(Color(v.type.colorHex), lighting)
        val bodyW = if (v.type == VehicleType.BUS) 12f * z else 7f * z
        val bodyH = 4.5f * z

        // Vehicle Chassis
        drawScope.drawRoundRect(
            color = vColor,
            topLeft = Offset(p.x - bodyW / 2f, p.y - bodyH),
            size = Size(bodyW, bodyH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * z)
        )

        // Cabin roof
        drawScope.drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(p.x - bodyW / 4f, p.y - bodyH * 1.5f),
            size = Size(bodyW / 2f, bodyH * 0.6f)
        )

        // Headlights at night
        if (isNight) {
            val hx = cos(v.angle) * 12f * z
            val hy = sin(v.angle) * 12f * z
            drawScope.drawLine(
                color = Color(0xFFFFF9C4).copy(alpha = 0.75f),
                start = Offset(p.x, p.y - 2f * z),
                end = Offset(p.x + hx, p.y + hy),
                strokeWidth = 3f * z
            )
            // Emergency beacon flash
            if (v.isEmergencyMission) {
                val sirenColor = if (sin(animTick * 6f) > 0) Color.Red else Color.Blue
                drawScope.drawCircle(sirenColor, 3f * z, Offset(p.x, p.y - bodyH * 1.6f))
            }
        }
    }

    private fun drawPixelPedestrian(
        drawScope: DrawScope,
        p: Pedestrian,
        tileW: Float,
        tileH: Float,
        lighting: DayNightLighting,
        weather: WeatherType
    ) {
        val screenPos = camera.project(p.x, p.y, 0f)
        val z = camera.zoom

        val skinColor = Color(0xFFFFCC80)
        val shirtColor = when (p.colorIndex % 4) {
            0 -> Color(0xFFE53935)
            1 -> Color(0xFF1E88E5)
            2 -> Color(0xFF43A047)
            else -> Color(0xFFFFB300)
        }

        val px = screenPos.x
        val py = screenPos.y

        // Head
        drawScope.drawRect(skinColor, Offset(px - 1f * z, py - 6f * z), Size(2f * z, 2f * z))
        // Shirt
        drawScope.drawRect(shirtColor, Offset(px - 1.5f * z, py - 4f * z), Size(3f * z, 2.5f * z))
        // Legs (animated walking swing)
        val legOffset = if (p.walkAnimFrame % 2 == 0) 0.8f * z else -0.8f * z
        drawScope.drawRect(Color(0xFF37474F), Offset(px - 1f * z, py - 1.5f * z), Size(1f * z, 2f * z))
        drawScope.drawRect(Color(0xFF37474F), Offset(px + 0.5f * z + legOffset, py - 1.5f * z), Size(1f * z, 2f * z))

        // Umbrella in rain
        if (weather == WeatherType.RAIN || weather == WeatherType.STORM) {
            drawScope.drawOval(
                color = Color(0xFFE91E63),
                topLeft = Offset(px - 4f * z, py - 10f * z),
                size = Size(8f * z, 4f * z)
            )
        }
    }

    // ==========================================
    // WEATHER & DAY/NIGHT ATMOSPHERE
    // ==========================================

    private fun drawWeatherAndAtmosphere(
        drawScope: DrawScope,
        weather: WeatherType,
        lighting: DayNightLighting,
        dayTime: Float
    ) {
        val w = camera.viewportWidth
        val h = camera.viewportHeight

        // Night / Dusk color overlay tint
        if (lighting.tintOverlayAlpha > 0f) {
            drawScope.drawRect(
                color = lighting.tintColor.copy(alpha = lighting.tintOverlayAlpha)
            )
        }

        // Weather Particles
        when (weather) {
            WeatherType.RAIN -> {
                val rainColor = Color(0xFFB0BEC5).copy(alpha = 0.55f)
                val count = 45
                for (i in 0 until count) {
                    val rx = ((i * 37f + weatherTick * 25f) % w)
                    val ry = ((i * 53f + weatherTick * 45f) % h)
                    drawScope.drawLine(
                        color = rainColor,
                        start = Offset(rx, ry),
                        end = Offset(rx - 4f, ry + 12f),
                        strokeWidth = 1.5f
                    )
                }
            }
            WeatherType.STORM -> {
                val rainColor = Color(0xFF90A4AE).copy(alpha = 0.75f)
                val count = 70
                for (i in 0 until count) {
                    val rx = ((i * 29f + weatherTick * 35f) % w)
                    val ry = ((i * 47f + weatherTick * 60f) % h)
                    drawScope.drawLine(
                        color = rainColor,
                        start = Offset(rx, ry),
                        end = Offset(rx - 6f, ry + 16f),
                        strokeWidth = 2f
                    )
                }
                // Lightning Flash
                if (sin(weatherTick * 0.8f) > 0.96f) {
                    drawScope.drawRect(Color.White.copy(alpha = 0.65f))
                }
            }
            WeatherType.CLOUDY -> {
                // Soft drifting cloud shadows
                val cloudX = (weatherTick * 4f) % (w + 200f) - 100f
                drawScope.drawOval(
                    color = Color.Black.copy(alpha = 0.08f),
                    topLeft = Offset(cloudX, h * 0.35f),
                    size = Size(180f, 90f)
                )
            }
            WeatherType.SUNNY -> {}
        }
    }

    // ==========================================
    // PIXEL DRAW UTILITIES
    // ==========================================

    private fun drawPixelIsometricBox(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        z: Float,
        width: Float,
        height: Float,
        roofColor: Color,
        leftWallColor: Color,
        rightWallColor: Color
    ) {
        val halfW = (width * z) / 2f
        val halfD = halfW / 2f
        val bH = height * z

        // 1. Left Wall (Polygon)
        val leftWall = Path().apply {
            moveTo(cx - halfW, cy - halfD)
            lineTo(cx, cy)
            lineTo(cx, cy - bH)
            lineTo(cx - halfW, cy - halfD - bH)
            close()
        }
        drawScope.drawPath(leftWall, color = leftWallColor, style = Fill)
        drawScope.drawPath(leftWall, color = Color.Black.copy(alpha = 0.15f), style = Stroke(width = 1f))

        // 2. Right Wall (Polygon)
        val rightWall = Path().apply {
            moveTo(cx, cy)
            lineTo(cx + halfW, cy - halfD)
            lineTo(cx + halfW, cy - halfD - bH)
            lineTo(cx, cy - bH)
            close()
        }
        drawScope.drawPath(rightWall, color = rightWallColor, style = Fill)
        drawScope.drawPath(rightWall, color = Color.Black.copy(alpha = 0.15f), style = Stroke(width = 1f))

        // 3. Top Diamond Roof
        val roof = Path().apply {
            moveTo(cx, cy - halfD * 2f - bH)
            lineTo(cx + halfW, cy - halfD - bH)
            lineTo(cx, cy - bH)
            lineTo(cx - halfW, cy - halfD - bH)
            close()
        }
        drawScope.drawPath(roof, color = roofColor, style = Fill)
        drawScope.drawPath(roof, color = Color.Black.copy(alpha = 0.15f), style = Stroke(width = 1f))
    }

    private fun drawConstructionSite(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        z: Float,
        density: DensityLevel,
        lighting: DayNightLighting,
        anim: Float
    ) {
        val frameColor = blendWithLighting(Color(0xFFFFB300), lighting)
        val h = when (density) {
            DensityLevel.LOW -> 12f * z
            DensityLevel.MEDIUM -> 22f * z
            DensityLevel.HIGH -> 36f * z
            DensityLevel.NONE -> 8f * z
        }

        // Scaffolding lattice frame
        drawScope.drawRect(frameColor, Offset(cx - 10f * z, cy - h), Size(20f * z, h), style = Stroke(width = 1.5f * z))
        drawScope.drawLine(frameColor, Offset(cx - 10f * z, cy), Offset(cx + 10f * z, cy - h), strokeWidth = 1.2f * z)
        drawScope.drawLine(frameColor, Offset(cx + 10f * z, cy), Offset(cx - 10f * z, cy - h), strokeWidth = 1.2f * z)

        // Yellow Tower Crane
        if (density == DensityLevel.HIGH || density == DensityLevel.MEDIUM) {
            val craneColor = blendWithLighting(Color(0xFFFFD600), lighting)
            val mastTop = cy - (h + 16f * z)
            drawScope.drawLine(craneColor, Offset(cx, cy), Offset(cx, mastTop), strokeWidth = 2.5f * z)
            // Crane boom arm
            drawScope.drawLine(craneColor, Offset(cx - 14f * z, mastTop), Offset(cx + 18f * z, mastTop), strokeWidth = 2f * z)
            // Hoisting cable
            val cableX = cx + sin(anim * 1.5f) * 8f * z
            drawScope.drawLine(Color.White, Offset(cableX, mastTop), Offset(cableX, mastTop + 8f * z), strokeWidth = 1f * z)
        }
    }

    private fun drawPixelSmokePuff(drawScope: DrawScope, sx: Float, sy: Float, z: Float, anim: Float) {
        val smokeColor = Color(0xFF757575).copy(alpha = 0.5f)
        val y1 = sy - (3f + (anim * 3f) % 12f) * z
        val y2 = sy - (7f + (anim * 3f + 4f) % 12f) * z
        drawScope.drawCircle(smokeColor, 3f * z, Offset(sx + sin(anim) * 2f * z, y1))
        drawScope.drawCircle(smokeColor, 4.5f * z, Offset(sx + sin(anim + 1f) * 3f * z, y2))
    }

    private fun drawPixelFireAndSmoke(drawScope: DrawScope, cx: Float, cy: Float, z: Float, anim: Float) {
        // Leaping 2D pixel flames
        val fRadius = 8f * z + sin(anim * 4f) * 3f * z
        drawScope.drawCircle(Color(0xFFFF3D00).copy(alpha = 0.85f), fRadius, Offset(cx, cy))
        drawScope.drawCircle(Color(0xFFFFEA00).copy(alpha = 0.9f), fRadius * 0.6f, Offset(cx, cy))
        // Billowing dark smoke
        drawScope.drawCircle(Color(0xFF212121).copy(alpha = 0.7f), 6f * z, Offset(cx, cy - 10f * z))
    }

    private fun drawTriangle(drawScope: DrawScope, cx: Float, cy: Float, width: Float, height: Float, color: Color) {
        val path = Path().apply {
            moveTo(cx, cy - height)
            lineTo(cx + width / 2f, cy)
            lineTo(cx - width / 2f, cy)
            close()
        }
        drawScope.drawPath(path, color = color, style = Fill)
    }

    private fun drawPixelHoverCursor(
        drawScope: DrawScope,
        hx: Int,
        hy: Int,
        tileW: Float,
        tileH: Float,
        tool: ActiveTool
    ) {
        val p = camera.project(hx.toFloat(), hy.toFloat())
        val halfW = tileW / 2f
        val halfH = tileH / 2f

        val diamondPath = Path().apply {
            moveTo(p.x, p.y - halfH)
            lineTo(p.x + halfW, p.y)
            lineTo(p.x, p.y + halfH)
            lineTo(p.x - halfW, p.y)
            close()
        }

        val cColor = when (tool.mode) {
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

        drawScope.drawPath(diamondPath, color = cColor.copy(alpha = 0.38f), style = Fill)
        drawScope.drawPath(diamondPath, color = cColor, style = Stroke(width = 2.5f))
    }

    private fun getOverlayColor(tile: GridTile, overlay: OverlayMode, fallback: Color): Color {
        return when (overlay) {
            OverlayMode.ZONES -> when (tile.zone.category) {
                ZoneCategory.RESIDENTIAL -> when (tile.zone.density) {
                    DensityLevel.LOW -> Color(0xFF81C784)
                    DensityLevel.MEDIUM -> Color(0xFF4CAF50)
                    DensityLevel.HIGH -> Color(0xFF2E7D32)
                    DensityLevel.NONE -> fallback
                }
                ZoneCategory.COMMERCIAL -> when (tile.zone.density) {
                    DensityLevel.LOW -> Color(0xFF64B5F6)
                    DensityLevel.MEDIUM -> Color(0xFF2196F3)
                    DensityLevel.HIGH -> Color(0xFF1565C0)
                    DensityLevel.NONE -> fallback
                }
                ZoneCategory.INDUSTRIAL -> when (tile.zone.density) {
                    DensityLevel.LOW -> Color(0xFFFFEE58)
                    DensityLevel.MEDIUM -> Color(0xFFFFCA28)
                    DensityLevel.HIGH -> Color(0xFFF57F17)
                    DensityLevel.NONE -> fallback
                }
                ZoneCategory.NONE -> fallback
            }
            OverlayMode.TRAFFIC -> if (tile.road != RoadType.NONE) {
                when {
                    tile.trafficVolume > 0.7f -> Color(0xFFE53935)
                    tile.trafficVolume > 0.4f -> Color(0xFFFFB300)
                    else -> Color(0xFF43A047)
                }
            } else fallback
            OverlayMode.POLLUTION -> {
                val pRatio = (tile.airPollution / 100f).coerceIn(0f, 1f)
                if (pRatio > 0.05f) Color(0xFF8E24AA).copy(alpha = pRatio * 0.75f) else fallback
            }
            OverlayMode.LAND_VALUE -> {
                val lv = (tile.landValue / 100f).coerceIn(0f, 1f)
                Color(0xFFFFD54F).copy(alpha = 0.2f + lv * 0.75f)
            }
            OverlayMode.POWER -> {
                if (tile.building?.isPowered == true || tile.utility?.category == UtilityCategory.POWER) {
                    Color(0xFF00E5FF).copy(alpha = 0.65f)
                } else if (tile.building != null && !tile.building!!.isPowered) {
                    Color(0xFFFF1744).copy(alpha = 0.7f)
                } else fallback
            }
            OverlayMode.WATER -> {
                if (tile.building?.isWatered == true || tile.utility?.category == UtilityCategory.WATER) {
                    Color(0xFF00B0FF).copy(alpha = 0.65f)
                } else if (tile.building != null && !tile.building!!.isWatered) {
                    Color(0xFFFF5252).copy(alpha = 0.7f)
                } else fallback
            }
            OverlayMode.SERVICES -> {
                val cov = (tile.policeCoverage + tile.fireCoverage + tile.healthCoverage + tile.educationCoverage + tile.parkCoverage).coerceIn(0, 100) / 100f
                Color(0xFF00E676).copy(alpha = 0.15f + cov * 0.7f)
            }
            OverlayMode.NORMAL -> fallback
        }
    }

    private fun calculateDayNightLighting(dayTime: Float): DayNightLighting {
        return when {
            // Dawn (05:00 - 08:00)
            dayTime in 5.0f..8.0f -> {
                val t = (dayTime - 5.0f) / 3.0f
                DayNightLighting(
                    skyColor = lerpColor(Color(0xFF101935), Color(0xFF87CEEB), t),
                    lightMultiplier = 0.55f + t * 0.45f,
                    tintColor = Color(0xFFFFB74D),
                    tintOverlayAlpha = (1f - t) * 0.25f
                )
            }
            // Daytime (08:00 - 17:00)
            dayTime in 8.0f..17.0f -> {
                DayNightLighting(
                    skyColor = Color(0xFF81D4FA),
                    lightMultiplier = 1.0f,
                    tintColor = Color.White,
                    tintOverlayAlpha = 0f
                )
            }
            // Dusk / Sunset (17:00 - 20:00)
            dayTime in 17.0f..20.0f -> {
                val t = (dayTime - 17.0f) / 3.0f
                DayNightLighting(
                    skyColor = lerpColor(Color(0xFF81D4FA), Color(0xFF0D1B2A), t),
                    lightMultiplier = 1.0f - t * 0.55f,
                    tintColor = Color(0xFFFF7043),
                    tintOverlayAlpha = t * 0.35f
                )
            }
            // Night (20:00 - 05:00)
            else -> {
                DayNightLighting(
                    skyColor = Color(0xFF070B19),
                    lightMultiplier = 0.45f,
                    tintColor = Color(0xFF1E293B),
                    tintOverlayAlpha = 0.45f
                )
            }
        }
    }

    private fun blendWithLighting(color: Color, lighting: DayNightLighting): Color {
        return Color(
            red = (color.red * lighting.lightMultiplier).coerceIn(0f, 1f),
            green = (color.green * lighting.lightMultiplier).coerceIn(0f, 1f),
            blue = (color.blue * lighting.lightMultiplier).coerceIn(0f, 1f),
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

    private data class DayNightLighting(
        val skyColor: Color,
        val lightMultiplier: Float,
        val tintColor: Color,
        val tintOverlayAlpha: Float
    )
}
