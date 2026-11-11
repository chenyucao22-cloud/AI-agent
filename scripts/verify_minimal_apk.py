#!/usr/bin/env python3
"""Fail closed against the built artifact, independent of source manifest merging."""
import argparse, pathlib, re, subprocess, zipfile
p = argparse.ArgumentParser()
p.add_argument("apk"); p.add_argument("--aapt", required=True)
a = p.parse_args()
xml = subprocess.check_output([a.aapt, "dump", "xmltree", a.apk, "AndroidManifest.xml"], text=True)
for element in ("uses-permission", "uses-permission-sdk-23", "permission", "service", "receiver", "provider", "queries"):
    assert not re.search(r"E: " + re.escape(element) + r"(?: |$)", xml, re.M), element
assert 'package="org.k40agent.minimal"' in xml
assert len(re.findall(r"E: activity ", xml)) == 1
for flag in ("debuggable", "allowBackup"):
    lines = [line for line in xml.splitlines() if "android:" + flag + "(" in line]
    assert all("=(type 0x12)0x0" in line for line in lines), flag
    if flag == "allowBackup": assert lines
with zipfile.ZipFile(a.apk) as z:
    for name in z.namelist():
        if re.fullmatch(r"classes[0-9]*\.dex", name):
            data = z.read(name)
            for prefix in (b"Lorg/k40agent/accessibility/", b"Lorg/k40agent/bridge/", b"Lorg/k40agent/shizuku/", b"Lrikka/shizuku/"):
                assert prefix not in data, (name, prefix)
print("PASS: zero permissions/services/receivers/providers; no retired modules; no debugging/backup; minimal package")
