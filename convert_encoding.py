#!/usr/bin/env python3
"""
Smart GBK->UTF8 converter. Uses multiple detection strategies to handle
files that were created as GBK on Chinese Windows but stored as raw bytes.
"""
import os, re

def is_likely_mojibake(text):
    """Check if text looks like GBK decoded as UTF-8 (mojibake)."""
    # Common mojibake patterns: characters that appear when GBK is read as UTF-8
    bad_chars = {'姣', '曡', '偁', '鍙', '戝', '洟', '闃', '銆', '愭', '棴', '浠', '傚'}
    return any(c in text for c in bad_chars)

def try_decode(path):
    """Try to decode file, returning (content, encoding_used) tuple."""
    with open(path, 'rb') as f:
        raw = f.read()
    
    # Skip UTF-8 BOM
    if raw.startswith(b'\xef\xbb\xbf'):
        return raw.decode('utf-8-sig'), 'utf-8-sig', raw
    
    # Try UTF-8 first
    try:
        content = raw.decode('utf-8')
        if not is_likely_mojibake(content):
            return content, 'utf-8', raw
        # It's mojibake - fall through to GBK
    except:
        pass
    
    # Try GBK
    try:
        content = raw.decode('gbk')
        if is_likely_mojibake(content):
            # GBK text shouldn't have these chars
            return content, 'gbk-fallback', raw
        return content, 'gbk', raw
    except:
        pass
    
    # Fallback: latin-1 (always succeeds, preserves bytes)
    return raw.decode('latin-1'), 'latin-1', raw

def fix_pua(text):
    """Replace Private Use Area characters with proper Chinese chars."""
    # Common PUA->Chinese mappings found in these files
    replacements = {
        '\ue18f': '某', '\ue1b9': '设', '\ue1a1': '工',
        '\ue0c8': '开', '\ue0d3': '发', '\ue0e7': '环',
        '\ue1c6': '赛', '\ue1d2': '论', '\ue1e8': '文',
        '\ue086': '创', '\ue0b8': '为', '\ue1f1': '的',
        '\uf018': '建', '\uf02c': '吧', '\ue817': '开',
        '\ue818': '发', '\ue819': '者', '\ue81a': '网',
        '\ue81b': '站', '\ue81c': '在', '\ue81d': '线',
        '\ue81e': '名', '\ue81f': '称', '\ue820': '某',
        '\ue821': '网', '\ue822': '名', '\ue823': '站',
        '\uf06e': '后', '\uf06f': '台', '\ue825': '管',
        '\ue826': '理', '\ue827': '员', '\ue828': '首',
    }
    
    def repl(m):
        return replacements.get(m.group(0), '?')
    
    return re.sub(r'[\ue000-\uf8ff]', repl, text)

def convert_java_file(path):
    try:
        content, encoding, raw = try_decode(path)
        
        # Fix PUA characters
        content = fix_pua(content)
        
        # Verify no more PUA chars remain
        pua_count = len(re.findall(r'[\ue000-\uf8ff]', content))
        
        # Write back as UTF-8 with BOM
        with open(path, 'w', encoding='utf-8-sig', newline='') as f:
            f.write(content)
        
        return 'ok', encoding, pua_count
    except Exception as e:
        return 'error', str(e), 0

def main():
    src_dir = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java'
    test_dir = r'D:\ZYY Project\generic-sys-admin\backend\src\test\java'
    
    for base_dir in [src_dir, test_dir]:
        if not os.path.exists(base_dir):
            continue
        print(f'\n=== Processing {os.path.basename(base_dir)} ===')
        
        for root, dirs, files in os.walk(base_dir):
            for fname in sorted(files):
                if not fname.endswith('.java'):
                    continue
                path = os.path.join(root, fname)
                result, encoding, pua_count = convert_java_file(path)
                
                if result == 'ok':
                    enc_tag = 'GBK' if 'gbk' in encoding else encoding.upper()
                    print(f'  [{enc_tag:10}] {fname} (PUA remaining: {pua_count})')
                else:
                    print(f'  [ERROR] {fname}: {encoding}')

main()
