#!/usr/bin/env python3
"""
Try to recover original GBK text by:
1. Taking the corrupted UTF-8 bytes
2. Treating each 2-byte UTF-8 sequence as if it were a GBK byte pair
3. Building the original GBK string
"""
import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
line285 = lines[284]

# Find the corrupted section between 'summary = "' and '", description = "'
# (note: description = might NOT be present, since it's a parameter)
# Let's find the pattern
at_sum = line285.find(b'summary = "')
print(f'summary = " at index: {at_sum}')

# The bytes after summary = "
chi_start = at_sum + len(b'summary = "')
remaining = line285[chi_start:]

print(f'Remaining hex: {remaining.hex()}')

# Find the end of the summary value
# In the hex: ...3f2c 2064657363726970... = ?, description...
desc_pattern = b'", description = "'
at_desc = remaining.find(desc_pattern)
print(f'description pattern at: {at_desc} (from remaining start)')

summary_bytes = remaining[:at_desc]
desc_start_in_remaining = at_desc + len(desc_pattern)
desc_bytes = remaining[desc_start_in_remaining:]

print(f'\nSummary corrupted bytes ({len(summary_bytes)}): {summary_bytes.hex()}')
print(f'Description corrupted bytes ({len(desc_bytes)}): {desc_bytes.hex()}')

# The corrupted summary is a series of 2-byte UTF-8 sequences
# Each Chinese char was originally a GBK 2-byte sequence
# When GBK bytes [GB1, GB2] are read as UTF-8, they become the UTF-8 encoding of those byte values

# Let's try: treat each 2 UTF-8 bytes as a single character via Latin-1 (which maps byte->char directly)
# Then look at what Unicode codepoints we get
print(f'\n=== Treating UTF-8 pairs as Latin-1 chars ===')
chars = []
for i in range(0, len(summary_bytes), 2):
    b1 = summary_bytes[i]
    b2 = summary_bytes[i+1] if i+1 < len(summary_bytes) else 0
    # As Latin-1, each byte is a character
    c1 = chr(b1)
    c2 = chr(b2)
    chars.append(c1)
    chars.append(c2)
    print(f'  {b1:02X} {b2:02X} -> {c1!r} {c2!r}')

# Now try GBK decode of the summary_bytes as if they were GBK
print(f'\n=== Decoding summary_bytes as GBK ===')
try:
    gbk_text = summary_bytes.decode('gbk')
    print(f'GBK text: {gbk_text}')
except Exception as e:
    print(f'Error: {e}')

# Try CP1252
print(f'\n=== Decoding as CP1252 ===')
try:
    cp_text = summary_bytes.decode('cp1252')
    print(f'CP1252 text: {cp_text}')
except Exception as e:
    print(f'Error: {e}')

# Let me also check: the desc section
print(f'\n=== Description section ===')
for i in range(0, min(len(desc_bytes), 50), 2):
    b1 = desc_bytes[i]
    b2 = desc_bytes[i+1] if i+1 < len(desc_bytes) else 0
    print(f'  {b1:02X} {b2:02X}')
