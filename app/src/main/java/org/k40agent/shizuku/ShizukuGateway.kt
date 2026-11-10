package org.k40agent.shizuku
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
interface ShizukuGateway { fun status(): String }
class LocalShizukuGateway : ShizukuGateway {
 override fun status(): String = try {
  if (!Shizuku.pingBinder()) "未连接（基础功能可用）"
  else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) "运行中，已授权（仅检测）"
  else "运行中，未授权（本版本无需授权）"
 } catch (_: Exception) { "不可用（基础功能可用）" }
}
