package com.autophoto.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.autophoto.ai.data.local.PhotoEntity
import com.autophoto.ai.domain.model.PhotoFilter
import com.autophoto.ai.ui.AppViewModelProvider
import com.autophoto.ai.ui.components.GlassPill
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons
import com.autophoto.ai.utils.DateUtils
import com.autophoto.ai.utils.PhotoPaths

@Composable
fun HomeScreen(
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    vm: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = true)
    val photos by vm.photos.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppColors.Surface).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header besar bergaya referensi
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Home", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink)
                    Text("AutoPhoto AI", fontSize = 14.sp, color = AppColors.Muted)
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.AutoAwesome, null, tint = AppColors.Ink, modifier = Modifier.size(22.dp)) }
            }
        }

        // Kartu hero -> buka kamera
        item { HeroCard(onClick = onOpenCamera) }

        // Foto terbaru
        item {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenGallery),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Terbaru", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink,
                    modifier = Modifier.weight(1f))
                Text("${photos?.size ?: 0}", color = AppColors.Muted, fontSize = 15.sp)
                Icon(Icons.Rounded.ChevronRight, null, tint = AppColors.Muted)
            }
        }
        item {
            val list = photos
            if (list.isNullOrEmpty()) {
                if (list != null) {
                    Box(
                        Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(28.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) { Text("Belum ada foto, yuk mulai foto!", color = AppColors.Muted, fontSize = 14.sp) }
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list.take(10), key = { it.id }) { RecentItem(it) { onOpenPhoto(it.id) } }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(400.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(AppColors.Dark)
            .clickable(onClick = onClick)
    ) {
        // Glow oranye di bagian bawah (mirip panel "Action Plate")
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, AppColors.Accent.copy(alpha = 0.40f)))
            )
        )
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassPill("AI PIPELINE", icon = Icons.Rounded.AutoAwesome)
            }
            Spacer(Modifier.weight(1f))
            listOf("01  Auto Crop", "02  Auto Zoom", "03  Auto Filter", "04  Auto Enhance").forEach {
                Text(
                    it, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mulai Foto", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("Tap untuk membuka kamera", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFD9D9DB)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.CameraAlt, null, tint = AppColors.Ink, modifier = Modifier.size(30.dp)) }
            }
        }
    }
}

@Composable
private fun RecentItem(photo: PhotoEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.width(140.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(PhotoPaths.thumbOrFull(photo.filePath)).size(360, 480).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(24.dp)).background(Color(0xFFE6E6E6))
        )
        Spacer(Modifier.height(8.dp))
        Text(DateUtils.format(photo.createdAt), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AppColors.Ink)
        Text(
            PhotoFilter.fromId(photo.filterName)?.displayName ?: photo.filterName,
            fontSize = 12.sp, color = AppColors.Muted, maxLines = 1
        )
    }
}
