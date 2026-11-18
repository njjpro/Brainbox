package com.njjpro.brainbox.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.njjpro.brainbox.notify.ReminderHealth
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
    val notificationsOn = remember(permissionTick) { ReminderHealth.notificationsAllowed(context) }
    val channelBlocked = remember(permissionTick) { ReminderHealth.channelBlocked(context) }
    val exactAlarmOn = remember(permissionTick) { ReminderHealth.exactAlarmsAllowed(context) }
    val batteryFree = remember(permissionTick) { ReminderHealth.batteryUnrestricted(context) }
    val autoStartPage = remember { ReminderHealth.autoStartPageAvailable(context) }
    val nextAlarm = remember(permissionTick) { ReminderHealth.nextAlarmTime(context) }

    // 从系统设置页返回时刷新上面的状态
    LifecycleResumeEffect(Unit) {
        permissionTick++
        onPauseOrDispose { }
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
                    Text("提醒可靠性", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "提醒由系统闹钟触发，App 不需要挂在后台。下面几项任何一项没开，提醒都可能到不了。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    SettingRow(
                        title = if (channelBlocked) "通知（「定时提醒」渠道被关闭）" else "通知",
                        enabled = notificationsOn && !channelBlocked,
                        actionLabel = "去开启",
                        onAction = {
                            if (channelBlocked) {
                                ReminderHealth.openChannelSettings(context)
                            } else {
                                ReminderHealth.openNotificationSettings(context)
                            }
                        },
                    )
                    SettingRow(
                        title = "闹钟与提醒（精确闹钟）",
                        enabled = exactAlarmOn,
                        actionLabel = "去开启",
                        onAction = { ReminderHealth.openExactAlarmSettings(context) },
                    )
                    SettingRow(
                        title = "电池优化（不受限制）",
                        enabled = batteryFree,
                        actionLabel = "去设置",
                        onAction = { ReminderHealth.openBatterySettings(context) },
                    )
                    if (autoStartPage) {
                        SettingRow(
                            title = "自启动 / 后台运行",
                            enabled = false,
                            actionLabel = "去开启",
                            onAction = {
                                if (!ReminderHealth.openAutoStartSettings(context)) {
                                    ReminderHealth.openAppDetails(context)
                                }
                            },
                            ignoreState = true,
                        )
                    }

                    Text(
                        text = nextAlarm?.let { "下一次提醒：${formatDateTime(it)}" } ?: "当前没有排定的提醒。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val ok = vm.sendTestNotification()
                            Toast.makeText(
                                context,
                                if (ok) "已发出测试通知" else "通知没开，先去打开通知权限",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }) { Text("立即发通知") }

                        OutlinedButton(onClick = {
                            vm.scheduleTestReminder(10)
                            Toast.makeText(context, "10 秒后会响一次，可以把 App 划掉试试", Toast.LENGTH_LONG)
                                .show()
                        }) { Text("10 秒后提醒") }
                    }
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
    ignoreState: Boolean = false,
) {
    val prefix = when {
        ignoreState -> "ℹ️"
        enabled -> "✅"
        else -> "⚠️"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$prefix $title",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (ignoreState || !enabled) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
