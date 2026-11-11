package com.autophoto.ai.domain.live

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.autophoto.ai.domain.AutoFilterSelector
import com.autophoto.ai.domain.MoodAnalyzer
import com.autophoto.ai.domain.SubjectProcessor
import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.utils.BitmapUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.min

/**
 * Menganalisis frame preview ~2x per detik: cari subjek, tentukan tata letak ideal
 * (rule of thirds), hitung arah geser kamera + kebutuhan zoom, dan saran filter.
 */
class LiveAnalyzer(
    private val subjects: SubjectProcessor = SubjectProcessor(),
    private val moods: MoodAnalyzer = MoodAnalyzer(),
    private val selector: AutoFilterSelector = AutoFilterSelector(),
    private val onResult: (Guidance) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile var enabled = true
    @Volatile var mirror = false
    @Volatile var zoomRatio = 1f
    @Volatile var zoomMax = 1f
    @Volatile var autoZoom = true
    @Volatile var tiltDeg = 0f
    @Volatile var levelEnabled = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val busy = AtomicBoolean(false)
    private var lastRun = 0L
    private var streak = 0
    private var runs = 0

    companion object {
        private const val INTERVAL_MS = 450L
        private const val FILL = 0.70f      // subjek ideal mengisi 70% frame
        private const val MOVE_TH = 0.06f   // toleransi posisi
    }

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (!enabled || now - lastRun < INTERVAL_MS || !busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        lastRun = now

        var frame: Bitmap? = null
        try {
            val rot = image.imageInfo.rotationDegrees
            val raw = BitmapUtils.scaleToMaxSide(image.toBitmap(), 320)
            frame = if (rot == 0) raw else Bitmap.createBitmap(
                raw, 0, 0, raw.width, raw.height,
                Matrix().apply { postRotate(rot.toFloat()) }, true
            )
        } catch (e: Exception) {
            frame = null
        } finally {
            image.close()
        }

        val bmp = frame
        if (bmp == null) {
            busy.set(false)
            return
        }
        scope.launch {
            try {
                val bounds = subjects.detectBounds(bmp)
                val mood = moods.analyze(bmp)
                onResult(build(bmp.width, bmp.height, bounds, mood))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // abaikan: frame berikutnya dicoba lagi
            } finally {
                busy.set(false)
            }
        }
    }

    private fun build(w: Int, h: Int, b: Rect?, mood: MoodResult): Guidance {
        runs++
        val filter = selector.select(mood)
        if (b == null) {
            streak = 0
            return Guidance(
                analyzing = runs < 2,
                message = if (runs < 2) "AI menganalisis, tahan stabil" else "Arahkan kamera ke subjek utama",
                hasFace = mood.hasFace,
                suggestedFilter = filter,
                mood = mood
            )
        }

        var l = b.left / w.toFloat()
        var r = b.right / w.toFloat()
        val t = b.top / h.toFloat()
        val bt = b.bottom / h.toFloat()
        if (mirror) { // preview kamera depan tampil seperti cermin
            val nl = 1f - r
            r = 1f - l
            l = nl
        }
        val bw = (r - l).coerceAtLeast(0.01f)
        val bh = (bt - t).coerceAtLeast(0.01f)
        val cx = (l + r) / 2f
        val cy = (t + bt) / 2f

        // Zoom yang dibutuhkan agar subjek mengisi ~70% frame
        val need = min(FILL / bw, FILL / bh).coerceIn(0.6f, 4f)
        val maxF = (zoomMax / zoomRatio.coerceAtLeast(0.1f)).coerceAtLeast(1f)
        val reach = need.coerceAtMost(maxF)
        val tooFar = need > maxF * 1.15f

        // Titik target (rule of thirds) setelah zoom, lalu dikonversi ke frame saat ini
        val tx = if (bw * reach > 0.62f) 0.5f else if (cx < 0.5f) 1f / 3f else 2f / 3f
        val ty = if (mood.hasFace) 0.42f else 0.5f
        val txNow = (0.5f + (tx - 0.5f) / reach).coerceIn(0.05f, 0.95f)
        val tyNow = (0.5f + (ty - 0.5f) / reach).coerceIn(0.05f, 0.95f)
        val dx = cx - txNow
        val dy = cy - tyNow

        val dir = when {
            abs(dx) >= abs(dy) && abs(dx) > MOVE_TH -> if (dx > 0) MoveDir.RIGHT else MoveDir.LEFT
            abs(dy) > MOVE_TH -> if (dy > 0) MoveDir.DOWN else MoveDir.UP
            else -> MoveDir.NONE
        }
        val zoomOk = abs(reach - 1f) < 0.15f
        val tiltOk = !levelEnabled || abs(tiltDeg) < 4f
        val ready = dir == MoveDir.NONE && zoomOk && tiltOk
        streak = if (ready) streak + 1 else 0

        val message = when {
            dir == MoveDir.RIGHT -> "Geser kamera ke kanan  →"
            dir == MoveDir.LEFT -> "←  Geser kamera ke kiri"
            dir == MoveDir.DOWN -> "Arahkan kamera ke bawah  ↓"
            dir == MoveDir.UP -> "Arahkan kamera ke atas  ↑"
            !zoomOk && need > 1f && tooFar -> "Dekati subjek"
            !zoomOk && need > 1f -> if (autoZoom) "AI sedang zoom in…" else "Zoom in sedikit"
            !zoomOk -> if (autoZoom && zoomRatio > 1.05f) "AI sedang zoom out…" else "Mundur sedikit"
            !tiltOk -> "Luruskan kamera"
            else -> "Komposisi pas — tahan!"
        }

        return Guidance(
            analyzing = false,
            hasSubject = true,
            left = l, top = t, right = r, bottom = bt,
            targetX = txNow, targetY = tyNow,
            dir = dir,
            zoomFactor = need,
            hasFace = mood.hasFace,
            ready = ready,
            readyStreak = streak,
            message = message,
            suggestedFilter = filter,
            mood = mood
        )
    }
}
