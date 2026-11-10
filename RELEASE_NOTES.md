K40 Agent 0.1.0 — 本地功能预览版（Debug APK）

手机浏览器直接点击 **Assets → K40-Agent-debug.apk** 下载，用文件管理器打开后按系统提示安装，无需电脑或解压。

- APK 来自此版本源码的 GitHub Actions 成功构建，已运行单元测试与 Lint，并通过 apksigner 签名校验。
- Android 13及以上，minSdk33、targetSdk36。
- 默认白名单只有本应用和 Android 设置，所有任务由本机用户主动启动。
- 没有远程控制、网络上传、UI文字读取或截图采集功能；Shizuku 为可选状态检测。
- 安装后手动启用系统无障碍服务，先验证打开设置、返回桌面、停止任务。
- 尚未完成红米 K40 真机验收，不用消息发送、付款或删除操作进行首次测试。
- Debug 开发签名用于预览。GitHub 构建使用的开发签名与早前云工作区 APK 可能不同；首次安装不受影响。若旧版签名不同，覆盖安装会被 Android 拒绝。

校验信息：SHA256SUMS.txt、apk-signature.txt。完整源码可在本仓库查看。
