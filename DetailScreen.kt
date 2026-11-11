package com.autophoto.ai.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.autophoto.ai.ui.AppViewModelProvider
import com.autophoto.ai.ui.components.CircleIconButton
import com.autophoto.ai.ui.components.MoodInfoCard
import com.autophoto.ai.ui.components.rememberPhotoExporter
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons
import com.autophoto.ai.utils.DateUtils
import com.autophoto.ai.utils.ShareUtils
import java.io.File

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    vm: DetailViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = false)
    val context = LocalContext.current
    val photo by vm.photo.collectAsStateWithLifecycle()
    val export = rememberPhotoExporter { ok ->
        Toast.makeText(context, if (ok) "Tersimpan ke galeri HP" else "Gagal menyimpan", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.Rounded.ArrowBack, onBack, contentDescription = "Kembali")
            Text(
                photo?.let { DateUtils.format(it.createdAt) } ?: "",
                color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        val p = photo
        if (p != null) {
            // Foto full screen
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(p.filePath)).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Info filter + parameter mood
            MoodInfoCard(
                filterId = p.filterName,
                brightness = p.brightnessLevel,
                saturation = p.saturationLevel,
                temperature = p.colorTemperature,
                hasFace = p.hasFace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircleIconButton(Icons.Rounded.Share, { ShareUtils.sharePhoto(context, p.filePath) },
                    size = 56.dp, container = AppColors.DarkPanel, contentDescription = "Share")
                CircleIconButton(Icons.Rounded.SaveAlt, { export(p.filePath) },
                    size = 56.dp, container = AppColors.DarkPanel, contentDescription = "Simpan ke galeri HP")
                CircleIconButton(Icons.Rounded.Delete, { vm.delete(onBack) },
                    size = 56.dp, container = AppColors.DarkPanel, content = Color(0xFFFF6B5E),
                    contentDescription = "Hapus")
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth())
        }
    }
}
