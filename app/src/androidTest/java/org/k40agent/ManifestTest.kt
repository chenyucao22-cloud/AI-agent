package org.k40agent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
@RunWith(AndroidJUnit4::class)
class ManifestTest {
 @Test fun noPermissionsServicesReceiversProvidersBackupOrDebugging() {
  val c = InstrumentationRegistry.getInstrumentation().targetContext
  val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS
  val info = c.packageManager.getPackageInfo(c.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
  assertTrue(info.requestedPermissions.isNullOrEmpty())
  assertTrue(info.services.isNullOrEmpty()); assertTrue(info.receivers.isNullOrEmpty()); assertTrue(info.providers.isNullOrEmpty())
  assertEquals(0, c.applicationInfo.flags and (ApplicationInfo.FLAG_ALLOW_BACKUP or ApplicationInfo.FLAG_DEBUGGABLE))
 }
}
