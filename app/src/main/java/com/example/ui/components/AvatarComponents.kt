package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.AppTheme
import java.io.File

val PRESET_AVATARS = listOf(
    "icon:🔥", "icon:⚡", "icon:🦊", "icon:🦁",
    "icon:🐯", "icon:🐼", "icon:🐻", "icon:🐺",
    "icon:🥑", "icon:🥗", "icon:🍎", "icon:☕",
    "icon:🏃", "icon:🧘", "icon:🏆", "icon:💎",
    "icon:🚀", "icon:🌟"
)

@Composable
fun UserAvatarView(
    avatarId: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) modifier.clickable { onClick() } else modifier

    Surface(
        modifier = clickModifier.size(size),
        shape = CircleShape,
        color = AppTheme.colors.surfaceElevated,
        border = BorderStroke(1.5.dp, AppTheme.colors.primary.copy(alpha = 0.7f)),
        shadowElevation = if (AppTheme.colors.isDark) 4.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (avatarId != null && (avatarId.startsWith("uri:") || avatarId.startsWith("file:") || avatarId.startsWith("/"))) {
                val raw = avatarId.removePrefix("uri:")
                val model: Any = if (raw.startsWith("file://")) {
                    val path = raw.removePrefix("file://").substringBefore("?")
                    File(path)
                } else if (raw.startsWith("/")) {
                    val path = raw.substringBefore("?")
                    File(path)
                } else {
                    Uri.parse(raw)
                }

                AsyncImage(
                    model = model,
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (avatarId != null && avatarId.startsWith("icon:")) {
                val emoji = avatarId.removePrefix("icon:")
                val fontSize = (size.value * 0.52f).sp
                Text(
                    text = emoji,
                    fontSize = fontSize,
                    modifier = Modifier.padding(1.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Avatar",
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}

@Composable
fun AvatarPickerDialog(
    currentAvatarId: String?,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Permanently persist the picked image file in the app internal storage
                val avatarFile = File(context.filesDir, "user_avatar_custom.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    avatarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val persistentUri = "file://${avatarFile.absolutePath}?t=${System.currentTimeMillis()}"
                onAvatarSelected(persistentUri)
            } catch (e: Exception) {
                onAvatarSelected("uri:$uri")
            }
            onDismiss()
        }
    }

    SlideUpBottomSheetDialog(onDismissRequest = onDismiss) { dismissWithAnim ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHOOSE AVATAR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = AppTheme.colors.textMuted
                )
                IconButton(
                    onClick = dismissWithAnim,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AppTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current avatar preview
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppTheme.colors.primary.copy(alpha = 0.25f),
                                AppTheme.colors.surfaceElevated
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                UserAvatarView(avatarId = currentAvatarId, size = 76.dp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Upload custom picture button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.surfaceElevated,
                    contentColor = AppTheme.colors.textPrimary
                ),
                border = BorderStroke(1.dp, AppTheme.colors.border)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Photo from Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Or choose an avatar icon",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of preset icons (adaptive grid to avoid overflow on any screen)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 44.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PRESET_AVATARS) { avatar ->
                    val isSelected = currentAvatarId == avatar
                    val emoji = avatar.removePrefix("icon:")
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable {
                                onAvatarSelected(avatar)
                                dismissWithAnim()
                            },
                        shape = CircleShape,
                        color = if (isSelected) AppTheme.colors.primary.copy(alpha = 0.22f) else AppTheme.colors.surfaceElevated,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) AppTheme.colors.primary else AppTheme.colors.border
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

