#!/usr/bin/env python3
"""
Restore ALL Java files from git, then properly convert from GBK to UTF-8 (no BOM).
Handles the specific case where git stores files as GBK (no BOM) on Chinese Windows.
"""
import os, subprocess, glob, re

PROJECT = r'D:\ZYY Project\generic-sys-admin'
BACKEND_SRC = os.path.join(PROJECT, 'backend', 'src', 'main', 'java')
BACKEND_TEST = os.path.join(PROJECT, 'backend', 'src', 'test', 'java')

def run(cmd):
    result = subprocess.run(cmd, shell=True, cwd=PROJECT, 
                          capture_output=True, text=True,
                          encoding='utf-8', errors='replace')
    return result.stdout + result.stderr

def convert_file(path):
    """Detect encoding and convert to clean UTF-8 without BOM."""
    with open(path, 'rb') as f:
        raw = f.read()
    
    # Remove BOM if present
    if raw.startswith(b'\xef\xbb\xbf'):
        raw = raw[3:]
    
    # Try UTF-8 first
    try:
        text = raw.decode('utf-8')
        # Check for mojibake indicators
        bad_chars = {'姣', '曡', '偁', '鍙', '戝', '銆', '浼', '灞', '鏍'}
        has_mojibake = any(c in text for c in bad_chars)
        if has_mojibake:
            # It's mojibake - need GBK
            try:
                text = raw.decode('gbk')
            except:
                return False, 'mojibake but cant decode gbk'
        # Already valid UTF-8
        with open(path, 'w', encoding='utf-8', newline='') as f:
            f.write(text)
        return True, 'UTF-8'
    except:
        pass
    
    # Try GBK
    try:
        text = raw.decode('gbk')
    except:
        return False, 'cant decode'
    
    # Fix any unicode escapes that came from double-encoding
    # Pattern: \u followed by hex digits
    def fix_u_escape(m):
        try:
            # This might be a \uXXXX that was double-encoded
            return m.group(0)
        except:
            return '?'
    
    # Write as UTF-8 without BOM
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    return True, 'GBK->UTF8'

def fix_local_file_storage(path):
    """Fix LocalFileStorageStrategy.java - replace problematic unicode escapes."""
    with open(path, 'r', encoding='utf-8') as f:
        text = f.read()
    
    # Fix the \u unicode escape in the path comment
    # The problematic line was: // 存储在 D:\data\uploads 或 Linux /data/uploads
    # It contained \u which java interprets as unicode escape
    text = text.replace('admin\\uploads', 'uploads')
    
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    print(f'  Fixed LocalFileStorageStrategy.java unicode escape')

# Restore ALL java files from git
print('=== Step 1: Restoring all files from git ===')
java_files = glob.glob(os.path.join(BACKEND_SRC, '**', '*.java'), recursive=True)
java_files += glob.glob(os.path.join(BACKEND_TEST, '**', '*.java'), recursive=True)
print(f'Total Java files: {len(java_files)}')

for f in java_files:
    rel = os.path.relpath(f, PROJECT).replace('\\', '/')
    run(f'git checkout HEAD -- "{rel}"')

# Convert each file
print('\n=== Step 2: Converting to UTF-8 ===')
converted = 0
utf8_ok = 0
failed = []

for f in sorted(java_files):
    fname = os.path.basename(f)
    ok, result = convert_file(f)
    if ok:
        if result == 'GBK->UTF8':
            converted += 1
            print(f'  [GBK->UTF8] {fname}')
        else:
            utf8_ok += 1
    else:
        failed.append((fname, result))
        print(f'  [FAILED] {fname}: {result}')

print(f'\nConverted: {converted}, Already UTF-8: {utf8_ok}, Failed: {len(failed)}')

# Fix LocalFileStorageStrategy specifically
LOCAL = os.path.join(BACKEND_SRC, 'com', 'zyy', 'config', 'LocalFileStorageStrategy.java')
if os.path.exists(LOCAL):
    print('\n=== Step 3: Fixing LocalFileStorageStrategy ===')
    fix_local_file_storage(LOCAL)

# Report
print('\n=== Summary ===')
print(f'Files processed: {len(java_files)}')
print(f'Converted from GBK: {converted}')
print(f'Already were UTF-8: {utf8_ok}')
if failed:
    print(f'Failed: {failed}')
else:
    print('All OK!')
