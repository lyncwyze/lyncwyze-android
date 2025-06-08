package com.intelly.lyncwyze.Assest.utilities

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.core.exponentialDecay
import com.intelly.lyncwyze.Assest.networkWork.NetworkManager
import java.io.File
import java.io.FileOutputStream

object ImageUtilities {
    /**
     * Saves a bitmap to the cache directory and returns its URI.
     *
     * @param context The context used to access cache directory.
     * @param bitmap The bitmap to save.
     * @return The URI of the saved bitmap.
     */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
        // Create a file in the cache directory
        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        // Write the bitmap to the file
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        // Return the URI of the saved file
        return Uri.fromFile(file)
    }


    suspend fun imageFullPath(path: String): String? {
        val response = NetworkManager.apiService.loadImage(path)
        return if (response.isSuccessful) {
            response.body()?.string() // Convert ResponseBody to String
        } else {
            null
        }
    }
}