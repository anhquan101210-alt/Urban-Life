package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.example.game.audio.SoundManager
import com.example.game.model.GraphicsQuality

@Composable
fun SettingsDialog(
    graphicsQuality: GraphicsQuality,
    showFps: Boolean,
    soundManager: SoundManager,
    onGraphicsChanged: (GraphicsQuality) -> Unit,
    onToggleFps: () -> Unit,
    onSaveCity: () -> Unit,
    onLoadCity: () -> Unit,
    onResetCity: () -> Unit,
    onDismiss: () -> Unit
) {
    var soundOn by remember { mutableStateOf(soundManager.soundEnabled) }
    var hapticsOn by remember { mutableStateOf(soundManager.hapticsEnabled) }

    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("settings_dialog"),
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
                    Column {
                        Text(
                            text = "⚙ GAME SETTINGS",
                            color = PixelColors.AccentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Urban Life v0.1 alpha",
                            color = Color(0xFF90A4AE),
                            fontSize = 9.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 6.dp))

                // Graphics Quality Selector
                Text("GRAPHICS PRESET", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GraphicsQuality.values().forEach { gq ->
                        val isSelected = graphicsQuality == gq
                        PixelButton(
                            onClick = { onGraphicsChanged(gq) },
                            isSelected = isSelected,
                            selectedColor = PixelColors.AccentCyan,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = gq.name,
                                color = if (isSelected) Color.White else Color(0xFFCFD8DC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggles
                SettingToggleRow(label = "Show FPS Counter", isChecked = showFps, onCheckedChange = { onToggleFps() })
                SettingToggleRow(label = "Sound Effects", isChecked = soundOn, onCheckedChange = {
                    soundOn = it
                    soundManager.soundEnabled = it
                })
                SettingToggleRow(label = "Haptic Vibration", isChecked = hapticsOn, onCheckedChange = {
                    hapticsOn = it
                    soundManager.hapticsEnabled = it
                })

                Spacer(modifier = Modifier.height(10.dp))

                // City Save & Load
                Text("CITY MANAGEMENT", color = PixelColors.AccentCyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelButton(
                        onClick = { onSaveCity(); onDismiss() },
                        backgroundColor = Color(0xFF1B5E20),
                        borderColor = PixelColors.AccentGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💾 Save City", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    PixelButton(
                        onClick = { onLoadCity(); onDismiss() },
                        backgroundColor = Color(0xFF0D47A1),
                        borderColor = PixelColors.AccentBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📂 Load City", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    PixelButton(
                        onClick = { onResetCity(); onDismiss() },
                        backgroundColor = Color(0xFF7F0000),
                        borderColor = PixelColors.AccentRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗑 Reset Map", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFECEFF1), fontSize = 11.sp)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PixelColors.AccentCyan,
                checkedTrackColor = Color(0xFF006064),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
