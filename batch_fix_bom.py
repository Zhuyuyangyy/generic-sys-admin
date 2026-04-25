import os
import glob

# Find all Java files
java_files = glob.glob(r'D:\ZYY Project\generic-sys-admin\backend\src\**\*.java', recursive=True)

bom_files = []
for f in java_files:
    with open(f, 'rb') as file:
        content = file.read(3)
        if content.startswith(b'\xef\xbb\xbf'):
            bom_files.append(f)
            print(f'BOM found: {f}')

print(f'\nTotal files with BOM: {len(bom_files)}')

# Fix all BOM files
for f in bom_files:
    with open(f, 'rb') as file:
        content = file.read()
    
    # Remove BOM
    content = content[3:]
    
    # Write back
    with open(f, 'wb') as file:
        file.write(content)
    
    print(f'Fixed: {f}')

print(f'\nAll BOM files fixed!')
