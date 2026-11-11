package com.autophoto.ai.domain.model

import android.graphics.Bitmap

enum class ColorTemperature(val label: String) {
    WARM("warm"), COOL("cool"), NEUTRAL("neutral");

    companion object {
        fun fromLabel(label: String) = values().firstOrNull { it.label == label } ?: NEUTRAL
    }
}

/** Hasil analisis suasana foto. */
data class MoodResult(
    val brightnessLevel: Float,          // 0 (gelap) .. 1 (terang)
    val colorTemperature: ColorTemperature,
    val saturationLevel: Float,          // 0 (pudar) .. 1 (vivid)
    val hasFace: Boolean
)

enum class PhotoFilter(val id: String, val displayName: String) {
    WARM_BOOST("warm_boost", "Warm Boost"),
    VINTAGE_SOFT("vintage_soft", "Vintage Soft"),
    PORTRAIT_POP("portrait_pop", "Portrait Pop"),
    CINEMATIC_COOL("cinematic_cool", "Cinematic Cool"),
    NIGHT_ENHANCE("night_enhance", "Night Enhance");

    companion object {
        fun fromId(id: String) = values().firstOrNull { it.id == id }
    }
}

/** Output pipeline: crop -> zoom -> filter -> enhance. */
data class ProcessedPhoto(
    val original: Bitmap,
    val result: Bitmap,
    val mood: MoodResult,
    val filter: PhotoFilter,
    val subjectFound: Boolean
)
