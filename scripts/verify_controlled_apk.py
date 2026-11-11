#!/usr/bin/env python3
import argparse,re,subprocess,zipfile
p=argparse.ArgumentParser();p.add_argument("apk");p.add_argument("--aapt",required=True);a=p.parse_args()
def dump(path): return subprocess.check_output([a.aapt,"dump","xmltree",a.apk,path],text=True)
m=dump("AndroidManifest.xml")
for element in ("uses-permission","uses-permission-sdk-23","permission","receiver","provider"):
 assert not re.search(r"E: "+re.escape(element)+r"(?: |$)",m,re.M),element
assert 'package="org.k40agent.controlled"' in m
assert len(re.findall(r"E: service ",m))==1
assert 'org.k40agent.accessibility.ControlledService' in m
assert 'android.permission.BIND_ACCESSIBILITY_SERVICE' in m
assert len(re.findall(r"E: activity ",m))==1
for flag in ("debuggable","allowBackup"):
 lines=[line for line in m.splitlines() if "android:"+flag+"(" in line]
 assert all("=(type 0x12)0x0" in line for line in lines),flag
 if flag=="allowBackup":assert lines
# Non-debuggable APKs may shorten resource filenames; resolve through the resource table.
resources=subprocess.check_output([a.aapt,"dump","--values","resources",a.apk],text=True)
match=re.search(r'(?m)^\s*resource (0x[0-9a-f]+) [^\n]*:xml/controlled_service:[^\n]*\n\s*\(string8\) "([^"]+)"',resources)
assert match,"service resource missing"
assert "=@"+match.group(1) in m,"service metadata does not reference expected config"
cfg=dump(match.group(2))
for flag in ("canRetrieveWindowContent","canPerformGestures"):
 assert any("android:"+flag+"(" in line and "0xffffffff" in line for line in cfg.splitlines()),flag
for forbidden in ("canTakeScreenshot","canRequestFilterKeyEvents","canRequestTouchExplorationMode"):
 assert not any("android:"+forbidden+"(" in line and "0xffffffff" in line for line in cfg.splitlines()),forbidden
with zipfile.ZipFile(a.apk) as z:
 for name in z.namelist():
  if re.fullmatch(r"classes[0-9]*\.dex",name):
   data=z.read(name)
   for prefix in (b"Lrikka/shizuku/",b"Lorg/k40agent/bridge/",b"Lorg/k40agent/accessibility/AgentService;"):
    assert prefix not in data,(name,prefix)
print("PASS: no requested permissions/network, one protected accessibility service, no receivers/providers, no debugging/backup, gestures enabled, no screenshot/key filtering or old modules")
