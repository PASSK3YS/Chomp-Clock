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
        color = Color(0xFF18181B),
        border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f)),
        shadowElevation = 4.dp
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
                    tint = Color(0xFF60A5FA),
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF18181B),
            border = BorderStroke(1.dp, Color(0xFF27272A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                        color = Color(0xFFA1A1AA)
                    )
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

                Spacer(modifier = Modifier.height(16.dp))

                // Current avatar preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF3B82F6).copy(alpha = 0.3f), Color(0xFF18181B))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatarView(avatarId = currentAvatarId, size = 76.dp)
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        containerColor = Color(0xFF27272A),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46))
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo from Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Or choose an avatar icon",
                    color = Color(0xFF71717A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Grid of preset icons
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PRESET_AVATARS) { avatar ->
                        val isSelected = currentAvatarId == avatar
                        val emoji = avatar.removePrefix("icon:")
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    onAvatarSelected(avatar)
                                    onDismiss()
                                },
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFF27272A),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF3F3F46)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
