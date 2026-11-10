package org.k40agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.k40agent.automation.*

class AgentService : AccessibilityService() {
 companion object {
  private var current = java.lang.ref.WeakReference<AgentService>(null)
  val instance: AgentService? get() = current.get()
 }
 private val handler = Handler(Looper.getMainLooper())
 val policy = Policy("org.k40agent")
 private var panel: LinearLayout? = null
 private var title: TextView? = null
 private var controls: LinearLayout? = null
 private var confirmation: (() -> Unit)? = null
 private var deadline = 0L
 private var demoSequence: DemoSequence? = null
 private val screenOff = object : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == Intent.ACTION_SCREEN_OFF) stop() }
 }
 private var receiverRegistered = false
 private val manager get() = getSystemService(WindowManager::class.java)
 private val unlocked get() = !getSystemService(KeyguardManager::class.java).isKeyguardLocked
 private fun root(): AccessibilityNodeInfo? = windows.firstOrNull {
  it.type == AccessibilityWindowInfo.TYPE_APPLICATION && (it.isActive || it.isFocused)
 }?.root
 override fun onServiceConnected() {
  current = java.lang.ref.WeakReference(this); AgentState.connected = true
  if (!receiverRegistered) {
   registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
   receiverRegistered = true
  }
  val saved = getSharedPreferences("policy", MODE_PRIVATE).getStringSet("allowed", null)
  saved?.let { policy.allowed.addAll(it) }
  AgentState.log("无障碍已连接；任务未启动")
 }
 fun setAllowed(packageName: String, allow: Boolean) {
  stop()
  if (packageName == packageName() || packageName == "com.android.settings") return
  if (allow) policy.allowed.add(packageName) else policy.allowed.remove(packageName)
  getSharedPreferences("policy", MODE_PRIVATE).edit().putStringSet("allowed", policy.allowed.toSet()).apply()
  AgentState.log("白名单已更新；旧任务已撤销")
 }
 private fun packageName() = "org.k40agent"
 fun startLocal() {
  stop(false)
  if (!unlocked || root()?.packageName?.toString() != packageName()) {
   AgentState.log("请在解锁后的 K40 Agent 内启动"); return
  }
  policy.start(); AgentState.active = true
  deadline = android.os.SystemClock.elapsedRealtime() + 120_000
  try { showPanel() } catch (_: Exception) { stop(); AgentState.log("控制条不可用，任务已停止"); return }
  AgentState.log("本地任务运行中（120 秒自动结束）")
  handler.postDelayed({ stop() }, 120_000)
 }
 fun stop(log: Boolean = true) {
  demoSequence?.stop(); demoSequence = null
  policy.stop(); AgentState.active = false; confirmation = null
  handler.removeCallbacksAndMessages(null)
  panel?.let { runCatching { manager.removeView(it) } }
  panel = null; controls = null; title = null
  if (log) AgentState.log("已停止；待执行操作已撤销")
 }
 private fun permitted(action: Action, confirmed: Boolean = false): Boolean {
  if (android.os.SystemClock.elapsedRealtime() >= deadline || !unlocked) { stop(); return false }
  val ok = policy.permits(action, root()?.packageName?.toString(), unlocked, confirmed)
  if (!ok) AgentState.log("操作被拒绝：当前应用未获允许或任务已停止")
  return ok
 }
 fun openSettings(): Boolean {
  if (!permitted(Action.OPEN_SETTINGS)) return false
  startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  AgentState.log("已请求打开 Android 设置")
  return true
 }
 fun home(): Boolean {
  if (!permitted(Action.HOME)) return false
  val ok = performGlobalAction(GLOBAL_ACTION_HOME)
  stop(false); AgentState.log(if (ok) "已返回桌面；任务结束" else "返回桌面失败；任务结束")
  return ok
 }
 private fun back() {
  if (!permitted(Action.BACK)) return
  AgentState.log(if (performGlobalAction(GLOBAL_ACTION_BACK)) "已请求返回上一页" else "返回失败")
  resetControls()
 }
 fun demo() {
  startLocal()
  if (!policy.active) return
  val sequence = DemoSequence(object : DevicePort {
   override fun openSettings() = this@AgentService.openSettings()
   override fun home() = this@AgentService.home()
   override fun cancel() { handler.removeCallbacksAndMessages(null) }
  })
  demoSequence = sequence
  if (!sequence.start()) { stop(); return }
  val generation = policy.generation
  handler.postDelayed({ if (policy.active && policy.generation == generation) sequence.finish() }, 3000)
 }
 private fun showPanel() {
  panel = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL; setPadding(12, 8, 12, 8); setBackgroundColor(0xffe9eef5.toInt())
  }
  title = TextView(this).apply { text = "K40 Agent · 本地任务 · 120秒"; setTextColor(0xff172235.toInt()) }
  panel!!.addView(title)
  val row = LinearLayout(this)
  fun fixed(label: String, action: () -> Unit) { row.addView(Button(this).apply { text = label; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, 52.dp(), 1f)) }
  fixed("停止") { stop() }; fixed("桌面") { home() }; fixed("返回") { back() }
  panel!!.addView(row)
  controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
  panel!!.addView(controls)
  val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
   PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP }
  manager.addView(panel, params)
  resetControls()
 }
 private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
 private fun button(label: String, action: () -> Unit) {
  controls?.addView(Button(this).apply { text = label; setOnClickListener { action() } })
 }
 private fun resetControls() {
  confirmation = null; controls?.removeAllViews()
  button("读取结构（无文字）") { inspect() }
  button("向上滑动…") { confirmSwipe() }
 }
 private data class Target(val path: List<Int>, val bounds: Rect, val klass: String, val window: Int, val pkg: String)
 private fun snapshot(): List<Target> {
  val root = root() ?: return emptyList()
  val pkg = root.packageName?.toString() ?: return emptyList()
  if (pkg !in policy.allowed) return emptyList()
  val found = mutableListOf<Target>(); var unsafe = false; var visited = 0
  fun visit(node: AccessibilityNodeInfo, path: List<Int>, depth: Int) {
   if (++visited > 500 || depth > 24) { unsafe = true; return }
   // Never access text, contentDescription, viewIdResourceName, extras or screenshots.
   if (node.isPassword || node.isEditable) unsafe = true
   val bounds = Rect(); node.getBoundsInScreen(bounds)
   if (node.isVisibleToUser && node.isEnabled && node.isClickable && !bounds.isEmpty) {
    found.add(Target(path, bounds, node.className?.toString().orEmpty(), node.windowId, pkg))
   }
   for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it, path + i, depth + 1) }
  }
  visit(root, emptyList(), 0)
  return if (unsafe) emptyList() else found
 }
 private fun inspect() {
  if (!permitted(Action.INSPECT)) return
  val nodes = snapshot()
  AgentState.log("结构检查：${nodes.size} 个可操作节点（文字未读取）")
  controls?.removeAllViews()
  // Keep the stop control visible: bounded list, three targets per inspection.
  nodes.take(3).forEachIndexed { i, target ->
   button("节点 ${i + 1} · ${target.klass.substringAfterLast('.')} · ${target.bounds.centerX()},${target.bounds.centerY()} …") { confirmClick(target) }
  }
  button("取消 / 返回控制") { resetControls() }
 }
 private fun ask(message: String, action: () -> Unit) {
  controls?.removeAllViews(); title?.text = message
  confirmation = action
  button("我已核对目标，确认本次操作") { val pending = confirmation; resetControls(); title?.text = "K40 Agent · 本地任务"; pending?.invoke() }
  button("取消") { title?.text = "K40 Agent · 本地任务"; resetControls() }
 }
 private fun confirmClick(target: Target) {
  val generation = policy.generation
  ask("确认点击 ${target.pkg} (${target.bounds.centerX()},${target.bounds.centerY()})？涉及发送、删除、付款时请核对。") {
   if (generation != policy.generation || !permitted(Action.CLICK, true)) return@ask
   if (target !in snapshot()) { AgentState.log("界面已变化或包含输入区域，点击已拒绝"); return@ask }
   var node = root()
   for (index in target.path) node = node?.getChild(index)
   val ok = node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
   AgentState.log(if (ok) "已执行一次人工确认的点击" else "点击失败")
  }
 }
 private fun confirmSwipe() {
  if (!permitted(Action.INSPECT)) return
  val target = root() ?: return
  val pkg = target.packageName?.toString(); val window = target.windowId; val generation = policy.generation
  ask("确认向上滑动当前白名单应用？") {
   if (generation != policy.generation || !permitted(Action.SWIPE, true)) return@ask
   val current = root()
   if (current?.windowId != window || current.packageName?.toString() != pkg || snapshot().isEmpty()) {
    AgentState.log("界面变化或敏感输入区域，滑动已拒绝"); return@ask
   }
   val screen = Rect(); current.getBoundsInScreen(screen)
   val y1 = screen.top + screen.height() * .85f; val y2 = screen.top + screen.height() * .65f
   val path = Path().apply { moveTo(screen.exactCenterX(), y1); lineTo(screen.exactCenterX(), y2) }
   val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 150)).build()
   AgentState.log(if (dispatchGesture(gesture, null, null)) "已提交人工确认的滑动（150ms）" else "滑动失败")
  }
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (!policy.active) return
  if (!unlocked) { stop(); return }
  val pkg = root()?.packageName?.toString()
  if (pkg != null && pkg !in policy.allowed) stop()
  // Window transitions invalidate pending approvals, without reading event text.
  if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
   (confirmation != null && event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.windowId == root()?.windowId)) resetControls()
 }
 override fun onInterrupt() { stop() }
 override fun onUnbind(intent: Intent?): Boolean {
  stop(); current.clear(); AgentState.connected = false
  return super.onUnbind(intent)
 }
 override fun onDestroy() { if (receiverRegistered) unregisterReceiver(screenOff); receiverRegistered = false; stop(false); current.clear(); AgentState.connected = false; super.onDestroy() }
}
