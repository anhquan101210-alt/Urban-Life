package com.example.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.game.model.CityStats
import com.example.game.model.WeatherType
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
    onOpenDemand: () -> Unit,
    onOpenCityInfo: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    PixelPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        borderColor = PixelColors.PanelBorder,
        backgroundColor = PixelColors.PanelBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Primary Metrics (Population, Happiness, Treasury, Date/Weather)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Population
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1E30))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("👥", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = numberFormat.format(stats.population),
                        color = Color(0xFFE0E6ED),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.testTag("population_text")
                    )
                }

                // Happiness
                val hapColor = when {
                    stats.happiness >= 80 -> PixelColors.AccentGreen
                    stats.happiness >= 55 -> PixelColors.AccentGold
                    else -> PixelColors.AccentRed
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1E30))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(if (stats.happiness >= 70) "🙂" else "😐", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${stats.happiness}%",
                        color = hapColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }

                // Treasury & Cashflow
                val netIncome = stats.dailyIncome - stats.dailyExpenses
                val netIncomeColor = if (netIncome >= 0) PixelColors.AccentGreen else PixelColors.AccentRed
                val sign = if (netIncome >= 0) "+" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1E30))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text("💰", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = currencyFormat.format(stats.treasury),
                        color = if (stats.treasury >= 0) Color(0xFF81C784) else PixelColors.AccentRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "($sign$$netIncome/d)",
                        color = netIncomeColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.5.sp
                    )
                }

                // Weather & Game Time
                val hours = stats.dayTime.toInt()
                val minutes = ((stats.dayTime - hours) * 60).toInt()
                val timeString = String.format(Locale.US, "%02d:%02d", hours, minutes)
                val weatherIcon = when (stats.weather) {
                    WeatherType.SUNNY -> "☀"
                    WeatherType.CLOUDY -> "☁"
                    WeatherType.RAIN -> "🌧"
                    WeatherType.STORM -> "⛈"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F1E30))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(weatherIcon, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Day ${stats.dayCount} $timeString",
                        color = Color(0xFFB0BEC5),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }

            // 2. Interactive RCI Demand Widget
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0F1E30))
                    .border(BorderStroke(1.dp, Color(0xFF1E3A5F)), RoundedCornerShape(4.dp))
                    .clickable { onOpenDemand() }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
                    .testTag("rci_widget")
            ) {
                Text(
                    text = "RCI",
                    color = Color(0xFF90A4AE),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                PixelDemandBar(label = "R", value = stats.residentialDemand / 100f, color = PixelColors.AccentGreen)
                PixelDemandBar(label = "C", value = stats.commercialDemand / 100f, color = PixelColors.AccentBlue)
                PixelDemandBar(label = "I", value = stats.industrialDemand / 100f, color = PixelColors.AccentOrange)
            }

            // 3. Paused Indicator (Blinking when simSpeed == 0)
            if (stats.simSpeed == 0) {
                val infiniteTransition = rememberInfiniteTransition(label = "paused_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFFF1744).copy(alpha = alpha * 0.4f))
                        .border(BorderStroke(1.dp, Color(0xFFFF5252)), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⏸ PAUSED",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.5.sp
                    )
                }
            }

            // 4. Speed Controls & Extra Shortcuts
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pause Button
                SpeedButton(
                    label = "⏸",
                    isSelected = stats.simSpeed == 0,
                    testTag = "speed_btn_pause",
                    onClick = { onSpeedChanged(0) }
                )

                // 1X Speed
                SpeedButton(
                    label = "1X",
                    isSelected = stats.simSpeed == 1,
                    testTag = "speed_btn_1x",
                    onClick = { onSpeedChanged(1) }
                )

                // 2X Speed
                SpeedButton(
                    label = "2X",
                    isSelected = stats.simSpeed == 2,
                    testTag = "speed_btn_2x",
                    onClick = { onSpeedChanged(2) }
                )

                // 4X Speed
                SpeedButton(
                    label = "4X",
                    isSelected = stats.simSpeed == 4,
                    testTag = "speed_btn_4x",
                    onClick = { onSpeedChanged(4) }
                )

                // City Overview Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF132A44))
                        .border(1.dp, Color(0xFF204A75), RoundedCornerShape(4.dp))
                        .clickable { onOpenCityInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏙", fontSize = 11.sp)
                }

                // Settings Button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF132A44))
                        .border(1.dp, Color(0xFF204A75), RoundedCornerShape(4.dp))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Optional FPS Badge
                if (showFps) {
                    Text(
                        text = "${fps}fps",
                        color = Color(0xFF78909C),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedButton(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp, 22.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) PixelColors.AccentCyan.copy(alpha = 0.35f) else Color(0xFF0F1E30))
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) PixelColors.AccentCyan else Color(0xFF1E3A5F)
                ),
                RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PixelColors.AccentCyan else Color(0xFFCFD8DC),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}
