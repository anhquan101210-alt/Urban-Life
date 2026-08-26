package com.example.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ExitConfirmDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        PixelPanel(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .wrapContentHeight()
                .testTag("exit_confirm_dialog"),
            borderColor = PixelColors.AccentRed,
            backgroundColor = PixelColors.PanelBgSolid
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EXIT URBAN LIFE?",
                    color = PixelColors.AccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Make sure to save your city progress in Settings before exiting.",
                    color = Color(0xFFCFD8DC),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF1E3A5F)
                    ) {
                        Text("RESUME", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    PixelButton(
                        onClick = onConfirmExit,
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFFB71C1C),
                        borderColor = Color(0xFFFF5252)
                    ) {
                        Text("EXIT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
