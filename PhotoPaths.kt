package com.autophoto.ai.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoPaths {
    /** filesDir/photos/x.jpg -> filesDir/thumbs/x.jpg */
    fun thumbnailFile(filePath: String): File {
        val f = File(filePath)
        return File(File(f.parentFile?.parentFile, "thumbs"), f.name)
    }

    /** Pakai thumbnail bila ada, kalau tidak pakai file asli. */
    fun thumbOrFull(filePath: String): File = thumbnailFile(filePath).takeIf { it.exists() } ?: File(filePath)
}

object DateUtils {
    private val formatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
    fun format(millis: Long): String = formatter.format(Date(millis))
}
