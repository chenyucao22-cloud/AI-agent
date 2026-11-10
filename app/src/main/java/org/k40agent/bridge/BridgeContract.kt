package org.k40agent.bridge

import org.k40agent.automation.Action
/** Contract only. No listener, socket, transport, token issuer or remote execution is implemented. */
data class SessionGrant(val sessionId: String, val peerKeySha256: String, val issuedAtEpochSeconds: Long,
 val expiresAtEpochSeconds: Long, val actions: Set<Action>, val packages: Set<String>)
data class BridgeRequest(val requestId: String, val sessionId: String, val action: Action, val packageName: String)
interface AuthenticatedTransport {
 /** Must verify both peer certificates and locally pinned public keys before delivering requests. */
 fun closeAndRevoke()
}
interface GrantStore {
 fun isRevoked(sessionId: String): Boolean
 /** Atomic consume required to reject replay, scoped to a session. */
 fun consumeNonce(sessionId: String, requestId: String): Boolean
}
class BridgeGate(private val store: GrantStore) {
 fun permits(grant: SessionGrant, request: BridgeRequest, now: Long, mutuallyAuthenticated: Boolean): Boolean {
  if (!mutuallyAuthenticated || store.isRevoked(grant.sessionId)) return false
  if (grant.peerKeySha256.isBlank() || request.requestId.isBlank() || request.sessionId != grant.sessionId) return false
  if (grant.expiresAtEpochSeconds <= grant.issuedAtEpochSeconds || grant.expiresAtEpochSeconds - grant.issuedAtEpochSeconds !in 1..300) return false
  if (now < grant.issuedAtEpochSeconds || now >= grant.expiresAtEpochSeconds) return false
  if (request.action !in grant.actions || request.packageName !in grant.packages) return false
  return store.consumeNonce(grant.sessionId, request.requestId)
 }
}
object RemoteControl { const val ENABLED = false }
