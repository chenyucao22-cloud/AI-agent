package org.k40agent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import android.content.pm.PackageManager
@RunWith(AndroidJUnit4::class)
class ManifestTest {
 @Test fun noNetworkPermissionAndNoBackup() {
  val context = InstrumentationRegistry.getInstrumentation().targetContext
  val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
  assertFalse(info.requestedPermissions.orEmpty().contains("android.permission.INTERNET"))
  assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP)
 }
}
