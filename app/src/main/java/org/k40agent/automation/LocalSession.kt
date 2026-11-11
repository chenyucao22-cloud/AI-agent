package org.k40agent.automation

/** App-local timer only. No callbacks can invoke Android or cross-app actions. */
class LocalSession {
    var generation = 0; private set
    var remaining = 0; private set
    val active get() = remaining > 0
    fun start(): Int { generation++; remaining = 10; return generation }
    fun stop() { generation++; remaining = 0 }
    fun tick(token: Int): Boolean {
        if (token != generation || !active) return false
        remaining--
        return true
    }
}
enum class LocalAction { OPEN_SETTINGS, HOME }
object UserActionGate {
    fun permits(resumed: Boolean, unlocked: Boolean, interactive: Boolean) = resumed && unlocked && interactive
}
