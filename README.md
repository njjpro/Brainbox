# Brainbox

一个给自己用的本地记事本 App（Android，Kotlin + Jetpack Compose + Room）。

五类记录：

| 分类 | 用途 |
| --- | --- |
| ⏰ 提醒 | 到点推送通知，比如「明天 9:00 去买菜」。支持单次、每天、每周、每月重复 |
| 🧪 Vibe Coding | 写码时冒出来的想法、待试的技术方案 |
| 💭 思考感想 | 日常思考、读书/复盘记录 |
| 🔬 科研 Idea | 研究方向、实验设计、论文点子 |
| 🧾 每月报销 | 按**报销周期**记账：日期 / 金额 / 事项 / 类别 / 是否已报销，顶部显示本期合计与未报金额；支持粘贴账单批量导入 |

数据全部存在手机本地（Room/SQLite），不联网、不上传、无分析、无广告（联网只用于检查更新）。

## 功能要点

- **提醒**：`AlarmManager` + `BroadcastReceiver`，通知点进去直接打开对应记录；重启手机、改时区、App 升级后自动恢复所有未来提醒；提醒列表支持「延后 10 分钟」「关闭提醒」。
- **提醒可靠**：优先用 `setAlarmClock`（系统里优先级最高的闹钟，Doze 下依然准点，多数国产 ROM 的省电清理也会跳过它），失败再降级 `setExactAndAllowWhileIdle` → `setAndAllowWhileIdle`。设置页有「提醒可靠性」面板，逐项显示通知权限、精确闹钟、电池优化、自启动的状态并可一键跳转。
- **报销按周期归期**：报销通常每月 21–26 号提交，所以账目按「上期截止日次日 ~ 本期截止日」归期，而不是自然月。默认结算日 26 日 → 11 月这一期 = 10-27 ~ 11-26，11-27 之后的支出自动落到 12 月这一期。结算日可在「设置 → 报销周期」里调（1–31，月底自动夹取）。金额以「分」为单位存储（`Long`，避免浮点误差），按笔勾选已报销，底部显示全部未报合计。
- **账单导入**：报销页右上角「导入」→ 粘贴整段账单文本 → 自动解析出日期、类别、事项、金额 → 逐条确认类别后批量写入。解析规则是「事项关键词优先，其次文本里标的类别，都没有就落『其他』」；标题行、合计行自动跳过。类别固定七个：交通 / 餐饮 / 住宿 / 耗材 / 会议 / 版面费 / 其他。
- **搜索 / 置顶**：每个分类内可全文搜索标题与正文；记录可置顶。
- **检查更新**：从 GitHub Releases 读最新版本号，与本地 `versionName` 比较，可下载 APK 并拉起系统安装器。

## 提醒没响？先做这三步

提醒是系统闹钟触发的，**App 不需要挂在后台**。真机上不响，九成是下面几项里的一项被关了：

1. **设置 → 提醒可靠性**，逐项检查：通知权限、精确闹钟、电池优化（改成「不受限制」）、自启动（小米/华为/OPPO/vivo 等机型）。
2. 点 **「10 秒后提醒」**，然后把 App 从最近任务里划掉。10 秒后正常应弹出通知 —— 这一步验证的是「闹钟 → 广播 → 通知」整条链路。
3. 只有 **「立即发通知」** 能响、**「10 秒后提醒」** 不响 → 是闹钟被系统拦了（自启动/电池优化）；两个都不响 → 是通知权限或通知渠道被关了。

排查日志（`adb logcat -s BrainboxReminder BrainboxNotify`）会打出每次排闹钟、触发、发通知的结果。

## 安装

去 [Releases](https://github.com/njjpro/Brainbox/releases) 下载最新 `brainbox-<version>.apk`，安装即可（首次需在系统里允许「安装未知应用」）。

要求 Android 8.0（API 26）及以上。

## 自行构建

需要 JDK 17 与 Android SDK 34：

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk   # 内含 platforms;android-34、build-tools;34.0.0
./gradlew assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`。

> 签名信息从仓库根目录的 `keystore.properties` 读取（该文件与 `*.jks` 都已 gitignore，密钥不会进仓库）。没有这个文件时构建出的是未签名 APK，仍然可以 `./gradlew assembleDebug` 调试。
>
> 自己构建的话，在根目录建 `keystore.properties`：
>
> ```properties
> storeFile=app/brainbox-release.jks
> storePassword=你的密码
> keyAlias=你的别名
> keyPassword=你的密码
> ```

## 单元测试

```bash
./gradlew testReleaseUnitTest
```

覆盖金额解析/格式化、版本号比较、重复提醒的下次触发时间（含 1 月 31 日按月推进得到 2 月 28 日这类边界）、过期提醒前移、标题回退等纯逻辑。

> ⚠️ Windows 已知问题：**项目路径含非 ASCII 字符（如中文目录）时，单测会报 `ClassNotFoundException`**（Gradle 测试进程读 classpath 的编码问题），编译、打包不受影响。要在这种情况下跑单测，把项目复制到纯 ASCII 路径再执行。

## 发布新版本（触发 App 内更新）

1. 改 `app/build.gradle.kts` 里的 `versionCode`（必须递增）与 `versionName`。
2. `./gradlew assembleRelease`。
3. 在 GitHub 建 Release，tag 用 `v<versionName>`（例如 `v1.0.1`），把 `app-release.apk` 作为 release asset 上传。
4. 旧版 App 里的「设置 → 检查更新」会读到新版本并提示下载。

App 启动时会静默检查更新（12 小时内最多一次），发现新版本只在设置页提示，不会打扰使用。

## 目录结构

```
app/src/main/java/com/njjpro/brainbox/
├── data/        Room 实体、DAO、数据库、Repository
├── notify/      闹钟调度、通知、开机/升级后恢复提醒
├── update/      GitHub Releases 检查更新、下载、拉起安装
├── ui/          Compose 界面（首页/分类列表/编辑页/提醒/报销/导入/设置）与 ViewModel
├── BrainboxApp.kt
└── MainActivity.kt
```

## 许可

MIT，见 [LICENSE](LICENSE)。
