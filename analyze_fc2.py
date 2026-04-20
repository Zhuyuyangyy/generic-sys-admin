#!/usr/bin/env python3
"""
Analyze FileController.java line 285 and try to reverse-engineer
the original GBK content from the corrupted UTF-8 bytes.
"""
import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
line = lines[284]  # line 285 (0-indexed)

# The line starts with "@Operation(summary = \"" followed by corrupted UTF-8
# Let's find where the corruption starts
print(f'Full line ({len(line)} bytes):')
print(f'HEX: {line.hex()}')

# The ASCII part: @Operation(summary = "
# Let's find where the Chinese starts
at_op = line.find(b'@Operation')
print(f'\n@Operation at byte: {at_op}')

# Find "summary = \"" 
sum_idx = line.find(b'summary = "')
print(f'summary = " at byte: {sum_idx}')

# Chinese starts after 'summary = "'
chinese_start = sum_idx + len(b'summary = "')
print(f'Chinese starts at byte: {chinese_start}')

# The corrupted bytes (from 'summary = "' to '", description = "')
desc_start = line.find(b'", description = "')
print(f'description = " at byte: {desc_start}')

corrupted_bytes = line[chinese_start:desc_start]
print(f'\nCorrupted bytes ({len(corrupted_bytes)}): {corrupted_bytes.hex()}')

# Each Chinese char = 2 UTF-8 bytes (cX XX pattern, where cX is c2 or c3)
# Let's extract the 2-byte pairs
pairs = []
i = 0
while i < len(corrupted_bytes):
    b = corrupted_bytes[i]
    if b >= 0xc0:  # Start of multi-byte UTF-8
        if i+1 < len(corrupted_bytes):
            pairs.append(bytes([b, corrupted_bytes[i+1]]))
            i += 2
        else:
            pairs.append(bytes([b]))
            i += 1
    else:
        pairs.append(bytes([b]))
        i += 1

print(f'\nUTF-8 pairs ({len(pairs)} items):')
for j, p in enumerate(pairs):
    try:
        char_utf8 = p.decode('utf-8')
    except:
        char_utf8 = '?'
    print(f'  {j}: {p.hex()} -> U+{ord(char_utf8):04X} ({char_utf8})')

# Now try to find the original GBK bytes
# In GBK, Chinese chars are 2 bytes where first is 0x81-0xFE, second is 0x40-0xFE
# Let's look at pairs that could be valid GBK second bytes
print(f'\n\n=== Trying to recover original GBK ===')
print(f'The UTF-8 pairs represent Chinese chars from original GBK encoding.')
print(f'When GBK bytes [GB1, GB2] were interpreted as UTF-8, they became [0xC0+|GB1>>6|, GB1&0x3F|XX]')
print(f'This is complex - let\'s try a different approach:')
print(f'1. The U+XXXX values of the UTF-8 chars tell us the Unicode codepoints')
print(f'2. The original GBK bytes can be approximated from the UTF-8 bytes')
print(f'\nRecovered Unicode codepoints:')
unicode_chars = []
for p in pairs:
    try:
        ch = p.decode('utf-8')
        unicode_chars.append(ch)
        print(f'  {p.hex()} = U+{ord(ch):04X} ({ch})', end='')
    except:
        pass

print(f'\n\nUnicode string: {"".join(unicode_chars)}')
