import os

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\config\Knife4jConfig.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total lines: {len(lines)}')

# Print lines 43-52
for i in range(42, min(52, len(lines))):
    line = lines[i]
    try:
        txt_utf8 = line.decode('utf-8')
        print(f'L{i+1}: {txt_utf8[:80]}')
    except Exception as e:
        print(f'L{i+1} ERROR: {e} | hex: {line[:30].hex()}')
