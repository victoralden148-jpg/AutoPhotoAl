package com.autophoto.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadata satu foto hasil edit. File gambarnya disimpan di internal storage. */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val filterName: String,        // id filter, mis. "warm_boost"
    val brightnessLevel: Float,    // 0..1
    val colorTemperature: String,  // warm / cool / neutral
    val saturationLevel: Float,    // 0..1
    val hasFace: Boolean,
    val createdAt: Long
)
