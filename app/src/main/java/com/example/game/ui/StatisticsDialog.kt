package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.CityStats
import com.example.game.model.HistoryPoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StatisticsDialog(
    stats: CityStats,
    history: List<HistoryPoint>,
    onDismiss: () -> Unit
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val empRate = if (stats.population > 0) ((stats.jobsEmployed.toFloat() / (stats.population * 0.6f)) * 100).toInt().coerceIn(0, 100) else 100

    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("statistics_dialog"),
            borderColor = PixelColors.AccentBlue,
            backgroundColor = PixelColors.PanelBgSolid
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 CITY STATISTICS & HISTORY",
                        color = PixelColors.AccentBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Demographics
                    Text("DEMOGRAPHICS & JOBS", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetricCard(
                            label = "Population",
                            value = numberFormat.format(stats.population),
                            color = PixelColors.AccentGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Employment Rate",
                            value = "$empRate%",
                            color = PixelColors.AccentBlue,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Happiness",
                            value = "${stats.happiness}%",
                            color = PixelColors.AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. Power & Water
                    Text("UTILITIES INFRASTRUCTURE", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)

                    UtilityBar(
                        label = "⚡ Electrical Grid",
                        produced = stats.powerCapacityMW,
                        consumed = stats.powerDemandMW,
                        unit = "MW",
                        barColor = PixelColors.AccentGold
                    )

                    UtilityBar(
                        label = "💧 Water Supply",
                        produced = stats.waterCapacityMG,
                        consumed = stats.waterDemandMG,
                        unit = "MG",
                        barColor = PixelColors.AccentCyan
                    )

                    // 3. City Ratings
                    Text("CITY INDEX RATINGS", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetricCard(
                            label = "Traffic Flow",
                            value = "${100 - stats.trafficIndex}%",
                            color = if (stats.trafficIndex <= 35) PixelColors.AccentGreen else PixelColors.AccentRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Air Pollution",
                            value = "${stats.airPollutionIndex}%",
                            color = if (stats.airPollutionIndex <= 30) PixelColors.AccentGreen else PixelColors.AccentRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Avg Land Value",
                            value = "$${stats.averageLandValue}",
                            color = PixelColors.AccentGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. History Chart
                    if (history.size > 2) {
                        Text("POPULATION TREND (RECENT TICKS)", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .background(Color(0xFF0F1E30), RoundedCornerShape(4.dp))
                                .padding(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val maxPop = history.maxOfOrNull { it.population }?.coerceAtLeast(10) ?: 10
                                history.takeLast(24).forEach { pt ->
                                    val heightFraction = (pt.population.toFloat() / maxPop).coerceIn(0.06f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .fillMaxHeight(heightFraction)
                                            .background(PixelColors.AccentGreen, RoundedCornerShape(1.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    PixelButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF1E3A5F)
                    ) {
                        Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF0F1E30), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(text = label, color = Color(0xFF90A4AE), fontSize = 8.5.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
    }
}

@Composable
private fun UtilityBar(
    label: String,
    produced: Int,
    consumed: Int,
    unit: String,
    barColor: Color
) {
    val usageRatio = if (produced > 0) (consumed.toFloat() / produced).coerceIn(0f, 1.5f) else if (consumed > 0) 1.5f else 0f
    val isDeficit = produced < consumed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1E30), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFECEFF1), fontSize = 9.5.sp)
            Text(
                text = "$consumed / $produced $unit",
                color = if (isDeficit) PixelColors.AccentRed else barColor,
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF1A293E), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usageRatio.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(if (isDeficit) PixelColors.AccentRed else barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}
