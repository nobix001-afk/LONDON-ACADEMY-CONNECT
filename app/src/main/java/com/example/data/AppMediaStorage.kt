package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class AppMediaStorage(context: Context) {
    private val appContext = context.applicationContext

    fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    fun saveSelfieBitmap(bitmap: Bitmap, ownerId: String): String {
        return try {
            val dir = File(appContext.filesDir, "selfies")
            if (!dir.exists()) dir.mkdirs()
            val safeId = ownerId.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "user" }
            val fileName = "selfie_${safeId}_${System.currentTimeMillis()}.jpg"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    fun saveAadhaarUri(uri: Uri, admissionNo: String): String {
        return try {
            val dir = File(appContext.filesDir, "aadhaar")
            if (!dir.exists()) dir.mkdirs()
            val safeAdm = admissionNo.trim().replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "student" }
            val file = File(dir, "aadhaar_${safeAdm}_${System.currentTimeMillis()}.jpg")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 0L) file.absolutePath else uri.toString()
        } catch (_: Exception) {
            uri.toString()
        }
    }
}
