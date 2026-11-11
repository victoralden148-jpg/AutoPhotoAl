package com.autophoto.ai.ui.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.autophoto.ai.ui.components.rememberPhotoExporter
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons
import com.autophoto.ai.utils.DateUtils
import com.autophoto.ai.utils.PhotoPaths
import com.autophoto.ai.utils.ShareUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    onOpenPhoto: (Long) -> Unit,
    vm: GalleryViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = true)
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val photos by vm.photos.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<PhotoEntity?>(null) }

    val export = rememberPhotoExporter { ok ->
        scope.launch { snackbar.showSnackbar(if (ok) "Tersimpan ke galeri HP" else "Gagal menyimpan ke galeri") }
    }

    Box(Modifier.fillMaxSize().background(AppColors.Surface)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Text(
                "Galeri", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = AppColors.Ink,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Semua Foto", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                    color = AppColors.Ink, modifier = Modifier.weight(1f))
                Text("${photos?.size ?: 0}", color = AppColors.Muted, fontSize = 15.sp)
            }

            val list = photos
            when {
                list == null -> Unit
                list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada foto, yuk mulai foto!", color = AppColors.Muted, fontSize = 15.sp)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            onClick = { onOpenPhoto(photo.id) },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                selected = photo
                            }
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }

    // Bottom sheet (long press): Hapus / Share / Simpan ke galeri HP
    selected?.let { photo ->
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Color.White) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                SheetAction(Icons.Rounded.SaveAlt, "Simpan ke galeri HP") {
                    selected = null
                    export(photo.filePath)
                }
                SheetAction(Icons.Rounded.Share, "Share") {
                    selected = null
                    ShareUtils.sharePhoto(context, photo.filePath)
                }
                SheetAction(Icons.Rounded.Delete, "Hapus", tint = Color(0xFFD32F2F)) {
                    selected = null
                    vm.delete(photo)
                    scope.launch { snackbar.showSnackbar("Foto dihapus") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridItem(photo: PhotoEntity, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    Box(
        Modifier.aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE6E6E6))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        // Thumbnail kecil + lazy loading dari LazyGrid + cache memori/disk Coil
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(PhotoPaths.thumbOrFull(photo.filePath))
                .size(360, 480)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                PhotoFilter.fromId(photo.filterName)?.displayName ?: photo.filterName,
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
            )
            Text(DateUtils.format(photo.createdAt), color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color = AppColors.Ink, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = tint)
        Text(label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(0.dp))
}
