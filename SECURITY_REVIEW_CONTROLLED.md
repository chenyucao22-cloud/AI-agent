# v0.3.0 受控版设计审查

日期：2026-11-11。静态审查、JVM测试和成品 APK 检查；未连接 K40，未做动态抓包、界面测试或第三方独立渗透测试。

## 与旧版和最小版的关系

最小版固定为 `minimal-permissions` 分支，起点 e2795987db1427db81d1f4613d6a9c65b9af9118，仍保留 v0.2.0-minimal Release。本版在独立 controlled-automation 分支开发，不改动最小分支。旧版节点路径点击、Shizuku 和可调用桥接模块均未恢复。

## 权限和能力

Manifest 无 uses-permission（包括 INTERNET）、无 receivers/providers，仅 Launcher Activity 与受 BIND_ACCESSIBILITY_SERVICE 保护的无障碍服务。该 permission 是组件绑定门槛，不是应用自行取得的签名权限。禁用调试和备份，无 Shizuku 或 Root 调用。AndroidX自动合并组件通过删除规则移除；最终产物必须通过 scripts/verify_controlled_apk.py，而不只检查源码。

`controlled_service.xml` 开启窗口内容和手势能力，未开启截图、按键过滤或触摸探索。窗口内容能力在系统层仍然较强，这是官方窗口 API 的限制，不能声称系统只授予了读取包名。`ControlledService.readScreen()` 只读当前聚焦应用窗口（控制层触摸可能改变 active 标记，因此不要求应用窗口同时 isActive）的 root.packageName 与窗口ID/边界/旋转；不遍历节点，不访问 UI 文本、描述、event.text/event.source、输入内容或截图。事件对象可能包含文字，但程序不访问这些字段。任务结束后事件处理立即返回。

未用 XML packageNames 过滤事件，因为需要发现切到非白名单应用；它不等同于可以操作所有应用。实际动作只允许固定目标、持续白名单、解锁/亮屏、确认窗口身份一致且控制条可见。只检查包名时可能先取得白名单外窗口根节点对象的包名元数据，随后拒绝；不承诺完全不接触白名单外元数据。

## 确认与撤销

`SessionGuard` 将一次性票据绑定到 generation/revision/serial、完整 Screen、操作、坐标和签发时刻，最长5秒，要求至少1200ms本机持按；成功或失败均消费票据。禁止重复消费、旧会话、过期、倒退时间、越界和不同窗口。UI 的确认按钮拒绝遮挡触摸、移出和多点触摸，取消手势立即撤销。

`ControlledService` 的执行路径只能从确认按钮进入。任务只能由 MainActivity 的本机启动对话框触发，固定目标60秒。所有点击/滑动/返回/桌面都确认，不靠不可靠的语义分类放行重要操作。启动会话仅根据用户明确选择打开目标应用，无延迟自动导航或批处理。

活动窗口内容/状态事件撤销确认，窗口边界/旋转/身份变化撤销；键盘或系统活动窗口、目标离开、锁屏、停止、超时、服务中断会停止或拒绝。250ms可见性/状态复核为补充，不取代事件检查。所有系统动作提交前再次核验。回调使用 generation，旧手势完成回调不会复活停止的会话。

**剩余限制：** Android 事件异步，确认和 dispatchGesture 不是原子事务，仍存在最后一刻界面变化的竞态。不能保证点击语义永远不变，也不能可靠识别支付、验证码等所有页面。因此不读取这些文字，所有动作一律人工确认，不支持无人值守操作，建议不将认证、密码管理器或支付应用加入名单。禁止系统UI/权限控制器/安装器及MIUI安全中心作为新增目标。没有自动发送消息、删除或付款接口，但用户确认的普通点击仍可能产生这些业务后果，需要用户核对。

## 可见执行与停止

服务只在用户启动的任务中创建控制条；一直显示目标、倒计时和停止按钮。选点覆盖层也有停止按钮，首次触碰只选点、不透传给应用；红圈提示位置。控制条可上下移动，覆盖区域拒绝点击。停止清理计时、确认和覆盖层。已提交的150ms手势和已经完成的点击不能保证撤回。系统可在授权后绑定无障碍服务；“没有隐藏任务”不代表“进程从不在后台存活”。

## 数据与未来通信

白名单包名只保存在本地 SharedPreferences；应用列表只在用户点击添加时枚举可启动应用，不申请 QUERY_ALL_PACKAGES。日志只含动作状态，最多50条，内存保存。无读取屏幕文字、截图、网络、shell、动态代码或外部命令解析路径。启动 Activity 忽略外部 extras，不接受远端任务。

Codex Cloud 接口仅在 docs/BRIDGE_PROTOCOL.md 设计，未编译、未运行，不能经网络启动任务。设计要求双向认证、短期范围令牌、一次性nonce、撤销和本地不可伪造确认。只有独立实现审查后才能启用未来通信。

## 验证

JVM测试覆盖一次性确认、短按拒绝、变更撤销、窗口/旋转身份、超时、停止重启、越界及系统包限制。Manifest设备测试仅编译，不声称真机执行。成品门禁核对无网络/其他权限、唯一受保护服务、禁用debug/backup、没有截图/按键过滤能力和退役模块。完整构建结果见 BUILD_REPORT.md；K40验收步骤见 PHONE_INSTALL.md。

代码入口：[前台窗口核验](app/src/main/java/org/k40agent/accessibility/ControlledService.kt#L54)、[一次性确认策略](app/src/main/java/org/k40agent/automation/SessionGuard.kt#L6)、[启动与名单界面](app/src/main/java/org/k40agent/ui/MainActivity.kt#L18)、[成品门禁](scripts/verify_controlled_apk.py)。
