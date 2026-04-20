#!/usr/bin/env python3
import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines')

# Show lines 270-295
with open(r'D:\ZYY Project\generic-sys-admin\fc_lines_270_295.txt', 'w', encoding='utf-8') as out:
    for i in range(269, min(295, len(lines))):
        line = lines[i]
        try:
            txt = line.decode('utf-8')
        except:
            txt = f'ERROR: {line[:20].hex()}'
        out.write(f'L{i+1}: {txt}\n')

print('Written to fc_lines_270_295.txt')
