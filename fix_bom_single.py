import sys
file_path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\common\BaseController.java'

with open(file_path, 'rb') as f:
    content = f.read()

# Remove UTF-8 BOM
bom = bytes([0xef, 0xbb, 0xbf])
if content.startswith(bom):
    content = content[3:]
    print('BOM removed')
else:
    print('No BOM found, checking content...')

with open(file_path, 'wb') as f:
    f.write(content)

with open(file_path, 'rb') as f:
    first3 = f.read(3)
    print('First 3 bytes after fix:', first3.hex())
