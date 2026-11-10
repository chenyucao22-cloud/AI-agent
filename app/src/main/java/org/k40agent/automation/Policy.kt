package org.k40agent.automation

enum class Action { OPEN_SETTINGS, HOME, BACK, INSPECT, CLICK, SWIPE }
class Policy(private val self: String) {
    val allowed = linkedSetOf(self, "com.android.settings")
    var active = false; private set
    var generation = 0; private set
    fun start() { generation++; active = true }
    fun stop() { generation++; active = false }
    fun permits(action: Action, foreground: String?, unlocked: Boolean, confirmed: Boolean = false): Boolean {
        if (!active || !unlocked) return false
        if (foreground !in allowed) return false
        return action !in setOf(Action.CLICK, Action.SWIPE) || confirmed
    }
}
interface DevicePort { fun openSettings(): Boolean; fun home(): Boolean; fun cancel() }
/** One user-started sequence; cancellation is checked again before every step. */
class DemoSequence(private val port: DevicePort) {
    private var stopped = true
    fun start(): Boolean { stopped = false; return port.openSettings() }
    fun finish(): Boolean = !stopped && port.home()
    fun stop() { stopped = true; port.cancel() }
}
