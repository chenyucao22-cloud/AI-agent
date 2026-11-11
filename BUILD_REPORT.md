# v0.2.0-minimal 验证结果

2026-11-11（Asia/Shanghai）。本地 `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` 成功。JVM测试 5 项通过，Lint 0错误、4项版本更新提示警告。设备测试APK仅编译，未在设备执行。

成品 APK 检查：零申请权限、零自定义权限、零 Service/Receiver/Provider、无已移除的业务模块/ Shizuku 类；单一 Launcher Activity；禁用调试与备份。旧 v0.1.0 APK 被同一门禁拒绝，证明检查并非固定通过。

本地 APK 大小：20766155 字节；SHA-256：`ace1902e72fa1bb77c9e3a4a5c65d3873c2a96fdb87899162bc709b5a709af42`。这是工作区构建的校验值，**不能用于 GitHub Actions 重新签名后的发布 APK**；发布件校验值以该 Release 的 SHA256SUMS.txt 为准。

日志、测试XML、成品Manifest见 security/v0.2.0-local-verification。旧报告已移入 security/v0.1.0-local-verification。

GitHub Actions 将独立运行相同构建/检查，再验证 APK 签名后发布。实际云端状态以 Actions 记录为准。红米 K40 真机验证尚未执行；请按照 PHONE_INSTALL.md 测试普通导航和计时取消，勿开启旧版受限权限。
