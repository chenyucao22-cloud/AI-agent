K40 Agent 0.2.0 最小权限版

直接下载 Assets 中 **K40-Agent-minimal.apk**。无需电脑、解压、无障碍或 Shizuku。

安全审查与修复详情见仓库 SECURITY_REVIEW.md，包含旧版真实 APK 权限清单和固定源码位置。

- 成品零权限申请，无 AccessibilityService、ShizukuProvider、接收器或初始化 Provider。
- 不读取其他应用节点/文字，不截图，不联网、不远程执行，无跨应用点击/滑动。
- 仅本机用户按钮触发打开设置或回桌面；停止测试为应用内10秒计时，离开前台会取消。
- debuggable=false，禁用备份；仍使用开发签名。
- 独立包名 org.k40agent.minimal，可与旧版并存；请自行在系统中停用旧版无障碍，最小版不会替你修改权限。
- 未绕过 Android 受限制设置。最小版没有需要启用的无障碍服务，也不等同于完整自动化版本。
- GitHub Actions 构建、测试、成品权限门禁及签名校验成功后发布；尚未完成 K40 真机验证。

附 SHA256SUMS.txt 与 apk-signature.txt。
