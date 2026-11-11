package com.autophoto.ai.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private fun o(x: Float, y: Float) = Offset(x, y)

/** Contoh pose berupa figur garis (koordinat 0..1). */
enum class PoseGuide(val label: String, val head: Offset, val lines: List<List<Offset>>) {
    STANDING(
        "Berdiri", o(.5f, .10f),
        listOf(
            listOf(o(.5f, .18f), o(.5f, .55f)),
            listOf(o(.5f, .21f), o(.34f, .38f), o(.32f, .55f)),
            listOf(o(.5f, .21f), o(.66f, .38f), o(.68f, .55f)),
            listOf(o(.5f, .55f), o(.44f, .77f), o(.44f, .97f)),
            listOf(o(.5f, .55f), o(.56f, .77f), o(.56f, .97f))
        )
    ),
    HAND_ON_HIP(
        "Tangan di pinggang", o(.5f, .10f),
        listOf(
            listOf(o(.5f, .18f), o(.5f, .55f)),
            listOf(o(.5f, .21f), o(.30f, .36f), o(.43f, .50f)),
            listOf(o(.5f, .21f), o(.70f, .36f), o(.57f, .50f)),
            listOf(o(.5f, .55f), o(.42f, .77f), o(.42f, .97f)),
            listOf(o(.5f, .55f), o(.60f, .76f), o(.66f, .96f))
        )
    ),
    PEACE(
        "Peace", o(.52f, .10f),
        listOf(
            listOf(o(.5f, .18f), o(.5f, .55f)),
            listOf(o(.5f, .21f), o(.70f, .28f), o(.62f, .10f)),
            listOf(o(.5f, .21f), o(.34f, .38f), o(.32f, .55f)),
            listOf(o(.5f, .55f), o(.44f, .77f), o(.44f, .97f)),
            listOf(o(.5f, .55f), o(.56f, .77f), o(.56f, .97f))
        )
    ),
    WALK(
        "Berjalan", o(.5f, .10f),
        listOf(
            listOf(o(.5f, .18f), o(.5f, .55f)),
            listOf(o(.5f, .21f), o(.38f, .38f), o(.44f, .52f)),
            listOf(o(.5f, .21f), o(.62f, .36f), o(.58f, .50f)),
            listOf(o(.5f, .55f), o(.40f, .76f), o(.34f, .97f)),
            listOf(o(.5f, .55f), o(.60f, .74f), o(.68f, .94f))
        )
    ),
    SIT(
        "Duduk", o(.45f, .14f),
        listOf(
            listOf(o(.45f, .22f), o(.45f, .60f)),
            listOf(o(.45f, .60f), o(.75f, .62f)),
            listOf(o(.75f, .62f), o(.75f, .96f)),
            listOf(o(.45f, .26f), o(.58f, .44f), o(.74f, .58f))
        )
    ),
    LOOK_BACK(
        "Menoleh", o(.56f, .10f),
        listOf(
            listOf(o(.52f, .18f), o(.47f, .55f)),
            listOf(o(.52f, .21f), o(.66f, .34f), o(.60f, .48f)),
            listOf(o(.52f, .21f), o(.36f, .36f), o(.40f, .52f)),
            listOf(o(.47f, .55f), o(.50f, .78f), o(.40f, .97f)),
            listOf(o(.47f, .55f), o(.44f, .78f), o(.54f, .97f))
        )
    )
}

fun DrawScope.drawPose(pose: PoseGuide, topLeft: Offset, size: Size, color: Color, strokeWidth: Float) {
    fun map(p: Offset) = Offset(topLeft.x + p.x * size.width, topLeft.y + p.y * size.height)
    drawCircle(color, radius = size.height * 0.075f, center = map(pose.head), style = Stroke(strokeWidth))
    pose.lines.forEach { pts ->
        val path = Path()
        pts.forEachIndexed { i, p ->
            val m = map(p)
            if (i == 0) path.moveTo(m.x, m.y) else path.lineTo(m.x, m.y)
        }
        drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Figur pose kecil untuk thumbnail / kartu pilihan. */
@Composable
fun PoseFigure(pose: PoseGuide, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier.fillMaxSize()) {
        drawPose(
            pose,
            Offset(size.width * 0.1f, size.height * 0.06f),
            Size(size.width * 0.8f, size.height * 0.88f),
            color,
            strokeWidth = 3.dp.toPx()
        )
    }
}
