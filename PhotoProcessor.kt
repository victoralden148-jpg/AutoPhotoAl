package com.autophoto.ai.domain

import com.autophoto.ai.domain.model.ProcessedPhoto
import com.autophoto.ai.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pipeline lengkap, urutan sesuai spesifikasi:
 * decode -> auto crop -> auto zoom -> analisis mood -> auto filter -> auto enhance
 */
class PhotoProcessor(
    private val subjectProcessor: SubjectProcessor = SubjectProcessor(),
    private val moodAnalyzer: MoodAnalyzer = MoodAnalyzer(),
    private val filterSelector: AutoFilterSelector = AutoFilterSelector(),
    private val enhancer: AutoEnhancer = AutoEnhancer()
) {
    suspend fun process(imagePath: String, hint: CaptureHint? = null): ProcessedPhoto = withContext(Dispatchers.Default) {
        val original = BitmapUtils.decodeFile(imagePath, 1600)

        // Foto sudah dibingkai AI saat preview: cukup pas-kan ke 3:4, lalu filter + enhance
        if (hint != null && hint.framedLive) {
            val framedLive = subjectProcessor.coverFit(original)
            val liveMood = moodAnalyzer.analyze(framedLive)
            val liveFilter = hint.filter ?: filterSelector.select(liveMood)
            val liveEnhanced = enhancer.enhance(filterSelector.apply(framedLive, liveFilter))
            return@withContext ProcessedPhoto(original, liveEnhanced, liveMood, liveFilter, subjectFound = true)
        }

        // 1. Auto crop
        val bounds = subjectProcessor.detectBounds(original)
        val crop = bounds?.let { subjectProcessor.cropToSubject(original, it) }

        // 2. Auto zoom (atau fallback center-crop)
        val framed = if (crop != null) subjectProcessor.autoZoom(crop) else subjectProcessor.coverFit(original)

        // 3. Analisis mood pada area subjek, lalu auto filter
        val mood = moodAnalyzer.analyze(crop?.bitmap ?: original)
        val filter = hint?.filter ?: filterSelector.select(mood)
        val filtered = filterSelector.apply(framed, filter)

        // 4. Auto enhance (setelah filter)
        val enhanced = enhancer.enhance(filtered)

        ProcessedPhoto(original, enhanced, mood, filter, subjectFound = crop != null)
    }
}
