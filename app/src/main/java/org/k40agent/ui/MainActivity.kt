package org.k40agent.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.k40agent.accessibility.AgentService
import org.k40agent.automation.AgentState
import org.k40agent.shizuku.LocalShizukuGateway

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { MaterialTheme {
   var shizuku by remember { mutableStateOf(LocalShizukuGateway().status()) }
   var packageInput by remember { mutableStateOf("") }
   var whitelist by remember { mutableStateOf(listOf("org.k40agent", "com.android.settings")) }
   var confirmPackage by remember { mutableStateOf<String?>(null) }
   Surface(Modifier.fillMaxSize()) {
    Column(Modifier.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
     Text("K40 Agent", style = MaterialTheme.typography.headlineLarge)
     Text("本地执行端 · 远程控制关闭", style = MaterialTheme.typography.titleMedium)
     Text("仅在你启动任务后操作白名单应用。读取范围仅为节点类型与位置，不读取文字、密码、验证码或支付信息，不采集截图。点击与滑动必须本机确认。锁屏或切换到白名单外应用会停止任务。")
     Text("无障碍：${if (AgentState.connected) "已连接" else "未连接"} ｜ ${AgentState.status}")
     Button(onClick = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("打开系统无障碍设置") }
     Text("在系统设置中找到 K40 Agent，阅读说明后自行启用。Android 13 侧载应用可能需在应用信息菜单中手动允许受限设置；本应用不会代为授权。MIUI 菜单名称可能不同。")
     Button(enabled = AgentState.connected, onClick = { AgentService.instance?.startLocal() }) { Text("启动本地操作（120秒）") }
     Button(enabled = AgentState.connected, onClick = { AgentService.instance?.startLocal(); AgentService.instance?.openSettings() }) { Text("启动并打开 Android 设置") }
     Button(enabled = AgentState.connected, onClick = { AgentService.instance?.demo() }) { Text("自测：打开设置 → 3秒后返回桌面") }
     Button(onClick = { AgentService.instance?.stop() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("立即停止") }
     Text("停止会撤销排队操作及确认；已完成的点击无法撤回，已提交的短滑动最长仍可能持续150ms。屏幕顶部控制条也可立即停止。")
     HorizontalDivider()
     Text("Shizuku：$shizuku")
     OutlinedButton(onClick = { shizuku = LocalShizukuGateway().status(); whitelist = AgentService.instance?.policy?.allowed?.toList() ?: whitelist }) { Text("刷新状态 / 白名单") }
     Text("Shizuku 仅检测，不申请权限或执行命令。未运行也可使用基础功能。")
     Text("应用白名单（默认：本应用、Android 设置）")
     whitelist.forEach { pkg ->
      Row { Text(pkg, Modifier.weight(1f)); if (pkg !in listOf("org.k40agent", "com.android.settings")) TextButton(onClick = {
       AgentService.instance?.setAllowed(pkg, false); whitelist = AgentService.instance?.policy?.allowed?.toList() ?: whitelist
      }) { Text("移除") } }
     }
     OutlinedTextField(value = packageInput, onValueChange = { packageInput = it }, label = { Text("应用包名") }, singleLine = true)
     OutlinedButton(enabled = AgentState.connected && packageInput.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+")), onClick = { confirmPackage = packageInput }) { Text("添加到白名单…") }
     Text("首版只展示最多3个可点击节点；含可编辑或密码节点的页面拒绝读取与操作。请勿将密码管理器、支付或身份验证应用加入白名单。")
     HorizontalDivider(); Text("操作日志（仅内存，最多80条，无 UI 内容）")
     AgentState.logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
   }
   confirmPackage?.let { pkg -> AlertDialog(onDismissRequest = { confirmPackage = null }, title = { Text("允许操作 $pkg？") }, text = { Text("启用后仍须主动启动任务并逐次确认点击和滑动。更改白名单会停止当前任务。") }, confirmButton = { TextButton(onClick = {
    AgentService.instance?.setAllowed(pkg, true); whitelist = AgentService.instance?.policy?.allowed?.toList() ?: whitelist; confirmPackage = null; packageInput = ""
   }) { Text("允许") } }, dismissButton = { TextButton(onClick = { confirmPackage = null }) { Text("取消") } }) }
  } }
 }
}
