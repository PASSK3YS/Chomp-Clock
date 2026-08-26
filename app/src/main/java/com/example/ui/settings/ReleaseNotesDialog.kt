package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppTheme

@Composable
fun ReleaseNotesDialog(
    releaseNotes: List<ReleaseNoteItem>,
    currentVersion: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = AppTheme.colors.background,
            border = BorderStroke(1.dp, AppTheme.colors.border)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AppTheme.colors.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AppTheme.colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Release Notes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Chomp Clock Version History",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AppTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Installed Build banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppTheme.colors.surfaceElevated,
                    border = BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = AppTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Installed: v$currentVersion",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AppTheme.colors.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Latest Verified Build",
                                color = AppTheme.colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable List of Version Changes
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(releaseNotes) { note ->
                        ReleaseNoteCard(note = note)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            uriHandler.openUri("https://github.com/PASSK3YS/Chomp-Clock/releases")
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "GitHub Releases",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Done",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReleaseNoteCard(note: ReleaseNoteItem) {
    var isExpanded by remember { mutableStateOf(note.isLatestVerified) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isLatestVerified) AppTheme.colors.surfaceElevated else AppTheme.colors.surface
        ),
        border = BorderStroke(
            1.dp,
            if (note.isLatestVerified) AppTheme.colors.primary.copy(alpha = 0.4f) else AppTheme.colors.border
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (note.isLatestVerified) AppTheme.colors.primary else AppTheme.colors.border
                    ) {
                        Text(
                            text = note.version,
                            color = if (note.isLatestVerified) Color.White else AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.date,
                        color = AppTheme.colors.textMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AppTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AppTheme.colors.textPrimary
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    if (!note.fullBody.isNullOrBlank()) {
                        Text(
                            text = note.fullBody,
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    note.highlights.forEach { highlight ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = AppTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = highlight,
                                fontSize = 12.sp,
                                color = AppTheme.colors.textPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
