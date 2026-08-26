package com.example.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.Disaster

@Composable
fun DisasterAlert(
    disaster: Disaster,
    onLocate: (Disaster) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PixelPanel(
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth(0.9f)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("disaster_alert_banner"),
        borderColor = Color(0xFFFF1744),
        backgroundColor = Color(0xF24A0E17)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = PixelColors.AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "🔥 ${disaster.title.uppercase()}",
                        color = Color(0xFFFF8A80),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                    Text(
                        text = disaster.description,
                        color = Color(0xFFFFCDD2),
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PixelButton(
                    onClick = { onLocate(disaster) },
                    backgroundColor = Color(0xFFC62828),
                    borderColor = Color(0xFFFF5252),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("LOCATE", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
