# 实际构建报告

日期：2026-11-11T00:53:25+08:00（Asia/Shanghai）

## 结果

- `assembleDebug`：成功，APK 为 `app/build/outputs/apk/debug/app-debug.apk`。
- `testDebugUnitTest`：7 项通过，0失败、0错误。
- `lintDebug`：0错误，8项警告。
- `assembleDebugAndroidTest`：成功，设备测试 APK 可编译；**未执行 connectedDebugAndroidTest 或 K40 真机验收**。
- APK Signature Scheme v2 校验通过；minSdk 33，targetSdk 36。
- 合并 Manifest 检查无 INTERNET 权限，无截图/网络上传接口。
- 没有执行真实消息发送、付款或删除文件。
- APK 大小：26462983 bytes。
- SHA-256：`13f0bc56982024fc4e51a741d0f1382a606c642b3899855c4dc952c7e863fed3`。

## 环境与复现

云环境最初没有 Android SDK，Java 21 仅为 JRE，无 javac。已在工作区安装官方 Android SDK Platform 36、Build Tools 36.0.0、Platform Tools、Temurin JDK17 和 Gradle8.13。首次构建失败原因已解决。SDK/Gradle 的缓存与用户目录放在工作区；下载 JDK 使用系统 CA trust store 适配云代理，未关闭证书验证。标准 Android Studio / GitHub Actions 无需这些云代理设置。

最终执行：

```sh
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

构建日志保存在 `verification/build.log`，JVM 测试 XML 和 Lint 报告保存在 `verification/`。设备端测试未运行：本次未连接目标手机；云端 adb 初始化还受默认用户目录只读限制。未声称真机或 MIUI 无障碍行为已通过。

## 剩余 Lint 警告

- `AndroidGradlePluginVersion`：A newer version of Gradle than 8.13 is available: 8.14.6
- `GradleDependency`：A newer version of androidx.compose:compose-bom than 2025.10.01 is available: 2026.09.00
- `GradleDependency`：A newer version of androidx.activity:activity-compose than 1.11.0 is available: 1.13.0
- `GradleDependency`：A newer version of androidx.lifecycle:lifecycle-runtime-ktx than 2.9.4 is available: 2.11.0
- `UseKtx`：Use the KTX extension function `SharedPreferences.edit` instead?
- `SetTextI18n`：String literal in `setText` can not be translated. Use Android resources instead.
- `SetTextI18n`：String literal in `setText` can not be translated. Use Android resources instead.
- `SetTextI18n`：String literal in `setText` can not be translated. Use Android resources instead.

依赖更新提示不影响本次固定稳定版本的构建；界面目前仅提供中文，部分原生控制条字符串尚未资源化。

## 交付位置

完整工程位于 `/workspace/work/k40-agent`。指定的 `/codex/browser/projectless/0001-qq-codex/output` 在此云执行环境不存在，当前用户无法创建且 sudo 不可用，因此保留工作区中的工程和构建产物。未上传至外部服务或发布 GitHub 仓库；GitHub Actions 工作流已提供但未在 GitHub 上运行。
