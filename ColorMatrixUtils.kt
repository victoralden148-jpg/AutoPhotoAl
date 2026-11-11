package com.autophoto.ai.utils

import android.graphics.ColorMatrix

/** Helper kecil untuk menyusun ColorMatrix. */
object ColorMatrixUtils {
    fun saturation(s: Float) = ColorMatrix().apply { setSaturation(s) }

    fun scaleRgb(r: Float, g: Float, b: Float) = ColorMatrix().apply { setScale(r, g, b, 1f) }

    /** contrast c berpivot di 127.5, brightness b dalam skala 0..255. */
    fun contrastBrightness(c: Float, b: Float): ColorMatrix {
        val t = (1f - c) * 127.5f + b
        return ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /** Gabungkan beberapa matrix berurutan. */
    fun combine(vararg matrices: ColorMatrix): ColorMatrix {
        val result = ColorMatrix()
        matrices.forEach { result.postConcat(it) }
        return result
    }
}
