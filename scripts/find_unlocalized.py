#!/usr/bin/env python3
import glob, os, re

def check_file(path):
    print(f"=== Checking {path} ===")
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.readlines()
    
    patterns = [
        (re.compile(r'new Label\(\s*"([^"]+)"'), "Label"),
        (re.compile(r'new Button\(\s*"([^"]+)"'), "Button"),
        (re.compile(r'new Tab\(\s*"([^"]+)"'), "Tab"),
        (re.compile(r'new Tooltip\(\s*"([^"]+)"'), "Tooltip"),
        (re.compile(r'\.setText\(\s*"([^"]+)"'), "setText"),
        (re.compile(r'addLegendItem\([^,]+,[^,]+,\s*"([^"]+)"'), "addLegendItem")
    ]
    
    for idx, l in enumerate(lines, 1):
        if "I18n." in l:
            continue
        for p, kind in patterns:
            for m in p.findall(l):
                m_str = m.strip()
                if len(m_str) > 1 and not m_str.startswith("-fx") and not m_str.startswith("#") and not m_str.startswith("http") and not m_str.startswith("%.") and not m_str.startswith("(") and not m_str.startswith("button") and not m_str.startswith("label") and not m_str.startswith("card"):
                    safe_m = m_str.encode('ascii', 'backslashreplace').decode('ascii')
                    print(f"Line {idx} [{kind}]: {safe_m}")

for path in glob.glob("src/main/java/org/ether/society/ui/*.java"):
    check_file(path)
