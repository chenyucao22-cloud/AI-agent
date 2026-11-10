package org.k40agent.automation
import org.junit.Assert.*
import org.junit.Test
class PolicyTest {
 @Test fun requiresUserStartAndUnlockedAllowlistedWindow() {
  val p = Policy("self")
  assertFalse(p.permits(Action.HOME, "self", true))
  p.start()
  assertTrue(p.permits(Action.OPEN_SETTINGS, "self", true))
  assertTrue(p.permits(Action.HOME, "com.android.settings", true))
  assertFalse(p.permits(Action.HOME, "other", true))
  assertFalse(p.permits(Action.HOME, "self", false))
 }
 @Test fun mutationsAlwaysRequireConfirmation() {
  val p = Policy("self"); p.start()
  for (action in listOf(Action.CLICK, Action.SWIPE)) {
   assertFalse(p.permits(action, "self", true))
   assertTrue(p.permits(action, "self", true, true))
  }
 }
 @Test fun stopRevokesEvenConfirmedActions() {
  val p = Policy("self"); p.start(); val old = p.generation; p.stop()
  assertTrue(p.generation > old)
  Action.entries.forEach { assertFalse(p.permits(it, "self", true, true)) }
 }
 @Test fun demoOpensSettingsThenHome() {
  val calls = mutableListOf<String>()
  val port = object : DevicePort {
   override fun openSettings(): Boolean { calls.add("settings"); return true }
   override fun home(): Boolean { calls.add("home"); return true }
   override fun cancel() { calls.add("cancel") }
  }
  val demo = DemoSequence(port); assertTrue(demo.start()); assertTrue(demo.finish())
  assertEquals(listOf("settings", "home"), calls)
 }
 @Test fun stopPreventsQueuedHome() {
  val calls = mutableListOf<String>()
  val demo = DemoSequence(object : DevicePort {
   override fun openSettings(): Boolean { calls.add("settings"); return true }
   override fun home(): Boolean { calls.add("home"); return true }
   override fun cancel() { calls.add("cancel") }
  })
  demo.start(); demo.stop(); assertFalse(demo.finish())
  assertEquals(listOf("settings", "cancel"), calls)
 }
}
