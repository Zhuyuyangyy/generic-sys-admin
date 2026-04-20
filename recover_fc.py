#!/usr/bin/env python3
import re, os, sys

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'

with open(path, 'r', encoding='utf-8', errors='replace') as f:
    content = f.read()

lines = content.split('\n')
op_lines = [(i+1, line) for i, line in enumerate(lines) if '@Operation' in line or 'summary' in line or 'description' in line]

# Write to file instead of printing to avoid encoding issues
with open(r'D:\ZYY Project\generic-sys-admin\fc_ops.txt', 'w', encoding='utf-8') as out:
    out.write(f'Found {len(op_lines)} @Operation related lines:\n')
    for i, (num, line) in enumerate(op_lines):
        # Check for mojibake: characters in Latin Extended-A range that aren't real Chinese
        # Real Chinese chars are in CJK ranges: U+4E00-U+9FFF, U+3400-U+4DBF
        has_bad = any(0x80 <= ord(c) <= 0x9FF or 0x2000 <= ord(c) <= 0x2BFF 
                     for c in line if c not in ' \t\n\r.,;:\'\"()[]{}@Operation/')
        tag = '[MOJIBAKE]' if has_bad else '[OK]'
        out.write(f'  L{num}: {tag} {line[:100]}\n')

    out.write('\nCorruption check: non-ASCII in @Operation lines:\n')
    for i, (num, line) in enumerate(op_lines):
        non_ascii = [(c, ord(c)) for c in line if ord(c) > 127 and c not in ' \t\n\r']
        if non_ascii:
            out.write(f'  L{num}: {[(c, f"U+{o:04X}") for c, o in non_ascii[:10]]}\n')

print('Done - see fc_ops.txt')
