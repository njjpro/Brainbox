package com.njjpro.brainbox.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.njjpro.brainbox.notify.Notifications
import com.njjpro.brainbox.update.UpdateChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: BrainboxViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val updateState = vm.updateState
    var permissionTick by remember { mutableStateOf(0) }
    val notificationsOn = remember(permissionTick) { Notifications.hasPermission(context) }
    val exactAlarmOn = remember(permissionTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Brainbox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "版本 ${vm.currentVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "一个本地记事本：提醒、vibe coding 想法、思考感悟、科研 Idea、每月报销。数据只存在这台手机上。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("检查更新", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                    when (updateState) {
                        is UpdateUiState.Idle -> {
                            Text(
                                text = "从 GitHub Releases 获取最新版本。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { vm.checkForUpdates() }) { Text("检查更新") }
                        }

                        is UpdateUiState.Checking -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                                Text("正在检查…", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        is UpdateUiState.UpToDate -> {
                            Text(
                                text = "已是最新版本 ${vm.currentVersion}。",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedButton(onClick = { vm.checkForUpdates() }) { Text("再检查一次") }
                        }

                        is UpdateUiState.Available -> {
                            val info = (updateState as UpdateUiState.Available).info
                            Text(
                                text = "发现新版本 ${info.versionName}（当前 ${vm.currentVersion}）",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (info.apkSize > 0) {
                                Text(
                                    text = "安装包大小 ${UpdateChecker.sizeText(info.apkSize)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = info.releaseNotes.take(600),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.downloadAndInstall() }) { Text("下载并安装") }
                                TextButton(onClick = { vm.dismissUpdate() }) { Text("稍后") }
                            }
                        }

                        is UpdateUiState.Downloading -> {
                            val state = updateState as UpdateUiState.Downloading
                            Text(
                                text = "正在下载 ${state.info.versionName}… ${state.progress}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        is UpdateUiState.Ready -> {
                            val state = updateState as UpdateUiState.Ready
                            Text(
                                text = "已下载 ${state.info.versionName}，可在系统安装器里完成安装。",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { UpdateChecker.install(context, state.file) }) { Text("重新打开安装器") }
                        }

                        is UpdateUiState.Error -> {
                            Text(
                                text = (updateState as UpdateUiState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.checkForUpdates() }) { Text("重试") }
                                OutlinedButton(onClick = { UpdateChecker.openInstallPermission(context) }) {
                                    Text("安装权限")
                                }
                                TextButton(onClick = { vm.dismissUpdate() }) { Text("关闭") }
                            }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("提醒权限", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    SettingRow(
                        title = "通知",
                        enabled = notificationsOn,
                        actionLabel = "去开启",
                        onAction = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }
                            permissionTick++
                        },
                    )
                    SettingRow(
                        title = "精确闹钟（不开启会延迟几分钟）",
                        enabled = exactAlarmOn,
                        actionLabel = "去开启",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    .setData(Uri.parse("package:${context.packageName}"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                runCatching { context.startActivity(intent) }
                            }
                            permissionTick++
                        },
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "开源 MIT。源码与版本发布在 GitHub。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/${UpdateChecker.OWNER}/${UpdateChecker.REPO}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }) { Text("打开 GitHub 仓库") }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    enabled: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (enabled) "✅ $title" else "⚠️ $title",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (!enabled) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
