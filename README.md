# K40 Agent

开源（MIT）Android 本地自动化执行端，面向 Redmi K40 / Android 13，无需 Root、解锁 Bootloader 或 Shizuku 权限。**当前没有远程控制能力，不假设 Codex Cloud 能连接手机。**

## 架构

- `ui/MainActivity`：Compose 本机授权引导、白名单、任务状态与内存日志。
- `automation/Policy`：默认拒绝、用户启动会话、锁屏检查、确认与撤销代次；`DemoSequence`：可测试的设备步骤接口。
- `accessibility/AgentService`：系统授权服务，可见控制条、最长120秒会话、白名单检查、结构读取、点击、短滑动、返回和桌面。
- `shizuku/ShizukuGateway`：可替换状态接口，官方 API binder 检测，不申请授权或执行 shell。
- `bridge/BridgeContract`：未来通信的认证/授权边界，未连接执行器，无端口、无网络权限。

## 构建

固定工具链：JDK 17、Gradle 8.13、AGP 8.13.2、Kotlin 2.2.21、Compose BOM 2025.10.01、minSdk 33、compile/targetSdk 36（稳定 API）。Android Studio 使用支持 AGP 8.13 的稳定版本。

1. 安装 Android Studio 和 Android SDK Command-line Tools。
2. 在 SDK Manager 安装 Android SDK Platform 36、Build Tools 36.0.0、Platform Tools。
3. 接受官方 SDK 许可证；设置 `ANDROID_HOME`，或在工程根目录创建 `local.properties`：`sdk.dir=/你的/Android/Sdk`。
4. 执行：

```sh
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon testDebugUnitTest lintDebug
```

Windows 使用 `gradlew.bat`。APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。Debug 使用自动生成的开发签名；不要当作生产签名发布。不同机器构建的 debug 签名可能不同，不能直接覆盖安装。

GitHub Actions `.github/workflows/android.yml` 自动安装 SDK，构建 APK、运行单元测试与 lint，并上传 APK/报告。工作流已配置，是否实际运行以 GitHub Actions 记录为准。

## 手机直接下载

仅使用手机的完整步骤见 `PHONE_INSTALL.md`。优先在本仓库 Releases 的 Assets 中直接下载 APK。Actions 成功后在运行摘要或 Artifacts 下载 `K40-Agent-debug`，解压 ZIP 后安装 APK；下载保留30天。

## 安装与使用

```sh
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

也可将 APK 复制到手机，由系统安装器手动安装并按系统提示授权安装来源。USB 调试不是基础功能的运行条件。

打开 K40 Agent → 打开系统无障碍设置 → 手动启用服务。Android 13 的侧载限制可能要求在应用信息菜单中手动允许受限设置；MIUI 的设置路径以实机为准。不通过 adb 或 Shizuku 自动修改安全设置。

默认白名单为 `org.k40agent` 和 `com.android.settings`。添加包名需本机确认；更改白名单停止旧任务。重新打开应用后点击“刷新状态 / 白名单”读取已保存配置。

启动本地操作后，顶部出现持续可见控制条：停止、桌面、返回、读取结构、滑动。打开设置按钮同时启动任务。桌面动作结束会话；进入未允许应用、锁屏、服务断开或120秒超时会停止。没有后台自启任务、通知监听、文字输入、shell 执行或定时远程任务。

读取结构只返回类名、点击能力和屏幕位置，不访问节点文字、描述、资源 ID、事件文字、剪贴板或截图。遇到密码/可编辑节点，或树过大，整页拒绝结构结果和点击/滑动。可操作节点首版只展示前三个；不是通用脚本编辑器。白名单外窗口不遍历节点。

每次点击和滑动都须在本机控制条明确确认；发送消息、删除、付款没有专用自动化接口。确认展示包名和坐标，用户应自行核对实际屏幕。**不能通过节点结构可靠判断所有业务风险**，因此所有点击均按需人工确认处理，不宣称有完整敏感页面识别能力。付款/身份验证/密码管理器应保持在白名单外。不绕过锁屏、系统权限或安全验证。

停止撤销队列、会话代次和待确认动作，但不能撤销已完成的点击，也无法保证中断已经交给系统的150毫秒手势。日志仅为内存中最多80条的操作结果，不保存 UI 内容，进程退出清空。无 INTERNET 权限，无上传截图或 UI 文本功能。

## 测试和真机验收

JVM 测试覆盖默认拒绝、设置/桌面动作策略、锁屏、白名单、点击确认、停止撤销、设备序列、令牌有效期、双向认证前提、范围限制、撤销和重放。测试使用假设备端口，**不等于无障碍真机验证**。

`ManifestTest` 为设备端测试，验证最终安装包无 INTERNET 权限且禁用备份：

```sh
./gradlew connectedDebugAndroidTest
```

红米 K40 上手动验收（不发送消息）：

1. 不启用无障碍：启动按钮禁用，系统授权引导可用。
2. 启用后点击“启动并打开 Android 设置”：设置打开，顶部停止条可见。
3. 点击控制条“桌面”：返回启动器，任务结束，控制条消失。
4. 点击“自测”：打开设置，3秒后回桌面；重复一次并在3秒内点“停止”，之后不应自动回桌面。
5. 在设置中“读取结构”，核对没有 UI 文字；只在无风险页面确认点击/滑动，点“返回”验证上一页。
6. 点击/滑动确认期间取消或锁屏；不应执行待确认动作。切到白名单外应用应停止。
7. 关闭 Shizuku，重新测试打开设置/桌面/停止，基础功能仍可用。
8. Android 设置若含可编辑节点会拒绝结构读取；这属于保守限制。顶部控制条可能遮挡内容；首版不点击控制条下的屏幕坐标，而使用节点 ACTION_CLICK。

真实消息、文件删除和付款不在测试范围。没有连接设备时，不声称执行上述真机步骤。

## 未来桥接约束

此处仅设计接口，不是可投入使用的认证实现。生产桥接须独立完成安全评审：TLS1.3 双向证书验证及公钥绑定，本机显示配对身份与授权范围；签名短期令牌（最长300秒），绑定会话、设备、接收方、nonce、动作和应用范围；原子拒绝重放；本机撤销立即关闭连接并取消操作；到期或断线默认拒绝。密钥放 Android Keystore，不在日志中记录令牌。网络请求不能构造“用户已确认”标志，不能直接调用执行端；每个跨应用任务仍须本机用户主动启动，高风险操作须本机逐次确认。当前 `BridgeGate` 仅检查已验证上下文，不验证密码学证书或令牌签名，且未接入 AccessibilityService。

实际构建结果见 `BUILD_REPORT.md`。
