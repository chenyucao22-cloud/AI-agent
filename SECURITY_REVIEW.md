# K40 Agent 安全审查与最小权限修复

审查日期：2026-11-11（Asia/Shanghai）。范围：已发布 v0.1.0 的实际 APK、合并 Manifest、对应源码与执行路径；修复版为 v0.2.0-minimal。不是独立第三方渗透测试，未连接红米 K40，不能确认你手机内安装包与本次 APK 完全相同，也不能据此证明不存在任何漏洞。

旧发布 APK SHA-256：`a71032f5a1cbc45784862d120bb95c0c3338ccc1095e274ffa88a38544ed6569`。二进制 Manifest 与权限原始输出见 `security/v0.1.0-apk-manifest.txt` 和 `security/v0.1.0-apk-permissions.txt`。旧版代码位置均固定到已发布提交，避免修复后行号漂移。

## 1. 全部权限与组件

原始 Manifest 自身没有 uses-permission，但依赖合并后 APK 实际存在以下内容：

| 类型 | 名称 | 实际意义 / 审查结果 |
|---|---|---|
| 声明并申请 | `org.k40agent.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX 引入，signature 级自定义权限，不是联系人/文件等敏感读取权限。最小版无接收器，已移除。 |
| 申请 | `moe.shizuku.manager.permission.API_V23` | Shizuku 依赖引入；运行代码未调用 requestPermission，但基础测试不需要它，已删除依赖与声明。 |
| Service 绑定限制 | `android.permission.BIND_ACCESSIBILITY_SERVICE` | 限制谁能绑定服务，不等于应用被授予这个签名权限。服务 exported=true，只有获系统绑定权限的调用方可绑定。最小版删除整个服务。 |
| Provider 访问限制 | `android.permission.INTERACT_ACROSS_USERS_FULL` | 保护导出的 ShizukuProvider，不是应用取得跨用户权限。最小版删除 Provider。 |
| Receiver 访问限制 | `android.permission.DUMP` | 保护 AndroidX ProfileInstallReceiver 的诊断入口，不是应用取得 DUMP 权限。最小版删除该接收器及启动 Provider。 |

旧 APK 无 INTERNET、ACCESS_NETWORK_STATE、摄像头、麦克风、联系人、短信、存储、通知、悬浮窗 SYSTEM_ALERT_WINDOW、开机自启、安装包、WRITE_SECURE_SETTINGS 或 Root 相关权限申请。无障碍覆盖层由服务能力提供，不需要 SYSTEM_ALERT_WINDOW。旧版为 debuggable=true；被授权的调试环境有额外检查能力，最小版设置 debuggable=false（仍使用开发签名）。备份两个版本均关闭。

位置：[旧 Manifest](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/AndroidManifest.xml#L1)、[旧 Gradle 依赖](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/build.gradle.kts#L11)。最小版 [Manifest](app/src/main/AndroidManifest.xml) 中 tools:node=remove 是合并期删除规则，不是申请权限。两个 runtime-only 组件删除节点局部标注 tools:ignore=MissingClass，避免 Lint 用编译类路径解析删除目标而误报；不是屏蔽成品检查，实际 APK 仍必须通过零 Provider/Receiver 的门禁。

## 2. 无障碍能力、读取与事件处理

**高优先级：能力范围大于基础验证所需，已通过移除功能消除该路径。** [旧服务配置](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/res/xml/accessibility_service.xml#L1) 开启 canRetrieveWindowContent、canPerformGestures、flagRetrieveInteractiveWindows；没有配置 packageNames。框架可向服务交付所有应用的三类窗口事件，事件对象本身可能含 UI 内容，虽然业务代码没有访问 event.text，也没有保存或上传这些字段。

[旧 AgentService:45](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L45) 先获取应用窗口 root；[85](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L85)、[149](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L149) 才检查包名和白名单。因而“仅允许的应用才接触节点”说法不够严谨：白名单外仍可能先获取 root。快照遍历读取类名、矩形、是否可点击/编辑/密码等结构，不访问 text、contentDescription、viewIdResourceName、extras；含可编辑/密码节点时丢弃结果。但不能识别没有输入框的所有验证码或付款页面，也不能当作完整敏感信息防护。

[旧事件处理:212](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L212) 非活动会话立即返回；运行中检查锁屏和前台包名、使部分确认失效。窗口为空时没有立即停止；eventType=WINDOWS_CHANGED 不保证取消所有确认。节点遍历的500节点限制也不是严格总访问上限，父循环仍可继续取子节点。以上不应依靠白名单和过滤宣称完全安全。

## 3. 点击确认竞态与自动操作

**高优先级：确认不能严格绑定目标语义。** [旧确认与点击:184](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L184) 用 path/bounds/class/window/pkg 重新比对，再重新获取 root 执行点击；同位置的控件内容可能变化，两次获取之间也存在时间窗口。100ms事件通知及包名/位置校验无法保证点击仍是用户确认的语义目标。滑动 [195](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L195) 同样依赖窗口与短时检查。最小版彻底移除跨应用点击、滑动和相应确认代码；不伪称增加一次通用确认就能修复竞态。

[旧自测:108](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/accessibility/AgentService.kt#L108) 在用户按“自测”后3秒自动 HOME。这是公开说明并由用户启动的序列，不是发现了未授权远控；但下一步没有逐次人工确认，已从最小版删除。旧版没有收到网络请求、开机广播或计时器自行启动操作的入口；服务连接本身只初始化状态。旧版停止清理回调和确认，但不能撤回已完成点击或已经交给系统的150ms手势。

## 4. 网络、收集、隐藏执行、提权

**未在审查路径发现后台联网、截图上传、密码/验证码文字读取、远程执行或提权代码。** APK 无网络权限；未找到 socket/HTTP/WebView、屏幕截图、MediaProjection、剪贴板、通知监听、文件扫描或联系人/SMS读取路径。日志只记录动作状态，内存有界；SharedPreferences 保存白名单包名（本地配置数据，仍属于保存信息，应明确说明）。未执行动态网络抓包，第三方依赖未逐行审计，故不作绝对无风险保证。

[旧 ShizukuGateway](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/shizuku/ShizukuGateway.kt#L1) 只 pingBinder/checkSelfPermission，无 shell/exec/UserService 提权调用。导出的 Provider 是官方 binder 传递集成入口，仍为不必要的攻击面，已移除。

[旧 BridgeContract](https://github.com/chenyucao22-cloud/AI-agent/blob/c5a8ed65932284d06df5955280fe9fddae5ffa0a/app/src/main/java/org/k40agent/bridge/BridgeContract.kt#L1) 是未接入执行器的接口和纯策略校验，没有网络监听、令牌签名实现或远程命令分发；布尔 mutuallyAuthenticated 不能当成真正双向认证。最小版删除这部分代码，避免未来误接入。旧版无障碍服务是系统常驻绑定服务，任务期间有可见控制条，不能把“没有隐藏任务”误说成“完全无后台组件”。

## 5. 最小版具体修复

- [Manifest](app/src/main/AndroidManifest.xml)：无无障碍服务、ShizukuProvider、接收器或初始化 Provider；合并后应为零 uses-permission、零自定义 permission。启动 Activity 是唯一导出组件，忽略所有传入 extras。
- [MainActivity:25](app/src/main/java/org/k40agent/ui/MainActivity.kt#L25)：操作前检查 RESUMED、设备解锁、屏幕可交互；遮挡触摸过滤。[54](app/src/main/java/org/k40agent/ui/MainActivity.kt#L54) 只有两个固定 Intent，必须点击按钮，设置限制到 com.android.settings；不接受任意包名、URL、脚本或命令。
- [计时:67](app/src/main/java/org/k40agent/ui/MainActivity.kt#L67)：计时仅更新本应用文字，结束不调用外部动作；onPause 和停止按钮清理回调，代次令牌拒绝旧回调。
- [LocalSession](app/src/main/java/org/k40agent/automation/LocalSession.kt)：没有 Android 或设备动作接口，无法从计时完成回调调用外部动作。
- [Gradle](app/build.gradle.kts)：移除 Shizuku 依赖、禁用 debuggable；独立包名 org.k40agent.minimal，与旧版并存，避免不同开发签名导致覆盖安装失败。旧版不会自动被停用，应由用户在系统中停用或卸载。
- [APK 静态门禁](scripts/verify_minimal_apk.py)：本地与 CI 对成品 APK 检查权限、组件、debug/backup标志、包名以及是否残留已移除模块的 DEX 类；不只是检查源码 Manifest。

## 6. 关于“受限制的设置”及验证边界

Android 13 对部分侧载来源开启无障碍服务的保护提示本身不能证明 APK 恶意，也不能通过本次审查保证旧版应被放行。本次不使用 adb、Shizuku、Root、写安全设置或其他办法解除限制。最小版根本不注册无障碍服务，不需启用这项权限；这是减少功能与权限，不是绕过安全检查。

基础验证只覆盖本应用按钮打开设置、返回桌面、本地计时取消。不能替代跨应用节点读取、点击、滑动的验证；这些能力已暂停交付。未连接目标手机，未执行设备端测试或真实业务操作。构建、自动测试及 APK 实测结果见 BUILD_REPORT.md 和 Actions 记录。
