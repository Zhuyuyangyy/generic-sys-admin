import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\util\JsonUtil.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines, {len(raw)} bytes')

# Show lines 5-10 fully
for i in range(4, min(10, len(lines))):
    line = lines[i]
    print(f'\nL{i+1} FULL:')
    print(f'  hex: {line.hex()}')
    print(f'  len: {len(line)}')
    # Check if line ends with semicolon
    if line.strip().endswith(b';') or line.strip().endswith(b'//') or not line.strip() or b'import' in line:
        ends_ok = 'SEMI-OK' if line.strip() else 'EMPTY'
    else:
        ends_ok = 'NO-SEMI!'
    print(f'  status: {ends_ok}')
    try:
        print(f'  text: {line.decode("utf-8")}')
    except:
        print(f'  text: (decode error)')
