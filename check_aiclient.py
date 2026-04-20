#!/usr/bin/env python3
"""Analyze AIClient.java corrupted lines."""
import os

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\client\AIClient.java'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines')

log = []

# Show lines 118-132 (0-indexed: 117-131)
for i in range(117, 132):
    if i < len(lines):
        line = lines[i]
        log.append(f'\nL{i+1} ({len(line)} bytes):')
        log.append(f'HEX: {line[:80].hex()}\n')
        try:
            txt = line.decode('utf-8')
        except:
            txt = f'DECODE ERROR'
        log.append(f'TEXT: {txt[:100]}\n')

        # Show non-ASCII positions
        if True:
            for j, b in enumerate(line):
                if b > 127:
                    if j < len(line) - 1 and line[j+1] > 127:
                        pair = bytes([b, line[j+1]])
                        try:
                            ch = pair.decode('utf-8')
                            log.append(f'  [{j}] UTF8-2: {pair.hex()} = U+{ord(ch):04X} ({ch!r})\n')
                        except:
                            log.append(f'  [{j}] PAIR-ERR: {pair.hex()}\n')
                    else:
                        log.append(f'  [{j}] CHAR: {b:02x}\n')
                elif b == 0x3f and j > 0 and line[j-1] > 127:
                    pass  # skip ? that follows corrupted char
                elif b < 32 and b != 0x0a and b != 0x0d:
                    log.append(f'  [{j}] CTRL: {b:02x}\n')

with open(r'D:\ZYY Project\generic-sys-admin\aiclient_analysis.txt', 'w', encoding='utf-8') as f:
    f.writelines(log)

print('Done - see aiclient_analysis.txt')
