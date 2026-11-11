# 未来 Codex Cloud 桥接接口设计（未实现，默认关闭）

当前 APK 无 INTERNET 权限、无 listener/socket/HTTP、无 token 或可被外部调用的命令入口。以下只是一份设计，不构成可用远程功能。

## 连接与授权

手机由用户主动配对独立桥接服务，TLS1.3 双向认证并固定双方公钥；手机密钥存 Android Keystore。双方交换设备ID、随机挑战和签名，短期令牌有效期最长300秒，绑定设备、会话、接收方、scope、nonce及过期时间。拒绝过期、撤销、重复nonce和超范围操作；持久化撤销代次，断线默认拒绝。网络重试不能重放已消费动作。

## 提案数据接口

```kotlin
// Design only: not compiled into this APK.
data class ActionProposal(
 val protocolVersion: Int, val requestId: String, val sessionId: String,
 val targetPackage: String, val operation: String,
 val normalizedX: Double?, val normalizedY: Double?, val expiresAt: Long
)
interface VerifiedProposalInbox { fun receive(proposal: ActionProposal) }
interface Revocation { fun revokeSession(sessionId: String) }
```

坐标必须为有限数且在0到1之间，拒绝多余字段和超长队列。操作只允许固定枚举 tap/swipeUp/swipeDown/home/back；没有 shell、任意 Intent、文字输入、文件访问、URL启动或坐标批处理。提案不包含 confirmed/approved 标志：远端不能替用户确认。它只能进入待审提案箱，不能调用 startTask/dispatchGesture/performGlobalAction。收到提案不自动启动目标应用或会话。

手机用户必须主动启动固定目标白名单任务，再在本机核对当前屏幕、用短期一次性确认票执行；远端令牌不能扩大本机允许范围。发送、删除和支付等仍需逐次本机人工确认。撤销/停止同时清空队列、确认和连接权限。默认不向桥接服务发送屏幕内容或截图；任何未来数据出站能力需单独设计和明确本机授权。

实施前仍需独立完成证书/令牌签名验证、密钥轮换、重放持久化、时钟偏移处理、流量限制及安全评审。不要用普通布尔变量表示“已经双向认证”。
