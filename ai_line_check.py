#!/usr/bin/env python3
"""Check current working tree AIClient.java lines 115-135."""

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\client\AIClient.java'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\n')  # Unix newlines in file
print(f'File: {len(lines)} lines (split by \\n)')
print(f'Raw file has \\r\\n: {b"\\r\\n" in raw}')

# Try \r\n split
lines_crlf = raw.split(b'\r\n')
print(f'File: {len(lines_crlf)} lines (split by \\r\\n)')

# Use whichever gave 364 lines (from compile error count)
# Actually let's check what's at line 120 (0-indexed: 119)
target = lines_crlf  # assume CRLF
for i in range(114, 135):
    if i < len(target):
        line = target[i]
        print(f'\n=== L{i+1} ({len(line)} bytes) ===')
        # Find non-ASCII
        for j in range(min(len(line), 100)):
            b = line[j]
            if b > 127:
                # Check if 2-byte UTF-8
                if j+1 < len(line) and 0x80 <= line[j+1] <= 0xbf:
                    pair = bytes([b, line[j+1]])
                    try:
                        ch = pair.decode('utf-8')
                        print(f'  [{j}] UTF8 PAIR {pair.hex()} = U+{ord(ch):04X} "{ch}"')
                    except:
                        print(f'  [{j}] UTF8 ERR {pair.hex()}')
                else:
                    print(f'  [{j}] CHAR {b:02x}')
            elif b == 0x3f and j > 0 and line[j-1] > 127:
                pass  # skip ? after corrupted char
            elif b < 32 and b not in (9, 10, 13):
                print(f'  [{j}] CTRL {b:02x}')
