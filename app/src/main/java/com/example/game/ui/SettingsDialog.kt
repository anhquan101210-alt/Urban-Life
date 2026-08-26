package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(10.dp)
                .testTag("settings_dialog"),
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
                        text = "GAME SETTINGS",
                        color = Color(0xFFB0BEC5),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                // Graphics Quality Selector
                Text("GRAPHICS PRESET", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GraphicsQuality.values().forEach { gq ->
                        val isSelected = graphicsQuality == gq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF29B6F6) else Color(0x22FFFFFF))
                                .clickable { onGraphicsChanged(gq) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = gq.name,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Options
                SettingToggleRow(label = "Show FPS Counter", isChecked = showFps, onCheckedChange = { onToggleFps() })
                SettingToggleRow(label = "Sound Effects", isChecked = soundOn, onCheckedChange = {
                    soundOn = it
                    soundManager.soundEnabled = it
                })
                SettingToggleRow(label = "Haptic Vibration", isChecked = hapticsOn, onCheckedChange = {
                    hapticsOn = it
                    soundManager.hapticsEnabled = it
                })

                Spacer(modifier = Modifier.height(14.dp))

                // Save / Load / New City Actions
                Text("SAVE & RESTART", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSaveCity(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_city_button")
                    ) {
                        Text("Save City", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onLoadCity(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("load_city_button")
                    ) {
                        Text("Load City", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onResetCity(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("new_city_button")
                    ) {
                        Text("New City", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        Text(text = label, color = Color.White, fontSize = 12.sp)
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF29B6F6),
                checkedTrackColor = Color(0xFF0288D1),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}
