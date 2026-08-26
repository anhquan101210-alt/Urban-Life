package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.WorldMap
import com.example.game.model.*
import com.example.game.renderer.Camera3D

@Composable
fun MinimapWidget(
    world: WorldMap,
    camera: Camera3D,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var currentZoomPreset by remember { mutableIntStateOf(2) }

    PixelPanel(
        modifier = modifier
            .testTag("minimap_widget")
            .wrapContentSize(),
        borderColor = PixelColors.PanelBorder,
        backgroundColor = PixelColors.PanelBg
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Minimap Title & Expand/Collapse Toggle
            Row(
                modifier = Modifier
                    .width(if (isExpanded) 120.dp else 60.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "MAP" else "MAP",
                    color = PixelColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = "Toggle Minimap",
                    tint = Color.LightGray,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }

            if (isExpanded) {
                // 2D Pixel Minimap Canvas (Isometric diamond / ortho minimap projection)
                Box(
                    modifier = Modifier
                        .size(116.dp, 100.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF091422))
                        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val gx = (offset.x / 116.dp.toPx() * world.width).coerceIn(0f, 35f)
                                val gy = (offset.y / 100.dp.toPx() * world.height).coerceIn(0f, 35f)
                                camera.centerOn(gx, gy)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                val gx = (change.position.x / 116.dp.toPx() * world.width).coerceIn(0f, 35f)
                                val gy = (change.position.y / 100.dp.toPx() * world.height).coerceIn(0f, 35f)
                                camera.centerOn(gx, gy)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val tileW = size.width / world.width.toFloat()
                        val tileH = size.height / world.height.toFloat()

                        for (x in 0 until world.width) {
                            for (y in 0 until world.height) {
                                val tile = world.getTile(x, y) ?: continue
                                val px = x * tileW
                                val py = y * tileH

                                val tileColor = when {
                                    tile.building?.onFire == true -> Color(0xFFFF3D00)
                                    tile.building != null -> when (tile.zone) {
                                        ZoneType.RESIDENTIAL_LOW, ZoneType.RESIDENTIAL_MED, ZoneType.RESIDENTIAL_HIGH -> Color(0xFF4CAF50)
                                        ZoneType.COMMERCIAL_LOW, ZoneType.COMMERCIAL_MED, ZoneType.COMMERCIAL_HIGH -> Color(0xFF2196F3)
                                        ZoneType.INDUSTRIAL_LOW, ZoneType.INDUSTRIAL_MED, ZoneType.INDUSTRIAL_HIGH -> Color(0xFFFFB300)
                                        else -> Color(0xFFB0BEC5)
                                    }
                                    tile.service != null -> Color(0xFFE91E63)
                                    tile.utility != null -> Color(0xFFFFEB3B)
                                    tile.road != RoadType.NONE -> Color(0xFFECEFF1)
                                    tile.zone == ZoneType.RESIDENTIAL_LOW || tile.zone == ZoneType.RESIDENTIAL_MED || tile.zone == ZoneType.RESIDENTIAL_HIGH -> Color(0x774CAF50)
                                    tile.zone == ZoneType.COMMERCIAL_LOW || tile.zone == ZoneType.COMMERCIAL_MED || tile.zone == ZoneType.COMMERCIAL_HIGH -> Color(0x772196F3)
                                    tile.zone == ZoneType.INDUSTRIAL_LOW || tile.zone == ZoneType.INDUSTRIAL_MED || tile.zone == ZoneType.INDUSTRIAL_HIGH -> Color(0x77FFB300)
                                    tile.terrain == TerrainType.WATER -> Color(0xFF0288D1)
                                    tile.terrain == TerrainType.SHORE -> Color(0xFFDCE775)
                                    tile.terrain == TerrainType.ROCK -> Color(0xFF78909C)
                                    tile.treeType > 0 -> Color(0xFF2E7D32)
                                    else -> Color(0xFF388E3C)
                                }

                                drawRect(
                                    color = tileColor,
                                    topLeft = Offset(px, py),
                                    size = Size(tileW + 0.5f, tileH + 0.5f)
                                )
                            }
                        }

                        // Draw Camera Viewport indicator frame
                        val camGX = camera.targetX.coerceIn(0f, 35f)
                        val camGY = camera.targetY.coerceIn(0f, 35f)
                        val camPX = camGX * tileW
                        val camPY = camGY * tileH

                        val viewRange = 8f / camera.zoom
                        val viewW = (viewRange * tileW * 2f).coerceIn(16f, size.width)
                        val viewH = (viewRange * tileH * 2f).coerceIn(14f, size.height)

                        drawRect(
                            color = Color(0xFF00E5FF),
                            topLeft = Offset((camPX - viewW / 2f).coerceIn(0f, size.width - viewW), (camPY - viewH / 2f).coerceIn(0f, size.height - viewH)),
                            size = Size(viewW, viewH),
                            style = Stroke(width = 1.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quick Camera Tools (Zoom +, Zoom -, Center, and 4 Presets)
                Row(
                    modifier = Modifier.width(116.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelCamButton(
                        text = "+",
                        onClick = { camera.zoomBy(1.25f) }
                    )
                    PixelCamButton(
                        text = "🎯",
                        onClick = { camera.centerOn(18f, 18f) }
                    )
                    PixelCamButton(
                        text = "-",
                        onClick = { camera.zoomBy(0.8f) }
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 4 Zoom Level Presets (1=City, 2=District, 3=Neigh, 4=Street)
                Row(
                    modifier = Modifier.width(116.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZoomPresetButton("1", "City", currentZoomPreset == 1) {
                        currentZoomPreset = 1
                        camera.setZoomPreset(1)
                    }
                    ZoomPresetButton("2", "Dist", currentZoomPreset == 2) {
                        currentZoomPreset = 2
                        camera.setZoomPreset(2)
                    }
                    ZoomPresetButton("3", "Nbr", currentZoomPreset == 3) {
                        currentZoomPreset = 3
                        camera.setZoomPreset(3)
                    }
                    ZoomPresetButton("4", "Strt", currentZoomPreset == 4) {
                        currentZoomPreset = 4
                        camera.setZoomPreset(4)
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelCamButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp, 20.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF132A44))
            .border(1.dp, Color(0xFF204A75), RoundedCornerShape(3.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun RowScope.ZoomPresetButton(
    label: String,
    sub: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) PixelColors.AccentCyan.copy(alpha = 0.35f) else Color(0xFF102035))
            .border(
                1.dp,
                if (isSelected) PixelColors.AccentCyan else Color(0xFF1F3A5A),
                RoundedCornerShape(3.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PixelColors.AccentCyan else Color(0xFF90A4AE),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}
