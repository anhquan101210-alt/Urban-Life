package com.example.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Palette for TheoTown-inspired Pixel Art UI
object PixelColors {
    val PanelBg = Color(0xF20A1626)          // Deep dark teal/navy
    val PanelBgSolid = Color(0xFF0D1B2A)
    val PanelInner = Color(0xFF132A44)        // Inner container dark teal
    val PanelBorder = Color(0xFF1E3A5F)       // Outer border
    val PixelHighlight = Color(0xFF388E3C)    // Mint/emerald green
    val AccentCyan = Color(0xFF00E5FF)        // Neon Cyan
    val AccentGold = Color(0xFFFFD54F)        // Warm Gold
    val AccentGreen = Color(0xFF66BB6A)       // Lush Green
    val AccentBlue = Color(0xFF42A5F5)        // Vivid Blue
    val AccentOrange = Color(0xFFFFA726)      // Industrial Orange
    val AccentRed = Color(0xFFEF5350)         // Emergency Red
    val TextPrimary = Color(0xFFECEFF1)       // Crisp White-Blue
    val TextSecondary = Color(0xFF90A4AE)     // Soft Slate
    val TextMuted = Color(0xFF607D8B)         // Muted
    val SelectedGlow = Color(0x6600E5FF)      // Cyan Glow
}

@Composable
fun PixelPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = PixelColors.PanelBorder,
    backgroundColor: Color = PixelColors.PanelBg,
    elevation: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(8.dp))
            .padding(1.dp)
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(7.dp))
    ) {
        content()
    }
}

@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelColors.PanelInner,
    borderColor: Color = PixelColors.PanelBorder,
    isSelected: Boolean = false,
    selectedColor: Color = PixelColors.AccentCyan,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.95f else 1.0f

    val effectiveBg = when {
        !enabled -> Color(0xFF1A2634)
        isSelected -> selectedColor.copy(alpha = 0.28f)
        else -> backgroundColor
    }

    val effectiveBorder = when {
        !enabled -> Color(0xFF2C3E50)
        isSelected -> selectedColor
        else -> borderColor
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(effectiveBg)
            .border(BorderStroke(if (isSelected) 2.dp else 1.5.dp, effectiveBorder), RoundedCornerShape(6.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

@Composable
fun PixelDemandBar(
    label: String,
    value: Float, // 0.0 to 1.0
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        // Vertical stepped bar (10 segments)
        val segments = 8
        val activeSegments = (value.coerceIn(0f, 1f) * segments).toInt()

        Column(
            modifier = Modifier
                .width(8.dp)
                .height(20.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(2.dp))
                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(2.dp))
                .padding(1.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            for (i in 0 until segments) {
                val indexFromBottom = i
                val isActive = indexFromBottom < activeSegments
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .padding(vertical = 0.2.dp)
                        .background(
                            if (isActive) color else Color(0x22FFFFFF),
                            RoundedCornerShape(1.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun PixelBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, color), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}
