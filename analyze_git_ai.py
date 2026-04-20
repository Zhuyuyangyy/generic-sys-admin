#!/usr/bin/env python3
"""Check AIClient.java from git history and identify corrupted lines."""
import subprocess, os

PROJECT = r'D:\ZYY Project\generic-sys-admin'

# Get file from efc3657 commit (first commit that added AIClient.java)
result = subprocess.run(
    ['git', 'show', 'efc3657:backend/src/main/java/com/zyy/client/AIClient.java'],
    cwd=PROJECT, capture_output=True
)

# The output is bytes that might be corrupted
content_bytes = result.stdout
print(f'Bytes length: {len(content_bytes)}')

# Split by lines
lines = content_bytes.split(b'\n')
print(f'Lines: {len(lines)}')

log = []
# Check lines 115-135
for i in range(114, min(135, len(lines))):
    line = lines[i]
    log.append(f'\nL{i+1} ({len(line)} bytes):\n')
    log.append(f'HEX: {line[:60].hex()}\n')
    # Check for mojibake indicator bytes
    has_mojibake = False
    for j in range(len(line)-1):
        b1, b2 = line[j], line[j+1]
        # Check for UTF-8 2-byte sequences that are Latin-1 chars (not Chinese)
        if b1 in (0xc2, 0xc3) and 0x80 <= b2 <= 0xbf:
            # This could be Latin-1 chars encoded as UTF-8
            has_mojibake = True
            break
    if has_mojibake:
        log.append(f'  [POSSIBLE MOJIBAKE]\n')
    try:
        txt = line.decode('utf-8')
        log.append(f'  TEXT: {txt[:80]}\n')
    except:
        log.append(f'  UTF8 ERROR\n')

with open(r'D:\ZYY Project\generic-sys-admin\aiclient_git.txt', 'wb') as f:
    # Write as raw bytes to avoid encoding issues
    f.write(content_bytes)

with open(r'D:\ZYY Project\generic-sys-admin\aiclient_analysis2.txt', 'w', encoding='utf-8', errors='replace') as f:
    f.writelines(log)

print('\nRaw file written to aiclient_git.txt')
print('Analysis written to aiclient_analysis2.txt')
