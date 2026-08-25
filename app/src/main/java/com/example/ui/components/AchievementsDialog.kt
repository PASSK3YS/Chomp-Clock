package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.Achievement

@Composable
fun AchievementsDialog(
    achievements: List<Achievement>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Achievements") },
        text = {
            LazyColumn {
                items(achievements) { a ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(a.title, fontWeight = FontWeight.Bold, color = if (a.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            Text(a.description, style = MaterialTheme.typography.bodySmall)
                            if (a.isUnlocked) {
                                Text("Unlocked!", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                            } else {
                                Text("Locked", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
