package com.autophoto.ai.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.autophoto.ai.utils.MediaStoreExporter
import com.autophoto.ai.utils.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mengembalikan fungsi "simpan ke galeri HP".
 * Meminta izin storage sesuai versi Android bila belum ada, lalu menyalin via MediaStore.
 */
@Composable
fun rememberPhotoExporter(onResult: (Boolean) -> Unit): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    var pendingPath by remember { mutableStateOf<String?>(null) }

    fun doExport(path: String) {
        scope.launch {
            val uri = withContext(Dispatchers.IO) { MediaStoreExporter.export(context, path) }
            currentOnResult(uri != null)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val path = pendingPath
        pendingPath = null
        if (path != null) {
            // Android 10+ tidak butuh izin tulis untuk MediaStore; Android 9- wajib
            val ok = !PermissionUtils.exportNeedsPermission() || result.values.all { it }
            if (ok) doExport(path) else currentOnResult(false)
        }
    }

    return { path ->
        if (PermissionUtils.hasStoragePermissions(context)) doExport(path)
        else {
            pendingPath = path
            launcher.launch(PermissionUtils.storagePermissions())
        }
    }
}
