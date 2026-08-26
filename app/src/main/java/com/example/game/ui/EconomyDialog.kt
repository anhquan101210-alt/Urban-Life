package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.model.CityStats
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EconomyDialog(
    stats: CityStats,
    onTaxRatesChanged: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var resTax by remember { mutableStateOf(stats.residentialTaxRate.toFloat()) }
    var comTax by remember { mutableStateOf(stats.commercialTaxRate.toFloat()) }
    var indTax by remember { mutableStateOf(stats.industrialTaxRate.toFloat()) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    val netDaily = stats.dailyIncome - stats.dailyExpenses

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(12.dp)
                .testTag("economy_dialog"),
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
                        text = "CITY TREASURY & TAXATION",
                        color = Color(0xFF81C784),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                // Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FinanceSummaryCard(
                        title = "Daily Income",
                        amount = "+${currencyFormat.format(stats.dailyIncome)}",
                        color = Color(0xFF81C784),
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryCard(
                        title = "Daily Expenses",
                        amount = "-${currencyFormat.format(stats.dailyExpenses)}",
                        color = Color(0xFFEF5350),
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryCard(
                        title = "Net Cashflow",
                        amount = (if (netDaily >= 0) "+" else "") + currencyFormat.format(netDaily),
                        color = if (netDaily >= 0) Color(0xFF4CAF50) else Color(0xFFFF1744),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TAX POLICIES",
                    color = Color(0xFF90CAF9),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tax Sliders
                TaxSliderItem(
                    label = "Residential Tax",
                    value = resTax,
                    color = Color(0xFF4CAF50),
                    onValueChange = {
                        resTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                TaxSliderItem(
                    label = "Commercial Tax",
                    value = comTax,
                    color = Color(0xFF2196F3),
                    onValueChange = {
                        comTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                TaxSliderItem(
                    label = "Industrial Tax",
                    value = indTax,
                    color = Color(0xFFFFC107),
                    onValueChange = {
                        indTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "High taxes increase immediate revenue but reduce building growth and citizen happiness.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun FinanceSummaryCard(
    title: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x22FFFFFF)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = title, color = Color.LightGray, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TaxSliderItem(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = 11.sp)
            Text(
                text = "${value.roundToInt()}%",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 1f..20f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
