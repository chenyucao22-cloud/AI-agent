K40 Agent 0.3.0 受控自动化版

手机直接下载 **K40-Agent-controlled.apk**。应用名“K40 Agent 受控版”，独立包名 org.k40agent.controlled，与最小版并存。

- 使用 Android 官方无障碍服务，必须由用户在系统设置中手动开启。
- Android 13 / MIUI 可能阻止侧载应用启用无障碍；本版不解除或绕过“受限制的设置”。若系统拒绝，自动操作不可用。
- 固定目标、60秒会话、持续可见停止条。默认只允许本应用/Android设置，新增应用必须本机确认。
- 选点、上滑、下滑、返回、桌面均需5秒内按住1.2秒确认，只执行一次；重要操作没有免确认路径。
- 不遍历节点或读取 UI 文字，不截图、不联网、不启用远控；只核验窗口包名及几何信息。键盘或安全窗口出现时停止。
- 确认与手势存在 Android 无法原子化的剩余竞态；不支持无人值守。已提交150ms手势无法保证撤回。
- 最小版独立保留在 minimal-permissions 分支和 v0.2.0-minimal Release。
- Actions 通过测试、成品权限检查和签名验证后发布。仍使用开发签名但禁用调试。尚未完成 K40 真机验收。

审查：SECURITY_REVIEW_CONTROLLED.md；手机验证：PHONE_INSTALL.md；未来接口设计：docs/BRIDGE_PROTOCOL.md。
