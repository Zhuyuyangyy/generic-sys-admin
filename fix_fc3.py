#!/usr/bin/env python3
"""Directly fix L291 by replacing corrupted error message."""

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\controller\FileController.java'

with open(path, 'rb') as f:
    raw = f.read()

lines_raw = raw.split(b'\r\n')
print(f'File has {len(lines_raw)} lines')

# L291 is index 290 - show the raw bytes
print(f'L291 raw hex: {lines_raw[290].hex()}')
print(f'L291 raw bytes: {lines_raw[290][:50]}')

# Find the return error line
# We know it contains: return error("
# and has corrupted Chinese inside
at_error = lines_raw[290].find(b'return error("')
if at_error >= 0:
    print(f'Found "return error(" at byte {at_error}')
    # The line ends with );
    print(f'Full line (as UTF-8 text, errors=replace): {lines_raw[290].decode("utf-8", errors="replace")}')
    
# Now fix: replace the corrupted line with correct bytes
correct_line = b'            return error("\xe6\x96\x87\xe4\xbb\xb6URL\xe5\x88\x97\xe8\xa1\xa8\xe4\xb8\xba\xe7\xa9\xba\xe6\x88\x96\xe6\xa0\xbc\xe5\xbc\x8f\xe9\x94\x99\xe8\xaf\xaf");'
print(f'\nCorrect line: {correct_line.decode("utf-8")}')

# Replace
lines_raw[290] = correct_line

# Write back
with open(path, 'wb') as f:
    f.write(b'\r\n'.join(lines_raw))

print('\nFile saved!')

# Verify
with open(path, 'rb') as f:
    verify = f.read()
vlines = verify.split(b'\r\n')
print(f'\nVerification - L291: {vlines[290].decode("utf-8")}')
print(f'L291 hex: {vlines[290].hex()}')
