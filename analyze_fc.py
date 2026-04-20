import sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines')

# Show line 285 with full hex
line = lines[284]
print(f'\nL285 ({len(line)} bytes):')
print(f'HEX: {line.hex()}')

# Decode as GBK (what the original was)
try:
    gbk_text = line.decode('gbk')
    print(f'\nGBK decode (what original Chinese should be):')
    print(gbk_text)
except Exception as e:
    print(f'GBK decode error: {e}')

# Show what the mojibake parts are in UTF-8 decode
try:
    utf8_text = line.decode('utf-8')
    print(f'\nUTF8 decode (garbled):')
    print(utf8_text)
except Exception as e:
    print(f'UTF8 decode error: {e}')

# Find the problematic sequence: c3 a9 c2 96 c2 b9... 
# These are multi-byte UTF-8 chars. Let's extract them
print(f'\nUTF-8 byte analysis:')
i = 0
while i < len(line):
    b = line[i]
    if b < 0x80:
        print(f'  [{i}] ASCII: {chr(b)}')
        i += 1
    elif b >= 0xc0 and b < 0xe0:
        # 2-byte UTF-8
        if i+1 < len(line):
            seq = bytes([b, line[i+1]])
            print(f'  [{i}] 2-byte UTF-8: {seq.hex()} = {seq.decode("utf-8", errors="replace")}')
        i += 2
    elif b >= 0xe0:
        # 3-byte UTF-8
        if i+2 < len(line):
            seq = bytes([b, line[i+1], line[i+2]])
            print(f'  [{i}] 3-byte UTF-8: {seq.hex()} = {seq.decode("utf-8", errors="replace")}')
        i += 3
    else:
        print(f'  [{i}] continuation byte: {b:02x}')
        i += 1
