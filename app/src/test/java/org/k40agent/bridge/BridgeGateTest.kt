package org.k40agent.bridge
import org.junit.Assert.*
import org.junit.Test
import org.k40agent.automation.Action
class BridgeGateTest {
 private class MemoryStore : GrantStore {
  var revoked = false; val used = mutableSetOf<String>()
  override fun isRevoked(sessionId: String) = revoked
  override fun consumeNonce(sessionId: String, requestId: String) = used.add("$sessionId/$requestId")
 }
 private val grant = SessionGrant("s", "pinned-key", 100, 200, setOf(Action.HOME), setOf("self"))
 private val request = BridgeRequest("n", "s", Action.HOME, "self")
 @Test fun authenticatedScopedRequestOnlyAndNoReplay() {
  val gate = BridgeGate(MemoryStore())
  assertFalse(gate.permits(grant, request, 120, false))
  assertFalse(gate.permits(grant, request.copy(packageName = "other"), 120, true))
  assertFalse(gate.permits(grant, request.copy(action = Action.CLICK), 120, true))
  assertTrue(gate.permits(grant, request, 120, true))
  assertFalse(gate.permits(grant, request, 120, true))
 }
 @Test fun rejectsExpiredRevokedAndOverlongGrants() {
  val store = MemoryStore(); val gate = BridgeGate(store)
  assertFalse(gate.permits(grant, request, 200, true))
  assertFalse(gate.permits(grant, request, 99, true))
  assertFalse(gate.permits(grant.copy(expiresAtEpochSeconds = 500), request, 120, true))
  store.revoked = true; assertFalse(gate.permits(grant, request, 120, true))
  assertFalse(RemoteControl.ENABLED)
 }
}
