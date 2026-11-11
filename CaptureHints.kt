package com.autophoto.ai.domain

import com.autophoto.ai.domain.model.PhotoFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * Catatan dari layar kamera untuk pipeline pemrosesan.
 * framedLive = true -> foto sudah dibingkai AI saat preview, jadi tidak di-crop ulang.
 * filter     = filter yang dipakai di preview (null = pilih otomatis dari analisis foto).
 */
data class CaptureHint(val framedLive: Boolean, val filter: PhotoFilter?)

object CaptureHints {
    private val map = ConcurrentHashMap<String, CaptureHint>()
    fun put(fileName: String, hint: CaptureHint) { map[fileName] = hint }
    fun get(fileName: String): CaptureHint? = map[fileName]
}
