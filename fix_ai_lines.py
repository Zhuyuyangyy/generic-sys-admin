#!/usr/bin/env python3
"""Fix corrupted lines in AIClient.java by replacing corrupted sections with ASCII."""
import os

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\client\AIClient.java'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')  # CRLF
print(f'Lines: {len(lines)}')

# The compilation errors are at L120 and L130 (1-indexed)
# Line 120: corrupted string with Chinese chars
# Line 130: missing semicolon (also corrupted string)

# Let's look at the raw bytes of line 120 and 130
print(f'\nL120 ({len(lines[119])} bytes):')
print(f'HEX: {lines[119].hex()}')

print(f'\nL130 ({len(lines[129])} bytes):')  
print(f'HEX: {lines[129].hex()}')

# Strategy: Find the Java string boundaries in line 120 and 130
# Replace the corrupted string content with ASCII equivalents

# For line 120 (0-indexed: 119):
# The line starts with ASCII: "                .header("Content-Type", "application/json")"
# Then there should be more headers
# The corrupted part is in a string literal
# Let's find where the corruption starts (first non-ASCII after the method call)

line120 = lines[119]
# Find position of first non-ASCII
first_bad_120 = -1
for j, b in enumerate(line120):
    if b > 127:
        first_bad_120 = j
        break

print(f'\nL120 first bad byte at: {first_bad_120}')
if first_bad_120 >= 0:
    print(f'Context around pos {first_bad_120}: {line120[max(0,first_bad_120-10):first_bad_120+20].hex()}')
    print(f'Text (clean part): {line120[:first_bad_120]}')

# For line 130 (0-indexed: 129):
line130 = lines[129]
first_bad_130 = -1
for j, b in enumerate(line130):
    if b > 127:
        first_bad_130 = j
        break

print(f'\nL130 first bad byte at: {first_bad_130}')
if first_bad_130 >= 0:
    print(f'Context: {line130[max(0,first_bad_130-10):first_bad_130+20].hex()}')
    print(f'Text (clean part): {line130[:first_bad_130]}')
