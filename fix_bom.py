#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix BOM (Byte Order Mark) for all Java files in the project
"""
import os

def fix_java_files(root_dir):
    """Find and fix all Java files with BOM"""
    fixed_count = 0
    total_count = 0
    
    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                total_count += 1
                
                with open(filepath, 'rb') as f:
                    content = f.read()
                
                # Check for UTF-8 BOM
                if content.startswith(b'\xef\xbb\xbf'):
                    # Remove BOM
                    content = content[3:]
                    with open(filepath, 'wb') as f:
                        f.write(content)
                    print(f'Fixed: {filepath}')
                    fixed_count += 1
    
    return total_count, fixed_count

if __name__ == '__main__':
    import sys
    
    root_dir = sys.argv[1] if len(sys.argv) > 1 else 'backend/src/main/java'
    
    print(f'Fixing BOM in: {root_dir}')
    total, fixed = fix_java_files(root_dir)
    print(f'\nTotal files scanned: {total}')
    print(f'Files fixed (BOM removed): {fixed}')
