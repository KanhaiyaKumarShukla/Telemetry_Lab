package com.example.telemetrylab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun JankWarningCard(
    jankPercentage: Float,
    modifier: Modifier = Modifier
) {
    val severity = when {
        jankPercentage > 10 -> "HIGH"
        jankPercentage > 5 -> "MODERATE"
        else -> "LOW"
    }

    val severityColor = when {
        jankPercentage > 10 -> MaterialTheme.colorScheme.error
        jankPercentage > 5 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f),
            contentColor = severityColor
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Surface(
                color = severityColor.copy(alpha = 0.2f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$severity JANK DETECTED",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${jankPercentage.toInt()}% of frames exceeding 16.67ms threshold",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = severityColor.copy(alpha = 0.8f)
                    )
                )
            }

            // Severity Indicator
            Surface(
                color = severityColor,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = severity,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
