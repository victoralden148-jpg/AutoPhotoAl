package com.autophoto.ai.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.autophoto.ai.data.local.PhotoDao
import com.autophoto.ai.data.local.PhotoEntity
import com.autophoto.ai.domain.model.MoodResult
import com.autophoto.ai.domain.model.PhotoFilter
import com.autophoto.ai.utils.BitmapUtils
import com.autophoto.ai.utils.PhotoPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Satu-satunya pintu data: mengurus file di internal storage + metadata di Room.
 * Layout: filesDir/photos/<nama>.jpg (full) dan filesDir/thumbs/<nama>.jpg (thumbnail kecil).
 */
class PhotoRepository(
    private val context: Context,
    private val dao: PhotoDao
) {
    val photos: Flow<List<PhotoEntity>> = dao.getAll()

    suspend fun getById(id: Long): PhotoEntity? = dao.getById(id)

    /** Simpan bitmap hasil edit + thumbnail + metadata. Mengembalikan id baris Room. */
    suspend fun savePhoto(bitmap: Bitmap, mood: MoodResult, filter: PhotoFilter): Long =
        withContext(Dispatchers.IO) {
            val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
            val thumbsDir = File(context.filesDir, "thumbs").apply { mkdirs() }
            val name = "AutoPhoto_${System.currentTimeMillis()}.jpg"

            val file = File(photosDir, name)
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }

            // Thumbnail kecil supaya grid galeri ringan
            val thumb = BitmapUtils.scaleToMaxSide(bitmap, 480)
            FileOutputStream(File(thumbsDir, name)).use { thumb.compress(Bitmap.CompressFormat.JPEG, 85, it) }

            dao.insert(
                PhotoEntity(
                    filePath = file.absolutePath,
                    filterName = filter.id,
                    brightnessLevel = mood.brightnessLevel,
                    colorTemperature = mood.colorTemperature.label,
                    saturationLevel = mood.saturationLevel,
                    hasFace = mood.hasFace,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

    suspend fun delete(photo: PhotoEntity) = withContext(Dispatchers.IO) {
        dao.delete(photo)
        File(photo.filePath).delete()
        PhotoPaths.thumbnailFile(photo.filePath).delete()
    }
}
