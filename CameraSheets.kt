package com.autophoto.ai.ui.camera

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.autophoto.ai.ui.theme.AppColors
import kotlin.math.roundToInt

@Composable
fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (selected) AppColors.Accent else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Accent, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

private val sliderColors
    @Composable get() = SliderDefaults.colors(
        thumbColor = AppColors.Accent, activeTrackColor = AppColors.Accent,
        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    s: CameraUiState,
    ranges: ProControls.Ranges,
    onChange: ((CameraUiState) -> CameraUiState) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.DarkPanel, contentColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)
        ) {
            Text("Pengaturan kamera", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            SectionTitle("ASISTEN AI")
            SwitchRow("Panduan AI", "Arah geser kamera & tata letak ideal", s.aiGuide) { v -> onChange { it.copy(aiGuide = v) } }
            SwitchRow("Zoom otomatis", "AI mengatur zoom agar subjek pas", s.autoZoom) { v -> onChange { it.copy(autoZoom = v) } }
            SwitchRow("Jepret otomatis", "Foto diambil saat komposisi sudah pas", s.autoShutter) { v -> onChange { it.copy(autoShutter = v) } }

            SectionTitle("TAMPILAN")
            SwitchRow("Garis grid 3x3", null, s.showGrid) { v -> onChange { it.copy(showGrid = v) } }
            SwitchRow("Penunjuk level", "Garis horizon agar foto tidak miring", s.showLevel) { v -> onChange { it.copy(showLevel = v) } }

            SectionTitle("TIMER")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 10).forEach { t ->
                    OptionChip(if (t == 0) "Mati" else "${t}s", s.timerSeconds == t, { onChange { it.copy(timerSeconds = t) } })
                }
            }

            SectionTitle("EKSPOSUR  (EV ${"%+.1f".format(s.ev)})")
            Slider(
                value = s.ev, onValueChange = { v -> onChange { it.copy(ev = (v * 10f).roundToInt() / 10f) } },
                valueRange = -2f..2f, colors = sliderColors
            )

            SectionTitle("WHITE BALANCE")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhiteBalance.values().forEach { w ->
                    OptionChip(w.label, s.wb == w, { onChange { it.copy(wb = w) } })
                }
            }

            SectionTitle("MODE MANUAL")
            if (!ranges.manualSupported) {
                Text("ISO & rana manual tidak didukung kamera ini.", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            } else {
                SwitchRow("ISO & kecepatan rana", "Matikan untuk eksposur otomatis", s.manual) { v -> onChange { it.copy(manual = v) } }
                if (s.manual) {
                    Text("ISO ${s.iso}", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = s.iso.toFloat(),
                        onValueChange = { v -> onChange { it.copy(iso = v.roundToInt()) } },
                        valueRange = ranges.isoMin.toFloat()..ranges.isoMax.toFloat(), colors = sliderColors
                    )
                    Text("Rana 1/${ShutterDenominators[s.shutterIdx.coerceIn(0, ShutterDenominators.lastIndex)]} detik", color = Color.White, fontSize = 13.sp)
                    Slider(
                        value = s.shutterIdx.toFloat(),
                        onValueChange = { v -> onChange { it.copy(shutterIdx = v.roundToInt()) } },
                        valueRange = 0f..(ShutterDenominators.lastIndex).toFloat(),
                        steps = ShutterDenominators.size - 2, colors = sliderColors
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseSheet(
    s: CameraUiState,
    onChange: ((CameraUiState) -> CameraUiState) -> Unit,
    onDismiss: () -> Unit
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onChange { it.copy(refImage = uri, pose = null) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.DarkPanel, contentColor = Color.White) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 24.dp)
        ) {
            Text("Contoh pose", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val sug = s.suggestedPose
            if (sug != null) {
                Text(
                    "AI menyarankan: ${sug.label}", color = AppColors.Accent, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp).clickable { onChange { it.copy(pose = sug, refImage = null) } }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PoseCard("Tanpa pose", s.pose == null && s.refImage == null) {
                    onChange { it.copy(pose = null, refImage = null) }
                }
                PoseGuide.values().forEach { p ->
                    PoseCard(p.label, s.pose == p) {
                        onChange { it.copy(pose = p, refImage = null) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OptionChip("Pilih foto referensi dari galeri", s.refImage != null, {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            })
            if (s.refImage != null) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = s.refImage, contentDescription = "Referensi pose",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp, 128.dp).clip(RoundedCornerShape(14.dp))
                )
            }
            SwitchRow("Bayangan pose di preview", "Figur transparan sebagai panduan posisi", s.showPoseOverlay) { v ->
                onChange { it.copy(showPoseOverlay = v) }
            }
        }
    }
}

@Composable
private fun PoseCard(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp, 96.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(if (selected) 2.dp else 0.dp, AppColors.Accent, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
        ) {
            PoseCardFigure(label)
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PoseCardFigure(label: String) {
    val pose = PoseGuide.values().firstOrNull { it.label == label }
    if (pose != null) PoseFigure(pose)
}
