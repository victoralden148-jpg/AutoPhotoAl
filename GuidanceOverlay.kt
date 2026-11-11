package com.autophoto.ai.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.autophoto.ai.domain.live.Guidance
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val Cyan = Color(0xFF00E5FF)
private val Green = Color(0xFF34C759)
private val Yellow = Color(0xFFFFD60A)

/** Grid, kotak subjek, panah arah geser, target, garis level, bayangan pose, cincin fokus. */
@Composable
fun GuidanceOverlay(
    g: Guidance,
    aiOn: Boolean,
    showGrid: Boolean,
    showLevel: Boolean,
    tilt: Float,
    pose: PoseGuide?,
    focus: Offset?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height

        if (showGrid) {
            val c = Color.White.copy(alpha = 0.35f)
            drawLine(c, Offset(w / 3f, 0f), Offset(w / 3f, h), 1.dp.toPx())
            drawLine(c, Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), 1.dp.toPx())
            drawLine(c, Offset(0f, h / 3f), Offset(w, h / 3f), 1.dp.toPx())
            drawLine(c, Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), 1.dp.toPx())
        }

        if (pose != null) {
            val ph = h * 0.70f
            drawPose(
                pose, Offset(w / 2f - ph * 0.45f, h * 0.15f), Size(ph * 0.9f, ph),
                Color.White.copy(alpha = 0.32f), 4.dp.toPx()
            )
        }

        if (aiOn && !g.analyzing && g.hasSubject) {
            val accent = if (g.ready) Green else Cyan
            drawRoundRect(
                accent.copy(alpha = 0.9f),
                topLeft = Offset(g.left * w, g.top * h),
                size = Size((g.right - g.left) * w, (g.bottom - g.top) * h),
                cornerRadius = CornerRadius(18.dp.toPx()),
                style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
            )
            val from = Offset(g.centerX * w, g.centerY * h)
            val to = Offset(g.targetX * w, g.targetY * h)
            drawCircle(accent, 28.dp.toPx(), to, style = Stroke(3.dp.toPx()))
            drawCircle(accent.copy(alpha = 0.25f), 12.dp.toPx(), to)
            if (!g.ready) {
                drawCircle(accent, 7.dp.toPx(), from)
                drawLine(accent, from, to, 3.dp.toPx(), cap = StrokeCap.Round)
                val ang = atan2(to.y - from.y, to.x - from.x)
                val len = 14.dp.toPx()
                for (d in listOf(2.6f, -2.6f)) {
                    drawLine(
                        accent, to,
                        Offset(to.x + len * cos(ang + d), to.y + len * sin(ang + d)),
                        3.dp.toPx(), cap = StrokeCap.Round
                    )
                }
            }
        }

        if (showLevel) {
            val center = Offset(w / 2f, h / 2f)
            val level = abs(tilt) <= 1f
            val col = if (level) Yellow else Color.White.copy(alpha = 0.85f)
            val half = 36.dp.toPx()
            val side = 110.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x - side - 16.dp.toPx(), center.y), Offset(center.x - side, center.y), 2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.6f), Offset(center.x + side, center.y), Offset(center.x + side + 16.dp.toPx(), center.y), 2.dp.toPx(), cap = StrokeCap.Round)
            rotate(degrees = if (level) 0f else tilt, pivot = center) {
                drawLine(col, Offset(center.x - half, center.y), Offset(center.x + half, center.y), 2.5.dp.toPx(), cap = StrokeCap.Round)
            }
        }

        if (focus != null) {
            drawCircle(Yellow, 34.dp.toPx(), focus, style = Stroke(2.dp.toPx()))
            drawCircle(Yellow, 3.dp.toPx(), focus)
        }
    }
}
