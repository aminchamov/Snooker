package com.elocho.snooker.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtils {
    /**
     * Copies the content of a content:// URI to internal app storage
     * and returns a reliable file:// URI that survives app restarts.
     */
    fun copyUriToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        try {
            val contentResolver = context.contentResolver
            val avatarsDir = File(context.filesDir, "avatars")
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs()
            }

            val fileName = "avatar_${UUID.randomUUID()}.jpg"
            val destinationFile = File(avatarsDir, fileName)

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            return Uri.fromFile(destinationFile)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
