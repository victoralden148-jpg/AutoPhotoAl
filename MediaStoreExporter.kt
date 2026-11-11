package com.autophoto.ai.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Menyalin foto internal ke galeri HP (Pictures/AutoPhotoAI) lewat MediaStore. */
object MediaStoreExporter {

    /** Panggil dari background thread. Mengembalikan Uri hasil, atau null bila gagal. */
    fun export(context: Context, filePath: String): Uri? {
        val src = File(filePath)
        if (!src.exists()) return null

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, src.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AutoPhotoAI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val uri = try { resolver.insert(collection, values) } catch (e: Exception) { null } ?: return null
        return try {
            resolver.openOutputStream(uri)?.use { out -> src.inputStream().use { it.copyTo(out) } }
                ?: throw java.io.IOException("Output stream null")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }
}
