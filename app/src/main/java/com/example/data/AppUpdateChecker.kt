package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateCheckResult
    data class UpToDate(val currentVersionName: String, val currentVersionCode: Int) : AppUpdateCheckResult
    data class Error(val message: String) : AppUpdateCheckResult
}

object AppUpdateChecker {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun checkForUpdate(
        versionInfoUrl: String = BuildConfig.APK_VERSION_INFO_URL,
        fallbackDownloadUrl: String = BuildConfig.APK_UPDATE_URL,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME
    ): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        if (versionInfoUrl.isBlank()) {
            return@withContext AppUpdateCheckResult.Error("Update feed URL is not configured.")
        }

        try {
            val cacheBustedUrl = if (versionInfoUrl.contains("?")) {
                "$versionInfoUrl&t=${System.currentTimeMillis()}"
            } else {
                "$versionInfoUrl?t=${System.currentTimeMillis()}"
            }

            val request = Request.Builder()
                .url(cacheBustedUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .header("Accept", "application/json")
                .header("User-Agent", "LondonAcademyConnect/${currentVersionName}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext AppUpdateCheckResult.Error(
                        "Unable to reach update server (HTTP ${response.code})."
                    )
                }
                val body = response.body?.string().orEmpty()
                evaluateVersionJson(
                    jsonBody = body,
                    fallbackDownloadUrl = fallbackDownloadUrl,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
            }
        } catch (e: Exception) {
            AppUpdateCheckResult.Error(e.localizedMessage ?: "Network error checking for updates.")
        }
    }

    internal fun evaluateVersionJson(
        jsonBody: String,
        fallbackDownloadUrl: String,
        currentVersionCode: Int,
        currentVersionName: String
    ): AppUpdateCheckResult {
        val info = parseVersionInfo(jsonBody, fallbackDownloadUrl)
            ?: return AppUpdateCheckResult.Error("Invalid update metadata received.")

        return if (info.versionCode > currentVersionCode) {
            AppUpdateCheckResult.UpdateAvailable(info)
        } else {
            AppUpdateCheckResult.UpToDate(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode
            )
        }
    }

    internal fun parseVersionInfo(
        jsonBody: String,
        fallbackDownloadUrl: String
    ): AppUpdateInfo? {
        if (jsonBody.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonBody)
            val versionCode = json.optInt("versionCode", -1)
            if (versionCode <= 0) return null
            val versionName = json.optString("versionName", "").trim().ifBlank { "1.0.$versionCode" }
            val downloadUrl = json.optString("downloadUrl", "").trim().ifBlank { fallbackDownloadUrl }
            val releaseNotes = json.optString("releaseNotes", "").trim()
                .ifBlank { "New features, improvements, and bug fixes are ready." }
            val publishedAt = json.optString("publishedAt", "").trim()
            AppUpdateInfo(
                versionCode = versionCode,
                versionName = versionName,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                publishedAt = publishedAt
            )
        }.getOrNull()
    }

    fun openUpdateLink(context: Context, url: String = BuildConfig.APK_UPDATE_URL): Boolean {
        val target = url.trim().ifBlank { BuildConfig.APK_UPDATE_URL }
        if (target.isBlank()) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
