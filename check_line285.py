import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines')

# Show line 285 (index 284)
line = lines[284]
print(f'L285 hex: {line.hex()}')
print(f'L285 len: {len(line)}')

# Try UTF-8
try:
    txt = line.decode('utf-8')
    print(f'L285 UTF8: {txt}')
except Exception as e:
    print(f'UTF8 error: {e}')
    # Try GBK
    try:
        txt_gbk = line.decode('gbk')
        print(f'L285 GBK: {txt_gbk}')
    except:
        print(f'GBK error too')

# Also show surrounding lines
for i in range(282, 288):
    if i < len(lines):
        print(f'\nL{i+1}: {lines[i][:100].hex() if len(lines[i]) > 100 else lines[i].hex()}')
        try:
            print(f'  UTF8: {lines[i].decode("utf-8")[:80]}')
        except:
            try:
                print(f'  GBK: {lines[i].decode("gbk")[:80]}')
            except:
                print(f'  RAW: {lines[i][:80]}')
