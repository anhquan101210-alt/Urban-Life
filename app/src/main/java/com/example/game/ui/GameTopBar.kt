package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.game.model.CityStats
import com.example.game.renderer.Camera3D
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GameTopBar(
    stats: CityStats,
    camera: Camera3D,
    fps: Int,
    showFps: Boolean,
    onSpeedChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xDD0D1B2A),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. City Stats (Pop, Happiness, Treasury, Date)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Population
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Population",
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = NumberFormat.getNumberInstance().format(stats.population),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.testTag("population_text")
                    )
                }

                // Happiness
                val hapColor = when {
                    stats.happiness >= 80 -> Color(0xFF81C784)
                    stats.happiness >= 60 -> Color(0xFFFFD54F)
                    else -> Color(0xFFE57373)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (stats.happiness >= 70) Icons.Default.SentimentSatisfiedAlt else Icons.Default.SentimentDissatisfied,
                        contentDescription = "Happiness",
                        tint = hapColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.happiness}%",
                        color = hapColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Treasury
                val moneyColor = if (stats.treasury >= 0) Color(0xFF81C784) else Color(0xFFE57373)
                val dailyDelta = stats.dailyIncome - stats.dailyExpenses
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonetizationOn,
                        contentDescription = "Funds",
                        tint = moneyColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = currencyFormat.format(stats.treasury),
                            color = moneyColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.testTag("treasury_text")
                        )
                        Text(
                            text = (if (dailyDelta >= 0) "+$" else "-$") + NumberFormat.getNumberInstance().format(kotlin.math.abs(dailyDelta)) + "/d",
                            color = if (dailyDelta >= 0) Color(0xFFA5D6A7) else Color(0xFFEF9A9A),
                            fontSize = 9.sp
                        )
                    }
                }

                // Day / Night & Clock
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val hour = stats.dayTime.toInt()
                    val minute = ((stats.dayTime - hour) * 60).toInt()
                    val isDay = stats.dayTime in 6.0f..18.5f
                    Icon(
                        if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = "Time",
                        tint = if (isDay) Color(0xFFFFD54F) else Color(0xFF90CAF9),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Day ${stats.dayCount}  %02d:%02d".format(hour, minute),
                        color = Color(0xFFCFD8DC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 2. RCI Demand Indicator
            RciDemandMeter(
                rDemand = stats.residentialDemand,
                cDemand = stats.commercialDemand,
                iDemand = stats.industrialDemand
            )

            // 3. Camera shortcuts & Sim Speed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Camera buttons
                CameraQuickButtons(camera)

                // Speed controls
                SpeedButtons(
                    currentSpeed = stats.simSpeed,
                    onSpeedChanged = onSpeedChanged
                )

                // FPS
                if (showFps) {
                    Surface(
                        color = Color(0x88000000),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "$fps FPS",
                            color = if (fps >= 45) Color(0xFF81C784) else Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RciDemandMeter(
    rDemand: Float,
    cDemand: Float,
    iDemand: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .background(Color(0x66000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text("RCI", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        // R (Green)
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(18.dp)
                .background(Color(0x444CAF50), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(rDemand.coerceIn(0.1f, 1f))
                    .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
            )
        }
        // C (Blue)
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(18.dp)
                .background(Color(0x442196F3), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(cDemand.coerceIn(0.1f, 1f))
                    .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
            )
        }
        // I (Yellow)
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(18.dp)
                .background(Color(0x44FFC107), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(iDemand.coerceIn(0.1f, 1f))
                    .background(Color(0xFFFFC107), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun CameraQuickButtons(camera: Camera3D) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Home
        SmallIconButton(
            icon = Icons.Default.Home,
            tooltip = "Center City",
            testTag = "camera_home_button",
            onClick = { camera.centerOn(18f, 18f) }
        )
        // Iso
        SmallIconButton(
            icon = Icons.Default.ViewInAr,
            tooltip = "Isometric",
            testTag = "camera_iso_button",
            onClick = { camera.setIsometricView() }
        )
        // Close
        SmallIconButton(
            icon = Icons.Default.ZoomIn,
            tooltip = "Close View",
            testTag = "camera_close_button",
            onClick = { camera.setCloseView() }
        )
        // Top
        SmallIconButton(
            icon = Icons.Default.Navigation,
            tooltip = "Top Down",
            testTag = "camera_top_button",
            onClick = { camera.setTopView() }
        )
    }
}

@Composable
fun SpeedButtons(
    currentSpeed: Int,
    onSpeedChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .background(Color(0x66000000), RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SpeedItem(label = "⏸", isSelected = currentSpeed == 0, onClick = { onSpeedChanged(0) })
        SpeedItem(label = "1x", isSelected = currentSpeed == 1, onClick = { onSpeedChanged(1) })
        SpeedItem(label = "2x", isSelected = currentSpeed == 2, onClick = { onSpeedChanged(2) })
        SpeedItem(label = "4x", isSelected = currentSpeed == 4, onClick = { onSpeedChanged(4) })
    }
}

@Composable
fun SpeedItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF29B6F6) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x44FFFFFF))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}
