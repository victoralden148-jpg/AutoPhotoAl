package com.autophoto.ai.domain

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import com.autophoto.ai.domain.model.ColorTemperature
import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.domain.model.PhotoFilter
import com.autophoto.ai.utils.ColorMatrixUtils.combine
import com.autophoto.ai.utils.ColorMatrixUtils.contrastBrightness
import com.autophoto.ai.utils.ColorMatrixUtils.saturation
import com.autophoto.ai.utils.ColorMatrixUtils.scaleRgb
import com.autophoto.ai.utils.withColorMatrix

/** Memilih filter dari hasil analisis mood, lalu menerapkannya via ColorMatrix. */
class AutoFilterSelector {

    /** Urutan prioritas aturan: yang paling spesifik diperiksa lebih dulu. */
    fun select(mood: MoodResult): PhotoFilter = when {
        mood.brightnessLevel < 0.22f -> PhotoFilter.NIGHT_ENHANCE                        // low light
        mood.hasFace && mood.saturationLevel >= 0.35f -> PhotoFilter.PORTRAIT_POP         // vivid + wajah
        mood.brightnessLevel < 0.45f || mood.colorTemperature == ColorTemperature.COOL ->
            PhotoFilter.WARM_BOOST                                                        // gelap / dingin
        mood.colorTemperature == ColorTemperature.WARM -> PhotoFilter.VINTAGE_SOFT        // terang / hangat
        else -> PhotoFilter.CINEMATIC_COOL                                                // pudar / netral
    }

    fun colorMatrix(filter: PhotoFilter): ColorMatrix = when (filter) {
        PhotoFilter.WARM_BOOST -> combine(
            scaleRgb(1.10f, 1.00f, 0.88f), saturation(1.15f), contrastBrightness(1.05f, 12f)
        )
        PhotoFilter.VINTAGE_SOFT -> combine(
            saturation(0.80f), scaleRgb(1.06f, 1.00f, 0.90f), contrastBrightness(0.92f, 14f)
        )
        PhotoFilter.PORTRAIT_POP -> combine(
            saturation(1.30f), scaleRgb(1.04f, 1.00f, 0.98f), contrastBrightness(1.12f, 4f)
        )
        PhotoFilter.CINEMATIC_COOL -> combine(
            saturation(0.85f), scaleRgb(0.92f, 1.00f, 1.10f), contrastBrightness(1.15f, -6f)
        )
        PhotoFilter.NIGHT_ENHANCE -> combine(
            contrastBrightness(1.10f, 38f), saturation(1.10f), scaleRgb(0.98f, 1.00f, 1.05f)
        )
    }

    fun apply(bitmap: Bitmap, filter: PhotoFilter): Bitmap =
        bitmap.withColorMatrix(colorMatrix(filter))
}
