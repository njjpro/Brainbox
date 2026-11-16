package com.njjpro.brainbox.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

sealed interface CheckResult {
    data class Newer(val info: UpdateInfo) : CheckResult
    data object Latest : CheckResult
    data class Failed(val message: String) : CheckResult
}

/**
 * 基于 GitHub Releases 的自更新：查最新 release → 比较版本 → 下载 APK → 拉起系统安装器。
 */
object UpdateChecker {

    const val OWNER = "njjpro"
    const val REPO = "Brainbox"
    private const val LATEST_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    fun currentVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0.0.0"

    suspend fun check(currentVersion: String): CheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(LATEST_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Brainbox-Android")
            }
            val code = connection.responseCode
            if (code == 404) {
                return@withContext CheckResult.Failed("仓库还没有发布任何 Release")
            }
            if (code == 403) {
                return@withContext CheckResult.Failed("GitHub 接口被限流，稍后再试")
            }
            if (code != 200) {
                return@withContext CheckResult.Failed("GitHub 返回 HTTP $code")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return@withContext CheckResult.Failed("Release 缺少版本号") }
            val latest = tag.trim().removePrefix("v").removePrefix("V")
            if (compareVersions(latest, currentVersion) <= 0) {
                return@withContext CheckResult.Latest
            }

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var apkSize = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkSize = asset.optLong("size")
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) {
                return@withContext CheckResult.Failed("最新 Release 里没有 APK 附件")
            }

            CheckResult.Newer(
                UpdateInfo(
                    versionName = latest,
                    releaseNotes = json.optString("body").trim(),
                    apkUrl = apkUrl,
                    apkSize = apkSize,
                    pageUrl = json.optString("html_url"),
                ),
            )
        } catch (e: Exception) {
            CheckResult.Failed(e.message ?: "网络错误")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun download(
        context: Context,
        info: UpdateInfo,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "brainbox-${info.versionName}.apk")

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Brainbox-Android")
            }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("下载失败：HTTP ${connection.responseCode}")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            val percent = (downloaded * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
            onProgress(100)
            target
        } finally {
            connection?.disconnect()
        }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun install(context: Context, file: File): Boolean {
        if (!canInstall(context)) {
            openInstallPermission(context)
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /** 比较形如 1.2.3 的版本号；a > b 返回正数。 */
    fun compareVersions(a: String, b: String): Int {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V")
            .split('.', '-', '+')
            .map { it.toIntOrNull() ?: 0 }

        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val l = left.getOrElse(i) { 0 }
            val r = right.getOrElse(i) { 0 }
            if (l != r) return l - r
        }
        return 0
    }

    fun sizeText(bytes: Long): String = when {
        bytes <= 0L -> "未知大小"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    }
}
