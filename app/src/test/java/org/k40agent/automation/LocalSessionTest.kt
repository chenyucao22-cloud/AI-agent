package org.k40agent.automation
import org.junit.Assert.*
import org.junit.Test
class LocalSessionTest {
 @Test fun idleCannotTick() { val s = LocalSession(); assertFalse(s.tick(0)); assertEquals(0, s.remaining) }
 @Test fun stopRevokesPendingCallback() { val s = LocalSession(); val token = s.start(); s.stop(); assertFalse(s.tick(token)); assertFalse(s.active) }
 @Test fun restartingRejectsOldCallback() { val s = LocalSession(); val old = s.start(); val now = s.start(); assertFalse(s.tick(old)); assertTrue(s.tick(now)); assertEquals(9, s.remaining) }
 @Test fun timerEndsWithoutFurtherCallbacks() { val s = LocalSession(); val t = s.start(); repeat(10) { assertTrue(s.tick(t)) }; assertFalse(s.active); assertFalse(s.tick(t)); assertEquals(0, s.remaining) }
 @Test fun actionsRequireForegroundUnlockedInteractiveDevice() {
  for (front in listOf(false,true)) for (unlock in listOf(false,true)) for (interactive in listOf(false,true))
   assertEquals(front && unlock && interactive, UserActionGate.permits(front,unlock,interactive))
 }
}
