package com.example.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun StatsScreen(username: String) {
    val currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHourOfDay) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val dateFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.UK)
        Text(dateFormat.format(java.util.Date()), color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        Text(text = "$greeting, $username", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        Text("ANALYTICS", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF71717A), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))

        ChartCard("FASTING HISTORY (HOURS)", listOf(14f, 16f, 15f, 18f, 20f, 16f, 16f), MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        
        ChartCard("WEIGHT TREND", listOf(72f, 71.5f, 71.2f, 71.0f, 70.5f, 70.3f, 70.0f), MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(16.dp))
        
        ChartCard("CALORIE INTAKE", listOf(2200f, 2100f, 1900f, 2000f, 1800f, 1950f, 1850f), MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun ChartCard(title: String, dataPoints: List<Float>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFF18181B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF27272A)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFFA1A1AA), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(modifier = Modifier.fillMaxSize()) {
                val max = dataPoints.maxOrNull() ?: 1f
                val min = dataPoints.minOrNull() ?: 0f
                val range = (max - min).coerceAtLeast(1f)
                
                val width = size.width
                val height = size.height
                val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width
                
                val path = Path()
                dataPoints.forEachIndexed { index, value ->
                    val x = index * stepX
                    // Normalize value to height
                    val normalized = (value - min) / range
                    val y = height - (normalized * height)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                
                drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
            }
        }
    }
}
