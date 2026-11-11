package com.autophoto.ai.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.autophoto.ai.utils.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

/** Hasil crop: bitmap + posisi kotak subjek di dalam bitmap tsb. */
data class CropResult(val bitmap: Bitmap, val subject: Rect)

/** Auto crop (ML Kit Subject Segmentation) + auto zoom (subjek ~70% frame). */
class SubjectProcessor {

    companion object {
        const val FRAME_W = 1080
        const val FRAME_H = 1440          // frame akhir 3:4
        const val SUBJECT_FILL = 0.70f    // target subjek mengisi 70% frame
    }

    private val segmenter by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build()
        )
    }

    /** Bounding box subjek utama pada koordinat [bitmap]; null bila tidak ditemukan / gagal. */
    suspend fun detectBounds(bitmap: Bitmap): Rect? = try {
        val det = BitmapUtils.scaleToMaxSide(bitmap, 1024)
        val result = segmenter.process(InputImage.fromBitmap(det, 0)).await()
        val mask = result.foregroundConfidenceMask
        if (mask == null) null else {
            val w = det.width
            val h = det.height
            var minX = w; var minY = h; var maxX = -1; var maxY = -1
            val total = min(w * h, mask.limit())
            for (i in 0 until total) {
                if (mask.get(i) > 0.5f) {
                    val x = i % w
                    val y = i / w
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
            val area = (maxX - minX + 1).toLong() * (maxY - minY + 1)
            if (maxX < minX || area < w.toLong() * h * 0.02) null else {
                val ratio = bitmap.width / det.width.toFloat()
                Rect(
                    (minX * ratio).toInt(), (minY * ratio).toInt(),
                    ((maxX + 1) * ratio).toInt().coerceAtMost(bitmap.width),
                    ((maxY + 1) * ratio).toInt().coerceAtMost(bitmap.height)
                )
            }
        }
    } catch (e: Exception) {
        null // model belum terunduh / error -> pipeline lanjut tanpa crop
    }

    /** AUTO CROP: potong ke bounding box subjek (+ padding 5%). */
    fun cropToSubject(bitmap: Bitmap, bounds: Rect): CropResult {
        val padX = (bounds.width() * 0.05f).toInt()
        val padY = (bounds.height() * 0.05f).toInt()
        val left = max(0, bounds.left - padX)
        val top = max(0, bounds.top - padY)
        val right = min(bitmap.width, bounds.right + padX)
        val bottom = min(bitmap.height, bounds.bottom + padY)
        val cropped = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val rel = Rect(bounds.left - left, bounds.top - top, bounds.right - left, bounds.bottom - top)
        return CropResult(cropped, rel)
    }

    /**
     * AUTO ZOOM: skala hasil crop agar subjek mengisi ~70% frame 3:4,
     * dipusatkan, dengan latar blur dari foto itu sendiri.
     */
    fun autoZoom(crop: CropResult): Bitmap {
        val subj = crop.subject
        val scale = min(
            SUBJECT_FILL * FRAME_W / subj.width(),
            SUBJECT_FILL * FRAME_H / subj.height()
        )
        val out = Bitmap.createBitmap(FRAME_W, FRAME_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        drawBlurBackdrop(canvas, crop.bitmap)

        val m = Matrix().apply {
            postScale(scale, scale)
            postTranslate(
                FRAME_W / 2f - subj.exactCenterX() * scale,
                FRAME_H / 2f - subj.exactCenterY() * scale
            )
        }
        canvas.drawBitmap(crop.bitmap, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    /** Fallback bila subjek tidak ditemukan: center-crop ke frame 3:4. */
    fun coverFit(bitmap: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(FRAME_W, FRAME_H, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            bitmap, coverMatrix(bitmap.width, bitmap.height, FRAME_W, FRAME_H),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return out
    }

    private fun drawBlurBackdrop(canvas: Canvas, src: Bitmap) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        // Perkecil drastis lalu perbesar = blur murah tanpa RenderScript
        val mid = Bitmap.createScaledBitmap(
            src, max(1, src.width / 8), max(1, src.height / 8), true
        )
        val tiny = Bitmap.createBitmap(48, 64, Bitmap.Config.ARGB_8888)
        Canvas(tiny).drawBitmap(mid, coverMatrix(mid.width, mid.height, 48, 64), paint)
        canvas.drawBitmap(tiny, null, RectF(0f, 0f, FRAME_W.toFloat(), FRAME_H.toFloat()), paint)
        canvas.drawColor(Color.argb(70, 0, 0, 0)) // gelapkan sedikit agar subjek menonjol
    }

    private fun coverMatrix(sw: Int, sh: Int, dw: Int, dh: Int): Matrix {
        val s = max(dw / sw.toFloat(), dh / sh.toFloat())
        return Matrix().apply {
            postScale(s, s)
            postTranslate((dw - sw * s) / 2f, (dh - sh * s) / 2f)
        }
    }
}
