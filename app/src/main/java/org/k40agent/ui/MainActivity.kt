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
import androidx.lifecycle.Lifecycle
import org.k40agent.accessibility.ControlledService
import org.k40agent.automation.*

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.decorView.filterTouchesWhenObscured = true
  setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
   var task by remember { mutableStateOf<String?>(null) }
   var adding by remember { mutableStateOf<Pair<String,String>?>(null) }
   var apps by remember { mutableStateOf(listOf<Pair<String,String>>()) }
   Column(Modifier.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("K40 Agent 受控版",style=MaterialTheme.typography.headlineMedium)
    Text("0.3.0 · 本机逐次确认 · 无远程控制")
    Text("状态：${if(AgentState.connected) "无障碍已连接" else "无障碍未连接"} · ${AgentState.status}")
    Text("任务最长60秒，固定一个白名单应用。选点后按住1.2秒确认，点击/滑动/返回/桌面均需逐次确认。发送、删除、付款等重要动作请核对实际屏幕。界面变化会撤销确认。")
    Button(onClick={ ControlledService.instance?.stop(); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("打开系统无障碍设置") }
    Text("Android 13 对部分侧载来源限制开启无障碍。必须由你在系统中手动授权；如果系统仍阻止，请保留限制。本应用不使用 adb、Shizuku、Root 或写系统设置解除保护。未授权就不能自动操作。")
    Button(onClick={ ControlledService.instance?.stop() },colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)) { Text("立即停止") }
    HorizontalDivider(); Text("任务目标 / 白名单")
    AgentState.allowed.sorted().forEach { pkg ->
     Text(pkg)
     Row {
      Button(enabled=AgentState.connected,onClick={ task=pkg }) { Text("启动此应用任务…") }
      if(pkg !in setOf(packageName,"com.android.settings")) TextButton(onClick={ ControlledService.instance?.changeAllowed(pkg,false) }) { Text("移除") }
     }
    }
    OutlinedButton(enabled=AgentState.connected,onClick={
     ControlledService.instance?.stop("选择白名单：任务停止")
     val query=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
     apps=packageManager.queryIntentActivities(query,0).map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }.distinctBy { it.first }.filter { it.first !in AgentState.allowed && PackagePolicy.canAdd(it.first) }.sortedBy { it.second }
    }) { Text("选择可启动应用加入白名单") }
    apps.forEach { app -> TextButton(onClick={adding=app}) { Text("添加 ${app.second} (${app.first})") } }
    Text("仅读取窗口包名、ID、边界和旋转状态；不遍历节点，不读取文字、密码、验证码或支付信息，不截图、不联网。键盘或安全窗口出现会停止。Android 没有原子化确认执行接口，无法完全消除最后瞬间的界面变化；本版不是无人值守业务自动化。")
    Text("停止撤销未提交动作；已交给系统的150ms手势和已完成的操作不能保证撤回。")
    HorizontalDivider(); Text("本机内存日志（50条，无屏幕内容）")
    AgentState.logs.forEach { Text(it,style=MaterialTheme.typography.bodySmall) }
   }
   task?.let { pkg -> AlertDialog(onDismissRequest={task=null},title={Text("启动60秒任务？")},text={Text("目标：$pkg。将打开此应用并显示控制条；每次操作仍须本机按住确认。")},confirmButton={TextButton(onClick={
    if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) ControlledService.instance?.startTask(pkg)
    task=null
   }){Text("我主动启动")}},dismissButton={TextButton(onClick={task=null}){Text("取消")}}) }
   adding?.let { app -> AlertDialog(onDismissRequest={adding=null},title={Text("允许 ${app.second}？")},text={Text("包名：${app.first}。只允许你启动后的逐次确认操作。不要加入密码管理器、认证或支付应用。")},confirmButton={TextButton(onClick={ControlledService.instance?.changeAllowed(app.first,true); apps=emptyList(); adding=null}){Text("确认加入")}},dismissButton={TextButton(onClick={adding=null}){Text("取消")}}) }
  } } }
 }
}
