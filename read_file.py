import sys

path = sys.argv[1]
with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')
print(f'Total: {len(lines)} lines')

# Print lines 52-60 with hex for any string literals
for i in range(51, min(61, len(lines))):
    line = lines[i]
    print(f'--- L{i+1} ---')
    print(f'HEX: {line[:60].hex()}')
    try:
        txt = line.decode('utf-8')
    except Exception as e:
        txt = f'UTF8-ERR: {e}'
    print(f'TXT: {txt[:80]}')
    print()
