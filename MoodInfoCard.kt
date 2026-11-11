package com.autophoto.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autophoto.ai.domain.model.PhotoFilter
import com.autophoto.ai.ui.theme.AppColors

/** Kartu info filter + parameter mood (dipakai di layar Edit dan Detail). */
@Composable
fun MoodInfoCard(
    filterId: String,
    brightness: Float,
    saturation: Float,
    temperature: String,
    hasFace: Boolean,
    modifier: Modifier = Modifier
) {
    val filterLabel = PhotoFilter.fromId(filterId)?.displayName ?: filterId
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = AppColors.Accent, modifier = Modifier.size(18.dp))
            Text("Filter otomatis", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Text(filterLabel, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Bar("Brightness", brightness)
        Bar("Saturation", saturation)
        TextRow("Temperature", temperature)
        TextRow("Wajah", if (hasFace) "Terdeteksi" else "Tidak ada")
    }
}

@Composable
private fun Bar(label: String, value: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).height(4.dp).background(AppColors.Accent))
        }
        Spacer(Modifier.width(10.dp))
        Text("%.2f".format(value), color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun TextRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
