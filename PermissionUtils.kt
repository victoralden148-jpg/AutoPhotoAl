package com.autophoto.ai.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun hasCameraPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    /** Izin storage sesuai versi Android. */
    fun storagePermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun hasStoragePermissions(context: Context) = storagePermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /** Android 9 ke bawah WAJIB punya WRITE_EXTERNAL_STORAGE untuk menulis ke MediaStore. */
    fun exportNeedsPermission() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
}
