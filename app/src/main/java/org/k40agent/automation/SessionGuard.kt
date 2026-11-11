package org.k40agent.automation

enum class Operation { TAP, UP, DOWN, HOME, BACK }
data class Screen(val pkg: String, val id: Int, val left: Int, val top: Int, val right: Int, val bottom: Int, val rotation: Int)
data class Approval(val generation: Long, val revision: Long, val serial: Long, val screen: Screen, val operation: Operation, val x: Int, val y: Int, val issued: Long)
class SessionGuard {
    var target: String? = null; private set
    var generation = 0L; private set
    var revision = 0L; private set
    var pending: Approval? = null; private set
    private var serial = 0L
    private var deadline = 0L
    fun start(pkg: String, now: Long) { stop(); target = pkg; deadline = now + 60_000 }
    fun live(now: Long) = target != null && now < deadline
    fun stop() { generation++; target = null; pending = null }
    fun changed() { revision++; pending = null }
    fun cancel() { pending = null }
    fun propose(screen: Screen, op: Operation, x: Int, y: Int, now: Long): Approval? {
        if (!live(now) || screen.pkg != target) return null
        if (op == Operation.TAP && (x !in screen.left until screen.right || y !in screen.top until screen.bottom)) return null
        return Approval(generation, revision, ++serial, screen, op, x, y, now).also { pending = it }
    }
    fun valid(a: Approval, screen: Screen?, now: Long) = live(now) && pending == a && a.generation == generation && a.revision == revision && screen == a.screen && now >= a.issued && now - a.issued <= 5000
    fun consume(a: Approval, screen: Screen?, now: Long, heldMillis: Long): Boolean {
        val ok = heldMillis >= 1200 && valid(a, screen, now)
        pending = null // Every attempt consumes the approval, including failed attempts.
        return ok
    }
}
object PackagePolicy {
    val denied = setOf("com.android.systemui", "com.android.permissioncontroller", "com.google.android.permissioncontroller", "com.android.packageinstaller", "com.google.android.packageinstaller", "com.miui.securitycenter", "moe.shizuku.privileged.api")
    fun canAdd(pkg: String) = pkg.isNotBlank() && pkg !in denied
}
