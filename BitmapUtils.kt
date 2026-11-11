package com.autophoto.ai.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object BitmapUtils {

    /** Decode file hemat memori (inSampleSize) + koreksi rotasi EXIF. */
    fun decodeFile(path: String, maxSide: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(path, opts)
            ?: throw IllegalStateException("Gagal membaca foto")

        val degrees = when (
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val rotated = if (degrees == 0f) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height,
            Matrix().apply { postRotate(degrees) }, true
        )
        return scaleToMaxSide(rotated, maxSide)
    }

    /** Perkecil bila sisi terpanjang > maxSide (tidak pernah memperbesar). */
    fun scaleToMaxSide(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val s = maxSide / longest.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * s).toInt().coerceAtLeast(1),
            (bitmap.height * s).toInt().coerceAtLeast(1), true
        )
    }
}

/** Gambar ulang bitmap dengan ColorMatrix, hasilnya bitmap baru. */
fun Bitmap.withColorMatrix(cm: ColorMatrix): Bitmap {
    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
    Canvas(out).drawBitmap(this, 0f, 0f, paint)
    return out
}
