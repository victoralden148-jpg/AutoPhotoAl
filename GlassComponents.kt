package com.autophoto.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autophoto.ai.ui.theme.AppColors

/** Tombol bulat dengan ikon (gaya glass pada layar gelap). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = Color.White.copy(alpha = 0.14f),
    content: Color = Color.White,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(container).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = content, modifier = Modifier.size(size * 0.5f))
    }
}

/** Chip transparan ala "Seltún, Iceland" pada referensi kamera. */
@Composable
fun GlassPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

enum class NavTab { HOME, GALLERY }

/** Bottom navigation melayang berbentuk pill (Home | Kamera | Galeri). */
@Composable
fun FloatingNavBar(
    selected: NavTab,
    onHome: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(18.dp, CircleShape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color(0x14000000), CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Rounded.Home, "Home", selected == NavTab.HOME, onHome)
        // Kamera = aksi utama, selalu beraksen
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(AppColors.Accent).clickable(onClick = onCamera),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.CameraAlt, "Kamera", tint = Color.White, modifier = Modifier.size(24.dp)) }
        NavItem(Icons.Rounded.PhotoLibrary, "Galeri", selected == NavTab.GALLERY, onGallery)
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Color(0xFFEDEDED) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, label, tint = if (selected) AppColors.Ink else AppColors.Muted, modifier = Modifier.size(24.dp))
        AnimatedVisibility(visible = selected) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Ink)
        }
    }
}
