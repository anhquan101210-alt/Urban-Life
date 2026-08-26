package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun CityDemandDialog(
    stats: CityStats,
    onDismiss: () -> Unit
) {
    val rDemand = (stats.residentialDemand * 100).toInt()
    val cDemand = (stats.commercialDemand * 100).toInt()
    val iDemand = (stats.industrialDemand * 100).toInt()

    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .testTag("city_demand_dialog"),
            borderColor = PixelColors.PanelBorder,
            backgroundColor = PixelColors.PanelBgSolid
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Title Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 CITY RCI DEMAND",
                        color = PixelColors.AccentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                // Residential Demand Card
                DemandItemRow(
                    category = "RESIDENTIAL (R)",
                    demandPercent = rDemand,
                    color = PixelColors.AccentGreen,
                    status = when {
                        rDemand > 70 -> "High Demand"
                        rDemand > 35 -> "Moderate Demand"
                        else -> "Low Demand"
                    },
                    explanation = when {
                        rDemand > 70 -> "Citizens need more homes. Zone Low, Medium, or High Residential near road networks and services."
                        rDemand > 35 -> "Steady housing demand. Existing neighborhoods are expanding steadily."
                        else -> "Housing supply is sufficient. Citizens are seeking more jobs and leisure."
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Commercial Demand Card
                DemandItemRow(
                    category = "COMMERCIAL (C)",
                    demandPercent = cDemand,
                    color = PixelColors.AccentBlue,
                    status = when {
                        cDemand > 70 -> "High Demand"
                        cDemand > 35 -> "Moderate Demand"
                        else -> "Low Demand"
                    },
                    explanation = when {
                        cDemand > 70 -> "Residents want shopping, stores, and office jobs. Zone Commercial areas along busy avenues."
                        cDemand > 35 -> "Commercial retail and office capacity is well balanced."
                        else -> "Sufficient shops available. Boost population to increase commercial demand."
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Industrial Demand Card
                DemandItemRow(
                    category = "INDUSTRIAL (I)",
                    demandPercent = iDemand,
                    color = PixelColors.AccentOrange,
                    status = when {
                        iDemand > 70 -> "High Demand"
                        iDemand > 35 -> "Moderate Demand"
                        else -> "Low Demand"
                    },
                    explanation = when {
                        iDemand > 70 -> "Factories and tech parks are looking to invest. Zone Industrial away from residential neighborhoods."
                        iDemand > 35 -> "Industrial sector has stable production."
                        else -> "Industrial jobs quota is full."
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Close Button
                PixelButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF1E3A5F),
                    selectedColor = PixelColors.AccentCyan
                ) {
                    Text("CLOSE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DemandItemRow(
    category: String,
    demandPercent: Int,
    color: Color,
    status: String,
    explanation: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C1929))
            .border(1.dp, Color(0xFF1B324D), RoundedCornerShape(6.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            PixelBadge(text = "$demandPercent% • $status", color = color)
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Stepped progress bar
        val barFraction = (demandPercent / 100f).coerceIn(0.05f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF152233))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barFraction)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = explanation,
            color = Color(0xFFB0BEC5),
            fontSize = 9.5.sp,
            lineHeight = 13.sp
        )
    }
}
