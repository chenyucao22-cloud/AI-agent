package org.k40agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.accessibility.*
import android.widget.*
import org.k40agent.automation.*

class ControlledService : AccessibilityService() {
 companion object {
  private var ref = java.lang.ref.WeakReference<ControlledService>(null)
  val instance get() = ref.get()
 }
 private val guard = SessionGuard()
 private val handler = Handler(Looper.getMainLooper())
 private val wm get() = getSystemService(WindowManager::class.java)
 private var bar: LinearLayout? = null
 private var body: LinearLayout? = null
 private var label: TextView? = null
 private var picker: View? = null
 private var marker: View? = null
 private var barAtTop = false
 private var ready = false
 private var busy = false
 private var started = 0L
 private var lastScreen: Screen? = null
 private var receiverRegistered = false
 private val screenOff = object : BroadcastReceiver() {
  override fun onReceive(context: Context?, intent: Intent?) { if (intent?.action == Intent.ACTION_SCREEN_OFF) stop("熄屏：任务停止") }
 }
 private fun now() = SystemClock.elapsedRealtime()
 private fun unlocked() = !getSystemService(KeyguardManager::class.java).isKeyguardLocked && getSystemService(PowerManager::class.java).isInteractive
 override fun onServiceConnected() {
  ref = java.lang.ref.WeakReference(this); AgentState.connected = true
  AgentState.allowed = defaults() + getSharedPreferences("controlled", MODE_PRIVATE).getStringSet("allowed", emptySet()).orEmpty().filter { PackagePolicy.canAdd(it) }
  if (!receiverRegistered) { registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), RECEIVER_NOT_EXPORTED); receiverRegistered = true }
  AgentState.log("服务已连接；无任务")
 }
 private fun defaults() = setOf(packageName, "com.android.settings")
 fun changeAllowed(pkg: String, add: Boolean) {
  stop("白名单变更：旧任务已撤销")
  if (pkg in defaults() || !PackagePolicy.canAdd(pkg)) return
  AgentState.allowed = if (add) AgentState.allowed + pkg else AgentState.allowed - pkg
  getSharedPreferences("controlled", MODE_PRIVATE).edit().putStringSet("allowed", AgentState.allowed).apply()
 }
 // Only window metadata and root.packageName are inspected. No node traversal or event payload reading.
 private fun screen(): Screen? = runCatching { readScreen() }.getOrNull()
 private fun readScreen(): Screen? {
  if (!unlocked()) return null
  val ws = windows
  if (ws.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD || (it.isActive && it.type == AccessibilityWindowInfo.TYPE_SYSTEM) }) return null
  // Touching our non-focusable overlay can change isActive; the application input-focus window remains authoritative.
  val w = ws.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused } ?: return null
  val pkg = w.root?.packageName?.toString() ?: return null
  if (pkg != guard.target || pkg !in AgentState.allowed) return null
  val b = Rect(); w.getBoundsInScreen(b)
  if (b.isEmpty) return null
  val rotation = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: return null
  return Screen(pkg, w.id, b.left, b.top, b.right, b.bottom, rotation)
 }
 fun startTask(pkg: String) {
  stop("准备启动任务")
  if (pkg !in AgentState.allowed || !unlocked()) { AgentState.log("任务被拒绝"); return }
  guard.start(pkg, now()); started = now(); ready = false; busy = false; barAtTop = false
  try {
   showBar()
   val intent = when (pkg) {
    packageName -> null
    "com.android.settings" -> Intent(Settings.ACTION_SETTINGS).setPackage(pkg)
    else -> packageManager.getLaunchIntentForPackage(pkg) ?: throw ActivityNotFoundException()
   }
   intent?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
   AgentState.active = true; AgentState.log("运行中：60秒可见任务")
   handler.post(tick)
  } catch (_: Exception) { stop("无法显示控制条或启动目标应用") }
 }
 private val tick = object : Runnable {
  override fun run() {
   if (!guard.live(now()) || !unlocked() || bar?.isAttachedToWindow != true || bar?.isShown != true) { stop("任务结束或控制条不可见"); return }
   val s = screen()
   if (!ready) {
    if (s != null) { ready = true; lastScreen = s; menu() }
    else if (now() - started > 5000) { stop("无法确认目标前台窗口"); return }
   } else if (s == null) { stop("离开允许窗口、键盘或安全窗口出现"); return }
   else if (s != lastScreen) { lastScreen = s; invalidate() }
   val a = guard.pending
   if (a != null && !guard.valid(a, s, now())) invalidate()
   label?.text = "受控任务 · ${guard.target} · ${(60_000 - (now() - started)).coerceAtLeast(0) / 1000}秒"
   handler.postDelayed(this, 250)
  }
 }
 fun stop(reason: String = "已立即停止；确认和队列已撤销") {
  guard.stop(); handler.removeCallbacksAndMessages(null); ready = false; busy = false
  remove(picker); picker = null; remove(marker); marker = null; remove(bar); bar = null; body = null; label = null
  AgentState.active = false; AgentState.log(reason)
 }
 private fun remove(v: View?) { v?.let { runCatching { wm.removeView(it) } } }
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
 private fun overlay(v: View, height: Int, gravity: Int) {
  v.filterTouchesWhenObscured = true
  wm.addView(v, WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, height, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { this.gravity = gravity })
 }
 private fun showBar() {
  val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(4), dp(8), dp(4)); setBackgroundColor(0xffe6edf6.toInt()) }
  bar = layout
  label = TextView(this).apply { text = "正在核验目标窗口"; setTextColor(Color.BLACK) }; layout.addView(label)
  layout.addView(Button(this).apply { text = "立即停止"; setTextColor(Color.RED); setOnClickListener { stop() } })
  body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; layout.addView(body)
  overlay(layout, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
 }
 private fun button(text: String, parent: LinearLayout = body!!, action: () -> Unit) {
  parent.addView(Button(this).apply { this.text = text; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, dp(48), 1f).takeIf { parent.orientation == LinearLayout.HORIZONTAL } ?: LinearLayout.LayoutParams(-1, dp(48)))
 }
 private fun menu() {
  if (body == null) return
  body!!.removeAllViews()
  if (!ready || busy) return
  val row = LinearLayout(this); body!!.addView(row)
  button("选点", row) { selectPoint() }; button("上滑", row) { propose(Operation.UP) }; button("下滑", row) { propose(Operation.DOWN) }
  val row2 = LinearLayout(this); body!!.addView(row2)
  button("桌面", row2) { propose(Operation.HOME) }; button("返回", row2) { propose(Operation.BACK) }; button("移控制条", row2) {
   invalidate(); barAtTop = !barAtTop
   bar?.let { v -> val p=v.layoutParams as WindowManager.LayoutParams; p.gravity=if(barAtTop) Gravity.TOP else Gravity.BOTTOM; runCatching { wm.updateViewLayout(v,p) }.onFailure { stop("无法移动控制条") } }
  }
 }
 private fun invalidate() { guard.changed(); remove(picker); picker = null; remove(marker); marker = null; menu() }
 private fun outsideBar(x: Int, y: Int): Boolean {
  val v = bar ?: return false; val at = IntArray(2); v.getLocationOnScreen(at)
  return !Rect(at[0], at[1], at[0] + v.width, at[1] + v.height).contains(x,y)
 }
 private fun selectPoint() {
  val original = screen() ?: return
  val revision = guard.revision
  val frame = FrameLayout(this).apply { setBackgroundColor(0x1800aaff) }
  frame.addView(Button(this).apply { text = "触碰目标位置以选点（不会立即点击）· 点此停止"; setOnClickListener { stop() } }, FrameLayout.LayoutParams(-1, dp(64), if(barAtTop) Gravity.TOP else Gravity.BOTTOM))
  frame.setOnTouchListener { _, e ->
   if (e.action == MotionEvent.ACTION_UP) {
    val x = e.rawX.toInt(); val y = e.rawY.toInt()
    remove(picker); picker = null
    if (e.flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) == 0 && revision == guard.revision && screen() == original && outsideBar(x,y)) propose(Operation.TAP,x,y)
   }
   true
  }
  picker = frame
  try { overlay(frame,-1,Gravity.TOP) } catch (_: Exception) { stop("无法显示选点控制层") }
 }
 private fun propose(op: Operation, x: Int = 0, y: Int = 0) {
  if (!ready || busy) return
  val s = screen() ?: run { stop("目标窗口不可用"); return }
  val a = guard.propose(s,op,x,y,now()) ?: return
  body?.removeAllViews()
  val verb = when(op) { Operation.TAP -> "点击"; Operation.UP -> "上滑"; Operation.DOWN -> "下滑"; Operation.HOME -> "返回桌面"; Operation.BACK -> "返回上一页" }
  body?.addView(TextView(this).apply { text = "${s.pkg} · $verb ${if (op == Operation.TAP) "($x,$y)" else ""}\n核对发送、删除、付款等后果；5秒内按住确认。"; setTextColor(Color.BLACK) })
  if (op == Operation.TAP) {
   marker = object : View(this) {
    override fun onDraw(canvas: Canvas) { val paint = Paint().apply { color=Color.RED; strokeWidth=4f; style=Paint.Style.STROKE }; val location=IntArray(2); getLocationOnScreen(location); canvas.drawCircle((x-location[0]).toFloat(),(y-location[1]).toFloat(),dp(16).toFloat(),paint) }
   }
   // Marker must never intercept the user's confirmation or the injected gesture.
   wm.addView(marker,WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP })
  }
  var down = 0L
  body?.addView(Button(this).apply {
   text = "按住1.2秒确认一次"; filterTouchesWhenObscured = true
   setOnTouchListener { view, e ->
    val inside = e.x >= 0 && e.y >= 0 && e.x < view.width && e.y < view.height
    val clear = e.flags and (MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED) == 0
    when (e.actionMasked) {
     MotionEvent.ACTION_DOWN -> { down = if (inside && clear) now() else 0 }
     MotionEvent.ACTION_MOVE -> { if (!inside || !clear) { down=0; invalidate() } }
     MotionEvent.ACTION_POINTER_DOWN -> { down=0; invalidate() }
     MotionEvent.ACTION_CANCEL -> { down = 0; invalidate() }
     MotionEvent.ACTION_UP -> {
      val held = if (down > 0) now() - down else 0L; down = 0
      val flagsOk = inside && clear
      if (flagsOk && guard.consume(a,screen(),now(),held) && bar?.isShown == true) execute(a) else invalidate()
     }
    }; true
   }
  })
  button("取消") { invalidate() }
 }
 private fun execute(a: Approval) {
  remove(marker); marker = null
  if (!guard.live(now()) || screen() != a.screen || !unlocked()) { stop("执行前窗口复核失败"); return }
  busy = true; menu()
  if (a.operation == Operation.HOME || a.operation == Operation.BACK) {
   val ok = runCatching { performGlobalAction(if (a.operation == Operation.HOME) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK) }.getOrDefault(false)
   stop(if (ok) "已执行确认的导航；任务结束" else "系统拒绝导航；任务结束"); return
  }
  val s = a.screen
  val path = Path()
  if (a.operation == Operation.TAP) {
   if (!outsideBar(a.x,a.y)) { stop("目标与控制条重叠"); return }
   path.moveTo(a.x.toFloat(),a.y.toFloat())
  } else {
   val at = IntArray(2); bar!!.getLocationOnScreen(at)
   val bottom = (if(barAtTop) s.bottom else minOf(s.bottom,at[1])) - dp(20)
   val top = (if(barAtTop) maxOf(s.top,at[1]+bar!!.height) else s.top) + dp(40)
   if (bottom - top < dp(120)) { stop("可滑动区域不足"); return }
   val x = (s.left+s.right)/2f
   val high = top + (bottom-top)*.25f; val low = top + (bottom-top)*.75f
   path.moveTo(x,if(a.operation==Operation.UP)low else high);path.lineTo(x,if(a.operation==Operation.UP)high else low)
  }
  val generation = guard.generation
  val accepted = dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,150)).build(),object : GestureResultCallback() {
   override fun onCompleted(gestureDescription: GestureDescription?) { if (generation == guard.generation) { busy=false; guard.changed(); AgentState.log("已执行一次人工确认操作"); menu() } }
   override fun onCancelled(gestureDescription: GestureDescription?) { if (generation == guard.generation) stop("系统取消手势") }
  },handler)
  if (!accepted) stop("系统拒绝手势")
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (!guard.live(now()) || !ready) return
  val s = screen() ?: run { stop("前台窗口不可用"); return }
  if (s != lastScreen) { lastScreen=s; invalidate(); return }
  if (event?.windowId == s.id && (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)) invalidate()
 }
 override fun onInterrupt() { stop("服务中断") }
 override fun onUnbind(intent: Intent?): Boolean { stop("服务断开"); ref.clear(); AgentState.connected=false; return super.onUnbind(intent) }
 override fun onDestroy() { stop("服务已关闭"); if(receiverRegistered)unregisterReceiver(screenOff); ref.clear(); AgentState.connected=false; super.onDestroy() }
}
