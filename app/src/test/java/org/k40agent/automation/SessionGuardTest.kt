package org.k40agent.automation
import org.junit.Assert.*
import org.junit.Test
class SessionGuardTest {
 private val s=Screen("settings",1,0,0,1000,2000,0)
 private fun guard()=SessionGuard().apply { start("settings",100) }
 @Test fun idleAndWrongTargetDenied() { val g=SessionGuard(); assertNull(g.propose(s,Operation.HOME,0,0,100));g.start("other",100);assertNull(g.propose(s,Operation.TAP,50,50,101)) }
 @Test fun holdAndOneShotRequired() { val g=guard();val a=g.propose(s,Operation.TAP,50,50,101)!!;assertFalse(g.consume(a,s,1400,500));assertFalse(g.consume(a,s,1500,1300));val b=g.propose(s,Operation.UP,0,0,1600)!!;assertTrue(g.consume(b,s,2900,1200));assertFalse(g.consume(b,s,2901,1500)) }
 @Test fun contentChangeRevokes() { val g=guard();val a=g.propose(s,Operation.TAP,50,50,101)!!;g.changed();assertFalse(g.consume(a,s,1400,1200)) }
 @Test fun allWindowIdentityChangesDenied() { for(other in listOf(s.copy(pkg="other"),s.copy(id=2),s.copy(right=999),s.copy(rotation=1),null)) {val g=guard();val a=g.propose(s,Operation.HOME,0,0,101)!!;assertFalse(g.consume(a,other,1400,1200))} }
 @Test fun timeoutAndClockRollbackDenied() {for(time in listOf(100L,5102L,60100L)) {val g=guard();val a=g.propose(s,Operation.BACK,0,0,101)!!;assertFalse(g.consume(a,s,time,1200))} }
 @Test fun stopAndRestartRevokeOldApproval() {val g=guard();val a=g.propose(s,Operation.DOWN,0,0,101)!!;g.stop();g.start("settings",200);assertFalse(g.consume(a,s,1500,1300))}
 @Test fun noOutOfBoundsTap() {val g=guard();for(x in listOf(-1,1000))assertNull(g.propose(s,Operation.TAP,x,10,101))}
 @Test fun latestProposalWinsAndCancelWorks() {val g=guard();val a=g.propose(s,Operation.HOME,0,0,101)!!;val b=g.propose(s,Operation.BACK,0,0,102)!!;assertFalse(g.valid(a,s,1500));assertTrue(g.valid(b,s,1500));g.cancel();assertFalse(g.valid(b,s,1500))}
 @Test fun sensitiveSystemPackagesCannotBeAdded() {assertFalse(PackagePolicy.canAdd("com.android.systemui"));assertFalse(PackagePolicy.canAdd("com.android.permissioncontroller"));assertFalse(PackagePolicy.canAdd(""));assertTrue(PackagePolicy.canAdd("com.example.demo"))}
}
