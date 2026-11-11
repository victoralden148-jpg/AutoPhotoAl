package com.autophoto.ai.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {
    fun sharePhoto(context: Context, filePath: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(filePath))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Bagikan foto").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
