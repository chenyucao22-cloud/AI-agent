# K40 Agent 最小权限版 0.2.0

MIT 开源 Android 工程。面向红米 K40 / Android 13，Kotlin + Compose，minSdk33 / targetSdk36。

**本版不提供无障碍自动化，不需要启用“受限制的设置”。** 这是审查旧版后主动缩小的基础验证版本，无节点读取、点击、滑动、Shizuku、截图、网络或远程桥接。审查逐项结果与旧版固定代码位置见 [SECURITY_REVIEW.md](SECURITY_REVIEW.md)。

应用只支持用户点按“打开 Android 设置”“返回桌面”，以及应用内10秒可取消计时。计时结束只改变状态；离开应用立即取消计时。无运行时权限，无后台组件，无持久化日志。唯一导出组件为 Launcher Activity，不解释外部命令。合并 APK 的零权限/零服务/非调试标志由 CI 检查。

## 手机安装

在本仓库 Releases 下载 `K40-Agent-minimal.apk`，直接用文件管理器打开。无需电脑、解压、无障碍或 Shizuku。独立包名 `org.k40agent.minimal`，显示为“K40 Agent 最小版”，不会覆盖、关闭或卸载旧版。若旧版无障碍已启用，请自行在系统中停用；不要求解除系统的受限制设置。完整说明见 PHONE_INSTALL.md。

## 构建与验证

JDK17、Gradle8.13、AGP8.13.2、Kotlin2.2.21、Android SDK36 / Build Tools36.0.0。设置 ANDROID_HOME 或 local.properties 后：

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
python3 scripts/verify_minimal_apk.py app/build/outputs/apk/debug/app-debug.apk --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt"
```

Debug 构建使用开发签名，但显式禁用 debuggable。此版本不配置生产签名。设备测试已提供，需实际设备执行；不把 JVM 测试等同真机成功。

Actions 对 push/PR 自动构建；手动 Run workflow 勾选 publish_release，可在构建、测试、成品权限门禁及签名校验成功后发布 v0.2.0-minimal（拒绝覆盖已经发布的版本）。Release APK 是 Actions 成品，附 SHA256SUMS 和签名结果。

已退役的 v0.1.0 源码保留在 Git 历史与原 tag，以便核对审查。旧版本能力不会打包进最小版。历史本地报告位于 security/v0.1.0-local-verification，不能作为新版的测试结果。
