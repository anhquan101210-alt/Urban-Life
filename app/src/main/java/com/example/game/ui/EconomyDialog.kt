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
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("economy_dialog"),
            borderColor = PixelColors.AccentGreen,
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
                        text = "💰 TREASURY & TAXATION",
                        color = PixelColors.AccentGreen,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                // Finance Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinanceSummaryCard(
                        title = "Daily Income",
                        amount = "+${currencyFormat.format(stats.dailyIncome)}",
                        color = PixelColors.AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryCard(
                        title = "Daily Cost",
                        amount = "-${currencyFormat.format(stats.dailyExpenses)}",
                        color = PixelColors.AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    FinanceSummaryCard(
                        title = "Net Flow",
                        amount = (if (netDaily >= 0) "+" else "") + currencyFormat.format(netDaily),
                        color = if (netDaily >= 0) PixelColors.AccentGreen else PixelColors.AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "TAX RATES",
                    color = PixelColors.AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Tax Sliders
                TaxSliderItem(
                    label = "Residential Tax",
                    value = resTax,
                    color = PixelColors.AccentGreen,
                    onValueChange = {
                        resTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                TaxSliderItem(
                    label = "Commercial Tax",
                    value = comTax,
                    color = PixelColors.AccentBlue,
                    onValueChange = {
                        comTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                TaxSliderItem(
                    label = "Industrial Tax",
                    value = indTax,
                    color = PixelColors.AccentOrange,
                    onValueChange = {
                        indTax = it
                        onTaxRatesChanged(resTax.roundToInt(), comTax.roundToInt(), indTax.roundToInt())
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PixelButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF1E3A5F)
                ) {
                    Text("DONE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
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
    Column(
        modifier = modifier
            .background(Color(0xFF0F1E30), RoundedCornerShape(4.dp))
            .padding(6.dp)
    ) {
        Text(text = title, color = Color(0xFF90A4AE), fontSize = 9.sp)
        Text(text = amount, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun TaxSliderItem(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFECEFF1), fontSize = 10.sp)
            Text(
                text = "${value.roundToInt()}%",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
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
                inactiveTrackColor = Color(0x22FFFFFF)
            )
        )
    }
}
