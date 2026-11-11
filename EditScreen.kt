package com.autophoto.ai.ui.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autophoto.ai.ui.AppViewModelProvider
import com.autophoto.ai.ui.components.CircleIconButton
import com.autophoto.ai.ui.components.GlassPill
import com.autophoto.ai.ui.components.MoodInfoCard
import com.autophoto.ai.ui.theme.AppColors
import com.autophoto.ai.ui.theme.StatusBarIcons

@Composable
fun EditScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    vm: EditViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    StatusBarIcons(darkIcons = false)
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.savedId) { s.savedId?.let(onSaved) }

    Column(Modifier.fillMaxSize().background(AppColors.Dark).statusBarsPadding().navigationBarsPadding()) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconButton(Icons.Rounded.Close, { vm.discard(); onBack() }, contentDescription = "Buang")
            Text(
                "Hasil Auto AI", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 16.dp)
            )
        }

        when {
            s.isProcessing -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppColors.Accent)
                    Spacer(Modifier.height(16.dp))
                    Text("Memproses foto…", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text("Crop · Zoom · Filter · Enhance", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }

            s.result == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.error ?: "Terjadi kesalahan", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = vm::process,
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) { Text("Coba lagi") }
                }
            }

            else -> {
                val shown = if (s.showOriginal) s.original else s.result
                val image = remember(shown) { shown!!.asImageBitmap() }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(32.dp)).background(Color.Black)) {
                        Image(image, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        // Nama filter yang dipilih otomatis
                        GlassPill(
                            if (s.showOriginal) "Foto asli" else "Filter: ${s.filter?.displayName}",
                            icon = Icons.Rounded.AutoAwesome,
                            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        )
                        // Toggle bandingkan asli / hasil
                        Box(
                            Modifier.align(Alignment.BottomCenter).padding(16.dp)
                                .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                                .clickable { vm.toggleOriginal() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                if (s.showOriginal) "Lihat hasil" else "Bandingkan dengan asli",
                                color = Color.White, fontSize = 12.sp
                            )
                        }
                    }

                    s.mood?.let { m ->
                        MoodInfoCard(
                            filterId = s.filter?.id ?: "",
                            brightness = m.brightnessLevel,
                            saturation = m.saturationLevel,
                            temperature = m.colorTemperature.label,
                            hasFace = m.hasFace
                        )
                    }

                    // Ringkasan langkah pipeline
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        StepRow(if (s.subjectFound) "Auto crop: subjek ditemukan" else "Auto crop: subjek tidak terdeteksi (dilewati)")
                        StepRow(if (s.subjectFound) "Auto zoom: subjek ±70% frame" else "Auto zoom: center-crop 3:4")
                        StepRow("Auto filter: ${s.filter?.id}")
                        StepRow("Auto enhance: brightness & kontras")
                    }
                }

                // Aksi bawah
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { vm.discard(); onBack() },
                        modifier = Modifier.height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.DarkPanel, contentColor = Color.White)
                    ) { Text("Ulangi") }
                    Button(
                        onClick = vm::save,
                        enabled = !s.isSaving,
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                    ) { Text(if (s.isSaving) "Menyimpan…" else "Simpan", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun StepRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Check, null, tint = AppColors.Accent, modifier = Modifier.size(16.dp))
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
