package com.splash.offline.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ReleaseUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val releasePageUrl: String
)

sealed interface UpdateCheckResult {
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class UpdateAvailable(val info: ReleaseUpdateInfo) : UpdateCheckResult
    data object NoReleaseYet : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

enum class InstallLaunchResult {
    LAUNCHED,
    NEEDS_UNKNOWN_SOURCES_PERMISSION,
    FAILED
}

object AppUpdateManager {
    const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/Epsilon10X/Splash/releases/latest"

    fun getInstalledVersionName(context: Context): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName?.takeIf { it.isNotBlank() } ?: "2.0.17"
        } catch (_: Throwable) {
            "2.0.17"
        }
    }

    fun isNewerVersion(remote: String, current: String): Boolean {
        fun parseParts(raw: String): List<Int> {
            val cleaned = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            return cleaned.split('.').map { it.toIntOrNull() ?: 0 }
        }
        val r = parseParts(remote)
        val c = parseParts(current)
        val maxLen = maxOf(r.size, c.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    suspend fun checkForUpdate(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Splash-Android/$currentVersion")
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.NoReleaseYet
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Error("HTTP $code")
            }
            val bodyText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(bodyText)
            val tagName = json.optString("tag_name", "").trim()
            val remoteVersion = tagName.removePrefix("v").removePrefix("V")
            if (remoteVersion.isBlank()) {
                return@withContext UpdateCheckResult.NoReleaseYet
            }
            val assets = json.optJSONArray("assets") ?: return@withContext UpdateCheckResult.NoReleaseYet
            var preferredAsset: JSONObject? = null
            var fallbackApkAsset: JSONObject? = null
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val lower = name.lowercase(Locale.ROOT)
                if (!lower.endsWith(".apk")) continue
                if (fallbackApkAsset == null) fallbackApkAsset = asset
                if (lower.contains("release") && !lower.contains("debug")) {
                    preferredAsset = asset
                    break
                }
            }
            val chosen = preferredAsset ?: fallbackApkAsset ?: return@withContext UpdateCheckResult.NoReleaseYet
            val apkUrl = chosen.optString("browser_download_url", "").trim()
            if (apkUrl.isBlank()) {
                return@withContext UpdateCheckResult.NoReleaseYet
            }
            if (!isNewerVersion(remoteVersion, currentVersion)) {
                return@withContext UpdateCheckResult.UpToDate(currentVersion)
            }
            val info = ReleaseUpdateInfo(
                versionName = remoteVersion,
                releaseNotes = json.optString("body", "").trim(),
                apkDownloadUrl = apkUrl,
                apkFileName = chosen.optString("name", "Splash-update.apk").ifBlank { "Splash-update.apk" },
                apkSizeBytes = chosen.optLong("size", 0L),
                releasePageUrl = json.optString("html_url", "")
            )
            UpdateCheckResult.UpdateAvailable(info)
        } catch (t: Throwable) {
            UpdateCheckResult.Error(t.message ?: "Network error")
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        info: ReleaseUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            var currentUrl = info.apkDownloadUrl
            var redirects = 0
            while (true) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = false
                    setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                    setRequestProperty("User-Agent", "Splash-Android/Updater")
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank() || redirects >= 6) {
                        return@withContext Result.failure(IllegalStateException("Too many redirects"))
                    }
                    currentUrl = URL(URL(currentUrl), loc).toString()
                    redirects++
                    continue
                }
                if (code !in 200..299) {
                    val errCode = code
                    conn.disconnect()
                    return@withContext Result.failure(IllegalStateException("HTTP $errCode"))
                }
                break
            }

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { old ->
                if (old.name.endsWith(".apk", ignoreCase = true)) {
                    runCatching { old.delete() }
                }
            }
            val safeName = info.apkFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "Splash-${info.versionName}.apk" }
            val outFile = File(updatesDir, safeName)

            val contentLength = conn!!.contentLengthLong.takeIf { it > 0L } ?: info.apkSizeBytes
            var downloaded = 0L
            var lastReportedBytes = 0L

            withContext(Dispatchers.Main) { onProgress(0f) }
            conn.inputStream.use { input ->
                FileOutputStream(outFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0L && downloaded - lastReportedBytes >= 64 * 1024) {
                            lastReportedBytes = downloaded
                            val p = (downloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) { onProgress(p) }
                        }
                    }
                    output.flush()
                }
            }

            if (!outFile.exists() || outFile.length() <= 0L) {
                return@withContext Result.failure(IllegalStateException("Downloaded APK is empty"))
            }
            withContext(Dispatchers.Main) { onProgress(1f) }
            Result.success(outFile)
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {
            }
        }
    }

    fun installApk(context: Context, apkFile: File): InstallLaunchResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permIntent)
                return InstallLaunchResult.NEEDS_UNKNOWN_SOURCES_PERMISSION
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            InstallLaunchResult.LAUNCHED
        } catch (_: Throwable) {
            InstallLaunchResult.FAILED
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = (bytes / 1024.0).coerceAtLeast(1.0)
            String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
