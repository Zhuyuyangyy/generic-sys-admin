import sys, os

# Check JsonUtil.java
path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\util\JsonUtil.java'
with open(path, 'rb') as f:
    raw = f.read()

print(f'JsonUtil.java: {len(raw)} bytes, starts with: {raw[:20].hex()}')
lines = raw.split(b'\r\n')
print(f'Lines: {len(lines)}')

# Check SysUser.java
path2 = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\rbac\model\SysUser.java'
with open(path2, 'rb') as f:
    raw2 = f.read()
print(f'\nSysUser.java: {len(raw2)} bytes, starts with: {raw2[:20].hex()}')

# Write hex of first 10 lines to file
with open(r'D:\ZYY Project\generic-sys-admin\debug_hex.txt', 'w', encoding='utf-8') as out:
    for i, line in enumerate(lines[:15]):
        out.write(f'L{i+1} hex: {line[:40].hex()}\n')
        try:
            out.write(f'    UTF8: {line.decode("utf-8")[:60]}\n')
        except Exception as e:
            out.write(f'    ERR: {e}\n')
    
    out.write('\nSysUser first 5 lines:\n')
    sys_lines = raw2.split(b'\r\n')
    for i, line in enumerate(sys_lines[:5]):
        out.write(f'SL{i+1} hex: {line[:40].hex()}\n')
