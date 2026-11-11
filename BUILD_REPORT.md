# v0.3.0-controlled 构建与审查结果

2026-11-11（Asia/Shanghai）。本地 `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` 成功。JVM测试 9 项通过；Lint 0错误、12项警告（依赖更新、绘制分配、字符串资源化、KTX与自定义触摸控件可访问性提示）。确认控件有意要求本机持按，不通过普通无障碍 click 自动确认。

成品检查通过：无 uses-permission / INTERNET，唯一无障碍服务由 BIND_ACCESSIBILITY_SERVICE 保护，无 Receiver/Provider；禁用调试/备份，开启手势/窗口核验但不声明截图/按键过滤；无旧版 AgentService、Shizuku 或执行型桥接模块。资源文件可能经构建优化改名，门禁按 APK 资源表解析服务配置。

本地 APK 20816735 字节，SHA-256：`f5f45cc99d58ce0dc437d5f50467fb52850c8ee489949537339f55e7d4984cff`。这是本地件的校验值，不能用于 CI 重新签名的发布件；Release 以随附 SHA256SUMS.txt 为准。

独立最小版分支 minimal-permissions 仍为 e2795987db1427db81d1f4613d6a9c65b9af9118。受控版位于 controlled-automation。审查见 SECURITY_REVIEW_CONTROLLED.md，未来接口设计仅为文档。

未连接 K40。Android 设备测试只编译，未执行；没有声称窗口/触摸/MIUI 授权已真机通过，没有执行真实发送、删除或付款。用户应先按 PHONE_INSTALL.md 在本应用和 Android 设置验收。Android 13 的受限制设置不会因构建通过而自动解除。
