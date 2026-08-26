package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.CityStats
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CityOverviewDialog(
    stats: CityStats,
    onDismiss: () -> Unit
) {
    val numFormat = NumberFormat.getNumberInstance(Locale.US)
    val curFormat = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    val empRate = if (stats.population > 0) ((stats.jobsEmployed.toFloat() / (stats.population * 0.6f)) * 100).toInt().coerceIn(0, 100) else 100
    val powerDeficit = stats.powerCapacityMW < stats.powerDemandMW
    val waterDeficit = stats.waterCapacityMG < stats.waterDemandMG

    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("city_overview_dialog"),
            borderColor = PixelColors.PanelBorder,
            backgroundColor = PixelColors.PanelBgSolid
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏙 CITY OVERVIEW",
                        color = PixelColors.AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                // Key Stat Grid (2 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatTile("👥 Population", numFormat.format(stats.population), Color(0xFF64B5F6))
                        StatTile("💼 Employment Rate", "$empRate%", Color(0xFF81C784))
                        StatTile("🙂 Citizen Happiness", "${stats.happiness}%", Color(0xFFFFD54F))
                        StatTile("🏭 Available Jobs", numFormat.format(stats.jobsTotal), Color(0xFFBA68C8))
                        StatTile("💰 Treasury", curFormat.format(stats.treasury), Color(0xFF81C784))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatTile("🚗 Traffic Flow", "${100 - stats.trafficIndex}%", if (stats.trafficIndex < 35) Color(0xFF81C784) else Color(0xFFFFB74D))
                        StatTile("🏭 Air Pollution", "${stats.airPollutionIndex}%", if (stats.airPollutionIndex < 35) Color(0xFF81C784) else Color(0xFFE57373))
                        StatTile("🏡 Avg Land Value", "$${stats.averageLandValue}", Color(0xFFFFD54F))
                        StatTile("⚡ Power Demand", "${stats.powerDemandMW} MW", if (powerDeficit) Color(0xFFFF5252) else Color(0xFFFFD54F))
                        StatTile("💧 Water Demand", "${stats.waterDemandMG} MG", if (waterDeficit) Color(0xFFFF5252) else Color(0xFF64B5F6))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Infrastructure Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1B2A))
                        .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text("UTILITIES & GRID STATUS", color = PixelColors.AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Power: ${stats.powerDemandMW} / ${stats.powerCapacityMW} MW",
                            color = if (powerDeficit) Color(0xFFFF5252) else Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "💧 Water: ${stats.waterDemandMG} / ${stats.waterCapacityMG} MG",
                            color = if (waterDeficit) Color(0xFFFF5252) else Color(0xFF64B5F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
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

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0C1929))
            .border(1.dp, Color(0xFF182D46), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color(0xFFB0BEC5), fontSize = 10.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
    }
}
