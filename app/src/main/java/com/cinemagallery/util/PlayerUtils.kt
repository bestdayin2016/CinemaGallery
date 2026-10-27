package com.cinemagallery.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object PlayerUtils {

    fun playMediaInExternalPlayer(context: Context, filePath: String) {
        if (filePath.isBlank()) {
            Toast.makeText(context, "No media file path associated", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = File(filePath)
            val uri: Uri = if (file.exists()) {
                try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
            } else {
                Uri.parse(filePath)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(intent, "Open Video With...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to launch external player: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
