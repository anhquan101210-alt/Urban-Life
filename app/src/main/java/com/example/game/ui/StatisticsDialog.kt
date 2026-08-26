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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(10.dp)
                .testTag("statistics_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101C2E),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 16.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CITY METRICS & ANALYTICS",
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Demographics Grid
                    Text("DEMOGRAPHICS & LABOR", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            label = "Population",
                            value = NumberFormat.getNumberInstance().format(stats.population),
                            color = Color(0xFF81C784),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Jobs Available",
                            value = NumberFormat.getNumberInstance().format(stats.jobsTotal),
                            color = Color(0xFF64B5F6),
                            modifier = Modifier.weight(1f)
                        )
                        val empRate = if (stats.population > 0) ((stats.jobsEmployed.toFloat() / (stats.population * 0.6f)) * 100).toInt().coerceIn(0, 100) else 100
                        MetricCard(
                            label = "Employment Rate",
                            value = "$empRate%",
                            color = if (empRate >= 80) Color(0xFF81C784) else Color(0xFFFFB74D),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 2. Power & Water Infrastructure
                    Text("UTILITIES & CAPACITY", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    UtilityBar(
                        label = "Electrical Grid",
                        produced = stats.powerCapacityMW,
                        consumed = stats.powerDemandMW,
                        unit = "MW",
                        barColor = Color(0xFFFFB300)
                    )

                    UtilityBar(
                        label = "Water Network",
                        produced = stats.waterCapacityMG,
                        consumed = stats.waterDemandMG,
                        unit = "MG",
                        barColor = Color(0xFF00B0FF)
                    )

                    // 3. Quality of Life
                    Text("QUALITY OF LIFE", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricCard(
                            label = "Happiness",
                            value = "${stats.happiness}%",
                            color = if (stats.happiness >= 75) Color(0xFF81C784) else Color(0xFFE57373),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Traffic Congestion",
                            value = "${stats.trafficIndex}%",
                            color = if (stats.trafficIndex <= 40) Color(0xFF81C784) else Color(0xFFEF5350),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Avg Land Value",
                            value = "${stats.averageLandValue}",
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 4. Historical Population Trend
                    if (history.size > 2) {
                        Text("POPULATION GROWTH OVER TIME", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x22FFFFFF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val maxPop = history.maxOfOrNull { it.population }?.coerceAtLeast(10) ?: 10
                                history.takeLast(20).forEach { pt ->
                                    val heightFraction = (pt.population.toFloat() / maxPop).coerceIn(0.05f, 1f)
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .fillMaxHeight(heightFraction)
                                            .background(Color(0xFF64B5F6), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x22FFFFFF)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, color = Color.LightGray, fontSize = 9.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
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
    val isOverloaded = produced < consumed

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 11.sp)
            Text(
                text = "$consumed / $produced $unit",
                color = if (isOverloaded) Color(0xFFFF1744) else barColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(usageRatio.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(if (isOverloaded) Color(0xFFFF1744) else barColor, RoundedCornerShape(5.dp))
            )
        }
    }
}
