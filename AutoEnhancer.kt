package com.autophoto.ai.domain

import android.graphics.Bitmap
import com.autophoto.ai.utils.BitmapUtils
import com.autophoto.ai.utils.ColorMatrixUtils.contrastBrightness
import com.autophoto.ai.utils.withColorMatrix
import kotlin.math.sqrt

/** Brightness + kontras otomatis. Dijalankan SETELAH auto filter. */
class AutoEnhancer {

    fun enhance(bitmap: Bitmap): Bitmap {
        val small = BitmapUtils.scaleToMaxSide(bitmap, 96)
        val px = IntArray(small.width * small.height)
        small.getPixels(px, 0, small.width, 0, 0, small.width, small.height)

        // Rata-rata & simpangan baku luminance (skala 0..255)
        var sum = 0.0
        val lum = DoubleArray(px.size) {
            val p = px[it]
            val l = 0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)
            sum += l
            l
        }
        val mean = sum / lum.size
        val std = sqrt(lum.sumOf { (it - mean) * (it - mean) } / lum.size)

        // Geser brightness ke arah 128 (maks +-25), kontras lebih kuat bila foto flat
        val shift = ((128.0 - mean) * 0.5).coerceIn(-25.0, 25.0).toFloat()
        val contrast = when {
            std < 40 -> 1.20f
            std < 60 -> 1.12f
            else -> 1.06f
        }
        return bitmap.withColorMatrix(contrastBrightness(contrast, shift))
    }
}
