package org.k40agent.ui

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import org.k40agent.automation.*

class MainActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val session = LocalSession()
    private fun available() = UserActionGate.permits(
        lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked,
        getSystemService(PowerManager::class.java).isInteractive
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        // Incoming intents/extras never execute commands or resume a task.
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("K40 Agent 最小版", style = MaterialTheme.typography.headlineMedium)
                Text("0.2.0 · 无需无障碍或 Shizuku", style = MaterialTheme.typography.titleMedium)
                Text("本版仅验证打开设置、返回桌面和停止本地任务。没有无障碍服务，不读取其他应用，不点击或滑动其他应用，也没有远程控制。")
                Text("状态：${AgentState.status}")
                Button(onClick = { launch(LocalAction.OPEN_SETTINGS) }) { Text("打开 Android 设置（单次）") }
                Button(onClick = { launch(LocalAction.HOME) }) { Text("返回桌面（单次）") }
                HorizontalDivider()
                Text("停止功能测试：启动10秒应用内计时，然后点立即停止。计时结束只更新状态，不执行任何外部操作。离开应用或熄屏会取消计时。")
                Button(onClick = { startTimer() }) { Text("启动本地计时测试") }
                Text("剩余 ${AgentState.remaining} 秒")
                Button(onClick = { stopTimer("已停止；排队回调已撤销") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("立即停止") }
                Text("无需开启系统受限制的设置。如旧版仍已启用无障碍，请在系统中自行停用旧版；最小版不会更改系统权限。")
                HorizontalDivider()
                Text("操作日志（仅内存，最多40条）")
                AgentState.logs.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        } } }
    }
    private fun launch(action: LocalAction) {
        if (!available()) { stopTimer("拒绝操作：应用不在前台或设备未解锁"); return }
        stopTimer("本地计时已取消")
        val intent = when (action) {
            LocalAction.OPEN_SETTINGS -> Intent(Settings.ACTION_SETTINGS).setPackage("com.android.settings")
            LocalAction.HOME -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        }
        try {
            startActivity(intent)
            AgentState.log(if (action == LocalAction.OPEN_SETTINGS) "已请求打开系统设置" else "已请求返回桌面")
        } catch (_: ActivityNotFoundException) { AgentState.log("系统未提供可用的目标页面") }
          catch (_: SecurityException) { AgentState.log("系统拒绝启动目标页面") }
    }
    private fun startTimer() {
        if (!available()) return
        stopTimer("开始本地计时")
        val token = session.start()
        AgentState.remaining = session.remaining
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!available()) { stopTimer("计时已取消：应用离开前台或设备锁定"); return }
                if (!session.tick(token)) return
                AgentState.remaining = session.remaining
                if (session.active) handler.postDelayed(this, 1000)
                else AgentState.log("计时结束；未执行外部操作")
            }
        }, 1000)
    }
    private fun stopTimer(message: String) {
        session.stop(); handler.removeCallbacksAndMessages(null)
        AgentState.remaining = 0; AgentState.log(message)
    }
    override fun onPause() {
        if (session.active) stopTimer("应用离开前台；计时已停止")
        super.onPause()
    }
    override fun onDestroy() {
        session.stop(); handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
