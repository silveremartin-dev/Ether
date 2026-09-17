#!/usr/bin/env python3
import os
import re
import glob

def replace_in_file(filepath, replacements):
    if not os.path.exists(filepath):
        return
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()
    orig = content
    for target, repl in replacements:
        content = content.replace(target, repl)
    if content != orig:
        with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
        print(f"Updated {filepath}")

# 1. README.md
replace_in_file('README.md', [
    ('# Ether - Human Society & Cliodynamic Thermodynamic Simulation (v2.0 / v4.0)', '# Ether - Human Society & Cliodynamic Thermodynamic Simulation'),
    ('(v4.0)', ''),
    ('Ether 2.0', 'Ether'),
    ('v2.0.0', 'v1.0.0-beta.1'),
    ('2.0.0', '1.0.0-beta.1'),
    ('RELEASE_ANNOUNCEMENT_V2.md', 'RELEASE_ANNOUNCEMENT.md'),
    ('Version 2.0 Release Notes', 'Version 1.0.0-beta.1 Release Notes')
])

# 2. docs/DEPLOYMENT.md
replace_in_file('docs/DEPLOYMENT.md', [
    ('2.0.0', '1.0.0-beta.1'),
    ('v2.0.0', 'v1.0.0-beta.1'),
    ('Ether 2.0', 'Ether')
])

# 3. docs/SECURITY.md
replace_in_file('docs/SECURITY.md', [
    ('Ether 2.0', 'Ether')
])

# 4. docs/MODEL_COMPARISON.md
replace_in_file('docs/MODEL_COMPARISON.md', [
    ('Ether v4.0', 'Ether'),
    ('Ether (v4.0)', 'Ether'),
    ('Ether v4.0.0', 'Ether'),
    ('v4.0', '')
])

# 5. docs/SIMULATION_EQUATIONS_AND_VARIABLES.md
replace_in_file('docs/SIMULATION_EQUATIONS_AND_VARIABLES.md', [
    ('Version 4.5.0 — ', ''),
    ('Ether 2.0', 'Ether'),
    ('Ether 4.0', 'Ether'),
    ('v4.0.0', ''),
    ('v4.0', '')
])

# 6. docs/ARCHITECTURE.md
replace_in_file('docs/ARCHITECTURE.md', [
    ('Ether 2.0 / 4.0', 'Ether'),
    ('Ether 2.0', 'Ether'),
    ('Ether 4.0', 'Ether'),
    ('v4.0.0', ''),
    ('v4.0', '')
])

# 7. docs/posts/REDDIT_POST.md
replace_in_file('docs/posts/REDDIT_POST.md', [
    ('Ether 2.0:', 'Ether:'),
    ('Ether 2.0', 'Ether'),
    ('v2.0.0', 'v1.0.0-beta.1'),
    ('2.0.0', '1.0.0-beta.1')
])

# 8. docs/posts/LINKEDIN_POST.md
replace_in_file('docs/posts/LINKEDIN_POST.md', [
    ('Ether 2.0:', 'Ether:'),
    ('Ether 2.0', 'Ether'),
    ('v2.0.0', 'v1.0.0-beta.1'),
    ('2.0.0', '1.0.0-beta.1')
])

# 9. Rename RELEASE_ANNOUNCEMENT_V2.md to RELEASE_ANNOUNCEMENT.md and update
old_rel = 'docs/posts/RELEASE_ANNOUNCEMENT_V2.md'
new_rel = 'docs/posts/RELEASE_ANNOUNCEMENT.md'
if os.path.exists(old_rel):
    with open(old_rel, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('# Ether 2.0.0 — Official Release Announcement & Changelog', '# Ether 1.0.0-beta.1 — Official Release Announcement & Changelog')
    content = content.replace('2.0.0-LTS', '1.0.0-beta.1')
    content = content.replace('Ether 2.0', 'Ether')
    content = content.replace('Ether 2.0.0', 'Ether 1.0.0-beta.1')
    content = content.replace('v2.0.0', 'v1.0.0-beta.1')
    with open(new_rel, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)
    os.remove(old_rel)
    print(f"Renamed {old_rel} -> {new_rel}")

# 10. .github/workflows/release.yml
replace_in_file('.github/workflows/release.yml', [
    ("default: '2.0.0'", "default: '1.0.0-beta.1'"),
    ('RELEASE_ANNOUNCEMENT_V2.md', 'RELEASE_ANNOUNCEMENT.md')
])

# 11. scripts/package_release.ps1 & package_release.sh
replace_in_file('scripts/package_release.ps1', [
    ('$Version = "2.0.0"', '$Version = "1.0.0-beta.1"'),
    ('society-simulation-2.0.0-SNAPSHOT-executable.jar', 'society-simulation-1.0.0-beta.1-executable.jar')
])
replace_in_file('scripts/package_release.sh', [
    ('VERSION="${1:-2.0.0}"', 'VERSION="${1:-1.0.0-beta.1}"'),
    ('society-simulation-2.0.0-SNAPSHOT-executable.jar', 'society-simulation-1.0.0-beta.1-executable.jar')
])

# 12. install.bat / install.sh / run.bat
replace_in_file('install.bat', [
    ('society-simulation-2.0.0-SNAPSHOT-executable.jar', 'society-simulation-1.0.0-beta.1-executable.jar')
])
replace_in_file('install.sh', [
    ('society-simulation-2.0.0-SNAPSHOT-executable.jar', 'society-simulation-1.0.0-beta.1-executable.jar')
])
replace_in_file('run.bat', [
    ('society-simulation-2.0.0-SNAPSHOT-executable.jar', 'society-simulation-1.0.0-beta.1-executable.jar')
])

print("Version references cleaned successfully!")
