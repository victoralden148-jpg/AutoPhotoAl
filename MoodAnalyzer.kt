package com.autophoto.ai.domain

import android.graphics.Bitmap
import com.autophoto.ai.domain.model.ColorTemperature
import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.utils.BitmapUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

/** Menganalisis brightness, saturasi, suhu warna, dan keberadaan wajah. */
class MoodAnalyzer {

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
    }

    suspend fun analyze(bitmap: Bitmap): MoodResult {
        // Hitung statistik pada versi kecil (96px) supaya cepat
        val small = BitmapUtils.scaleToMaxSide(bitmap, 96)
        val w = small.width
        val h = small.height
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)

        var sumL = 0.0; var sumS = 0.0; var sumR = 0.0; var sumB = 0.0
        for (p in px) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            sumL += (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            sumS += if (mx == 0) 0.0 else (mx - mn).toDouble() / mx
            sumR += r
            sumB += b
        }
        val n = px.size.coerceAtLeast(1)
        val brightness = (sumL / n).toFloat()
        val saturation = (sumS / n).toFloat()
        val warmth = ((sumR - sumB) / n / 255.0).toFloat() // + = merah/hangat, - = biru/dingin

        val temperature = when {
            warmth > 0.05f -> ColorTemperature.WARM
            warmth < -0.05f -> ColorTemperature.COOL
            else -> ColorTemperature.NEUTRAL
        }
        return MoodResult(
            brightnessLevel = brightness.coerceIn(0f, 1f),
            colorTemperature = temperature,
            saturationLevel = saturation.coerceIn(0f, 1f),
            hasFace = detectFace(bitmap)
        )
    }

    private suspend fun detectFace(bitmap: Bitmap): Boolean = try {
        val scaled = BitmapUtils.scaleToMaxSide(bitmap, 640)
        faceDetector.process(InputImage.fromBitmap(scaled, 0)).await().isNotEmpty()
    } catch (e: Exception) {
        false // gagal deteksi -> anggap tidak ada wajah
    }
}
