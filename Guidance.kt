package com.autophoto.ai.domain.live

import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.domain.model.PhotoFilter

enum class MoveDir { NONE, LEFT, RIGHT, UP, DOWN }

/** Hasil analisis satu frame preview. Semua koordinat dinormalisasi 0..1 pada frame preview. */
data class Guidance(
    val analyzing: Boolean = true,
    val hasSubject: Boolean = false,
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    /** Titik tempat pusat subjek seharusnya berada (di frame saat ini). */
    val targetX: Float = 0.5f,
    val targetY: Float = 0.5f,
    val dir: MoveDir = MoveDir.NONE,
    /** >1 = perlu zoom in, <1 = perlu zoom out, 1 = pas. */
    val zoomFactor: Float = 1f,
    val hasFace: Boolean = false,
    val ready: Boolean = false,
    val readyStreak: Int = 0,
    val message: String = "AI menganalisis, tahan stabil",
    val suggestedFilter: PhotoFilter? = null,
    val mood: MoodResult? = null
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}
