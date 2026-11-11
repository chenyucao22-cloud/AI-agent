# K40 Agent 0.3.0 受控自动化版

Kotlin + Compose + Android 官方 AccessibilityService；Android 13及以上。MIT 开源。当前分支为 `controlled-automation`。**最小权限版独立保存在 `minimal-permissions` 分支及 v0.2.0-minimal tag，未覆盖。**

手机直接从本仓库 Releases 下载 **v0.3.0-controlled / K40-Agent-controlled.apk**。应用名“K40 Agent 受控版”，包名 `org.k40agent.controlled`，与旧版和最小版并存。

## 受控操作

1. 在系统中手动授权“K40 Agent 受控版”无障碍服务。未授权不能运行。
2. 默认白名单仅本应用和 `com.android.settings`。其他应用通过“选择可启动应用加入白名单”逐个人工确认添加；添加/移除都会停止旧任务。
3. 点目标应用“启动任务”并确认。会话固定一个目标，最多60秒。控制条持续显示目标和剩余时间，红色停止按钮始终可用。启动仅打开目标，不自动点击。
4. 点击“选点”，在实际屏幕上触碰目标，选点层会截获此触碰，不会立即点击目标。出现红圈后，核对实际界面及后果，5秒内按住确认按钮至少1.2秒再松开，仅执行一次。
5. 上滑、下滑、返回、桌面也需要同样的本机确认。导航执行后结束任务。控制条可上下移动，避免遮住目标；选择层也有停止按钮。
6. 窗口内容变化、窗口身份/边界/旋转变化、确认到期、锁屏、键盘/系统活动窗口出现、离开目标或服务中断时撤销确认或停止任务。

没有消息发送、付款、删除文件、文字输入的业务接口。**所有点击和滑动都视作可能重要的操作，逐次人工确认。** 不可靠的业务风险识别不用于跳过确认。发送/删除/付款的最终后果需用户核对实际屏幕；不使用真实业务做首次测试。

## Android 13 的限制

Android 13 对部分侧载来源的应用开启无障碍等敏感能力实施“受限制的设置”保护；MIUI 可能有额外审核和菜单差异。这不等于系统已认定应用恶意，也不等于审查通过后系统必须允许授权。本应用只打开官方无障碍设置页，不自动授权，不使用 adb、Root、Shizuku 或安全设置写入去解除限制。**如果系统仍然拒绝授权，请保留限制；此版本不能绕过它，也不能保证你当前安装来源一定能获准启用。** 最小版权限测试仍可独立使用。

## 数据与权限边界

成品没有 INTERNET 或其他 uses-permission、没有 Shizuku、截图或远程通信。唯一服务由 BIND_ACCESSIBILITY_SERVICE 保护。需要窗口内容能力是因为官方窗口 API 的前台核验依赖它；代码只查看活动应用窗口 root.packageName，以及窗口 ID/边界/旋转，**不访问 text/contentDescription/event.text/event.source，不遍历子节点**。键盘出现停止，避免操作输入区域。框架仍可能交付含文字的事件对象；应用不访问这些字段，不声称系统完全不向服务提供数据。

只在活动任务期间处理窗口事件。监听窗口事件不按白名单过滤，是为了发现切到非目标应用；操作始终受固定目标和白名单约束。名单在本机 SharedPreferences 保存，日志最多50条且仅内存。没有屏幕内容上传、后台自启任务或执行远程命令的接口。调试与备份均禁用（构建仍用开发签名）。

停止清理待确认动作和应用回调，但无法撤回已完成操作，已经交给 Android 的150ms手势仍可能完成。Android 没有把视觉确认与手势执行原子绑定的接口；最后瞬间变化的竞态无法彻底消除。动态页面频繁改变会使确认失败，这是保守行为，不是跳过确认的理由。本版是有人监督的单步执行器，不是无人值守脚本引擎。

未来接口设计见 `docs/BRIDGE_PROTOCOL.md`（文档，未编译入 APK）。审查结果见 `SECURITY_REVIEW_CONTROLLED.md`；最小版历史审查保留在 `SECURITY_REVIEW.md`。

## 构建与测试

JDK17、Gradle8.13、AGP8.13.2、Kotlin2.2.21、SDK36 / Build Tools36.0.0。配置 ANDROID_HOME 或 local.properties 后：

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
python3 scripts/verify_controlled_apk.py app/build/outputs/apk/debug/app-debug.apk --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt"
```

Actions 自动构建；手动触发时选择 `controlled-automation` 分支并勾选 publish_release，可在构建、测试、成品权限检查和签名验证后发布 v0.3.0-controlled。已发布版本拒绝覆盖。

JVM测试验证确认一次性、持按时间、失效与重放、窗口变化、超时、停止重启、越界、受限系统包；设备清单测试仅在实际设备运行时才算通过。真机步骤见 `PHONE_INSTALL.md`。不把静态检查和 JVM 通过当作 K40 无障碍实际可用的证明。
