package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AchievementCategory
import com.example.data.model.AchievementRarity
import com.example.data.model.DetailedAchievement

@Composable
fun AchievementsDialog(
    achievements: List<DetailedAchievement>,
    initialCategory: AchievementCategory = AchievementCategory.ALL,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var selectedAchievementForDetail by remember { mutableStateOf<DetailedAchievement?>(null) }

    val filteredAchievements = remember(achievements, selectedCategory) {
        if (selectedCategory == AchievementCategory.ALL) {
            achievements
        } else {
            achievements.filter { it.category == selectedCategory }
        }
    }

    val totalUnlocked = achievements.count { it.isUnlocked }
    val totalCount = achievements.size
    val unlockPercentage = if (totalCount > 0) (totalUnlocked.toFloat() / totalCount * 100).toInt() else 0

    val rankTitle = when {
        unlockPercentage >= 80 -> "Metabolic Grandmaster 👑"
        unlockPercentage >= 50 -> "Fasting Champion ⚡"
        unlockPercentage >= 25 -> "Intermittent Adept 🌟"
        unlockPercentage >= 10 -> "Dedicated Apprentice 🔥"
        else -> "Fasting Novice 🌱"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121214),
            border = BorderStroke(1.dp, Color(0xFF27272A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Achievements",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                rankTitle,
                                color = Color(0xFFFBBF24),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Banner Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF18181B),
                    border = BorderStroke(1.dp, Color(0xFF27272A))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Overall Progress",
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "$totalUnlocked / $totalCount Unlocked ($unlockPercentage%)",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) totalUnlocked.toFloat() / totalCount else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF34D399),
                            trackColor = Color(0xFF27272A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AchievementCategory.values()) { category ->
                        val isSelected = category == selectedCategory
                        val countInCategory = if (category == AchievementCategory.ALL) achievements.size else achievements.count { it.category == category }
                        val unlockedInCategory = if (category == AchievementCategory.ALL) totalUnlocked else achievements.count { it.category == category && it.isUnlocked }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF18181B),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF27272A)),
                            modifier = Modifier.clickable { selectedCategory = category }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(category.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    category.displayName,
                                    color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "($unlockedInCategory/$countInCategory)",
                                    color = if (isSelected) Color(0xFF93C5FD) else Color(0xFF71717A),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Achievement List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAchievements, key = { it.id }) { item ->
                        AchievementCard(
                            achievement = item,
                            onClick = { selectedAchievementForDetail = item }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog on tap
    if (selectedAchievementForDetail != null) {
        val ach = selectedAchievementForDetail!!
        AlertDialog(
            onDismissRequest = { selectedAchievementForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ach.iconEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(ach.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(ach.description, color = Color(0xFFD4D4D8), fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ach.rarity.color.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, ach.rarity.color)
                        ) {
                            Text(
                                ach.rarity.label.uppercase(),
                                color = ach.rarity.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = if (ach.isUnlocked) "Status: Unlocked ✓" else "Progress: ${ach.currentProgress} / ${ach.targetProgress}",
                            color = if (ach.isUnlocked) Color(0xFF34D399) else Color(0xFFA1A1AA),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    if (ach.tip.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ach.tip,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedAchievementForDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Text("Got it")
                }
            },
            containerColor = Color(0xFF18181B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AchievementCard(
    achievement: DetailedAchievement,
    onClick: () -> Unit
) {
    val isUnlocked = achievement.isUnlocked
    val borderBrush = if (isUnlocked) {
        Brush.horizontalGradient(
            listOf(
                achievement.rarity.color.copy(alpha = 0.8f),
                Color(0xFF34D399).copy(alpha = 0.8f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF27272A),
                Color(0xFF27272A)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color(0xFF1A1A22) else Color(0xFF161618)
        ),
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUnlocked) achievement.rarity.color.copy(alpha = 0.2f) else Color(0xFF202024),
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) achievement.rarity.color.copy(alpha = 0.5f) else Color(0xFF2E2E34)
                ),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUnlocked) {
                        Text(achievement.iconEmoji, fontSize = 22.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color(0xFF52525B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details & Progress
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = achievement.title,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else Color(0xFFD4D4D8),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = achievement.rarity.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = achievement.rarity.label.uppercase(),
                                color = achievement.rarity.color,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isUnlocked) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, Color(0xFF059669))
                        ) {
                            Text(
                                text = "UNLOCKED ✓",
                                color = Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${achievement.currentProgress}/${achievement.targetProgress}",
                            color = Color(0xFF71717A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp,
                    maxLines = 2
                )

                if (!isUnlocked && achievement.targetProgress > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF3B82F6),
                        trackColor = Color(0xFF27272A)
                    )
                }
            }
        }
    }
}
