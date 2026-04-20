#!/usr/bin/env python3
"""Fix AIClient.java - find and replace the corrupted ternary expression."""
import os

path = r'D:\ZYY Project\generic-sys-admin\backend\src\main\java\com\zyy\client\AIClient.java'

with open(path, 'rb') as f:
    raw = f.read()

lines = raw.split(b'\r\n')

# L120 = index 119
line = lines[119]
print(f'L120 length: {len(line)}')
print(f'L120 HEX: {line.hex()}')

# The clean part ends at byte 49 (0-indexed)
# We have: StrUtil.isBlank(token) ? "  <-- ends at byte 49 (the quote)
# Then 80 bytes of corruption, then ? : "CORRUPT" ); 
# Actually looking at the hex more carefully:
# The corruption ends with: 3f 3a 20 22 ... 3f 3b
# That's: ? : " ... ?);
# Which is: [cond1] ? [value1] : [value2] ?);
# This is WRONG Java syntax (two ternaries without proper grouping)

# The original should have been a simple ternary
# Let's just replace everything from the quote after "?" with valid code

# Find the first quote after "StrUtil.isBlank(token) ? "
# This is at position 48 (the quote)
quote_pos = line.find(b'StrUtil.isBlank(token) ? "')
if quote_pos >= 0:
    print(f'Found ternary quote at position {quote_pos}')
    # Find the closing quote for this string
    # The string content starts at quote_pos+1
    # We need to find where this string ends
    
    # Look for '";' or '":' which would end the string
    search_from = quote_pos + 1
    # Find the pattern '?"' which ends a string followed by ternary operator
    end_pattern = b'?"'
    end_pos = line.find(end_pattern, search_from)
    if end_pos >= 0:
        print(f'String ends at position {end_pos} with "?')
        # Find what comes after
        after_end = line[end_pos+2:]
        print(f'After "? : ": {after_end[:50].hex()}')
        print(f'After decoded (errors=replace): {after_end.decode("utf-8", errors="replace")[:50]}')
        
        # The correct line should be:
        # url, body, StrUtil.isBlank(token) ? "请输入token" : ""
        # Replace the entire corrupted string part with a simple replacement
        
        # Let's replace from the quote (pos 49) to the end of the line
        # with a simple valid ternary
        clean_prefix = line[:quote_pos+1]  # includes the opening quote
        correct_replacement = b'\xe8\xaf\xb7\xe8\xbe\x93\xe5\x85\xa5token"'  # "请输入token"
        # Actually let's use ASCII only to avoid any issues
        correct_replacement = b'[token required]'
        # Then close the ternary
        correct_suffix = b' : ""' + after_end[after_end.find(b'?'):] if b'?' in after_end else b' : "";'
        
        # Actually simpler: replace the whole thing with a valid line
        # StrUtil.isBlank(token) ? "" : "" ;
        new_line = line[:quote_pos+1] + b'[token required] : "" );'
        print(f'\nNew line: {new_line}')
        
        # Check if it's valid ASCII
        print(f'Is ASCII: {all(b < 128 for b in new_line)}')
        
        lines[119] = new_line
        
        with open(path, 'wb') as f:
            f.write(b'\r\n'.join(lines))
        
        print('\nFile saved!')
    else:
        print('Could not find end pattern "?')
else:
    print('Could not find ternary quote')
