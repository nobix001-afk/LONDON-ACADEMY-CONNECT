package com.example

import com.example.data.AppUpdateCheckResult
import com.example.data.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppUpdateCheckerTest {

    @Test
    fun `detects newer remote versionCode as UpdateAvailable`() {
        val json = """
            {
              "versionCode": 25,
              "versionName": "1.0.15",
              "downloadUrl": "https://github.com/nobix001-afk/LONDON-ACADEMY-CONNECT/releases/download/apk-latest/app-debug.apk",
              "releaseNotes": "v1.0.15: Auto-update support",
              "publishedAt": "2026-11-20T08:00:00Z"
            }
        """.trimIndent()

        val result = AppUpdateChecker.evaluateVersionJson(
            jsonBody = json,
            fallbackDownloadUrl = "https://fallback.example/app.apk",
            currentVersionCode = 1,
            currentVersionName = "1.0"
        )

        assertTrue(result is AppUpdateCheckResult.UpdateAvailable)
        val update = (result as AppUpdateCheckResult.UpdateAvailable).info
        assertEquals(25, update.versionCode)
        assertEquals("1.0.15", update.versionName)
        assertEquals("v1.0.15: Auto-update support", update.releaseNotes)
    }

    @Test
    fun `returns UpToDate when remote versionCode is not newer`() {
        val json = """
            {
              "versionCode": 25,
              "versionName": "1.0.15",
              "downloadUrl": ""
            }
        """.trimIndent()

        val result = AppUpdateChecker.evaluateVersionJson(
            jsonBody = json,
            fallbackDownloadUrl = "https://fallback.example/app.apk",
            currentVersionCode = 25,
            currentVersionName = "1.0.15"
        )

        assertTrue(result is AppUpdateCheckResult.UpToDate)
    }

    @Test
    fun `uses fallbackDownloadUrl when remote downloadUrl is blank`() {
        val json = """
            {
              "versionCode": 30,
              "versionName": "1.0.20",
              "downloadUrl": ""
            }
        """.trimIndent()

        val result = AppUpdateChecker.evaluateVersionJson(
            jsonBody = json,
            fallbackDownloadUrl = "https://fallback.example/app.apk",
            currentVersionCode = 10,
            currentVersionName = "1.0.0"
        )

        assertTrue(result is AppUpdateCheckResult.UpdateAvailable)
        assertEquals(
            "https://fallback.example/app.apk",
            (result as AppUpdateCheckResult.UpdateAvailable).info.downloadUrl
        )
    }
}
