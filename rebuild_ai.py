#!/usr/bin/env python3
"""Extract clean code from AIClient.java git history, removing corrupted Chinese."""
import subprocess, os

PROJECT = r'D:\ZYY Project\generic-sys-admin'

# Get file from git history (efc3657 commit - first version)
result = subprocess.run(
    ['git', 'show', 'efc3657:backend/src/main/java/com/zyy/client/AIClient.java'],
    cwd=PROJECT, capture_output=True
)

# The stdout is bytes - the file content
content_bytes = result.stdout

# Save raw bytes to temp file
temp_path = r'D:\ZYY Project\generic-sys-admin\temp_ai_java.bin'
with open(temp_path, 'wb') as f:
    f.write(content_bytes)

# Now read as lines and extract clean parts
# The corruption is in Chinese comments and strings
# ASCII code (imports, method signatures, braces) is clean

lines = content_bytes.split(b'\n')
clean_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    
    # Check if line has non-ASCII bytes (corruption indicator)
    has_cjk = False
    has_latin_extended = False
    j = 0
    while j < len(line):
        b = line[j]
        if b > 0x7f:
            # Non-ASCII byte
            if 0x80 <= b <= 0x9f:
                # C1 control chars - definitely corruption
                has_latin_extended = True
                break
            elif 0xc2 <= b <= 0xc3 and j+1 < len(line):
                # UTF-8 2-byte sequence - check if it's Latin Extended
                next_b = line[j+1]
                if 0x80 <= next_b <= 0xbf:
                    cp = ((b & 0x03) << 8) | next_b
                    if cp < 0x4E00 or cp > 0x9FFF:  # Not in CJK range
                        has_latin_extended = True
                        break
                    else:
                        j += 2
                        continue
            # CJK char - might be legitimate
            # Continue checking
        j += 1
    
    if has_latin_extended:
        # This line has corruption - try to find a clean replacement or skip
        # Heuristic: if this is a comment line (starts with // or *), replace with ASCII
        stripped = line.lstrip()
        if stripped.startswith(b'//'):
            # Replace comment with ASCII equivalent
            clean_lines.append(b'// [comment corrupted - skipped]')
            i += 1
            continue
        elif stripped.startswith(b'*'):
            clean_lines.append(b'/* [comment corrupted] */')
            i += 1
            continue
        else:
            # Inside code - find where corruption starts and truncate
            # Find the first non-ASCII position
            clean_part = b''
            for j, b in enumerate(line):
                if b > 0x7f:
                    break
                clean_part += bytes([b])
            if clean_part.strip():
                clean_lines.append(clean_part)
            i += 1
            continue
    else:
        # Line is clean
        clean_lines.append(line)
    i += 1

# Build clean content
clean_content = b'\n'.join(clean_lines)

# Write clean version to temp
clean_path = r'D:\ZYY Project\generic-sys-admin\temp_ai_clean.java'
with open(clean_path, 'wb') as f:
    f.write(clean_content)

print(f'Clean version has {len(clean_lines)} lines')
print(f'Saved to {clean_path}')

# Now let's see what the clean version looks like
print('\n=== CLEAN CONTENT ===')
with open(clean_path, 'rb') as f:
    raw = f.read()
try:
    txt = raw.decode('utf-8')
    print(txt[:3000])
except Exception as e:
    print(f'Decode error: {e}')
